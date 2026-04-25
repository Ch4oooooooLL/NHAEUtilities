package com.github.nhaeutilities.modules.patternrouting.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingRepairCommandService;

public class CommandNau extends CommandBase {

    private final PatternRoutingRepairCommandService repairService;

    public CommandNau() {
        this(new PatternRoutingRepairCommandService());
    }

    CommandNau(PatternRoutingRepairCommandService repairService) {
        this.repairService = repairService;
    }

    @Override
    public String getCommandName() {
        return "nau";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/nau <repairrouting>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();
        if ("repairrouting".equals(subCommand)) {
            handleRepairRouting(sender);
            return;
        }
        sendHelp(sender);
    }

    private void handleRepairRouting(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.only_player");
            return;
        }
        if (!PatternRoutingRuntime.isEnabled()) {
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.nau.repairrouting.disabled");
            return;
        }

        PatternRoutingRepairCommandService.RepairSummary summary = repairService.run((EntityPlayerMP) sender);
        send(
            sender,
            EnumChatFormatting.GOLD,
            "nhaeutilities.command.nau.repairrouting.summary.scanned",
            summary.scannedControllerCount,
            ((EntityPlayerMP) sender).worldObj.provider.dimensionId);
        if (summary.matchedControllerCount == 0) {
            send(sender, EnumChatFormatting.YELLOW, "nhaeutilities.command.nau.repairrouting.no_targets");
            return;
        }
        send(
            sender,
            EnumChatFormatting.GREEN,
            "nhaeutilities.command.nau.repairrouting.summary.matched",
            summary.matchedControllerCount,
            summary.repairedControllerCount,
            summary.failedControllerCount);
        send(
            sender,
            summary.failedHatchCount == 0 ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW,
            "nhaeutilities.command.nau.repairrouting.summary.hatches",
            summary.repairedHatchCount,
            summary.failedHatchCount,
            summary.recheckedHatchCount);
    }

    private void sendHelp(ICommandSender sender) {
        send(sender, EnumChatFormatting.GOLD, "nhaeutilities.command.nau.help.title");
        send(sender, EnumChatFormatting.YELLOW, "nhaeutilities.command.nau.help.repairrouting");
    }

    private void send(ICommandSender sender, EnumChatFormatting color, String key, Object... args) {
        sender.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }
}
