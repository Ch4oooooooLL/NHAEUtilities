package com.github.nhaeutilities.modules.superwirelesskit.item;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitBatchCollector;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitStackState;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitTargetResolver;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class SuperWirelessKitInteractionHandler {

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK || event.world == null || event.world.isRemote) {
            return;
        }

        EntityPlayer player = event.entityPlayer;
        if (player == null) {
            return;
        }

        ItemStack stack = player.getCurrentEquippedItem();
        if (stack == null || !(stack.getItem() instanceof ItemSuperWirelessKit)) {
            return;
        }

        boolean hasPendingBindings = SuperWirelessKitStackState.getPendingBindingCount(stack) > 0;
        if (SuperWirelessKitInteractionRules.shouldRejectNewTarget(hasPendingBindings) && player.isSneaking()) {
            sendMessage(player, EnumChatFormatting.RED, "nhaeutilities.msg.swk.pending_bindings_block_new_targets");
            event.setCanceled(true);
            return;
        }

        if (!SuperWirelessKitInteractionRules.shouldAttemptBatchCapture(player.isSneaking(), true, hasPendingBindings)) {
            return;
        }

        List<BindingTargetRef> roots = SuperWirelessKitTargetResolver.resolveTargetsAtBlock(event.world, event.x, event.y, event.z);
        if (roots.isEmpty()) {
            return;
        }

        Set<BindingTargetRef> knownTargets = new LinkedHashSet<BindingTargetRef>();
        knownTargets.addAll(SuperWirelessKitStackState.getQueuedTargets(stack));
        for (BindingRecord record : SuperWirelessKitStackState.getPendingBindings(stack)) {
            knownTargets.add(record.getTarget());
        }

        SuperWirelessKitBatchCollector collector = new SuperWirelessKitBatchCollector(
            target -> SuperWirelessKitTargetResolver.findAdjacentTargets(event.world, target));

        List<BindingTargetRef> acceptedTargets = new ArrayList<BindingTargetRef>();
        for (BindingTargetRef root : roots) {
            List<BindingTargetRef> discovered = collector.collect(root, knownTargets);
            for (BindingTargetRef target : discovered) {
                if (!knownTargets.contains(target)) {
                    knownTargets.add(target);
                    acceptedTargets.add(target);
                }
            }
        }

        if (acceptedTargets.isEmpty()) {
            sendMessage(player, EnumChatFormatting.YELLOW, "nhaeutilities.msg.swk.no_new_targets");
            event.setCanceled(true);
            return;
        }

        for (BindingTargetRef target : acceptedTargets) {
            SuperWirelessKitStackState.addQueuedTarget(stack, target);
        }

        sendMessage(
            player,
            EnumChatFormatting.GREEN,
            "nhaeutilities.msg.swk.batch_queued_targets",
            acceptedTargets.size(),
            SuperWirelessKitStackState.getQueuedTargetCount(stack));
        event.setCanceled(true);
    }

    private static void sendMessage(EntityPlayer player, EnumChatFormatting color, String key, Object... args) {
        player.addChatMessage(new ChatComponentText(color + tr(key, args)));
    }

    private static String tr(String key, Object... args) {
        try {
            if (args == null || args.length == 0) {
                String translated = StatCollector.translateToLocal(key);
                return translated != null && !translated.isEmpty() ? translated : key;
            }
            String translated = StatCollector.translateToLocalFormatted(key, args);
            return translated != null && !translated.isEmpty() ? translated : key;
        } catch (RuntimeException ignored) {
            return key;
        }
    }
}
