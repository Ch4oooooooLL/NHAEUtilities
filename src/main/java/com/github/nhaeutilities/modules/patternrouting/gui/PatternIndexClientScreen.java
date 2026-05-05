package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternIndexClientScreen {

    private PatternIndexClientScreen() {}

    public static ModularGui createPatternIndexGui(EntityPlayer player) {
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow window = GuiPatternIndex.createWindow(buildContext);
        return new ModularGui(new ModularUIContainer(muiContext, window));
    }

    public static ModularGui createAddFilterRuleGui(EntityPlayer player, FilterRule existingRule, int editIndex) {
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow window = GuiAddFilterRule.createWindow(buildContext, existingRule, editIndex);
        return new ModularGui(new ModularUIContainer(muiContext, window));
    }

    public static void openAddFilterRuleGui(EntityPlayer player, int editIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        EntityPlayer p = player != null ? player : mc.thePlayer;
        mc.displayGuiScreen(createAddFilterRuleGui(p, null, editIndex));
    }

    public static void openPatternIndexGui(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        EntityPlayer p = player != null ? player : mc.thePlayer;
        mc.displayGuiScreen(createPatternIndexGui(p));
    }

    public static void refreshOpenPatternIndexGui() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        GuiScreen current = mc.currentScreen;
        if (!(current instanceof ModularGui)) return;
        mc.thePlayer.closeScreen();
        mc.displayGuiScreen(createPatternIndexGui(mc.thePlayer));
    }

    public static ModularGui createStagingViewerGui(EntityPlayer player) {
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow window = GuiStagingViewer.createWindow(buildContext);
        return new ModularGui(new ModularUIContainer(muiContext, window));
    }
}
