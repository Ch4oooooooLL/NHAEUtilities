package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketRecipeConflictBatch;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketResolveConflictsBatch;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patterngenerator.util.ItemStackUtil;
import com.github.nhaeutilities.modules.shared.NHTextures;
import com.github.nhaeutilities.modules.shared.animation.ScreenHelper;

public class GuiRecipePicker {

    private static final int SELECT_BTN_W = 54;
    private static volatile ClientBatchState activeState;

    public static void openBatch(PacketRecipeConflictBatch message) {
        if (message == null) {
            return;
        }

        if (activeState != null && !isRecipePickerOpen()) {
            activeState = null;
        }

        if (activeState != null && activeState.applyPacket(message)) {
            return;
        }

        int[] priorSelections = activeState != null ? activeState.selections : null;
        ClientBatchState state = ClientBatchState.from(message, priorSelections);
        if (state == null) {
            return;
        }
        activeState = state;
        openStateWindow(state);
    }

    private static boolean isRecipePickerOpen() {
        ModularScreen current = ModularScreen.getCurrent();
        return current != null && "recipe_picker".equals(current.getName());
    }

    private static void openStateWindow(ClientBatchState state) {
        ModularPanel panel = createWindow(Minecraft.getMinecraft().thePlayer, state);
        ScreenHelper.open(panel);
    }

    public static ModularPanel createWindow(EntityPlayer player, ClientBatchState state) {
        int rowCapacity = state.rowCapacity;
        net.minecraft.client.gui.ScaledResolution res = new net.minecraft.client.gui.ScaledResolution(
            Minecraft.getMinecraft(),
            Minecraft.getMinecraft().displayWidth,
            Minecraft.getMinecraft().displayHeight);
        int maxH = Minecraft.getMinecraft().displayHeight / res.getScaleFactor() - 20;
        int guiWidth = ForgeConfig.getRecipePickerGuiWidth();
        int minHeight = ForgeConfig.getRecipePickerMinHeight();
        int idealHeight = ForgeConfig.getRecipePickerIdealHeight();
        int guiHeight = Math.min(maxH, Math.max(minHeight, idealHeight));
        int rowHeight = ForgeConfig.getRecipePickerRowHeight();
        int rowGap = ForgeConfig.getRecipePickerRowGap();

        ModularPanel panel = new ModularPanel("recipe_picker") {

            @Override
            public void onClose() {
                super.onClose();
                if (activeState == state) {
                    activeState = null;
                }
            }
        };
        panel.size(guiWidth, guiHeight);
        panel.background(GuiTextures.MC_BACKGROUND);

        TextWidget titleText = new TextWidget(
            IKey.dynamic(() -> EnumChatFormatting.BOLD + trimToPixelWidth(buildTitleText(state), guiWidth - 20)));
        titleText.scale(1.2f);
        titleText.size(guiWidth - 16, 20);
        titleText.pos(8, 8);
        titleText.shadow(false);
        panel.child(titleText);

        final int topY = 34;
        final int bottomBarH = 28;
        final int contentH = guiHeight - topY - bottomBarH;
        final int leftW = 218;
        final int gap = 6;
        final int rightX = 8 + leftW + gap;
        final int rightW = guiWidth - rightX - 8;

        TextWidget listTitle = new TextWidget(
            IKey.dynamic(
                () -> EnumChatFormatting.BOLD + t(
                    "nhaeutilities.gui.recipe_picker.list_title",
                    "Candidates: %s",
                    state.getCurrentRecipes()
                        .size())));
        listTitle.pos(8, topY - 10);
        listTitle.shadow(false);
        panel.child(listTitle);

        TextWidget detailTitle = new TextWidget(
            IKey.str(EnumChatFormatting.BOLD + t("nhaeutilities.gui.recipe_picker.detail_title", "Recipe detail")));
        detailTitle.pos(rightX, topY - 10);
        detailTitle.shadow(false);
        panel.child(detailTitle);

        ScrollWidget<?> candidateList = new ScrollWidget<>(new VerticalScrollData());
        candidateList.pos(8, topY);
        candidateList.size(leftW, contentH);

        int previewBtnW = leftW - 6 - SELECT_BTN_W - 4;

        for (int i = 0; i < rowCapacity; i++) {
            final int recipeIndex = i;
            int rowY = i * (rowHeight + rowGap);

            ButtonWidget<?> previewBtn = NHTextures.createButton();
            previewBtn.pos(2, rowY + 1);
            previewBtn.size(previewBtnW, rowHeight);
            previewBtn.setEnabledIf(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size())
                    && !state.awaitingServer);
            previewBtn.onMousePressed(mb -> {
                if (state.awaitingServer || state.inputLocked) {
                    return true;
                }
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                if (recipeIndex < 0 || recipeIndex >= currentRecipes.size()) {
                    return true;
                }
                state.selectedRecipeIndex = recipeIndex;
                state.statusText = EnumChatFormatting.DARK_GRAY
                    + t("nhaeutilities.gui.recipe_picker.status.previewed", "Previewed row %s", recipeIndex + 1);
                return true;
            });
            candidateList.child(previewBtn);

            TextWidget rowTitle = new TextWidget(IKey.dynamic(() -> {
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                if (recipeIndex < 0 || recipeIndex >= currentRecipes.size()) {
                    return "";
                }
                RecipeEntry recipe = currentRecipes.get(recipeIndex);
                return formatCandidateTitle(recipe, recipeIndex, state.selectedRecipeIndex == recipeIndex);
            }));
            rowTitle.setEnabledIf(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size()));
            rowTitle.pos(6, rowY + 6);
            rowTitle.shadow(false);
            candidateList.child(rowTitle);

