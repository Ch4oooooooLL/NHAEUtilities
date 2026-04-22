package com.github.nhaeutilities.proxy;

import java.util.Objects;

import net.minecraft.item.Item;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.modules.patterngenerator.PatternGeneratorModule;
import com.github.nhaeutilities.modules.patterngenerator.gui.GuiHandler;
import com.github.nhaeutilities.modules.patterngenerator.item.ModItems;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingModule;
import com.github.nhaeutilities.modules.superwirelesskit.SuperWirelessKitModule;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public ModuleRegistry createModuleRegistry(CoreConfig coreConfig) {
        ModuleRegistry moduleRegistry = new ModuleRegistry();
        registerBuiltInModules(coreConfig, moduleRegistry);
        return moduleRegistry;
    }

    protected void registerBuiltInModules(CoreConfig coreConfig, ModuleRegistry moduleRegistry) {
        Objects.requireNonNull(coreConfig, "coreConfig");
        Objects.requireNonNull(moduleRegistry, "moduleRegistry");
        moduleRegistry.register(new PatternGeneratorModule(coreConfig, this));
        moduleRegistry.register(new PatternRoutingModule(coreConfig));
        moduleRegistry.register(new SuperWirelessKitModule(coreConfig, this));
    }

    public void preInit(FMLPreInitializationEvent event, ModuleRegistry moduleRegistry) {
        moduleRegistry.preInit(event);
    }

    public void init(FMLInitializationEvent event, ModuleRegistry moduleRegistry, Object modInstance) {
        moduleRegistry.init(event, modInstance);
    }

    public void postInit(FMLPostInitializationEvent event, ModuleRegistry moduleRegistry) {
        moduleRegistry.postInit(event);
    }

    public void serverStarting(FMLServerStartingEvent event, ModuleRegistry moduleRegistry) {
        moduleRegistry.serverStarting(event);
    }

    public void registerPatternGeneratorIntegration(Object modInstance) {
        registerPatternGeneratorGuiHandler(modInstance);
        registerPatternGeneratorWirelessHandler();
        registerPatternGeneratorRecipe();
    }

    public void registerSuperWirelessKitIntegration() {
        registerSuperWirelessKitRecipe();
    }

    protected void registerPatternGeneratorGuiHandler(Object modInstance) {
        if (modInstance == null) {
            return;
        }

        try {
            NetworkRegistry.INSTANCE.registerGuiHandler(modInstance, new GuiHandler());
        } catch (Throwable t) {
            FMLLog.warning("[NHAEUtilities] Failed to register pattern generator GUI handler: %s", t.getMessage());
        }
    }

    protected void registerPatternGeneratorWirelessHandler() {
        if (ModItems.itemPatternGenerator == null) {
            return;
        }

        try {
            AEApi.instance()
                .registries()
                .wireless()
                .registerWirelessHandler((IWirelessTermHandler) ModItems.itemPatternGenerator);
        } catch (Throwable t) {
            FMLLog.warning("[NHAEUtilities] Failed to register pattern generator wireless handler: %s", t.getMessage());
        }
    }

    protected void registerPatternGeneratorRecipe() {
        if (ModItems.itemPatternGenerator == null) {
            return;
        }

        Item gtMetaItem = findItem("gregtech", "gt.metaitem.01");
        Item aeMaterial = findItem("appliedenergistics2", "item.ItemMultiMaterial");
        Item aePart = findItem("appliedenergistics2", "item.ItemMultiPart");
        if (gtMetaItem == null || aeMaterial == null || aePart == null) {
            FMLLog.warning("[NHAEUtilities] Pattern generator recipe ingredients are missing.");
            return;
        }

        addShapedRecipe(
            new net.minecraft.item.ItemStack(ModItems.itemPatternGenerator),
            "ABA",
            "BCB",
            "ABA",
            'A',
            new net.minecraft.item.ItemStack(gtMetaItem, 1, 32653),
            'B',
            new net.minecraft.item.ItemStack(aeMaterial, 1, 52),
            'C',
            new net.minecraft.item.ItemStack(aePart, 1, 340));
    }

    protected void registerSuperWirelessKitRecipe() {
        if (com.github.nhaeutilities.modules.superwirelesskit.item.ModItems.itemSuperWirelessKit == null) {
            return;
        }

        Item advancedWirelessKit = findItem("ae2stuff", "AdvWirelessKit");
        if (advancedWirelessKit == null) {
            FMLLog.warning("[NHAEUtilities] Super wireless kit recipe ingredient ae2stuff:AdvWirelessKit is missing.");
            return;
        }

        addShapedRecipe(
            new net.minecraft.item.ItemStack(
                com.github.nhaeutilities.modules.superwirelesskit.item.ModItems.itemSuperWirelessKit),
            "AAA",
            "AAA",
            "AAA",
            'A',
            new net.minecraft.item.ItemStack(advancedWirelessKit));
    }

    protected Item findItem(String modId, String itemName) {
        return GameRegistry.findItem(modId, itemName);
    }

    protected void addShapedRecipe(net.minecraft.item.ItemStack output, Object... inputs) {
        GameRegistry.addShapedRecipe(output, inputs);
    }

    public void closeCurrentScreen() {}

    public void openPatternDetailScreen(net.minecraft.entity.player.EntityPlayer player, int index,
        java.util.List<String> inputs, java.util.List<String> outputs) {}

    public void openPatternStorageScreen(net.minecraft.entity.player.EntityPlayer player) {}
}
