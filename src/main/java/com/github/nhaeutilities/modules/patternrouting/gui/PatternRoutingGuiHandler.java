package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.github.nhaeutilities.modules.patterngenerator.item.ItemPatternIndex;
import com.github.nhaeutilities.modules.patternrouting.item.ItemRecipeMapAnalyzer;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;

import cpw.mods.fml.common.network.IGuiHandler;

public class PatternRoutingGuiHandler implements IGuiHandler {

    private final IGuiHandler fallbackHandler;

    public PatternRoutingGuiHandler() {
        this(null);
    }

    public PatternRoutingGuiHandler(IGuiHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ItemRecipeMapAnalyzer.GUI_ID_ANALYSIS) {
            UIBuildContext buildContext = new UIBuildContext(player);
            ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
            ModularWindow window = GuiPatternRoutingAnalysis.createWindow(buildContext, player);
            return new ModularUIContainer(muiContext, window);
        }
        if (id == ItemPatternIndex.GUI_ID_PATTERN_INDEX) {
            UIBuildContext buildContext = new UIBuildContext(player);
            ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
            ModularWindow window = GuiPatternIndex.createWindow(buildContext);
            return new ModularUIContainer(muiContext, window);
        }
        if (id == ItemPatternIndex.GUI_ID_STAGING_STORAGE) {
            UIBuildContext buildContext = new UIBuildContext(player);
            ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
            ModularWindow window = GuiStagingViewer.createWindow(buildContext);
            return new ModularUIContainer(muiContext, window);
        }
        return fallbackHandler == null ? null : fallbackHandler.getServerGuiElement(id, player, world, x, y, z);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ItemRecipeMapAnalyzer.GUI_ID_ANALYSIS) {
            return PatternRoutingAnalysisClientScreen.createClientGuiOnOpen(player);
        }
        if (id == ItemPatternIndex.GUI_ID_PATTERN_INDEX) {
            return PatternIndexClientScreen.createPatternIndexGui(player);
        }
        if (id == ItemPatternIndex.GUI_ID_STAGING_STORAGE) {
            return PatternIndexClientScreen.createStagingViewerGui(player);
        }
        return fallbackHandler == null ? null : fallbackHandler.getClientGuiElement(id, player, world, x, y, z);
    }
}
