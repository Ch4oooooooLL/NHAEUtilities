package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingBlockRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingChunkRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.service.BindingRegistry;

import appeng.api.exceptions.FailedConnection;
import appeng.api.networking.IGridConnection;
import appeng.me.GridConnection;
import appeng.me.GridNode;

public class SuperWirelessRuntimeManager {

    private static final Logger LOGGER = LogManager.getLogger("NHAEUtilities-SuperWirelessKit");

    private final BindingDataStore dataStore;
    private final BindingNodeResolver resolver;
    private final Map<UUID, IGridConnection> activeConnections = new HashMap<UUID, IGridConnection>();

    public SuperWirelessRuntimeManager(BindingDataStore dataStore, BindingNodeResolver resolver) {
        this.dataStore = dataStore;
        this.resolver = resolver;
    }

    public BindingRegistry getRegistry(World world) {
        return dataStore.getRegistry(world);
    }

    public void refreshChunk(Chunk chunk) {
        if (chunk == null || chunk.worldObj == null || chunk.worldObj.isRemote) {
            return;
        }

        World world = chunk.worldObj;
        BindingChunkRef chunkRef = new BindingChunkRef(world.provider.dimensionId, chunk.xPosition, chunk.zPosition);
        List<BindingRecord> records = getRegistry(world).getBindingsTouchingChunk(chunkRef);
        for (BindingRecord record : records) {
            reconcile(world, record);
        }
    }

    public void refreshNode(GridNode node) {
        BindingBlockRef blockRef = resolver.getNodeBlockRef(node);
        if (blockRef == null || node.getWorld() == null || node.getWorld().isRemote) {
            return;
        }

        List<BindingRecord> records = getRegistry(node.getWorld()).getBindingsTouchingBlock(blockRef);
        for (BindingRecord record : records) {
            reconcile(node.getWorld(), record);
        }
    }

    public void onNodeDestroyed(GridNode node) {
        if (node == null) {
            return;
        }

        for (Map.Entry<UUID, IGridConnection> entry : new HashMap<UUID, IGridConnection>(activeConnections)
            .entrySet()) {
            IGridConnection connection = entry.getValue();
            if (connection == null) {
                activeConnections.remove(entry.getKey());
                continue;
            }

            if (connection.a() == node || connection.b() == node) {
                activeConnections.remove(entry.getKey());
            }
        }
    }

    public void onBlockBroken(World world, int x, int y, int z) {
        if (world == null || world.isRemote || world.provider == null) {
            return;
        }

        BindingRegistry registry = getRegistry(world);
        List<BindingRecord> touchedBindings = registry.getBindingsTouchingBlock(
            new BindingBlockRef(world.provider.dimensionId, x, y, z));
        for (BindingRecord record : touchedBindings) {
            destroyActive(record.getBindingId());
            registry.remove(record.getBindingId());
        }
    }

    public void onWorldUnload(World world) {
        if (world == null) {
            return;
        }

        for (Map.Entry<UUID, IGridConnection> entry : new HashMap<UUID, IGridConnection>(activeConnections)
            .entrySet()) {
            IGridConnection connection = entry.getValue();
            if (connection == null) {
                activeConnections.remove(entry.getKey());
                continue;
            }

            if (connection.a()
                .getWorld() == world
                || connection.b()
                    .getWorld() == world) {
                activeConnections.remove(entry.getKey());
            }
        }

        dataStore.clearWorld(world);
    }

