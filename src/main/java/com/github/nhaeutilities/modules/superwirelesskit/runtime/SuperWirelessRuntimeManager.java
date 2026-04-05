package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final GridConnectionFactory DEFAULT_CONNECTION_FACTORY = new DefaultGridConnectionFactory();

    private final BindingDataStore dataStore;
    private final BindingNodeResolver resolver;
    private final GridConnectionFactory connectionFactory;
    private final Map<UUID, IGridConnection> activeConnections = new HashMap<UUID, IGridConnection>();
    private final Map<World, Set<UUID>> deferredBindingsByWorld = new IdentityHashMap<World, Set<UUID>>();

    public interface GridConnectionFactory {

        IGridConnection connect(BindingRecord record, GridNode controllerNode, GridNode targetNode) throws FailedConnection;
    }

    public SuperWirelessRuntimeManager(BindingDataStore dataStore, BindingNodeResolver resolver) {
        this(dataStore, resolver, DEFAULT_CONNECTION_FACTORY);
    }

    public SuperWirelessRuntimeManager(BindingDataStore dataStore, BindingNodeResolver resolver,
        GridConnectionFactory connectionFactory) {
        this.dataStore = dataStore;
        this.resolver = resolver;
        this.connectionFactory = connectionFactory;
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
        SuperWirelessDebugLog.log(
            "REFRESH_CHUNK",
            "chunk=%d:%d,%d bindings=%d",
            Integer.valueOf(chunkRef.getDimensionId()),
            Integer.valueOf(chunkRef.getChunkX()),
            Integer.valueOf(chunkRef.getChunkZ()),
            Integer.valueOf(records.size()));
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
        SuperWirelessDebugLog.log(
            "REFRESH_NODE",
            "block=%s node=%d bindings=%d",
            formatBlock(blockRef),
            Integer.valueOf(System.identityHashCode(node)),
            Integer.valueOf(records.size()));
        for (BindingRecord record : records) {
            reconcile(node.getWorld(), record);
        }
    }

    public void onNodeDestroyed(GridNode node) {
        if (node == null) {
            return;
        }

        int removedCount = 0;
        for (Map.Entry<UUID, IGridConnection> entry : new HashMap<UUID, IGridConnection>(activeConnections)
            .entrySet()) {
            IGridConnection connection = entry.getValue();
            if (connection == null) {
                activeConnections.remove(entry.getKey());
                continue;
            }

            if (connection.a() == node || connection.b() == node) {
                destroyActive(entry.getKey());
                removedCount++;
            }
        }
        SuperWirelessDebugLog.log(
            "NODE_DESTROYED",
            "node=%d removedRuntimeConnections=%d",
            Integer.valueOf(System.identityHashCode(node)),
            Integer.valueOf(removedCount));
    }

    public void refreshDeferredBindings(World world) {
        if (world == null || world.isRemote) {
            return;
        }

        Set<UUID> deferredBindings = deferredBindingsByWorld.get(world);
        if (deferredBindings == null || deferredBindings.isEmpty()) {
            return;
        }

        BindingRegistry registry = getRegistry(world);
        List<UUID> deferredSnapshot = new ArrayList<UUID>(deferredBindings);
        SuperWirelessDebugLog.log(
            "REFRESH_DEFERRED",
            "dimension=%d pending=%d",
            Integer.valueOf(world.provider != null ? world.provider.dimensionId : 0),
            Integer.valueOf(deferredSnapshot.size()));
        for (UUID bindingId : deferredSnapshot) {
            BindingRecord record = registry.findById(bindingId);
            if (record == null) {
                clearDeferred(world, bindingId);
                continue;
            }
            reconcile(world, record);
        }
    }

    public void onBlockBroken(World world, int x, int y, int z) {
        if (world == null || world.isRemote || world.provider == null) {
            return;
        }

        BindingRegistry registry = getRegistry(world);
        List<BindingRecord> touchedBindings = registry
            .getBindingsTouchingBlock(new BindingBlockRef(world.provider.dimensionId, x, y, z));
        SuperWirelessDebugLog.log(
            "BLOCK_BROKEN",
            "block=%d:%d,%d,%d touchedBindings=%d",
            Integer.valueOf(world.provider.dimensionId),
            Integer.valueOf(x),
            Integer.valueOf(y),
            Integer.valueOf(z),
            Integer.valueOf(touchedBindings.size()));
        for (BindingRecord record : touchedBindings) {
            SuperWirelessDebugLog.log(
                "BLOCK_BROKEN_REMOVE_BINDING",
                "bindingId=%s controller=%s target=%s",
                record.getBindingId(),
                formatController(record),
                formatTarget(record));
            destroyActive(record.getBindingId());
            clearDeferred(world, record.getBindingId());
            registry.remove(record.getBindingId());
        }
    }

    public void onWorldUnload(World world) {
        if (world == null) {
            return;
        }

        int removedCount = 0;
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
                removedCount++;
            }
        }

        SuperWirelessDebugLog.log(
            "WORLD_UNLOAD",
            "dimension=%d removedRuntimeConnections=%d",
            Integer.valueOf(world.provider != null ? world.provider.dimensionId : 0),
            Integer.valueOf(removedCount));
        deferredBindingsByWorld.remove(world);
        dataStore.clearWorld(world);
    }

    public BindingReconcileResult reconcile(World world, BindingRecord record) {
        BindingRegistry registry = getRegistry(world);
        SuperWirelessDebugLog.log(
            "RECONCILE_START",
            "bindingId=%s controller=%s target=%s",
            record.getBindingId(),
            formatController(record),
            formatTarget(record));

        ResolvedControllerEndpoint controller = resolver.resolveController(world, record.getController());
        if (controller == null) {
            return trackDeferredState(world, record.getBindingId(), invalidateOrDefer(world, registry, record, true));
        }

        ResolvedBindingTarget target = resolver.resolveTarget(world, record.getTarget());
        if (target == null) {
            return trackDeferredState(world, record.getBindingId(), invalidateOrDefer(world, registry, record, false));
        }

        IGridConnection existing = activeConnections.get(record.getBindingId());
        if (existing != null && existing.a() == controller.getNode() && existing.b() == target.getNode()) {
            SuperWirelessDebugLog.log(
                "RECONCILE_ALREADY_CONNECTED",
                "bindingId=%s controllerNode=%d targetNode=%d",
                record.getBindingId(),
                Integer.valueOf(System.identityHashCode(controller.getNode())),
                Integer.valueOf(System.identityHashCode(target.getNode())));
            return trackDeferredState(world, record.getBindingId(), BindingReconcileResult.ALREADY_CONNECTED);
        }

        destroyActive(record.getBindingId());

        try {
            IGridConnection connection = connect(record, controller.getNode(), target.getNode());
            activeConnections.put(record.getBindingId(), connection);
            SuperWirelessDebugLog.log(
                "RECONCILE_CONNECTED",
                "bindingId=%s controllerNode=%d targetNode=%d",
                record.getBindingId(),
                Integer.valueOf(System.identityHashCode(controller.getNode())),
                Integer.valueOf(System.identityHashCode(target.getNode())));
            return trackDeferredState(world, record.getBindingId(), BindingReconcileResult.CONNECTED);
        } catch (FailedConnection e) {
            SuperWirelessDebugLog.log(
                "RECONCILE_CONNECTION_FAILED",
                "bindingId=%s controller=%s target=%s reason=%s:%s",
                record.getBindingId(),
                formatController(record),
                formatTarget(record),
                e.getClass()
                    .getSimpleName(),
                String.valueOf(e.getMessage()));
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
            return trackDeferredState(world, record.getBindingId(), BindingReconcileResult.CONNECTION_FAILED);
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
            SuperWirelessDebugLog.log(
                "RECONCILE_DEFERRED_CHUNK_UNLOADED",
                "bindingId=%s side=%s chunk=%d:%d,%d",
                record.getBindingId(),
                controllerSide ? "controller" : "target",
                Integer.valueOf(world.provider.dimensionId),
                Integer.valueOf(x >> 4),
                Integer.valueOf(z >> 4));
            return controllerSide ? BindingReconcileResult.CONTROLLER_UNLOADED : BindingReconcileResult.TARGET_UNLOADED;
        }

        boolean hostStillPresent = controllerSide ? resolver.hasControllerHost(world, record.getController())
            : resolver.hasCompatibleTargetHost(world, record.getTarget());
        if (hostStillPresent) {
            SuperWirelessDebugLog.log(
                "RECONCILE_DEFERRED_HOST_PRESENT",
                "bindingId=%s side=%s",
                record.getBindingId(),
                controllerSide ? "controller" : "target");
            return controllerSide ? BindingReconcileResult.CONTROLLER_UNLOADED : BindingReconcileResult.TARGET_UNLOADED;
        }

        registry.remove(record.getBindingId());
        SuperWirelessDebugLog.log(
            "RECONCILE_INVALID_BINDING_REMOVED",
            "bindingId=%s side=%s",
            record.getBindingId(),
            controllerSide ? "controller" : "target");
        return BindingReconcileResult.INVALID_BINDING_REMOVED;
    }

    private IGridConnection connect(BindingRecord record, GridNode controllerNode, GridNode targetNode)
        throws FailedConnection {
        return connectionFactory.connect(record, controllerNode, targetNode);
    }

    private void destroyActive(UUID bindingId) {
        IGridConnection existing = activeConnections.remove(bindingId);
        if (existing != null) {
            SuperWirelessDebugLog.log("RUNTIME_DESTROY_ACTIVE", "bindingId=%s", bindingId);
            try {
                existing.destroy();
            } catch (RuntimeException ignored) {}
        }
    }

    private BindingReconcileResult trackDeferredState(World world, UUID bindingId, BindingReconcileResult result) {
        if (result == BindingReconcileResult.CONTROLLER_UNLOADED || result == BindingReconcileResult.TARGET_UNLOADED) {
            markDeferred(world, bindingId);
        } else {
            clearDeferred(world, bindingId);
        }
        return result;
    }

    private void markDeferred(World world, UUID bindingId) {
        if (world == null || bindingId == null) {
            return;
        }

        Set<UUID> deferredBindings = deferredBindingsByWorld.get(world);
        if (deferredBindings == null) {
            deferredBindings = new LinkedHashSet<UUID>();
            deferredBindingsByWorld.put(world, deferredBindings);
        }
        deferredBindings.add(bindingId);
    }

    private void clearDeferred(World world, UUID bindingId) {
        if (world == null || bindingId == null) {
            return;
        }

        Set<UUID> deferredBindings = deferredBindingsByWorld.get(world);
        if (deferredBindings == null) {
            return;
        }

        deferredBindings.remove(bindingId);
        if (deferredBindings.isEmpty()) {
            deferredBindingsByWorld.remove(world);
        }
    }

    private static boolean isChunkLoaded(World world, int x, int z) {
        return world != null && world.getChunkProvider() != null
            && world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4);
    }

    private static String formatController(BindingRecord record) {
        return record.getController()
            .getDimensionId() + ":"
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
            .name() + "@"
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

    private static String formatBlock(BindingBlockRef blockRef) {
        return blockRef.getDimensionId() + ":" + blockRef.getX() + "," + blockRef.getY() + "," + blockRef.getZ();
    }

    private static final class DefaultGridConnectionFactory implements GridConnectionFactory {

        @Override
        public IGridConnection connect(BindingRecord record, GridNode controllerNode, GridNode targetNode)
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
    }
}
