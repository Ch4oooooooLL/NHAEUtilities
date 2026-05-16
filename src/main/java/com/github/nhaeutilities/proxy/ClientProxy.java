package com.github.nhaeutilities.proxy;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.modules.patterngenerator.gui.GuiPatternDetail;
import com.github.nhaeutilities.modules.patterngenerator.gui.GuiPatternStorage;
import com.github.nhaeutilities.modules.shared.nei.NeiRecipeExtractEventHandler;

public class ClientProxy extends CommonProxy {

    @Override
    protected void registerBuiltInModules(CoreConfig coreConfig, ModuleRegistry moduleRegistry) {
        super.registerBuiltInModules(coreConfig, moduleRegistry);
    }

    @Override
    public void registerClientEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new NeiRecipeExtractEventHandler());
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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }
        GuiPatternDetail.openFromNetwork(index, inputs, outputs);
    }

    @Override
    public void openPatternStorageScreen(EntityPlayer player) {
        EntityPlayer uiPlayer = resolvePlayer(player);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }
        GuiPatternStorage.open(uiPlayer);
    }

    private EntityPlayer resolvePlayer(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (player != null) {
            return player;
        }
        return mc.thePlayer;
    }
}
