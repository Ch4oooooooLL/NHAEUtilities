package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.integration.nei.NEIDragAndDropHandler;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.github.nhaeutilities.modules.patterngenerator.filter.ExplicitStackMatcher;
import com.github.nhaeutilities.modules.shared.DebugLog;

class FilterTextFieldWidget extends TextFieldWidget implements NEIDragAndDropHandler {

    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    private static final ExplicitFilterDropFormatter.DropChoiceSource[] ALL_CATEGORIES = {
        ExplicitFilterDropFormatter.DropChoiceSource.ITEM_ID, ExplicitFilterDropFormatter.DropChoiceSource.ORE_DICT,
        ExplicitFilterDropFormatter.DropChoiceSource.DISPLAY_NAME };

    private final Function<ItemStack, String> stackFormatter;
    private final Function<ItemStack, ExplicitFilterDropFormatter.DropChoices> dropChoicesBuilder;
    private Consumer<ExplicitFilterDropFormatter.DropChoices> dropChoicesListener;

    private ExplicitFilterDropFormatter.DropChoiceSource currentCategory = ExplicitFilterDropFormatter.DropChoiceSource.ITEM_ID;
    private final Map<ExplicitFilterDropFormatter.DropChoiceSource, ExplicitFilterDropFormatter.DropChoice> storedChoices = new EnumMap<ExplicitFilterDropFormatter.DropChoiceSource, ExplicitFilterDropFormatter.DropChoice>(
        ExplicitFilterDropFormatter.DropChoiceSource.class);
    boolean explicitBrackets;
    private boolean loadDegradePending;
    private boolean degradeEnabled = true;
    private Consumer<String> statusConsumer;
    private Runnable onSave;

    FilterTextFieldWidget() {
        this(ExplicitFilterDropFormatter::format, ExplicitFilterDropFormatter::buildChoices);
    }

    FilterTextFieldWidget(Function<ItemStack, String> stackFormatter) {
        this(stackFormatter, null);
    }

    private FilterTextFieldWidget(Function<ItemStack, String> stackFormatter,
        Function<ItemStack, ExplicitFilterDropFormatter.DropChoices> dropChoicesBuilder) {
        this.stackFormatter = stackFormatter != null ? stackFormatter : ExplicitFilterDropFormatter::format;
        this.dropChoicesBuilder = dropChoicesBuilder;
    }

    FilterTextFieldWidget setDropChoicesListener(
        Consumer<ExplicitFilterDropFormatter.DropChoices> dropChoicesListener) {
        this.dropChoicesListener = dropChoicesListener;
        return this;
    }

    void applyDropChoice(ExplicitFilterDropFormatter.DropChoice choice) {
        if (choice == null) {
            return;
        }
        setText(choice.getRawContent());
        currentCategory = choice.getSource();
        explicitBrackets = false;
        safeMarkForUpdate();
    }

    ExplicitFilterDropFormatter.DropChoiceSource getCurrentCategory() {
        return currentCategory;
    }

    List<ExplicitFilterDropFormatter.DropChoiceSource> getAvailableCycleChoices() {
        if (explicitBrackets) {
            return java.util.Collections.emptyList();
        }
        if (!storedChoices.isEmpty()) {
            return new ArrayList<ExplicitFilterDropFormatter.DropChoiceSource>(storedChoices.keySet());
        }
        List<ExplicitFilterDropFormatter.DropChoiceSource> result = new ArrayList<ExplicitFilterDropFormatter.DropChoiceSource>();
        for (ExplicitFilterDropFormatter.DropChoiceSource category : ALL_CATEGORIES) {
            result.add(category);
        }
        result.add(ExplicitFilterDropFormatter.DropChoiceSource.CUSTOM);
        return result;
    }

