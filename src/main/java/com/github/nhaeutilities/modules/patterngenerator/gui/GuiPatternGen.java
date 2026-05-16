package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketCreateCache;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketGeneratePatterns;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketPreviewRecipeCount;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketSaveFields;
import com.github.nhaeutilities.modules.shared.DebugLog;
import com.github.nhaeutilities.modules.shared.NHTextures;
import com.github.nhaeutilities.modules.shared.nei.NeiRecipeExtractionContext;

import codechicken.nei.api.IGuiContainerOverlay;

public class GuiPatternGen {

    private static final int DRAG_SELECTOR_W = 36;
    private static final int DRAG_SELECTOR_BG = 0xEE2A2A42;

    public static ModularPanel createWindow(EntityPlayer player, ItemStack held) {
        GuiPatternGenStatusBridge.clearStatus();
        GuiPatternGenStatusBridge.register(s -> {});

        DebugLog.info("[NHAE] GuiPatternGen.createWindow called: player=%s, held=%s", player, held);

        ItemStack freshHeld = player.getCurrentEquippedItem();
        DebugLog.info("[NHAE] GuiPatternGen: freshHeld=%s", freshHeld);
        if (freshHeld == null || !(freshHeld
            .getItem() instanceof com.github.nhaeutilities.modules.patterngenerator.item.ItemPatternGenerator)) {
            freshHeld = held;
            DebugLog.info("[NHAE] GuiPatternGen: using parameter held instead");
        }
        DebugLog.info(
            "[NHAE] GuiPatternGen: held NBT=%s",
            freshHeld != null && freshHeld.hasTagCompound() ? freshHeld.getTagCompound()
                .toString() : "null");

        int guiWidth = ForgeConfig.getPatternGenGuiWidth();
        int guiHeight = ForgeConfig.getPatternGenGuiHeight();

        final Runnable[] saveFunction = { () -> {} };
        ModularPanel panel = new ModularPanel("pattern_gen") {

            @Override
            public void onClose() {
                super.onClose();
            }

            @Override
            public void dispose() {
                DebugLog.info("[NHAE] GuiPatternGen panel.dispose called, state=%s", String.valueOf(getState()));
                saveFunction[0].run();
                NeiRecipeExtractionContext.instance()
                    .deactivate();
                GuiPatternGenStatusBridge.unregister();
                super.dispose();
            }
        };
        panel.background(GuiTextures.MC_BACKGROUND)
            .size(guiWidth, guiHeight);

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_gen.title", "Pattern Generator"))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(8, 8)
                        .size(guiWidth - 16, 20));

        ScrollWidget<?> scrollable = new ScrollWidget<>(new VerticalScrollData());
        scrollable.pos(8, 24)
            .size(guiWidth - 16, guiHeight - 24 - 40);

        int refY = 0;
        int fieldW = guiWidth - 16 - 80 - 12;
        int fullFieldW = guiWidth - 16 - 12;
        int inputX = 80;

