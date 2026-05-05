package com.github.nhaeutilities.modules.patternrouting.network;

import com.github.nhaeutilities.modules.patternrouting.service.FilterRuleConfig;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketRequestFilterRules implements IMessage {

    public PacketRequestFilterRules() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketRequestFilterRules, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestFilterRules message, MessageContext ctx) {
            FilterRuleConfig config = FilterRuleConfig.load();
            return new PacketFilterRulesResult(config.getRules());
        }
    }
}
