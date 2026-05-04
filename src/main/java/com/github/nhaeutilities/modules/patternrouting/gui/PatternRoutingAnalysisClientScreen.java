package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternRoutingAnalysisClientScreen {

    private PatternRoutingAnalysisClientScreen() {}

    public static ModularGui createClientGuiOnOpen(EntityPlayer player) {
        GuiPatternRoutingAnalysis.requestAnalysis(player);
        return createClientGui(player);
    }

    public static ModularGui createClientGui(EntityPlayer player) {
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow window = GuiPatternRoutingAnalysis.createWindow(buildContext, player);
        return new AnalyzerGui(new ModularUIContainer(muiContext, window));
    }

    public static boolean isAnalyzerScreen(Object screen) {
        return screen instanceof AnalyzerScreenMarker;
    }

    public static void refreshOpenAnalyzerGuiIfNeeded(Minecraft minecraft, String recipeMapId) {
        if (minecraft == null) {
            return;
        }
        EntityPlayer player = minecraft.thePlayer;
        GuiScreen currentScreen = minecraft.currentScreen;
        if (!GuiPatternRoutingAnalysis.shouldRefreshCurrentAnalyzerScreen(
            recipeMapId,
            isAnalyzerScreen(currentScreen),
            GuiPatternRoutingAnalysis.isHoldingAnalyzer(player),
            GuiPatternRoutingAnalysis.readStoredRecipeMap(player))) {
            return;
        }
        minecraft.displayGuiScreen(createClientGui(player));
    }

    public interface AnalyzerScreenMarker {
    }

    private static final class AnalyzerGui extends ModularGui implements AnalyzerScreenMarker {

        private AnalyzerGui(ModularUIContainer container) {
            super(container);
        }
    }
}