        scrollable.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_gen.section.recipe", "Recipe"))
                    .style(EnumChatFormatting.BOLD)).pos(6, refY + 3));

        FilterTextFieldWidget tfRecipeMap = new FilterTextFieldWidget();
        tfRecipeMap.setDegradeEnabled(false);
        final String initialRecipeMap = getSavedField(freshHeld, PacketSaveFields.NBT_RECIPE_MAP);
        tfRecipeMap.value(new StringValue.Dynamic(() -> tfRecipeMap.getText(), v -> tfRecipeMap.setText(v)));
        tfRecipeMap.setText(initialRecipeMap);
        tfRecipeMap.pos(6, refY + 14)
            .size(fullFieldW, 14);
        tfRecipeMap.setTextColor(0xFFFFFF);
        tfRecipeMap.background(new Rectangle().setColor(0xFF1E1E30));
        tfRecipeMap.setTextAlignment(Alignment.CenterLeft);
        tfRecipeMap.setOnSave(() -> saveFunction[0].run());
        scrollable.child(tfRecipeMap);
        refY += 38;

        scrollable.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_gen.section.filter", "Filters"))
                    .style(EnumChatFormatting.BOLD)).pos(6, refY + 3));

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.output_ore", "Output ore")))
                .pos(6, refY + 14 + 3));

        FilterTextFieldWidget tfOutputOre = new FilterTextFieldWidget();
        final String initialOutputOre = getSavedField(freshHeld, PacketSaveFields.NBT_OUTPUT_ORE);
        tfOutputOre.value(new StringValue.Dynamic(() -> tfOutputOre.getText(), v -> tfOutputOre.setText(v)));
        tfOutputOre.setText(initialOutputOre);
        tfOutputOre.markLoadDegradePending();
        tfOutputOre.enableEffectiveValueTooltip();
        tfOutputOre.setOnSave(() -> saveFunction[0].run());
        tfOutputOre.pos(inputX, refY + 14)
            .size(fieldW, 14);
        tfOutputOre.setTextColor(0xFFFFFF);
        tfOutputOre.background(new Rectangle().setColor(0xFF1E1E30));
        tfOutputOre.setTextAlignment(Alignment.CenterLeft);
        scrollable.child(tfOutputOre);
        attachDragChoiceSelector(scrollable, tfOutputOre, inputX, refY + 14, fieldW);

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.input_ore", "Input ore")))
                .pos(6, refY + 32 + 3));

        FilterTextFieldWidget tfInputOre = new FilterTextFieldWidget();
        final String initialInputOre = getSavedField(freshHeld, PacketSaveFields.NBT_INPUT_ORE);
        tfInputOre.value(new StringValue.Dynamic(() -> tfInputOre.getText(), v -> tfInputOre.setText(v)));
        tfInputOre.setText(initialInputOre);
        tfInputOre.markLoadDegradePending();
        tfInputOre.enableEffectiveValueTooltip();
        tfInputOre.setOnSave(() -> saveFunction[0].run());
        tfInputOre.pos(inputX, refY + 32)
            .size(fieldW, 14);
        tfInputOre.setTextColor(0xFFFFFF);
        tfInputOre.background(new Rectangle().setColor(0xFF1E1E30));
        tfInputOre.setTextAlignment(Alignment.CenterLeft);
        scrollable.child(tfInputOre);
        attachDragChoiceSelector(scrollable, tfInputOre, inputX, refY + 32, fieldW);

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.nc_item", "NC item")))
                .pos(6, refY + 50 + 3));

        FilterTextFieldWidget tfNCItem = new FilterTextFieldWidget();
        final String initialNCItem = getSavedField(freshHeld, PacketSaveFields.NBT_NC_ITEM);
        tfNCItem.value(new StringValue.Dynamic(() -> tfNCItem.getText(), v -> tfNCItem.setText(v)));
        tfNCItem.setText(initialNCItem);
        tfNCItem.markLoadDegradePending();
        tfNCItem.enableEffectiveValueTooltip();
        tfNCItem.setOnSave(() -> saveFunction[0].run());
        tfNCItem.pos(inputX, refY + 50)
            .size(fieldW, 14);
        tfNCItem.setTextColor(0xFFFFFF);
        tfNCItem.background(new Rectangle().setColor(0xFF1E1E30));
        tfNCItem.setTextAlignment(Alignment.CenterLeft);
        scrollable.child(tfNCItem);
        attachDragChoiceSelector(scrollable, tfNCItem, inputX, refY + 50, fieldW);

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.tier", "Target tier")))
                .pos(6, refY + 68 + 3));

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.output_slots", "Output slots")))
                .pos(6, refY + 86 + 3));

        final List<String> tiers = Arrays.asList(
            "Any",
            "ULV",
            "LV",
            "MV",
            "HV",
            "EV",
            "IV",
            "LuV",
            "ZPM",
            "UV",
            "UHV",
            "UEV",
            "UIV",
            "UMV",
            "UXV",
            "MAX");
        int savedTier = getSavedInt(freshHeld, PacketSaveFields.NBT_TARGET_TIER, -1);
        final int[] currentTierIndex = new int[] { Math.max(0, Math.min(tiers.size() - 1, savedTier + 1)) };

        ButtonWidget<?> btnTier = new ButtonWidget<>();
        btnTier.pos(inputX, refY + 68)
            .size(fieldW, 14);
        btnTier.background(new Rectangle().setColor(0xFF1E1E30));
        btnTier.onMousePressed(mb -> {
            if (mb == 0) {
                currentTierIndex[0] = (currentTierIndex[0] + 1) % tiers.size();
            } else if (mb == 1) {
                currentTierIndex[0] = (currentTierIndex[0] - 1 + tiers.size()) % tiers.size();
            }
            return true;
        });
        scrollable.child(btnTier);

        scrollable.child(
            new TextWidget(IKey.dynamic(() -> EnumChatFormatting.WHITE + tiers.get(currentTierIndex[0])))
                .pos(inputX + 4, refY + 68 + 3));

        FilterTextFieldWidget tfOutputSlots = new FilterTextFieldWidget(itemStack -> "");
        tfOutputSlots.setDegradeEnabled(false);
        final String initialOutputSlots = getSavedField(freshHeld, PacketSaveFields.NBT_OUTPUT_SLOTS);
        tfOutputSlots.value(new StringValue.Dynamic(() -> tfOutputSlots.getText(), v -> tfOutputSlots.setText(v)));
        tfOutputSlots.setText(initialOutputSlots);
        tfOutputSlots.pos(inputX, refY + 86)
            .size(fieldW, 14);
        tfOutputSlots.setTextColor(0xFFFFFF);
        tfOutputSlots.background(new Rectangle().setColor(0xFF1E1E30));
        tfOutputSlots.setTextAlignment(Alignment.CenterLeft);
        tfOutputSlots.setOnSave(() -> saveFunction[0].run());
        scrollable.child(tfOutputSlots);

        scrollable.child(new TextWidget(IKey.dynamic(() -> {
            String text = tfOutputSlots.getText();
            if (text == null || text.trim()
                .isEmpty()) {
                return EnumChatFormatting.DARK_GRAY + t(
                    "nhaeutilities.gui.pattern_gen.hint.output_slots",
                    "Comma-separated 1-based output slots; blank keeps all outputs.");
            }
            String error = validateOutputSlotsFormat(text);
            if (error != null) {
                return EnumChatFormatting.RED + error;
            }
            return EnumChatFormatting.GREEN
                + t("nhaeutilities.gui.pattern_gen.hint.output_slots.valid", "Valid selection.");
        })).pos(6, refY + 104 + 3));

        refY += 120;

        scrollable.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_gen.section.blacklist", "Blacklist"))
                    .style(EnumChatFormatting.BOLD)).pos(6, refY + 3));

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.blacklist_input", "Blacklist input")))
                .pos(6, refY + 14 + 3));

        FilterTextFieldWidget tfBlacklistIn = new FilterTextFieldWidget();
        final String initialBlacklistIn = getSavedField(freshHeld, PacketSaveFields.NBT_BLACKLIST_INPUT);
        tfBlacklistIn.value(new StringValue.Dynamic(() -> tfBlacklistIn.getText(), v -> tfBlacklistIn.setText(v)));
        tfBlacklistIn.setText(initialBlacklistIn);
        tfBlacklistIn.markLoadDegradePending();
        tfBlacklistIn.enableEffectiveValueTooltip();
        tfBlacklistIn.setOnSave(() -> saveFunction[0].run());
        tfBlacklistIn.pos(inputX, refY + 14)
            .size(fieldW, 14);
        tfBlacklistIn.setTextColor(0xFFFFFF);
        tfBlacklistIn.background(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistIn.setTextAlignment(Alignment.CenterLeft);
        scrollable.child(tfBlacklistIn);
        attachDragChoiceSelector(scrollable, tfBlacklistIn, inputX, refY + 14, fieldW);

        scrollable.child(
            new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_gen.label.blacklist_output", "Blacklist output")))
                .pos(6, refY + 32 + 3));

        FilterTextFieldWidget tfBlacklistOut = new FilterTextFieldWidget();
        final String initialBlacklistOut = getSavedField(freshHeld, PacketSaveFields.NBT_BLACKLIST_OUTPUT);
        tfBlacklistOut.value(new StringValue.Dynamic(() -> tfBlacklistOut.getText(), v -> tfBlacklistOut.setText(v)));
        tfBlacklistOut.setText(initialBlacklistOut);
        tfBlacklistOut.markLoadDegradePending();
        tfBlacklistOut.enableEffectiveValueTooltip();
        tfBlacklistOut.setOnSave(() -> saveFunction[0].run());
        tfBlacklistOut.pos(inputX, refY + 32)
            .size(fieldW, 14);
        tfBlacklistOut.setTextColor(0xFFFFFF);
        tfBlacklistOut.background(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistOut.setTextAlignment(Alignment.CenterLeft);
        scrollable.child(tfBlacklistOut);
        attachDragChoiceSelector(scrollable, tfBlacklistOut, inputX, refY + 32, fieldW);

        scrollable.child(
            new TextWidget(
                IKey.str(
                    t(
                        "nhaeutilities.gui.pattern_gen.hint.regex",
                        "Select match type (ID/Ore/Name) with the button; brackets are optional."))
                    .style(EnumChatFormatting.DARK_GRAY)).pos(6, refY + 50 + 3));

        scrollable.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_gen.hint.blacklist", "* disables the field."))
                    .style(EnumChatFormatting.DARK_GRAY)).pos(6, refY + 60 + 3));

        scrollable.child(
            new TextWidget(
                IKey.str(
                    t(
                        "nhaeutilities.gui.pattern_gen.hint.blacklist_examples",
                        "Type plain text for name/ore, or [8119] for ID match. Use brackets for multi-type rules."))
                    .style(EnumChatFormatting.DARK_GRAY)).pos(6, refY + 70 + 3));

        panel.child(scrollable);

        panel.child(new TextWidget(IKey.dynamic(GuiPatternGenStatusBridge::getStatus)).pos(8, guiHeight - 12));

        int totalContentHeight = refY + 40;
        scrollable.getScrollArea()
            .getScrollY()
            .setScrollSize(totalContentHeight);

        int btnW = 57;
        int btnH = 20;
        int btnGap = 3;
        int btnStartX = (guiWidth - (btnW * 3 + btnGap * 2)) / 2;
        int btnY = guiHeight - 32;

        saveFunction[0] = () -> NetworkHandler.INSTANCE.sendToServer(
            new PacketSaveFields(
                tfRecipeMap.getText(),
                tfOutputOre.getEffectiveValue(),
                tfInputOre.getEffectiveValue(),
                tfNCItem.getEffectiveValue(),
                tfBlacklistIn.getEffectiveValue(),
                tfBlacklistOut.getEffectiveValue(),
                tfOutputSlots.getText(),
                currentTierIndex[0] - 1));

        ButtonWidget<?> btnCache = NHTextures.createButton();
        btnCache.pos(btnStartX, btnY)
            .size(btnW, btnH)
            .overlay(
                IKey.str(t("nhaeutilities.gui.pattern_gen.button.build_cache", "Cache"))
                    .shadow(false));
        btnCache.onMousePressed(mb -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketCreateCache());
            GuiPatternGenStatusBridge.setStatus("Cache build requested");
            return true;
        });
        panel.child(btnCache);

        int btnPBX = btnStartX + btnW + btnGap;
        ButtonWidget<?> btnPreview = NHTextures.createButton();
        btnPreview.pos(btnPBX, btnY)
            .size(btnW, btnH)
            .overlay(
                IKey.str(t("nhaeutilities.gui.pattern_gen.button.preview_count", "Preview"))
                    .shadow(false));
        btnPreview.onMousePressed(mb -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                GuiPatternGenStatusBridge.setStatus("Recipe map is required.");
                return true;
            }
            NetworkHandler.INSTANCE.sendToServer(
                new PacketPreviewRecipeCount(
                    tfRecipeMap.getText(),
                    tfOutputOre.getEffectiveValue(),
                    tfInputOre.getEffectiveValue(),
                    tfNCItem.getEffectiveValue(),
                    tfBlacklistIn.getEffectiveValue(),
                    tfBlacklistOut.getEffectiveValue(),
                    currentTierIndex[0] - 1));
            GuiPatternGenStatusBridge.setStatus("Preview requested");
            return true;
        });
        panel.child(btnPreview);

        int btnGBX = btnStartX + (btnW + btnGap) * 2;
        ButtonWidget<?> btnGenerate = NHTextures.createButton();
        btnGenerate.pos(btnGBX, btnY)
            .size(btnW, btnH)
            .overlay(
                IKey.str(t("nhaeutilities.gui.pattern_gen.button.generate", "Generate"))
                    .shadow(false));
        btnGenerate.onMousePressed(mb -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                GuiPatternGenStatusBridge.setStatus("Recipe map is required.");
                return true;
            }
            saveFunction[0].run();
            NetworkHandler.INSTANCE.sendToServer(
                new PacketGeneratePatterns(
                    tfRecipeMap.getText(),
                    tfOutputOre.getEffectiveValue(),
                    tfInputOre.getEffectiveValue(),
                    tfNCItem.getEffectiveValue(),
                    tfBlacklistIn.getEffectiveValue(),
                    tfBlacklistOut.getEffectiveValue(),
                    "",
                    tfOutputSlots.getText(),
                    currentTierIndex[0] - 1));
            GuiPatternGenStatusBridge.setStatus("Generation requested");
            return true;
        });
        panel.child(btnGenerate);

        NeiRecipeExtractionContext.instance()
            .activate(data -> {
                DebugLog.info(
                    "[NHAE] NEI recipe extraction callback fired: recipeMapId=%s, recipeName=%s, circuit=%s",
                    data.recipeMapId,
                    data.recipeName,
                    data.snapshot.programmingCircuit);
                if (data.recipeMapId != null && !data.recipeMapId.isEmpty()) {
                    tfRecipeMap.setText(data.recipeMapId);
                }
                fillNcItemsFromSnapshot(tfNCItem, data);
                saveFunction[0].run();
                GuiScreen current = Minecraft.getMinecraft().currentScreen;
                if (current instanceof IGuiContainerOverlay overlay) {
                    Minecraft.getMinecraft()
                        .displayGuiScreen(overlay.getFirstScreenGeneral());
                }
            });

        return panel;
    }

    private static void fillNcItemsFromSnapshot(FilterTextFieldWidget field,
        com.github.nhaeutilities.modules.shared.nei.NeiRecipeData data) {
        java.util.ArrayList<ItemStack> ncStacks = new java.util.ArrayList<>();
        String circuit = data.snapshot.programmingCircuit;
        if (circuit != null && !circuit.isEmpty()) {
            ItemStack circuitStack = resolveItemSignature(circuit);
            if (circuitStack != null) {
                ncStacks.add(circuitStack);
            }
        }
        String nonConsumables = data.snapshot.nonConsumables;
        if (nonConsumables != null && nonConsumables.length() > 2) {
            parseNonConsumableItems(nonConsumables, ncStacks);
        }
        if (ncStacks.isEmpty()) {
            return;
        }
        if (ncStacks.size() == 1) {
            field.acceptItem(ncStacks.get(0));
        } else {
            StringBuilder combined = new StringBuilder();
            for (ItemStack stack : ncStacks) {
                if (combined.length() > 0) {
                    combined.append('|');
                }
                String itemId = ExplicitFilterDropFormatter.format(stack);
                combined.append('[')
                    .append(itemId)
                    .append(']');
            }
            field.setText(combined.toString());
            field.explicitBrackets = true;
            field.clearStoredDropChoices();
            field.safeMarkForUpdate();
        }
    }

    private static ItemStack resolveItemSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return null;
        }
        int atIndex = signature.lastIndexOf('@');
        String itemName;
        int meta = 0;
        if (atIndex >= 0) {
            itemName = signature.substring(0, atIndex);
            try {
                meta = Integer.parseInt(signature.substring(atIndex + 1));
            } catch (NumberFormatException ignored) {}
        } else {
            itemName = signature;
        }
        Item item = (Item) Item.itemRegistry.getObject(itemName);
        if (item == null) {
            return null;
        }
        return new ItemStack(item, 1, meta);
    }

    private static void parseNonConsumableItems(String json, java.util.ArrayList<ItemStack> out) {
        int searchFrom = 0;
        while (true) {
            int itemIdx = json.indexOf("\"item\":\"", searchFrom);
            if (itemIdx < 0) break;
            int valueStart = itemIdx + 8;
            int valueEnd = json.indexOf('"', valueStart);
            if (valueEnd < 0) break;
            String signature = json.substring(valueStart, valueEnd);
            ItemStack stack = resolveItemSignature(signature);
            if (stack != null) {
                out.add(stack);
            }
            searchFrom = valueEnd + 1;
        }
    }

    private static void attachDragChoiceSelector(ScrollWidget<?> scrollable, FilterTextFieldWidget field, int fieldX,
        int fieldY, int fieldWidth) {
        FilterDragChoiceButtonWidget selector = new FilterDragChoiceButtonWidget(field);
        selector.pos(fieldX + fieldWidth - DRAG_SELECTOR_W, fieldY);
        selector.size(DRAG_SELECTOR_W, 14);
        selector.background(new Rectangle().setColor(DRAG_SELECTOR_BG));
        selector.overlay(
            IKey.dynamic(field::getCurrentCategoryLabel)
                .color(0xFFFFFF)
                .alignment(Alignment.Center));
        selector.addTooltipLine(
            t("nhaeutilities.gui.pattern_gen.drag_choice.tooltip.line1", "Left click: next match type") + "\n"
                + t("nhaeutilities.gui.pattern_gen.drag_choice.tooltip.line2", "Right click: previous match type"));
        scrollable.child(selector);
    }

    private static String getSavedField(ItemStack stack, String key) {
        if (stack == null || !stack.hasTagCompound()) {
            return "";
        }
        return stack.getTagCompound()
            .hasKey(key)
                ? stack.getTagCompound()
                    .getString(key)
                : "";
    }

    private static int getSavedInt(ItemStack stack, String key, int def) {
        if (stack == null || !stack.hasTagCompound()) {
            return def;
        }
        return stack.getTagCompound()
            .hasKey(key)
                ? stack.getTagCompound()
                    .getInteger(key)
                : def;
    }

    private static String t(String key, String fallback, Object... args) {
        return com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil.trOr(key, fallback, args);
    }

    private static String validateOutputSlotsFormat(String rawSelection) {
        if (rawSelection == null || rawSelection.trim()
            .isEmpty()) {
            return null;
        }
        String[] tokens = rawSelection.split(",", -1);
        Set<Integer> slots = new HashSet<Integer>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                return t(
                    "nhaeutilities.gui.pattern_gen.error.output_slots_invalid",
                    "Invalid: use comma-separated positive integers");
            }
            final int slot;
            try {
                slot = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                return t(
                    "nhaeutilities.gui.pattern_gen.error.output_slots_invalid",
                    "Invalid: use comma-separated positive integers");
            }
            if (slot <= 0) {
                return t(
                    "nhaeutilities.gui.pattern_gen.error.output_slots_invalid",
                    "Invalid: use comma-separated positive integers");
            }
            if (!slots.add(slot)) {
                return t(
                    "nhaeutilities.gui.pattern_gen.error.output_slots_duplicate",
                    "Warning: duplicate slot %s",
                    slot);
            }
        }
        return null;
    }
}
