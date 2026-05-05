package com.github.nhaeutilities.modules.patternrouting.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.nhaeutilities.modules.patternrouting.gui.PatternIndexClientScreen;
import com.github.nhaeutilities.modules.patternrouting.gui.PatternIndexConfigState;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class PacketFilterRulesResult implements IMessage {

    private static final int MAX_RULES = 64;

    private List<FilterRule> rules;

    public PacketFilterRulesResult() {
        rules = new ArrayList<>();
    }

    public PacketFilterRulesResult(List<FilterRule> rules) {
        this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<FilterRule>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > MAX_RULES) count = 0;
        rules = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String typeName = ByteBufUtils.readUTF8String(buf);
            RuleType type;
            try {
                type = RuleType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String itemPattern = ByteBufUtils.readUTF8String(buf);
            String recipeMap = ByteBufUtils.readUTF8String(buf);
            rules.add(new FilterRule(type, itemPattern, recipeMap));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int count = Math.min(rules.size(), MAX_RULES);
        buf.writeInt(count);
        for (FilterRule rule : rules) {
            ByteBufUtils.writeUTF8String(buf, rule.type.name());
            ByteBufUtils.writeUTF8String(buf, rule.itemPattern);
            ByteBufUtils.writeUTF8String(buf, rule.recipeMapId);
        }
    }

    public static class Handler implements IMessageHandler<PacketFilterRulesResult, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketFilterRulesResult message, MessageContext ctx) {
            PatternIndexConfigState.setRules(message.rules);
            Minecraft.getMinecraft()
                .func_152344_a(() -> { PatternIndexClientScreen.refreshOpenPatternIndexGui(); });
            return null;
        }
    }
}
