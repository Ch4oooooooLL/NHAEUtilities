package com.github.nhaeutilities.modules.patternrouting.network;

import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRuleConfig;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketUpdateFilterRules implements IMessage {

    private static final int ACTION_ADD = 0;
    private static final int ACTION_EDIT = 1;
    private static final int ACTION_DELETE = 2;

    private int action;
    private int index;
    private FilterRule rule;

    public PacketUpdateFilterRules() {}

    private PacketUpdateFilterRules(int action, int index, FilterRule rule) {
        this.action = action;
        this.index = index;
        this.rule = rule;
    }

    public static PacketUpdateFilterRules add(FilterRule rule) {
        return new PacketUpdateFilterRules(ACTION_ADD, -1, rule);
    }

    public static PacketUpdateFilterRules edit(int index, FilterRule rule) {
        return new PacketUpdateFilterRules(ACTION_EDIT, index, rule);
    }

    public static PacketUpdateFilterRules delete(int index) {
        return new PacketUpdateFilterRules(ACTION_DELETE, index, null);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        index = buf.readInt();
        if (action == ACTION_DELETE) {
            rule = null;
            return;
        }
        String typeName = ByteBufUtils.readUTF8String(buf);
        RuleType type = RuleType.BLACKLIST;
        try {
            type = RuleType.valueOf(typeName);
        } catch (IllegalArgumentException e) {}
        String itemPattern = ByteBufUtils.readUTF8String(buf);
        String recipeMap = ByteBufUtils.readUTF8String(buf);
        rule = new FilterRule(type, itemPattern, recipeMap);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(index);
        if (action == ACTION_DELETE || rule == null) return;
        ByteBufUtils.writeUTF8String(buf, rule.type.name());
        ByteBufUtils.writeUTF8String(buf, rule.itemPattern);
        ByteBufUtils.writeUTF8String(buf, rule.recipeMapId);
    }

    public static class Handler implements IMessageHandler<PacketUpdateFilterRules, IMessage> {

        @Override
        public IMessage onMessage(PacketUpdateFilterRules message, MessageContext ctx) {
            FilterRuleConfig config = FilterRuleConfig.load();

            switch (message.action) {
                case ACTION_ADD:
                    if (message.rule != null && message.rule.isValid()) {
                        config.addRule(message.rule);
                    }
                    break;
                case ACTION_EDIT:
                    if (message.rule != null && message.rule.isValid()) {
                        config.updateRule(message.index, message.rule);
                    }
                    break;
                case ACTION_DELETE:
                    config.removeRule(message.index);
                    break;
                default:
                    break;
            }

            config.save();
            return new PacketFilterRulesResult(config.getRules());
        }
    }
}
