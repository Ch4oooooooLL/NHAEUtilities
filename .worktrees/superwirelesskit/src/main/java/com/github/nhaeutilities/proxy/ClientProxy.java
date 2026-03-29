package com.github.nhaeutilities.proxy;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.modules.patterngenerator.gui.GuiPatternDetail;
import com.github.nhaeutilities.modules.patterngenerator.gui.GuiPatternStorage;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;

public class ClientProxy extends CommonProxy {

    @Override
    protected void registerBuiltInModules(CoreConfig coreConfig, ModuleRegistry moduleRegistry) {
        super.registerBuiltInModules(coreConfig, moduleRegistry);
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    public void openPatternDetailScreen(EntityPlayer player, int index, List<String> inputs, List<String> outputs) {
        EntityPlayer uiPlayer = resolvePlayer(player);
        if (uiPlayer == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }

        UIBuildContext buildContext = new UIBuildContext(uiPlayer);
        ModularWindow detailWindow = GuiPatternDetail.createWindow(buildContext, index, inputs, outputs);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        mc.displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, detailWindow)));
    }

    @Override
    public void openPatternStorageScreen(EntityPlayer player) {
        EntityPlayer uiPlayer = resolvePlayer(player);
        if (uiPlayer == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }

        UIBuildContext buildContext = new UIBuildContext(uiPlayer);
        ModularWindow storageWindow = GuiPatternStorage.createWindow(buildContext, uiPlayer);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        mc.displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, storageWindow)));
    }

    private EntityPlayer resolvePlayer(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (player != null) {
            return player;
        }
        return mc.thePlayer;
    }
}