    void cycleCategory(int direction) {
        List<ExplicitFilterDropFormatter.DropChoiceSource> available = getAvailableCycleChoices();
        if (available.isEmpty()) {
            return;
        }
        int currentIdx = available.indexOf(currentCategory);
        if (currentIdx < 0) {
            currentIdx = 0;
        }
        int nextIdx = (currentIdx + direction) % available.size();
        if (nextIdx < 0) {
            nextIdx += available.size();
        }
        ExplicitFilterDropFormatter.DropChoiceSource nextCategory = available.get(nextIdx);

        if (nextCategory == ExplicitFilterDropFormatter.DropChoiceSource.CUSTOM) {
            currentCategory = ExplicitFilterDropFormatter.DropChoiceSource.CUSTOM;
        } else if (storedChoices.containsKey(nextCategory)) {
            ExplicitFilterDropFormatter.DropChoice choice = storedChoices.get(nextCategory);
            setText(choice.getRawContent());
            currentCategory = nextCategory;
        } else {
            currentCategory = nextCategory;
        }
        safeMarkForUpdate();
    }

    String getEffectiveValue() {
        if (explicitBrackets) {
            return getText();
        }
        String raw = getText().trim();
        if (raw.isEmpty() || "*".equals(raw)) {
            return raw;
        }
        switch (currentCategory) {
            case ITEM_ID:
                return "[" + raw + "]";
            case ORE_DICT:
                return "(" + escapeRegexToken(raw) + ")";
            case DISPLAY_NAME:
                return "{" + escapeRegexToken(raw) + "}";
            default:
                return raw;
        }
    }

    private static String escapeRegexToken(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return SPECIAL_REGEX_CHARS.matcher(text)
            .replaceAll("\\\\$0");
    }

    String getCurrentCategoryLabel() {
        switch (currentCategory) {
            case ITEM_ID:
                return "ID";
            case ORE_DICT:
                return "Ore";
            case DISPLAY_NAME:
                return "Name";
            case CUSTOM:
                return "Custom";
            default:
                return "";
        }
    }

    void markLoadDegradePending() {
        this.loadDegradePending = true;
    }

    FilterTextFieldWidget setDegradeEnabled(boolean enabled) {
        this.degradeEnabled = enabled;
        return this;
    }

    void setStatusConsumer(Consumer<String> statusConsumer) {
        this.statusConsumer = statusConsumer;
    }

    void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    void enableEffectiveValueTooltip() {
        tooltipBuilder(tooltip -> {
            String text = getText().trim();
            if (text.isEmpty() || "*".equals(text) || explicitBrackets) {
                return;
            }
            String effective = getEffectiveValue();
            if (!effective.equals(text)) {
                tooltip.addLine(
                    IKey.str(t("nhaeutilities.gui.pattern_gen.tooltip.effective_value", "Sends as: %s", effective))
                        .style(EnumChatFormatting.GRAY));
            }
        });
    }

    private static String t(String key, String fallback, Object... args) {
        return com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil.trOr(key, fallback, args);
    }

    @Override
    public void afterInit() {
        super.afterInit();
        if (loadDegradePending) {
            tryDegradeOrValidate();
        }
    }

    /**
     * Programmatically accept an ItemStack as if dropped from NEI.
     * Stores DropChoices and sets text to the default match.
     */
    void acceptItem(ItemStack stack) {
        if (stack == null) return;
        ExplicitFilterDropFormatter.DropChoices choices = dropChoicesBuilder != null ? dropChoicesBuilder.apply(stack)
            : null;
        String formatted = choices != null ? choices.getDefaultToken() : stackFormatter.apply(stack);
        if (formatted == null || formatted.trim()
            .isEmpty()) {
            return;
        }
        storedChoices.clear();
        explicitBrackets = false;
        if (choices != null && !choices.isEmpty()) {
            for (ExplicitFilterDropFormatter.DropChoice choice : choices.getOptions()) {
                storedChoices.put(choice.getSource(), choice);
            }
            ExplicitFilterDropFormatter.DropChoice defaultChoice = choices.getDefaultChoice();
            if (defaultChoice != null) {
                setText(defaultChoice.getRawContent());
                currentCategory = defaultChoice.getSource();
            } else {
                setText(choices.getDefaultToken());
            }
        } else {
            setText(formatted);
        }
        safeMarkForUpdate();
        if (dropChoicesListener != null) {
            dropChoicesListener.accept(
                choices != null && !choices.isEmpty() ? choices : ExplicitFilterDropFormatter.singleChoice(formatted));
        }
    }

