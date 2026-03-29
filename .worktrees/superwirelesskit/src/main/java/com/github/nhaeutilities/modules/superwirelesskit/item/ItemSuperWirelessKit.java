package com.github.nhaeutilities.modules.superwirelesskit.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.service.BindingBatchResult;
import com.github.nhaeutilities.modules.superwirelesskit.service.SuperWirelessBindingService;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitMode;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitStackState;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitTargetResolver;

public class ItemSuperWirelessKit extends Item {

    public ItemSuperWirelessKit() {
        setUnlocalizedName("nhaeutilities.super_wireless_kit");
        setTextureName("nhaeutilities:super_wireless_kit");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && SuperWirelessKitInteractionRules
            .shouldToggleMode(player.isSneaking(), isTargetingBlock(world, player))) {
            SuperWirelessKitMode next = SuperWirelessKitStackState.toggleMode(stack);
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + tr("nhaeutilities.msg.swk.mode_switched", next.name())));
        }
        return stack;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() || world.isRemote) {
            return false;
        }

        ControllerEndpointRef controller = SuperWirelessKitTargetResolver
            .resolveController(world, x, y, z, toFacing(side));
        if (SuperWirelessKitStackState.getMode(stack) == SuperWirelessKitMode.BIND) {
            if (controller == null) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + tr("nhaeutilities.msg.swk.controller_required")));
                return true;
            }

            BindingBatchResult result = SuperWirelessBindingService
                .bindQueuedTargets(world, player, stack, controller, world.getTotalWorldTime());
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + tr("nhaeutilities.msg.swk.bound_targets", result.getSuccessCount())));
            if (result.getFailureCount() > 0) {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.YELLOW
                            + tr("nhaeutilities.msg.swk.bind_failures", result.getFailureCount())));
            }
            return true;
        }

        if (controller != null) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + tr("nhaeutilities.msg.swk.queue_cannot_record_controller")));
            return true;
        }

        BindingTargetRef target = SuperWirelessKitTargetResolver
            .resolveTarget(world, x, y, z, toFacing(side), hitX, hitY, hitZ);
        if (target == null) {
            player.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + tr("nhaeutilities.msg.swk.invalid_target")));
            return true;
        }

        SuperWirelessKitStackState.addQueuedTarget(stack, target);
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GREEN + tr(
                    "nhaeutilities.msg.swk.queued_target",
                    SuperWirelessKitStackState.getQueuedTargets(stack)
                        .size())));
        return true;
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add(EnumChatFormatting.YELLOW + tr("nhaeutilities.tooltip.swk.title"));
        list.add(
            EnumChatFormatting.GRAY + tr(
                "nhaeutilities.tooltip.swk.mode",
                SuperWirelessKitStackState.getMode(stack)
                    .name()));
        list.add(
            EnumChatFormatting.GRAY + tr(
                "nhaeutilities.tooltip.swk.queue",
                SuperWirelessKitStackState.getQueuedTargets(stack)
                    .size()));
        list.add(
            EnumChatFormatting.GRAY + tr(
                "nhaeutilities.tooltip.swk.pending",
                SuperWirelessKitStackState.getPendingBindings(stack)
                    .size()));
        ControllerEndpointRef controller = SuperWirelessKitStackState.getController(stack);
        list.add(
            EnumChatFormatting.GRAY + tr(
                "nhaeutilities.tooltip.swk.controller",
                controller != null ? formatController(controller) : tr("nhaeutilities.gui.common.none")));
    }

    private static ForgeDirection toFacing(int side) {
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        return direction == ForgeDirection.UNKNOWN ? ForgeDirection.UP : direction;
    }

    private boolean isTargetingBlock(World world, EntityPlayer player) {
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, false);
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private static String formatController(ControllerEndpointRef controller) {
        return controller.getControllerType() + "@"
            + controller.getDimensionId()
            + ":"
            + controller.getX()
            + ","
            + controller.getY()
            + ","
            + controller.getZ()
            + "/"
            + controller.getFace()
                .name();
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
