package com.github.nhaeutilities.modules.patternrouting.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.github.nhaeutilities.modules.patternrouting.service.BookmarkBatchEncoder;

import codechicken.nei.LayoutManager;
import codechicken.nei.bookmark.BookmarkGrid;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class PacketBatchEncodeResult implements IMessage {

    private static final int MAX_ENCODED_ITEMS = 256;

    private int encoded;
    private int skippedMultiSource;
    private int skippedNoRecipe;
    private int failedBlank;
    private List<ItemStack> encodedItems;

    public PacketBatchEncodeResult() {
        encodedItems = new ArrayList<>();
    }

    public PacketBatchEncodeResult(BookmarkBatchEncoder.BatchEncodeResult result) {
        this.encoded = result.encoded;
        this.skippedMultiSource = result.skippedMultiSource;
        this.skippedNoRecipe = result.skippedNoRecipe;
        this.failedBlank = result.failedBlank;
        this.encodedItems = new ArrayList<>(result.encodedItems);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        encoded = buf.readInt();
        skippedMultiSource = buf.readInt();
        skippedNoRecipe = buf.readInt();
        failedBlank = buf.readInt();
        int count = buf.readInt();
        if (count < 0 || count > MAX_ENCODED_ITEMS) {
            count = 0;
        }
        encodedItems = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            if (stack != null) {
                encodedItems.add(stack);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(encoded);
        buf.writeInt(skippedMultiSource);
        buf.writeInt(skippedNoRecipe);
        buf.writeInt(failedBlank);
        int count = Math.min(encodedItems.size(), MAX_ENCODED_ITEMS);
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeItemStack(buf, encodedItems.get(i));
        }
    }

    public static class Handler implements IMessageHandler<PacketBatchEncodeResult, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBatchEncodeResult message, MessageContext ctx) {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.func_152344_a(() -> handleResult(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static void handleResult(PacketBatchEncodeResult message) {
            if (message.encoded > 0 && !message.encodedItems.isEmpty()) {
                removeFromBookmarks(message.encodedItems);
            }

            sendSummaryChat(message);
        }

        @SideOnly(Side.CLIENT)
        private static void removeFromBookmarks(List<ItemStack> encodedItems) {
            if (LayoutManager.bookmarkPanel == null) return;

            try {
                BookmarkGrid grid = LayoutManager.bookmarkPanel.getGrid();
                for (int i = grid.size() - 1; i >= 0; i--) {
                    Object item = grid.getBookmarkItem(i);
                    if (item == null) continue;
                    if (item instanceof codechicken.nei.bookmark.BookmarkItem) {
                        codechicken.nei.bookmark.BookmarkItem bmItem = (codechicken.nei.bookmark.BookmarkItem) item;
                        if (bmItem.itemStack != null && containsStack(encodedItems, bmItem.itemStack)) {
                            grid.removeRecipe(i, true);
                        }
                    }
                }
                LayoutManager.bookmarkPanel.save();
            } catch (Exception ignored) {}
        }

        @SideOnly(Side.CLIENT)
        private static boolean containsStack(List<ItemStack> encodedItems, ItemStack bookmarkStack) {
            if (bookmarkStack == null || bookmarkStack.getItem() == null) return false;
            for (ItemStack encoded : encodedItems) {
                if (encoded == null) continue;
                if (encoded.getItem() == bookmarkStack.getItem()
                    && encoded.getItemDamage() == bookmarkStack.getItemDamage()) {
                    return true;
                }
            }
            return false;
        }

        @SideOnly(Side.CLIENT)
        private static void sendSummaryChat(PacketBatchEncodeResult message) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) return;

            String prefix = EnumChatFormatting.AQUA + "[NHAEUtilities] " + EnumChatFormatting.RESET;
            StringBuilder sb = new StringBuilder(prefix);

            sb.append(
                String.format(
                    StatCollector.translateToLocal("nhaeutilities.msg.batch_encode.summary"),
                    message.encoded,
                    message.skippedMultiSource,
                    message.skippedNoRecipe,
                    message.failedBlank));

            mc.thePlayer.addChatMessage(new ChatComponentText(sb.toString()));
        }
    }
}
