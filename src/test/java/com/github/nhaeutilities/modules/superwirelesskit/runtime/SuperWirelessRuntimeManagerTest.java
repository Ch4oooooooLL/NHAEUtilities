package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingBlockRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.SuperWirelessSavedData;
import com.github.nhaeutilities.modules.superwirelesskit.service.BindingRegistry;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.GridNotification;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridNode;
import sun.misc.Unsafe;

public class SuperWirelessRuntimeManagerTest {

    @Test
    public void onNodeDestroyedRemovesBindingsForDestroyedControllerBlock() throws Exception {
        World world = allocateWorld(0);
        BindingRecord record = createRecord();
        BindingRegistry registry = new BindingRegistry(new SuperWirelessSavedData("test"));
        assertTrue(registry.add(record));

        BindingDataStore dataStore = new BindingDataStore();
        registryMap(dataStore).put(world, registry);

        BindingBlockRef controllerBlock = BindingBlockRef.of(record.getController());
        SuperWirelessRuntimeManager manager = new SuperWirelessRuntimeManager(
            dataStore,
            new TestBindingNodeResolver(controllerBlock));

        GridNode controllerNode = new GridNode(new TestGridBlock(world, controllerBlock));
        GridNode targetNode = new GridNode(new TestGridBlock(world, BindingBlockRef.of(record.getTarget())));
        activeConnectionMap(manager).put(record.getBindingId(), new TestGridConnection(controllerNode, targetNode));

        manager.onNodeDestroyed(controllerNode);

        assertNull(registry.findByTarget(record.getTarget()));
    }

    private static BindingRecord createRecord() {
        return new BindingRecord(
            UUID.nameUUIDFromBytes("destroyed-controller".getBytes(StandardCharsets.UTF_8)),
            new ControllerEndpointRef(0, 1, 2, 3, ForgeDirection.NORTH, "controller"),
            new BindingTargetRef(
                BindingTargetKind.TILE,
                0,
                10,
                20,
                30,
                ForgeDirection.UNKNOWN,
                new BindingFingerprint("mod:block", "tile.Class")),
            7,
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            1L);
    }

    @SuppressWarnings("unchecked")
    private static Map<World, BindingRegistry> registryMap(BindingDataStore dataStore) throws Exception {
        Field field = BindingDataStore.class.getDeclaredField("registries");
        field.setAccessible(true);
        return (Map<World, BindingRegistry>) field.get(dataStore);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, IGridConnection> activeConnectionMap(SuperWirelessRuntimeManager manager) throws Exception {
        Field field = SuperWirelessRuntimeManager.class.getDeclaredField("activeConnections");
        field.setAccessible(true);
        return (Map<UUID, IGridConnection>) field.get(manager);
    }

    private static World allocateWorld(int dimensionId) throws Exception {
        Unsafe unsafe = getUnsafe();
        World world = (World) unsafe.allocateInstance(WorldServer.class);
        WorldProvider provider = (WorldProvider) unsafe.allocateInstance(WorldProviderSurface.class);
        provider.dimensionId = dimensionId;

        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, provider);

        Field remoteField = World.class.getDeclaredField("isRemote");
        remoteField.setAccessible(true);
        remoteField.setBoolean(world, false);
        return world;
    }

    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static final class TestBindingNodeResolver extends BindingNodeResolver {

        private final BindingBlockRef blockRef;

        private TestBindingNodeResolver(BindingBlockRef blockRef) {
            this.blockRef = blockRef;
        }

        @Override
        public BindingBlockRef getNodeBlockRef(GridNode node) {
            return blockRef;
        }
    }

    private static final class TestGridBlock implements IGridBlock {

        private final DimensionalCoord location;
        private final IGridHost host = new TestGridHost();

        private TestGridBlock(World world, BindingBlockRef blockRef) {
            this.location = new DimensionalCoord(world, blockRef.getX(), blockRef.getY(), blockRef.getZ());
        }

        @Override
        public double getIdlePowerUsage() {
            return 0;
        }

        @Override
        public EnumSet<GridFlags> getFlags() {
            return EnumSet.noneOf(GridFlags.class);
        }

        @Override
        public boolean isWorldAccessible() {
            return false;
        }

        @Override
        public DimensionalCoord getLocation() {
            return location;
        }

        @Override
        public AEColor getGridColor() {
            return AEColor.Transparent;
        }

        @Override
        public void onGridNotification(GridNotification notification) {}

        @Override
        public void setNetworkStatus(IGrid grid, int channelsInUse) {}

        @Override
        public EnumSet<ForgeDirection> getConnectableSides() {
            return EnumSet.noneOf(ForgeDirection.class);
        }

        @Override
        public IGridHost getMachine() {
            return host;
        }

        @Override
        public void gridChanged() {}

        @Override
        public ItemStack getMachineRepresentation() {
            return null;
        }
    }

    private static final class TestGridHost implements IGridHost {

        @Override
        public IGridNode getGridNode(ForgeDirection dir) {
            return null;
        }

        @Override
        public AECableType getCableConnectionType(ForgeDirection dir) {
            return AECableType.NONE;
        }

        @Override
        public void securityBreak() {}
    }

    private static final class TestGridConnection implements IGridConnection {

        private final GridNode a;
        private final GridNode b;

        private TestGridConnection(GridNode a, GridNode b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void setAdHocChannels(int channels) {}

        @Override
        public IGridNode getOtherSide(IGridNode gridNode) {
            return gridNode == a ? b : a;
        }

        @Override
        public ForgeDirection getDirection(IGridNode gridNode) {
            return ForgeDirection.UNKNOWN;
        }

        @Override
        public void destroy() {}

        @Override
        public IGridNode a() {
            return a;
        }

        @Override
        public IGridNode b() {
            return b;
        }

        @Override
        public boolean hasDirection() {
            return false;
        }

        @Override
        public int getUsedChannels() {
            return 0;
        }
    }
}
