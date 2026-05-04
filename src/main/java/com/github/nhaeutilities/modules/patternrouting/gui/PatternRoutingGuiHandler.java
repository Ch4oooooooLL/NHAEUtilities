package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

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
        if (id != ItemRecipeMapAnalyzer.GUI_ID_ANALYSIS) {
            return fallbackHandler == null ? null : fallbackHandler.getServerGuiElement(id, player, world, x, y, z);
        }
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow window = GuiPatternRoutingAnalysis.createWindow(buildContext, player);
        return new ModularUIContainer(muiContext, window);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != ItemRecipeMapAnalyzer.GUI_ID_ANALYSIS) {
            return fallbackHandler == null ? null : fallbackHandler.getClientGuiElement(id, player, world, x, y, z);
        }
        return PatternRoutingAnalysisClientScreen.createClientGuiOnOpen(player);
    }
}
