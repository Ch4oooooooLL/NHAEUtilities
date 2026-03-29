package com.github.nhaeutilities.modules.patterngenerator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    public static final int GUI_ID = 101;
    public static final int GUI_ID_STORAGE = 102;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_ID) {
            com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                player);
            com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                buildContext,
                () -> {});
            com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternGen
                .createWindow(buildContext, player.getCurrentEquippedItem());
            return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window);
        }
        if (id == GUI_ID_STORAGE) {
            com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                player);
            com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                buildContext,
                () -> {});
            com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternStorage
                .createWindow(buildContext, player);
            return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        try {
            if (id == GUI_ID) {
                com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                    player);
                com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                    buildContext,
                    () -> {});
                com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternGen
                    .createWindow(buildContext, player.getCurrentEquippedItem());
                return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui(
                    new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window));
            }
            if (id == GUI_ID_STORAGE) {
                com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                    player);
                com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                    buildContext,
                    () -> {});
                com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternStorage
                    .createWindow(buildContext, player);
                return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui(
                    new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window));
            }
        } catch (Throwable t) {
            cpw.mods.fml.common.FMLLog.severe("[NHAEUtilities] Error creating client GUI element: " + t);
            t.printStackTrace();
        }
        return null;
    }
}