            TextWidget rowPreview = new TextWidget(IKey.dynamic(() -> {
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                if (recipeIndex < 0 || recipeIndex >= currentRecipes.size()) {
                    return "";
                }
                RecipeEntry recipe = currentRecipes.get(recipeIndex);
                return formatInputPreview(recipe, state.selectedRecipeIndex == recipeIndex);
            }));
            rowPreview.setEnabledIf(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size()));
            rowPreview.pos(6, rowY + 18);
            rowPreview.shadow(false);
            candidateList.child(rowPreview);

            int selectBtnX = 2 + previewBtnW + 4;
            ButtonWidget<?> selectBtn = NHTextures.createButton();
            selectBtn.pos(selectBtnX, rowY + 1);
            selectBtn.size(SELECT_BTN_W, rowHeight);
            selectBtn.overlay(
                IKey.str(EnumChatFormatting.BLACK + t("nhaeutilities.gui.recipe_picker.button.select", "Select"))
                    .shadow(false));
            selectBtn.setEnabledIf(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size())
                    && !state.awaitingServer);
            selectBtn.onMousePressed(mb -> {
                if (state.awaitingServer || state.inputLocked) {
                    return true;
                }
                state.inputLocked = true;
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                int chosenIndex = resolveChosenIndex(recipeIndex, state.selectedRecipeIndex, currentRecipes.size());
                if (chosenIndex < 0) {
                    state.statusText = EnumChatFormatting.RED
                        + t("nhaeutilities.gui.recipe_picker.status.no_candidate", "No selectable candidate.");
                    state.inputLocked = false;
                    return true;
                }

                state.selectedRecipeIndex = chosenIndex;
                state.selections[state.localIndex] = chosenIndex;

                int batchSize = state.recipeGroups.size();
                if (state.localIndex < batchSize - 1) {
                    state.localIndex++;
                    state.selectedRecipeIndex = state.getCurrentRecipes()
                        .isEmpty() ? -1 : 0;
                    state.statusText = buildDefaultStatusText(state);
                    state.inputLocked = false;
                    return true;
                }

                int[] payload = buildSubmittedSelections(state.selections, batchSize);
                state.inputLocked = false;
                NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflictsBatch(state.startIndex, false, payload));
                boolean isFinalConflict = state.currentConflictIndex() >= state.totalConflicts;
                if (isFinalConflict) {
                    state.statusText = EnumChatFormatting.YELLOW
                        + t("nhaeutilities.gui.recipe_picker.status.final_submitted", "Selections submitted.");
                    activeState = null;
                    ClientGUI.close();
                    return true;
                }
                state.awaitingServer = true;
                state.statusText = EnumChatFormatting.YELLOW
                    + t("nhaeutilities.gui.recipe_picker.status.batch_submitted", "Batch submitted.");
                return true;
            });
            candidateList.child(selectBtn);
        }

        candidateList.getScrollArea()
            .getScrollY()
            .setScrollSize(rowCapacity * (rowHeight + rowGap));
        panel.child(candidateList);

        panel.child(
            new Rectangle().setColor(0xD0141422)
                .asWidget()
                .pos(rightX, topY)
                .size(rightW, contentH));

        ScrollWidget<?> detailScroll = new ScrollWidget<>(new VerticalScrollData());
        detailScroll.pos(rightX + 6, topY + 6);
        detailScroll.size(rightW - 12, contentH - 12);

        final int maxDetailLines = ForgeConfig.getRecipePickerMaxDetailLines();
        for (int lineIndex = 0; lineIndex < maxDetailLines; lineIndex++) {
            final int idx = lineIndex;
            TextWidget line = new TextWidget(
                IKey.dynamic(() -> getDetailLine(state.getCurrentRecipes(), state.selectedRecipeIndex, idx)));
            line.pos(2, idx * 10);
            line.shadow(false);
            detailScroll.child(line);
        }
        detailScroll.getScrollArea()
            .getScrollY()
            .setScrollSize(maxDetailLines * 10);
        panel.child(detailScroll);

        TextWidget statusWidget = new TextWidget(IKey.dynamic(() -> trimToPixelWidth(state.statusText, guiWidth - 16)));
        statusWidget.pos(8, guiHeight - 11);
        statusWidget.shadow(false);
        panel.child(statusWidget);

        final int cancelBtnW = 54;
        final int cancelBtnH = 12;
        final int cancelBtnX = guiWidth - 8 - cancelBtnW;
        final int cancelBtnY = guiHeight - 24;

        ButtonWidget<?> cancelBtn = NHTextures.createButton();
        cancelBtn.pos(cancelBtnX, cancelBtnY);
        cancelBtn.size(cancelBtnW, cancelBtnH);
        String cancelText = t("nhaeutilities.gui.recipe_picker.button.cancel", "Cancel");
        cancelBtn.overlay(
            IKey.str(EnumChatFormatting.BLACK + cancelText)
                .shadow(false));
        cancelBtn.setEnabledIf(widget -> !state.awaitingServer && !state.inputLocked);
        cancelBtn.onMousePressed(mb -> {
            if (state.awaitingServer || state.inputLocked) {
                return true;
            }
            state.inputLocked = true;
            NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflictsBatch(state.startIndex, true, new int[0]));
            activeState = null;
            ClientGUI.close();
            return true;
        });
        panel.child(cancelBtn);

        return panel;
    }

    private static String formatCandidateTitle(RecipeEntry recipe, int index, boolean selected) {
        String prefix = selected ? EnumChatFormatting.DARK_BLUE + ">> " : EnumChatFormatting.DARK_GRAY + "   ";
        String displayName = trimText(getPrimaryOutputName(recipe), 18);
        return prefix + EnumChatFormatting.DARK_GRAY + "#" + (index + 1) + " " + displayName;
    }

    private static String formatInputPreview(RecipeEntry recipe, boolean selected) {
        String color = selected ? EnumChatFormatting.DARK_AQUA.toString() : EnumChatFormatting.DARK_GRAY.toString();
        return color + t(
            "nhaeutilities.gui.recipe_picker.preview.input",
            "Inputs: %s",
            trimText(buildInputPreview(recipe, 2), 24));
    }

    private static String buildInputPreview(RecipeEntry recipe, int maxParts) {
        if (recipe == null) {
            return t("nhaeutilities.gui.common.none", "None");
        }
        List<String> parts = new ArrayList<String>();

        if (recipe.inputs != null) {
            for (ItemStack input : recipe.inputs) {
                if (input == null) {
                    continue;
                }
                parts.add(safeText(ItemStackUtil.getSafeDisplayName(input)));
                if (parts.size() >= maxParts) {
                    break;
                }
            }
        }

        if (parts.size() < maxParts && recipe.fluidInputs != null) {
            for (FluidStack fluid : recipe.fluidInputs) {
                if (fluid == null || fluid.getFluid() == null) {
                    continue;
                }
                parts.add(safeText(fluid.getLocalizedName()) + " " + Math.max(0, fluid.amount) + "L");
                if (parts.size() >= maxParts) {
                    break;
                }
            }
        }

        int totalCount = countItemStacks(recipe.inputs) + countFluidStacks(recipe.fluidInputs);
        if (parts.isEmpty()) {
            return t("nhaeutilities.gui.common.none", "None");
        }
        String preview = String.join(", ", parts);
        if (totalCount > parts.size()) {
            preview += ", ...";
        }
        return preview;
    }

    private static String getDetailLine(List<RecipeEntry> recipes, int selectedIndex, int lineIndex) {
        RecipeEntry selected = getSelectedRecipe(recipes, selectedIndex);
        List<String> lines = buildDetailLines(selected);
        return lineIndex >= 0 && lineIndex < lines.size() ? lines.get(lineIndex) : "";
    }

    private static RecipeEntry getSelectedRecipe(List<RecipeEntry> recipes, int selectedIndex) {
        if (recipes == null) {
            return null;
        }
        if (selectedIndex < 0 || selectedIndex >= recipes.size()) {
            return null;
        }
        return recipes.get(selectedIndex);
    }

    private static List<String> buildDetailLines(RecipeEntry recipe) {
        ArrayList<String> lines = new ArrayList<String>();
        if (recipe == null) {
            lines.add(
                EnumChatFormatting.RED
                    + t("nhaeutilities.gui.recipe_picker.detail.not_selected", "No recipe selected."));
            return lines;
        }

        lines.add(
            EnumChatFormatting.AQUA + ""
                + EnumChatFormatting.BOLD
                + t("nhaeutilities.gui.recipe_picker.detail.meta", "Recipe info"));
        lines.add(
            EnumChatFormatting.WHITE + t(
                "nhaeutilities.gui.recipe_picker.detail.machine",
                "Machine: %s",
                trimText(safeText(recipe.machineDisplayName), 24)));
        lines.add(
            EnumChatFormatting.WHITE + t(
                "nhaeutilities.gui.recipe_picker.detail.recipe_map",
                "Recipe map: %s",
                trimText(safeText(recipe.recipeMapId), 24)));
        lines.add(
            EnumChatFormatting.WHITE
                + t("nhaeutilities.gui.recipe_picker.detail.duration", "Duration: %s", recipe.duration));
        lines.add(EnumChatFormatting.WHITE + "EU/t: " + recipe.euPerTick);
        lines.add("");

        appendItemSection(
            lines,
            t("nhaeutilities.gui.recipe_picker.detail.input_items", "Input items"),
            recipe.inputs,
            20);
        appendFluidSection(
            lines,
            t("nhaeutilities.gui.recipe_picker.detail.input_fluids", "Input fluids"),
            recipe.fluidInputs,
            20);
        appendItemSection(
            lines,
            t("nhaeutilities.gui.recipe_picker.detail.output_items", "Output items"),
            recipe.outputs,
            20);
        appendFluidSection(
            lines,
            t("nhaeutilities.gui.recipe_picker.detail.output_fluids", "Output fluids"),
            recipe.fluidOutputs,
            20);
        appendItemSection(
            lines,
            t("nhaeutilities.gui.recipe_picker.detail.special_items", "Special items"),
            recipe.specialItems,
            20);
        return lines;
    }

    private static void appendItemSection(List<String> lines, String title, ItemStack[] stacks, int maxLen) {
        int count = countItemStacks(stacks);
        lines.add(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + title + " (" + count + ")");
        if (count == 0) {
            lines.add(EnumChatFormatting.GRAY + " - " + t("nhaeutilities.gui.common.none", "None"));
            lines.add("");
            return;
        }

        for (ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }
            String name = safeText(ItemStackUtil.getSafeDisplayName(stack));
            int amount = stack.stackSize;
            lines.add(
                EnumChatFormatting.GRAY + " - " + EnumChatFormatting.WHITE + trimText(name, maxLen) + " x" + amount);
        }
        lines.add("");
    }

    private static void appendFluidSection(List<String> lines, String title, FluidStack[] fluids, int maxLen) {
        int count = countFluidStacks(fluids);
        lines.add(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + title + " (" + count + ")");
        if (count == 0) {
            lines.add(EnumChatFormatting.GRAY + " - " + t("nhaeutilities.gui.common.none", "None"));
            lines.add("");
            return;
        }

        for (FluidStack fluid : fluids) {
            if (fluid == null || fluid.getFluid() == null) {
                continue;
            }
            String name = safeText(fluid.getLocalizedName());
            lines.add(
                EnumChatFormatting.GRAY + " - "
                    + EnumChatFormatting.AQUA
                    + trimText(name, maxLen)
                    + EnumChatFormatting.GRAY
                    + " "
                    + Math.max(0, fluid.amount)
                    + "L");
        }
        lines.add("");
    }

    private static String getPrimaryOutputName(RecipeEntry recipe) {
        if (recipe == null) {
            return t("nhaeutilities.gui.common.unknown", "Unknown");
        }
        if (recipe.outputs != null) {
            for (ItemStack out : recipe.outputs) {
                if (out != null) {
                    return safeText(ItemStackUtil.getSafeDisplayName(out));
                }
            }
        }
        if (recipe.fluidOutputs != null) {
            for (FluidStack out : recipe.fluidOutputs) {
                if (out != null && out.getFluid() != null) {
                    return safeText(out.getLocalizedName());
                }
            }
        }
        return t("nhaeutilities.gui.common.unknown", "Unknown");
    }

    private static int countItemStacks(ItemStack[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack != null) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidStacks(FluidStack[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return 0;
        }
        int count = 0;
        for (FluidStack stack : stacks) {
            if (stack != null && stack.getFluid() != null) {
                count++;
            }
        }
        return count;
    }

    private static String trimText(String text, int maxLen) {
        String safe = safeText(text);
        if (safe.length() <= maxLen) {
            return safe;
        }
        return safe.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private static String trimToPixelWidth(String text, int maxWidth) {
        String safe = text != null ? text : "";
        net.minecraft.client.gui.FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        if (fr == null || fr.getStringWidth(safe) <= maxWidth) {
            return safe;
        }

        String suffix = "...";
        int suffixWidth = fr.getStringWidth(suffix);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            String candidate = sb.toString() + ch;
            if (fr.getStringWidth(candidate) + suffixWidth > maxWidth) {
                break;
            }
            sb.append(ch);
        }
        return sb + suffix;
    }

    private static String safeText(String text) {
        return text != null && !text.isEmpty() ? text : t("nhaeutilities.gui.common.none", "None");
    }

    private static int[] buildSubmittedSelections(int[] selections, int expectedCount) {
        if (selections == null || expectedCount <= 0) {
            return new int[0];
        }
        int size = Math.min(expectedCount, selections.length);
        for (int i = 0; i < size; i++) {
            if (selections[i] < 0) {
                return new int[0];
            }
        }
        return Arrays.copyOf(selections, size);
    }

    private static boolean isValidRecipeIndex(int index, int recipeCount) {
        return recipeCount > 0 && index >= 0 && index < recipeCount;
    }

    private static int resolveChosenIndex(int clickedIndex, int selectedIndex, int recipeCount) {
        if (isValidRecipeIndex(clickedIndex, recipeCount)) {
            return clickedIndex;
        }
        if (isValidRecipeIndex(selectedIndex, recipeCount)) {
            return selectedIndex;
        }
        return recipeCount > 0 ? 0 : -1;
    }

    private static int getMaxRecipeCount(List<List<RecipeEntry>> groups) {
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (List<RecipeEntry> recipes : groups) {
            if (recipes != null && recipes.size() > max) {
                max = recipes.size();
            }
        }
        return max;
    }

    private static String buildTitleText(ClientBatchState state) {
        String productName = state.getCurrentProductName();
        int current = state.currentConflictIndex();
        int total = state.totalConflicts;
        int remaining = current > 0 ? Math.max(0, total - current + 1) : Math.max(0, total);
        return t(
            "nhaeutilities.gui.recipe_picker.title",
            "Conflict selection: %s (%s remaining)",
            productName,
            remaining);
    }

    private static String buildDefaultStatusText(ClientBatchState state) {
        int recipeCount = state.getCurrentRecipes()
            .size();
        int current = state.currentConflictIndex();
        int total = state.totalConflicts;
        return recipeCount > 0
            ? EnumChatFormatting.DARK_GRAY
                + t("nhaeutilities.gui.recipe_picker.status.default", "Conflict %s/%s", current, total)
            : EnumChatFormatting.RED + t("nhaeutilities.gui.recipe_picker.status.default_empty", "No candidates.");
    }

    private static String t(String key, String fallback, Object... args) {
        return I18nUtil.trOr(key, fallback, args);
    }

    public static class ClientBatchState {

        public int startIndex;
        public int totalConflicts;
        public List<String> productNames;
        public List<List<RecipeEntry>> recipeGroups;
        public int[] selections;
        public final int rowCapacity;
        public int localIndex;
        public int selectedRecipeIndex;
        public String statusText;
        public boolean awaitingServer;
        public boolean inputLocked;

        private ClientBatchState(int rowCapacity) {
            this.startIndex = 1;
            this.totalConflicts = 1;
            this.productNames = new ArrayList<String>();
            this.recipeGroups = new ArrayList<List<RecipeEntry>>();
            this.selections = new int[0];
            this.rowCapacity = Math.max(1, rowCapacity);
            this.localIndex = 0;
            this.selectedRecipeIndex = -1;
            this.statusText = "";
            this.awaitingServer = false;
            this.inputLocked = false;
        }

        static ClientBatchState from(PacketRecipeConflictBatch packet) {
            return from(packet, null);
        }

        static ClientBatchState from(PacketRecipeConflictBatch packet, int[] existingSelections) {
            int capacity = resolveRowCapacity(packet);
            ClientBatchState state = new ClientBatchState(capacity);
            if (!state.applyPacket(packet)) {
                return null;
            }
            if (existingSelections != null && existingSelections.length > 0) {
                state.copyExistingSelections(existingSelections);
            }
            return state;
        }

        boolean applyPacket(PacketRecipeConflictBatch packet) {
            if (packet == null || packet.productNames == null || packet.recipeGroups == null) {
                return false;
            }
            if (packet.productNames.isEmpty() || packet.recipeGroups.isEmpty()) {
                return false;
            }

            int count = Math.min(packet.productNames.size(), packet.recipeGroups.size());
            if (count <= 0) {
                return false;
            }

            int requiredCapacity = resolveRowCapacity(packet);
            if (requiredCapacity > this.rowCapacity) {
                return false;
            }

            List<String> names = new ArrayList<String>(count);
            List<List<RecipeEntry>> groups = new ArrayList<List<RecipeEntry>>(count);
            for (int i = 0; i < count; i++) {
                names.add(packet.productNames.get(i));
                List<RecipeEntry> recipes = packet.recipeGroups.get(i);
                groups.add(recipes != null ? recipes : new ArrayList<RecipeEntry>());
            }

            this.startIndex = Math.max(1, packet.startIndex);
            this.totalConflicts = Math.max(1, packet.totalConflicts);
            this.productNames = names;
            this.recipeGroups = groups;
            this.selections = new int[groups.size()];
            Arrays.fill(this.selections, -1);
            this.localIndex = 0;
            this.selectedRecipeIndex = groups.get(0)
                .isEmpty() ? -1 : 0;
            this.awaitingServer = false;
            this.inputLocked = false;
            this.statusText = buildDefaultStatusText(this);
            return true;
        }

        void copyExistingSelections(int[] existingSelections) {
            if (existingSelections == null || existingSelections.length == 0 || this.selections == null) {
                return;
            }
            int len = Math.min(existingSelections.length, this.selections.length);
            for (int i = 0; i < len; i++) {
                int groupSize = getGroupSize(i);
                if (existingSelections[i] >= 0 && existingSelections[i] < groupSize) {
                    this.selections[i] = existingSelections[i];
                }
            }
        }

        private int getGroupSize(int groupIndex) {
            if (groupIndex < 0 || groupIndex >= recipeGroups.size()) {
                return 0;
            }
            List<RecipeEntry> group = recipeGroups.get(groupIndex);
            return group != null ? group.size() : 0;
        }

        private static int resolveRowCapacity(PacketRecipeConflictBatch packet) {
            if (packet == null) {
                return 1;
            }
            int announcedMax = Math.max(0, packet.maxCandidatesPerGroup);
            int packetMax = getMaxRecipeCount(packet.recipeGroups);
            return Math.max(1, Math.max(announcedMax, packetMax));
        }

        int currentConflictIndex() {
            return startIndex + localIndex;
        }

        String getCurrentProductName() {
            if (localIndex < 0 || localIndex >= productNames.size()) {
                return t("nhaeutilities.gui.recipe_picker.unknown_product", "Unknown product");
            }
            String name = productNames.get(localIndex);
            return name != null ? name : t("nhaeutilities.gui.recipe_picker.unknown_product", "Unknown product");
        }

        List<RecipeEntry> getCurrentRecipes() {
            if (localIndex < 0 || localIndex >= recipeGroups.size()) {
                return new ArrayList<RecipeEntry>();
            }
            List<RecipeEntry> recipes = recipeGroups.get(localIndex);
            return recipes != null ? recipes : new ArrayList<RecipeEntry>();
        }
    }
}
