package com.github.nhaeutilities.modules.superwirelesskit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.SuperWirelessSavedData;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingNodeResolver;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingReconcileResult;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.ResolvedBindingTarget;

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

public class BindingBatchProcessorTest {

    @Test
    public void sharedTargetNodesAreMergedIntoSinglePersistentBinding() {
        BindingRegistry registry = new BindingRegistry(new SuperWirelessSavedData("test"));
        ControllerEndpointRef controller = new ControllerEndpointRef(0, 1, 2, 3, ForgeDirection.NORTH, "controller");
        BindingRecord first = createRecord(controller, 10, 20, 30, ForgeDirection.UP, "target-a");
        BindingRecord alias = createRecord(controller, 10, 20, 31, ForgeDirection.DOWN, "target-b");
        List<BindingRecord> draftedBindings = new ArrayList<BindingRecord>();
        draftedBindings.add(first);
        draftedBindings.add(alias);

        GridNode sharedNode = new GridNode(new TestGridBlock(new Object(), 10, 20, 30));
        FakeResolver resolver = new FakeResolver();
        resolver.add(first.getTarget(), new ResolvedBindingTarget(first.getTarget(), new TileEntity(), new Object(), sharedNode));
        resolver.add(alias.getTarget(), new ResolvedBindingTarget(alias.getTarget(), new TileEntity(), new Object(), sharedNode));

        CountingReconciler reconciler = new CountingReconciler(BindingReconcileResult.CONNECTED);
        BindingBatchProcessor processor = new BindingBatchProcessor(resolver, reconciler);

        BindingBatchResult result = processor.bind(null, controller, draftedBindings, registry);

        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, reconciler.callCount);
        assertEquals(1, registry.values().size());
        assertTrue(registry.findByTarget(first.getTarget()) != null || registry.findByTarget(alias.getTarget()) != null);
    }

    private static BindingRecord createRecord(ControllerEndpointRef controller, int x, int y, int z, ForgeDirection side,
        String seed) {
        return new BindingRecord(
            UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)),
            controller,
            new BindingTargetRef(
                BindingTargetKind.TILE,
                controller.getDimensionId(),
                x,
                y,
                z,
                side,
                new BindingFingerprint("mod:block", "tile." + seed)),
            7,
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            1L);
    }

    private static final class FakeResolver extends BindingNodeResolver {

        private final Map<BindingTargetRef, ResolvedBindingTarget> targets = new HashMap<BindingTargetRef, ResolvedBindingTarget>();

        private void add(BindingTargetRef ref, ResolvedBindingTarget target) {
            targets.put(ref, target);
        }

        @Override
        public ResolvedBindingTarget resolveTarget(World world, BindingTargetRef targetRef) {
            return targets.get(targetRef);
        }
    }

    private static final class CountingReconciler implements BindingBatchProcessor.Reconciler {

        private final BindingReconcileResult result;
        private int callCount;

        private CountingReconciler(BindingReconcileResult result) {
            this.result = result;
        }

        @Override
        public BindingReconcileResult reconcile(World world, BindingRecord record) {
            callCount++;
            return result;
        }
    }

    private static final class TestGridBlock implements IGridBlock {

        private final Object machine;
        private final DimensionalCoord location;

        private TestGridBlock(Object machine, int x, int y, int z) {
            this.machine = machine;
            this.location = new DimensionalCoord(x, y, z, 0);
        }

        @Override
        public double getIdlePowerUsage() {
            return 0;
        }

        @Override
        public EnumSet<GridFlags> getFlags() {
            return EnumSet.of(GridFlags.REQUIRE_CHANNEL);
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
            return new IGridHost() {

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

                @Override
                public String toString() {
                    return String.valueOf(machine);
                }
            };
        }

        @Override
        public void gridChanged() {}

        @Override
        public ItemStack getMachineRepresentation() {
            return null;
        }
    }
}
