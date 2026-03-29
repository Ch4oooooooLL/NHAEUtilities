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
    public void togglesModeAndPreservesStoredController() {
        ItemStack stack = new ItemStack(new ItemSuperWirelessKit());
        ControllerEndpointRef controller = new ControllerEndpointRef(0, 8, 9, 10, ForgeDirection.SOUTH, "controller");

        SuperWirelessKitStackState.setController(stack, controller);
        SuperWirelessKitStackState.toggleMode(stack);

        assertEquals(SuperWirelessKitMode.BIND, SuperWirelessKitStackState.getMode(stack));
        assertNotNull(SuperWirelessKitStackState.getController(stack));
        assertEquals(controller, SuperWirelessKitStackState.getController(stack));
    }
}
