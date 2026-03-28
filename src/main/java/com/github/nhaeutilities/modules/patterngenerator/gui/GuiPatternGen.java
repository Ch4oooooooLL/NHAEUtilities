package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.awt.Desktop;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.config.ReplacementConfig;
import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketCreateCache;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketGeneratePatterns;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketPreviewRecipeCount;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketSaveFields;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public class GuiPatternGen {

    private static final int DRAG_SELECTOR_W = 36;
    private static final int DRAG_SELECTOR_BG = 0xEE2A2A42;

    public static ModularWindow createWindow(UIBuildContext buildContext, ItemStack held) {
        GuiPatternGenStatusBridge.clearStatus();

        int guiWidth = ForgeConfig.getPatternGenGuiWidth();
        int guiHeight = ForgeConfig.getPatternGenGuiHeight();
        ModularWindow.Builder builder = ModularWindow.builder(guiWidth, guiHeight);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget(EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_gen.title", "Pattern Generator"));
        titleText.setScale(1.2f);
        titleText.setSize(guiWidth - 16, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 24);
        scrollable.setSize(guiWidth - 16, guiHeight - 24 - 40);

        int refY = 0;
        int fieldW = guiWidth - 16 - 80 - 12;
        int fullFieldW = guiWidth - 16 - 12;
        int inputX = 80;

        TextWidget labelRecipe = new TextWidget(EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_gen.section.recipe", "Recipe"));
        labelRecipe.setPos(6, refY + 3);
        scrollable.widget(labelRecipe);

        FilterTextFieldWidget tfRecipeMap = new FilterTextFieldWidget();
        tfRecipeMap.setText(getSavedField(held, PacketSaveFields.NBT_RECIPE_MAP));
        tfRecipeMap.setPos(6, refY + 14);
        tfRecipeMap.setSize(fullFieldW, 14);
        tfRecipeMap.setTextColor(0xFFFFFF);
        tfRecipeMap.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfRecipeMap.setTextAlignment(Alignment.CenterLeft);
        scrollable.widget(tfRecipeMap);
        refY += 38;

        TextWidget labelFilter = new TextWidget(EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_gen.section.filter", "Filters"));
        labelFilter.setPos(6, refY + 3);
        scrollable.widget(labelFilter);

        TextWidget labelOutOre = new TextWidget(t("nhaeutilities.gui.pattern_gen.label.output_ore", "Output ore"));
        labelOutOre.setPos(6, refY + 14 + 3);
        scrollable.widget(labelOutOre);

        FilterTextFieldWidget tfOutputOre = new FilterTextFieldWidget();
        tfOutputOre.setText(getSavedField(held, PacketSaveFields.NBT_OUTPUT_ORE));
        tfOutputOre.setPos(inputX, refY + 14);
        tfOutputOre.setSize(fieldW, 14);
        tfOutputOre.setTextColor(0xFFFFFF);
        tfOutputOre.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfOutputOre.setTextAlignment(Alignment.CenterLeft);
        scrollable.widget(tfOutputOre);
        attachDragChoiceSelector(scrollable, tfOutputOre, inputX, refY + 14, fieldW);

        TextWidget labelInOre = new TextWidget(t("nhaeutilities.gui.pattern_gen.label.input_ore", "Input ore"));
        labelInOre.setPos(6, refY + 32 + 3);
        scrollable.widget(labelInOre);

        FilterTextFieldWidget tfInputOre = new FilterTextFieldWidget();
        tfInputOre.setText(getSavedField(held, PacketSaveFields.NBT_INPUT_ORE));
        tfInputOre.setPos(inputX, refY + 32);
        tfInputOre.setSize(fieldW, 14);
        tfInputOre.setTextColor(0xFFFFFF);
        tfInputOre.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfInputOre.setTextAlignment(Alignment.CenterLeft);
        scrollable.widget(tfInputOre);
        attachDragChoiceSelector(scrollable, tfInputOre, inputX, refY + 32, fieldW);

        TextWidget labelNC = new TextWidget(t("nhaeutilities.gui.pattern_gen.label.nc_item", "NC item"));
        labelNC.setPos(6, refY + 50 + 3);
        scrollable.widget(labelNC);

        FilterTextFieldWidget tfNCItem = new FilterTextFieldWidget();
        tfNCItem.setText(getSavedField(held, PacketSaveFields.NBT_NC_ITEM));
        tfNCItem.setPos(inputX, refY + 50);
        tfNCItem.setSize(fieldW, 14);
        tfNCItem.setTextColor(0xFFFFFF);
        tfNCItem.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfNCItem.setTextAlignment(Alignment.CenterLeft);
        scrollable.widget(tfNCItem);
        attachDragChoiceSelector(scrollable, tfNCItem, inputX, refY + 50, fieldW);

        TextWidget labelTier = new TextWidget(t("nhaeutilities.gui.pattern_gen.label.tier", "Target tier"));
        labelTier.setPos(6, refY + 68 + 3);
        scrollable.widget(labelTier);

        final List<String> tiers = Arrays.asList("Any", "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV",
            "UHV", "UEV", "UIV", "UMV", "UXV", "MAX");
        int savedTier = getSavedInt(held, PacketSaveFields.NBT_TARGET_TIER, -1);
        final int[] currentTierIndex = new int[] { Math.max(0, Math.min(tiers.size() - 1, savedTier + 1)) };

        ButtonWidget btnTier = new ButtonWidget();
        btnTier.setSynced(false, false);
        btnTier.setPos(inputX, refY + 68);
        btnTier.setSize(fieldW, 14);
        btnTier.setBackground(new Rectangle().setColor(0xFF1E1E30));

        TextWidget btnTierText = new TextWidget("");
        btnTierText.setStringSupplier(() -> EnumChatFormatting.WHITE + tiers.get(currentTierIndex[0]));
        btnTierText.setPos(inputX + 4, refY + 68 + 3);
        btnTier.setOnClick((clickData, widget) -> {
            if (clickData.mouseButton == 0) {
                currentTierIndex[0] = (currentTierIndex[0] + 1) % tiers.size();
            } else if (clickData.mouseButton == 1) {
                currentTierIndex[0] = (currentTierIndex[0] - 1 + tiers.size()) % tiers.size();
            }
        });
        scrollable.widget(btnTier);
        scrollable.widget(btnTierText);

        refY += 84;

        TextWidget labelBL = new TextWidget(EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_gen.section.blacklist", "Blacklist"));
        labelBL.setPos(6, refY + 3);
        scrollable.widget(labelBL);

        TextWidget labelBLIn = new TextWidget(t("nhaeutilities.gui.pattern_gen.label.blacklist_input", "Blacklist input"));
        labelBLIn.setPos(6, refY + 14 + 3);
        scrollable.widget(labelBLIn);

        FilterTextFieldWidget tfBlacklistIn = new FilterTextFieldWidget();
        tfBlacklistIn.setText(getSavedField(held, PacketSaveFields.NBT_BLACKLIST_INPUT));
        tfBlacklistIn.setPos(inputX, refY + 14);
        tfBlacklistIn.setSize(fieldW, 14);
        tfBlacklistIn.setTextColor(0xFFFFFF);
        tfBlacklistIn.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistIn.setTextAlignment(Alignment.CenterLeft);
        scrollable.widget(tfBlacklistIn);
        attachDragChoiceSelector(scrollable, tfBlacklistIn, inputX, refY + 14, fieldW);

        TextWidget labelBLOut = new TextWidget(t("nhaeutilities.gui.pattern_gen.label.blacklist_output", "Blacklist output"));
        labelBLOut.setPos(6, refY + 32 + 3);
        scrollable.widget(labelBLOut);

        FilterTextFieldWidget tfBlacklistOut = new FilterTextFieldWidget();
        tfBlacklistOut.setText(getSavedField(held, PacketSaveFields.NBT_BLACKLIST_OUTPUT));
        tfBlacklistOut.setPos(inputX, refY + 32);
        tfBlacklistOut.setSize(fieldW, 14);
        tfBlacklistOut.setTextColor(0xFFFFFF);
        tfBlacklistOut.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistOut.setTextAlignment(Alignment.CenterLeft);
        scrollable.widget(tfBlacklistOut);
        attachDragChoiceSelector(scrollable, tfBlacklistOut, inputX, refY + 32, fieldW);

        TextWidget regexHint = new TextWidget(EnumChatFormatting.DARK_GRAY + t(
            "nhaeutilities.gui.pattern_gen.hint.regex",
            "Regex, ore dict, or display name tokens are supported."));
        regexHint.setPos(6, refY + 50 + 3);
        scrollable.widget(regexHint);

        TextWidget blacklistHint = new TextWidget(EnumChatFormatting.DARK_GRAY + t(
            "nhaeutilities.gui.pattern_gen.hint.blacklist",
            "Blacklist fields exclude matching stacks."));
        blacklistHint.setPos(6, refY + 60 + 3);
        scrollable.widget(blacklistHint);

        TextWidget blacklistExamples = new TextWidget(EnumChatFormatting.DARK_GRAY + t(
            "nhaeutilities.gui.pattern_gen.hint.blacklist_examples",
            "Examples: [itemId], (oreName), {Display Name}"));
        blacklistExamples.setPos(6, refY + 70 + 3);
        scrollable.widget(blacklistExamples);

        refY += 82;

        int loadedRuleCount = ReplacementConfig.load();

        TextWidget labelRep = new TextWidget(EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_gen.section.replacements", "Replacement rules"));
        labelRep.setPos(6, refY + 3);
        scrollable.widget(labelRep);

        TextWidget labelRepCount = new TextWidget(t("nhaeutilities.gui.pattern_gen.replacements.count", "Loaded rules: %s", loadedRuleCount));
        labelRepCount.setPos(6, refY + 20);
        scrollable.widget(labelRepCount);

        int btnCfgX = guiWidth - 16 - 6 - 80;
        int btnCfgY = refY + 14;
        ButtonWidget btnConfig = new ButtonWidget();
        btnConfig.setSynced(false, false);
        btnConfig.setPos(btnCfgX, btnCfgY);
        btnConfig.setSize(80, 20);
        btnConfig.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnConfigText = new TextWidget(t("nhaeutilities.gui.pattern_gen.button.open_config", "Open config"));
        btnConfigText.setPos(btnCfgX + 16, btnCfgY + 6);
        btnConfig.setOnClick((cd, w) -> {
            try {
                File file = ReplacementConfig.getConfigFile();
                if (file != null && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop()
                        .open(file);
                }
            } catch (Exception ignored) {}
        });
        scrollable.widget(btnConfig);
        scrollable.widget(btnConfigText);

        builder.widget(scrollable);

        TextWidget statusWidget = new TextWidget("");
        statusWidget.setStringSupplier(GuiPatternGenStatusBridge::getStatus);
        statusWidget.setPos(8, guiHeight - 12);
        builder.widget(statusWidget);

        int btnW = 57;
        int btnH = 20;
        int btnGap = 3;
        int btnStartX = (guiWidth - (btnW * 3 + btnGap * 2)) / 2;
        int btnY = guiHeight - 32;

        Runnable saveFunction = () -> NetworkHandler.INSTANCE.sendToServer(
            new PacketSaveFields(
                tfRecipeMap.getText(),
                tfOutputOre.getText(),
                tfInputOre.getText(),
                tfNCItem.getText(),
                tfBlacklistIn.getText(),
                tfBlacklistOut.getText(),
                "",
                currentTierIndex[0] - 1));

        ButtonWidget btnCache = new ButtonWidget();
        btnCache.setSynced(false, false);
        btnCache.setPos(btnStartX, btnY);
        btnCache.setSize(btnW, btnH);
        btnCache.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnCacheText = new TextWidget(t("nhaeutilities.gui.pattern_gen.button.build_cache", "Cache"));
        btnCacheText.setPos(btnStartX + 10, btnY + 6);
        btnCache.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketCreateCache());
            GuiPatternGenStatusBridge.setStatus("Cache build requested");
        });
        builder.widget(btnCache);
        builder.widget(btnCacheText);

        int btnPBX = btnStartX + btnW + btnGap;
        ButtonWidget btnPreview = new ButtonWidget();
        btnPreview.setSynced(false, false);
        btnPreview.setPos(btnPBX, btnY);
        btnPreview.setSize(btnW, btnH);
        btnPreview.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnPreviewText = new TextWidget(t("nhaeutilities.gui.pattern_gen.button.preview_count", "Preview"));
        btnPreviewText.setPos(btnPBX + 10, btnY + 6);
        btnPreview.setOnClick((cd, w) -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                GuiPatternGenStatusBridge.setStatus("Recipe map is required.");
                return;
            }
            saveFunction.run();
            NetworkHandler.INSTANCE.sendToServer(
                new PacketPreviewRecipeCount(
                    tfRecipeMap.getText(),
                    tfOutputOre.getText(),
                    tfInputOre.getText(),
                    tfNCItem.getText(),
                    tfBlacklistIn.getText(),
                    tfBlacklistOut.getText(),
                    currentTierIndex[0] - 1));
            GuiPatternGenStatusBridge.setStatus("Preview requested");
        });
        builder.widget(btnPreview);
        builder.widget(btnPreviewText);

        int btnGBX = btnStartX + (btnW + btnGap) * 2;
        ButtonWidget btnGenerate = new ButtonWidget();
        btnGenerate.setSynced(false, false);
        btnGenerate.setPos(btnGBX, btnY);
        btnGenerate.setSize(btnW, btnH);
        btnGenerate.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnGenerateText = new TextWidget(t("nhaeutilities.gui.pattern_gen.button.generate", "Generate"));
        btnGenerateText.setPos(btnGBX + 6, btnY + 6);
        btnGenerate.setOnClick((cd, w) -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                GuiPatternGenStatusBridge.setStatus("Recipe map is required.");
                return;
            }
            saveFunction.run();
            NetworkHandler.INSTANCE.sendToServer(
                new PacketGeneratePatterns(
                    tfRecipeMap.getText(),
                    tfOutputOre.getText(),
                    tfInputOre.getText(),
                    tfNCItem.getText(),
                    tfBlacklistIn.getText(),
                    tfBlacklistOut.getText(),
                    "",
                    currentTierIndex[0] - 1));
            GuiPatternGenStatusBridge.setStatus("Generation requested");
        });
        builder.widget(btnGenerate);
        builder.widget(btnGenerateText);

        buildContext.addCloseListener(saveFunction);
        return builder.build();
    }

    private static void attachDragChoiceSelector(Scrollable scrollable, FilterTextFieldWidget field, int fieldX,
        int fieldY, int fieldWidth) {
        final ExplicitFilterDropFormatter.DropChoices[] currentChoices = new ExplicitFilterDropFormatter.DropChoices[] {
            ExplicitFilterDropFormatter.DropChoices.empty() };
        final int[] currentIndex = new int[] { -1 };

        FilterDragChoiceButtonWidget selector = new FilterDragChoiceButtonWidget(field);
        selector.setSynced(false, false);
        selector.setPos(fieldX + fieldWidth - DRAG_SELECTOR_W, fieldY);
        selector.setSize(DRAG_SELECTOR_W, 14);
        selector.setEnabled(widget -> hasAlternativeChoices(currentChoices[0]));
        selector.setBackground(() -> buildSelectorBackground(currentChoices[0], currentIndex[0]));
        selector.addTooltip("Cycle drag choices");
        selector.setOnClick((clickData, widget) -> {
            if (!hasAlternativeChoices(currentChoices[0])) {
                return;
            }

            int direction = clickData.mouseButton == 1 ? -1 : 1;
            currentIndex[0] = cycleIndex(currentIndex[0], currentChoices[0].size(), direction);
            field.applyDropChoice(currentChoices[0].getOptions()
                .get(currentIndex[0]));
        });
        scrollable.widget(selector);

        field.setDropChoicesListener(choices -> {
            currentChoices[0] = choices != null ? choices : ExplicitFilterDropFormatter.DropChoices.empty();
            currentIndex[0] = currentChoices[0].getDefaultIndex();
        });
    }

    private static boolean hasAlternativeChoices(ExplicitFilterDropFormatter.DropChoices choices) {
        return choices != null && choices.size() > 1;
    }

    private static int cycleIndex(int currentIndex, int size, int direction) {
        if (size <= 0) {
            return -1;
        }

        int safeCurrent = currentIndex >= 0 ? currentIndex : 0;
        int next = (safeCurrent + direction) % size;
        return next < 0 ? next + size : next;
    }

    private static IDrawable[] buildSelectorBackground(ExplicitFilterDropFormatter.DropChoices choices,
        int currentIndex) {
        return new IDrawable[] { new Rectangle().setColor(DRAG_SELECTOR_BG),
            new Text(resolveChoiceLabel(choices, currentIndex)).color(0xFFFFFF)
                .alignment(Alignment.Center) };
    }

    private static String resolveChoiceLabel(ExplicitFilterDropFormatter.DropChoices choices, int currentIndex) {
        if (choices == null || choices.isEmpty()) {
            return "";
        }

        int safeIndex = currentIndex;
        if (safeIndex < 0 || safeIndex >= choices.size()) {
            safeIndex = choices.getDefaultIndex();
        }
        if (safeIndex < 0 || safeIndex >= choices.size()) {
            return "";
        }

        ExplicitFilterDropFormatter.DropChoice choice = choices.getOptions()
            .get(safeIndex);
        switch (choice.getSource()) {
            case ITEM_ID:
                return "ID";
            case ORE_DICT:
                return "Ore";
            case DISPLAY_NAME:
                return "Name";
            case CUSTOM:
            default:
                return "Custom";
        }
    }

    private static String getSavedField(ItemStack stack, String key) {
        if (stack == null || !stack.hasTagCompound()) {
            return "";
        }
        return stack.getTagCompound()
            .hasKey(key) ? stack.getTagCompound()
                .getString(key) : "";
    }

    private static int getSavedInt(ItemStack stack, String key, int def) {
        if (stack == null || !stack.hasTagCompound()) {
            return def;
        }
        return stack.getTagCompound()
            .hasKey(key) ? stack.getTagCompound()
                .getInteger(key) : def;
    }

    private static String t(String key, String fallback, Object... args) {
        return com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil.trOr(key, fallback, args);
    }
}
