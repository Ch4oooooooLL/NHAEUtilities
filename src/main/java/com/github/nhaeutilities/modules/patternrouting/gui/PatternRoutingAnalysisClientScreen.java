package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.github.nhaeutilities.modules.shared.animation.ScreenHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternRoutingAnalysisClientScreen {

    private static volatile boolean analyzerScreenOpen = false;

    private PatternRoutingAnalysisClientScreen() {}

    public static void openClientGuiOnOpen(EntityPlayer player) {
        GuiPatternRoutingAnalysis.requestAnalysis(player);
    }

    public static void openClientGui(EntityPlayer player) {
        ModularPanel panel = GuiPatternRoutingAnalysis.createWindow(player);
        ScreenHelper.open(new AnalyzerScreen(panel));
        analyzerScreenOpen = true;
    }

    public static boolean isAnalyzerScreen() {
        return analyzerScreenOpen;
    }

    public static void refreshOpenAnalyzerGuiIfNeeded(Minecraft minecraft, String recipeMapId) {
        if (minecraft == null) {
            return;
        }
        EntityPlayer player = minecraft.thePlayer;
        boolean normalRefresh = GuiPatternRoutingAnalysis.shouldRefreshCurrentAnalyzerScreen(
            recipeMapId,
            isAnalyzerScreen(),
            GuiPatternRoutingAnalysis.isHoldingAnalyzer(player),
            GuiPatternRoutingAnalysis.readStoredRecipeMap(player));
        boolean fallbackRefresh = isAnalyzerScreen() && GuiPatternRoutingAnalysisState.hasPendingRefresh();
        if (!normalRefresh && !fallbackRefresh) {
            return;
        }
        GuiPatternRoutingAnalysisState.clearPendingRefresh();
        ModularPanel panel = GuiPatternRoutingAnalysis.createWindow(player);
        ScreenHelper.open(new AnalyzerScreen(panel));
    }

    public interface AnalyzerScreenMarker {
    }

    private static final class AnalyzerScreen extends ModularScreen implements AnalyzerScreenMarker {

        private AnalyzerScreen(ModularPanel panel) {
            super("nhaeutilities", panel);
        }

        @Override
        public void onClose() {
            super.onClose();
            analyzerScreenOpen = false;
        }

        @Override
        public void onOpen() {
            super.onOpen();
            analyzerScreenOpen = true;
        }
    }
}