    public BindingReconcileResult reconcile(World world, BindingRecord record) {
        BindingRegistry registry = getRegistry(world);

        ResolvedControllerEndpoint controller = resolver.resolveController(world, record.getController());
        if (controller == null) {
            return invalidateOrDefer(world, registry, record, true);
        }

        ResolvedBindingTarget target = resolver.resolveTarget(world, record.getTarget());
        if (target == null) {
            return invalidateOrDefer(world, registry, record, false);
        }

        IGridConnection existing = activeConnections.get(record.getBindingId());
        if (existing != null && existing.a() == controller.getNode() && existing.b() == target.getNode()) {
            return BindingReconcileResult.ALREADY_CONNECTED;
        }

        destroyActive(record.getBindingId());

        try {
            IGridConnection connection = connect(record, controller.getNode(), target.getNode());
            activeConnections.put(record.getBindingId(), connection);
            return BindingReconcileResult.CONNECTED;
        } catch (FailedConnection e) {
            LOGGER.warn(
                "Failed to create SuperWirelessKit binding {} [{}: {}] controller={} target={} controllerNode={} targetNode={} targetMachine={}",
                record.getBindingId(),
                e.getClass()
                    .getSimpleName(),
                e.getMessage(),
                formatController(record),
                formatTarget(record),
                System.identityHashCode(controller.getNode()),
                System.identityHashCode(target.getNode()),
                target.getMachine()
                    .getClass()
                    .getName(),
                e);
            registry.remove(record.getBindingId());
            return BindingReconcileResult.CONNECTION_FAILED;
        }
    }

    private BindingReconcileResult invalidateOrDefer(World world, BindingRegistry registry, BindingRecord record,
        boolean controllerSide) {
        destroyActive(record.getBindingId());

        int x = controllerSide ? record.getController()
            .getX()
            : record.getTarget()
                .getX();
        int z = controllerSide ? record.getController()
            .getZ()
            : record.getTarget()
                .getZ();
        if (!isChunkLoaded(world, x, z)) {
            return controllerSide ? BindingReconcileResult.CONTROLLER_UNLOADED : BindingReconcileResult.TARGET_UNLOADED;
        }

        boolean hostStillPresent = controllerSide ? resolver.hasControllerHost(world, record.getController())
            : resolver.hasCompatibleTargetHost(world, record.getTarget());
        if (hostStillPresent) {
            return controllerSide ? BindingReconcileResult.CONTROLLER_UNLOADED : BindingReconcileResult.TARGET_UNLOADED;
        }

        registry.remove(record.getBindingId());
        return BindingReconcileResult.INVALID_BINDING_REMOVED;
    }

    private IGridConnection connect(BindingRecord record, GridNode controllerNode, GridNode targetNode)
        throws FailedConnection {
        GridNodeAccess targetAccess = (GridNodeAccess) (Object) targetNode;
        GridNodeAccess controllerAccess = (GridNodeAccess) (Object) controllerNode;

        int originalTargetPlayerId = targetAccess.nhaeutilities$getPlayerIdRaw();
        int originalControllerPlayerId = controllerAccess.nhaeutilities$getPlayerIdRaw();

        try {
            targetAccess.nhaeutilities$setPlayerIdRaw(record.getBinderPlayerId());
            controllerAccess.nhaeutilities$setPlayerIdRaw(record.getBinderPlayerId());

            return new GridConnection(
                controllerNode,
                targetNode,
                VirtualGridConnectionRules.connectionDirectionForVirtualLink());
        } finally {
            targetAccess.nhaeutilities$setPlayerIdRaw(originalTargetPlayerId);
            controllerAccess.nhaeutilities$setPlayerIdRaw(originalControllerPlayerId);
        }
    }

    private void destroyActive(UUID bindingId) {
        IGridConnection existing = activeConnections.remove(bindingId);
        if (existing != null) {
            try {
                existing.destroy();
            } catch (RuntimeException ignored) {}
        }
    }

    private static boolean isChunkLoaded(World world, int x, int z) {
        return world != null && world.getChunkProvider() != null
            && world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4);
    }

    private static String formatController(BindingRecord record) {
        return record.getController()
            .getDimensionId()
            + ":"
            + record.getController()
                .getX()
            + ","
            + record.getController()
                .getY()
            + ","
            + record.getController()
                .getZ()
            + "/"
            + record.getController()
                .getFace()
                .name();
    }

    private static String formatTarget(BindingRecord record) {
        return record.getTarget()
            .getKind()
                .name()
            + "@"
            + record.getTarget()
                .getDimensionId()
            + ":"
            + record.getTarget()
                .getX()
            + ","
            + record.getTarget()
                .getY()
            + ","
            + record.getTarget()
                .getZ()
            + "/"
            + record.getTarget()
                .getSide()
                .name();
    }
}
