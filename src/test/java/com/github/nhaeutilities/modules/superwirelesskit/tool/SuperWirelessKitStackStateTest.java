package com.github.nhaeutilities.modules.superwirelesskit.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.item.ItemSuperWirelessKit;

public class SuperWirelessKitStackStateTest {

    @Test
    public void defaultsToQueueModeWithNoStoredState() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());

        assertEquals(SuperWirelessKitMode.QUEUE, SuperWirelessKitStackState.getMode(stack));
        assertTrue(
            SuperWirelessKitStackState.getQueuedTargets(stack)
                .isEmpty());
        assertTrue(
            SuperWirelessKitStackState.getPendingBindings(stack)
                .isEmpty());
        assertFalse(SuperWirelessKitStackState.hasController(stack));
    }

    @Test
    public void promotesQueuedTargetsIntoPendingBindings() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        ControllerEndpointRef controller = new ControllerEndpointRef(0, 1, 2, 3, ForgeDirection.NORTH, "controller");
        BindingTargetRef target = new BindingTargetRef(
            BindingTargetKind.TILE,
            0,
            10,
            20,
            30,
            ForgeDirection.UP,
            new BindingFingerprint("mod:block", "tile.Target"));

        SuperWirelessKitStackState.addQueuedTarget(stack, target);
        List<BindingRecord> promoted = SuperWirelessKitStackState.promoteQueuedTargetsToBindings(
            stack,
            controller,
            42,
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            999L);

        assertEquals(1, promoted.size());
        assertEquals(
            controller,
            promoted.get(0)
                .getController());
        assertEquals(
            target,
            promoted.get(0)
                .getTarget());
        assertTrue(
            SuperWirelessKitStackState.getQueuedTargets(stack)
                .isEmpty());
        assertEquals(
            1,
            SuperWirelessKitStackState.getPendingBindings(stack)
                .size());
        assertEquals(
            promoted.get(0),
            SuperWirelessKitStackState.getPendingBindings(stack)
                .get(0));
    }

    @Test
    public void pendingBindingsBlockNewTargetsAndCountSeparately() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        BindingTargetRef queued = createTarget(0);
        BindingRecord pending = createBindingRecord(1, ForgeDirection.SOUTH);

        SuperWirelessKitStackState.addQueuedTarget(stack, queued);
        SuperWirelessKitStackState.addPendingBinding(stack, pending);

        assertEquals(1, SuperWirelessKitStackState.getQueuedTargetCount(stack));
        assertEquals(1, SuperWirelessKitStackState.getPendingBindingCount(stack));
        assertFalse(SuperWirelessKitStackState.canAcceptNewTargets(stack));
        assertTrue(SuperWirelessKitStackState.containsTarget(stack, queued));
        assertTrue(SuperWirelessKitStackState.containsTarget(stack, pending.getTarget()));
    }

    @Test
    public void deduplicatesQueuedTileTargetsAcrossFaces() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        BindingTargetRef up = createTarget(0, ForgeDirection.UP);
        BindingTargetRef north = createTarget(0, ForgeDirection.NORTH);

        SuperWirelessKitStackState.addQueuedTarget(stack, up);
        SuperWirelessKitStackState.addQueuedTarget(stack, north);

        assertEquals(1, SuperWirelessKitStackState.getQueuedTargetCount(stack));
        assertEquals(1, SuperWirelessKitStackState.getQueuedTargets(stack).size());
        assertTrue(SuperWirelessKitStackState.containsTarget(stack, up));
        assertTrue(SuperWirelessKitStackState.containsTarget(stack, north));
    }

    @Test
    public void prepareBindingsCollapsesLegacyQueuedTileDuplicatesAcrossFaces() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        ControllerEndpointRef controller = new ControllerEndpointRef(0, 1, 2, 3, ForgeDirection.NORTH, "controller");

        SuperWirelessKitStackState.addQueuedTarget(stack, createTarget(0, ForgeDirection.UP));
        SuperWirelessKitStackState.addQueuedTarget(stack, createTarget(0, ForgeDirection.NORTH));

        List<BindingRecord> drafted = SuperWirelessKitStackState.prepareBindingsForController(
            stack,
            controller,
            42,
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            999L);

        assertEquals(1, drafted.size());
        assertEquals(1, SuperWirelessKitStackState.getPendingBindingCount(stack));
    }

    @Test
    public void pendingBindingsAreRedraftedForNewController() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        BindingRecord oldPending = createBindingRecord(2, ForgeDirection.NORTH);
        ControllerEndpointRef newController = new ControllerEndpointRef(0, 30, 31, 32, ForgeDirection.WEST, "controller");

        SuperWirelessKitStackState.addPendingBinding(stack, oldPending);

        List<BindingRecord> drafted = SuperWirelessKitStackState.prepareBindingsForController(
            stack,
            newController,
            77,
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            1234L);

        assertEquals(1, drafted.size());
        assertEquals(newController, drafted.get(0).getController());
        assertEquals(oldPending.getTarget(), drafted.get(0).getTarget());
        assertEquals(1, SuperWirelessKitStackState.getPendingBindingCount(stack));
        assertEquals(newController, SuperWirelessKitStackState.getPendingBindings(stack).get(0).getController());
        assertTrue(SuperWirelessKitStackState.getQueuedTargets(stack).isEmpty());
    }

    @Test
    public void togglesModeAndPreservesStoredController() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        ControllerEndpointRef controller = new ControllerEndpointRef(0, 8, 9, 10, ForgeDirection.SOUTH, "controller");

        SuperWirelessKitStackState.setController(stack, controller);
        SuperWirelessKitStackState.toggleMode(stack);

        assertEquals(SuperWirelessKitMode.BIND, SuperWirelessKitStackState.getMode(stack));
        assertNotNull(SuperWirelessKitStackState.getController(stack));
        assertEquals(controller, SuperWirelessKitStackState.getController(stack));
    }

    private static BindingTargetRef createTarget(int offset) {
        return createTarget(offset, ForgeDirection.UP);
    }

    private static BindingTargetRef createTarget(int offset, ForgeDirection side) {
        return new BindingTargetRef(
            BindingTargetKind.TILE,
            0,
            10 + offset,
            20,
            30,
            side,
            new BindingFingerprint("mod:block", "tile.Target" + offset));
    }

    private static BindingRecord createBindingRecord(int offset, ForgeDirection face) {
        return new BindingRecord(
            UUID.nameUUIDFromBytes(("binding-" + offset).getBytes()),
            new ControllerEndpointRef(0, 1, 2, 3, face, "controller"),
            createTarget(offset),
            42,
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            999L + offset);
    }
}
