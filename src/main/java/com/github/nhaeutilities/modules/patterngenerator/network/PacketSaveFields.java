package com.github.nhaeutilities.modules.patterngenerator.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server: persist pattern generator field values into held-item NBT.
 */
public class PacketSaveFields implements IMessage {

    public static final String NBT_RECIPE_MAP = "recipeMap";
    public static final String NBT_OUTPUT_ORE = "outputOre";
    public static final String NBT_INPUT_ORE = "inputOre";
    public static final String NBT_NC_ITEM = "ncItem";
    public static final String NBT_BLACKLIST_INPUT = "blacklistInput";
    public static final String NBT_BLACKLIST_OUTPUT = "blacklistOutput";
    public static final String NBT_REPLACEMENTS = "replacements";
    public static final String NBT_OUTPUT_SLOTS = "outputSlots";
    public static final String NBT_TARGET_TIER = "targetTier";

    private String recipeMap;
    private String outputOre;
    private String inputOre;
    private String ncItem;
    private String blacklistInput;
    private String blacklistOutput;
    private String replacements;
    private String outputSlots;
    private int targetTier;

    public PacketSaveFields() {}

    public PacketSaveFields(String recipeMap, String outputOre, String inputOre, String ncItem, String blacklistInput,
        String blacklistOutput, String replacements, String outputSlots, int targetTier) {
        this.recipeMap = recipeMap;
        this.outputOre = outputOre;
        this.inputOre = inputOre;
        this.ncItem = ncItem;
        this.blacklistInput = blacklistInput;
        this.blacklistOutput = blacklistOutput;
        this.replacements = replacements;
        this.outputSlots = outputSlots;
        this.targetTier = targetTier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMap = ByteBufUtils.readUTF8String(buf);
        outputOre = ByteBufUtils.readUTF8String(buf);
        inputOre = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
        blacklistInput = ByteBufUtils.readUTF8String(buf);
        blacklistOutput = ByteBufUtils.readUTF8String(buf);
        replacements = ByteBufUtils.readUTF8String(buf);
        outputSlots = ByteBufUtils.readUTF8String(buf);
        targetTier = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMap != null ? recipeMap : "");
        ByteBufUtils.writeUTF8String(buf, outputOre != null ? outputOre : "");
        ByteBufUtils.writeUTF8String(buf, inputOre != null ? inputOre : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
        ByteBufUtils.writeUTF8String(buf, blacklistInput != null ? blacklistInput : "");
        ByteBufUtils.writeUTF8String(buf, blacklistOutput != null ? blacklistOutput : "");
        ByteBufUtils.writeUTF8String(buf, replacements != null ? replacements : "");
        ByteBufUtils.writeUTF8String(buf, outputSlots != null ? outputSlots : "");
        buf.writeInt(targetTier);
    }

    public static class Handler implements IMessageHandler<PacketSaveFields, IMessage> {

        @Override
        public IMessage onMessage(PacketSaveFields message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            ItemStack held = player.getCurrentEquippedItem();
            writeFieldsIfPatternGenerator(held, message);
            return null;
        }
    }

    static boolean writeFieldsIfPatternGenerator(ItemStack held, PacketSaveFields message) {
        if (!isPatternGeneratorItem(held) || message == null) {
            return false;
        }

        NBTTagCompound tag = held.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            held.setTagCompound(tag);
        }

        tag.setString(NBT_RECIPE_MAP, message.recipeMap != null ? message.recipeMap : "");
        tag.setString(NBT_OUTPUT_ORE, message.outputOre != null ? message.outputOre : "");
        tag.setString(NBT_INPUT_ORE, message.inputOre != null ? message.inputOre : "");
        tag.setString(NBT_NC_ITEM, message.ncItem != null ? message.ncItem : "");
        tag.setString(NBT_BLACKLIST_INPUT, message.blacklistInput != null ? message.blacklistInput : "");
        tag.setString(NBT_BLACKLIST_OUTPUT, message.blacklistOutput != null ? message.blacklistOutput : "");
        tag.setString(NBT_REPLACEMENTS, message.replacements != null ? message.replacements : "");
        tag.setString(NBT_OUTPUT_SLOTS, message.outputSlots != null ? message.outputSlots : "");
        tag.setInteger(NBT_TARGET_TIER, message.targetTier);
        return true;
    }

    static boolean isPatternGeneratorItem(ItemStack held) {
        if (held == null || held.getItem() == null) {
            return false;
        }

        if ("ItemPatternGenerator".equals(
            held.getItem()
                .getClass()
                .getSimpleName())) {
            return true;
        }

        String unlocalizedName;
        try {
            unlocalizedName = held.getItem()
                .getUnlocalizedName();
        } catch (RuntimeException ignored) {
            return false;
        }

        if (unlocalizedName == null || unlocalizedName.isEmpty()) {
            return false;
        }

        return "nhaeutilities.pattern_generator".equals(unlocalizedName)
            || "item.nhaeutilities.pattern_generator".equals(unlocalizedName)
            || "ae2patterngen.pattern_generator".equals(unlocalizedName)
            || "item.ae2patterngen.pattern_generator".equals(unlocalizedName);
    }
}
