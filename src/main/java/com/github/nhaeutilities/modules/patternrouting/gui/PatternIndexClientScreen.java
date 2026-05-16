package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketRequestFilterRules;
import com.github.nhaeutilities.modules.shared.animation.ScreenHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternIndexClientScreen {

    private static boolean initialFetchDone = false;

    private PatternIndexClientScreen() {}

    public static void openPatternIndexGui(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (!initialFetchDone) {
            initialFetchDone = true;
            NetworkHandler.sendToServer(new PacketRequestFilterRules());
        }
        EntityPlayer p = player != null ? player : mc.thePlayer;
        ScreenHelper.open(GuiPatternIndex.createWindow(p));
    }

    public static void openAddFilterRuleGui(EntityPlayer player, int editIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        EntityPlayer p = player != null ? player : mc.thePlayer;
        ScreenHelper.open(GuiAddFilterRule.createWindow(p, null, editIndex));
    }

    public static void refreshOpenPatternIndexGui() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        mc.thePlayer.closeScreen();
        openPatternIndexGui(mc.thePlayer);
    }

    public static void openStagingViewerGui(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        EntityPlayer p = player != null ? player : mc.thePlayer;
        ScreenHelper.open(GuiStagingViewer.createWindow(p));
    }
}
