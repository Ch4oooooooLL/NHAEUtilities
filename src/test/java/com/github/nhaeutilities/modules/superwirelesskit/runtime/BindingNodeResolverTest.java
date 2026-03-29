package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.EnumSet;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingBlockRef;

import appeng.api.networking.GridFlags;
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

public class BindingNodeResolverTest {

    @Test
    public void getNodeBlockRefUsesMetaTileBaseTileEntityWhenMachineIsNotATileEntity() throws Exception {
        World world = allocateWorld(7);
        TileEntity baseTile = new TileEntity();
        baseTile.xCoord = 11;
        baseTile.yCoord = 22;
        baseTile.zCoord = 33;
        baseTile.setWorldObj(world);

        GridNode node = new GridNode(new TestGridBlock(world, new MetaMachine(baseTile)));

        BindingBlockRef blockRef = new BindingNodeResolver().getNodeBlockRef(node);

        assertNotNull(blockRef);
        assertEquals(7, blockRef.getDimensionId());
        assertEquals(11, blockRef.getX());
        assertEquals(22, blockRef.getY());
        assertEquals(33, blockRef.getZ());
    }

    private static World allocateWorld(int dimensionId) throws Exception {
        Unsafe unsafe = getUnsafe();
        World world = (World) unsafe.allocateInstance(WorldServer.class);
        WorldProvider provider = (WorldProvider) unsafe.allocateInstance(WorldProviderSurface.class);
        provider.dimensionId = dimensionId;

        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, provider);
        return world;
    }

    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static final class MetaMachine implements IGridHost {

        private final TileEntity baseTile;

        private MetaMachine(TileEntity baseTile) {
            this.baseTile = baseTile;
        }

        public TileEntity getBaseMetaTileEntity() {
            return baseTile;
        }

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

    private static final class TestGridBlock implements IGridBlock {

        private final DimensionalCoord location;
        private final Object machine;

        private TestGridBlock(World world, Object machine) {
            this.location = new DimensionalCoord(world, 0, 0, 0);
            this.machine = machine;
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
            return (IGridHost) machine;
        }

        @Override
        public void gridChanged() {}

        @Override
        public ItemStack getMachineRepresentation() {
            return null;
        }
    }

}