    /** Returns true if the cursor should be restored after field text was replaced. */
    boolean hasStoredDropChoices() {
        return !storedChoices.isEmpty();
    }

    void clearStoredDropChoices() {
        storedChoices.clear();
        explicitBrackets = false;
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        DebugLog
            .info("[NHAE] FilterTextFieldWidget.handleDragAndDrop called: stack=%s, button=%d", draggedStack, button);
        if (draggedStack == null) {
            return false;
        }

        ExplicitFilterDropFormatter.DropChoices choices = dropChoicesBuilder != null
            ? dropChoicesBuilder.apply(draggedStack)
            : null;
        String formatted = choices != null ? choices.getDefaultToken() : stackFormatter.apply(draggedStack);
        if (formatted == null || formatted.trim()
            .isEmpty()) {
            return false;
        }

        storedChoices.clear();
        explicitBrackets = false;
        if (choices != null && !choices.isEmpty()) {
            for (ExplicitFilterDropFormatter.DropChoice choice : choices.getOptions()) {
                storedChoices.put(choice.getSource(), choice);
            }
            ExplicitFilterDropFormatter.DropChoice defaultChoice = choices.getDefaultChoice();
            if (defaultChoice != null) {
                setText(defaultChoice.getRawContent());
                currentCategory = defaultChoice.getSource();
            } else {
                setText(choices.getDefaultToken());
            }
        } else {
            setText(formatted);
        }
        safeMarkForUpdate();
        if (dropChoicesListener != null) {
            dropChoicesListener.accept(
                choices != null && !choices.isEmpty() ? choices : ExplicitFilterDropFormatter.singleChoice(formatted));
        }
        return true;
    }

    @Override
    public boolean onMouseScroll(ModularScreen.UpOrDown scrollDirection, int amount) {
        if (explicitBrackets || storedChoices.isEmpty()) {
            return false;
        }
        int direction = scrollDirection.isDown() ? 1 : -1;
        cycleCategory(direction);
        return true;
    }

    @Override
    public void onRemoveFocus(ModularGuiContext context) {
        tryDegradeOrValidate();
        markTooltipDirty();
        if (onSave != null) {
            onSave.run();
        }
        super.onRemoveFocus(context);
    }

    void tryDegradeOrValidate() {
        if (!degradeEnabled) {
            return;
        }
        if (loadDegradePending) {
            loadDegradePending = false;
            ExplicitStackMatcher.DegradeResult result = ExplicitStackMatcher.degrade(getText());
            applyDegradeResult(result);
            return;
        }
        if (explicitBrackets) {
            ExplicitStackMatcher.DegradeResult result = ExplicitStackMatcher.degrade(getText());
            if (!result.isValid()) {
                if (statusConsumer != null) statusConsumer.accept(result.getErrorMessage());
                return;
            }
            if (!result.isExplicit()) {
                explicitBrackets = false;
                if (result.getCategory() != null) {
                    setText(result.getRawText());
                    currentCategory = result.getCategory();
                }
            }
            return;
        }
        ExplicitStackMatcher.DegradeResult result = ExplicitStackMatcher.degrade(getText());
        applyDegradeResult(result);
    }

    private void applyDegradeResult(ExplicitStackMatcher.DegradeResult result) {
        if (!result.isValid()) {
            if (statusConsumer != null) statusConsumer.accept(result.getErrorMessage());
            return;
        }
        if (result.isExplicit()) {
            explicitBrackets = true;
            currentCategory = ExplicitFilterDropFormatter.DropChoiceSource.CUSTOM;
            return;
        }
        if (result.getCategory() != null) {
            explicitBrackets = false;
            setText(result.getRawText());
            currentCategory = result.getCategory();
        }
    }

    @Override
    public void setText(@NotNull String text) {
        super.setText(text);
        markTooltipDirty();
    }

    void safeMarkForUpdate() {
        markTooltipDirty();
    }
}
