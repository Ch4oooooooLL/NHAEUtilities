package com.github.nhaeutilities.modules.patterngenerator.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.gui.ExplicitFilterDropFormatter;
import com.github.nhaeutilities.modules.patterngenerator.util.ItemStackUtil;
import com.github.nhaeutilities.modules.patterngenerator.util.OreDictUtil;

/**
 * Shared explicit matcher used by all generation filter inputs.
 *
 * <p>
 * Supported syntax:
 * <ul>
 * <li>{@code [id]} / {@code [id:meta]}</li>
 * <li>{@code (regex)} for ore dictionary names</li>
 * <li>{@code {regex}} for display names</li>
 * <li>{@code *} to disable the matcher</li>
 * </ul>
 */
public class ExplicitStackMatcher {

    private static final String[] EMPTY_STRINGS = new String[0];

    private final String source;
    private final boolean disabled;
    private final boolean invalid;
    private final MatchPlan matchPlan;
    private final StackMatchCache stackCache;

    public ExplicitStackMatcher(String source) {
        this(source, new StackMatchCache());
    }

    ExplicitStackMatcher(String source, StackMatchCache stackCache) {
        this.source = source;
        this.stackCache = stackCache != null ? stackCache : new StackMatchCache();

        String normalized = source != null ? source.trim() : "";
        if (normalized.isEmpty() || "*".equals(normalized)) {
            this.disabled = true;
            this.invalid = false;
            this.matchPlan = MatchPlan.NO_RULES;
            return;
        }

        ParsedRules rules = parseRules(normalized);
        this.disabled = false;
        this.invalid = !rules.hasRules();
        this.matchPlan = this.invalid ? MatchPlan.NO_RULES : rules.toMatchPlan();
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public boolean matches(ItemStack stack) {
        if (disabled || invalid || stack == null) {
            return false;
        }

        Item item = stack.getItem();
        if (item == null) {
            return false;
        }

        final int itemId = Item.getIdFromItem(item);
        final int meta = stack.getItemDamage();

        return matches(
            itemId,
            meta,
            () -> stackCache.getOrCreate(stack)
                .getOreNames(),
            () -> stackCache.getOrCreate(stack)
                .getDisplayName());
    }

    boolean matches(String displayName, int itemId, int meta, String[] oreNames) {
        if (disabled || invalid) {
            return false;
        }

        return matches(itemId, meta, () -> oreNames, () -> displayName);
    }

    boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier, Supplier<String> displayNameSupplier) {
        if (disabled || invalid) {
            return false;
        }
        return matchPlan.matches(itemId, meta, oreNamesSupplier, displayNameSupplier);
    }

    String getSource() {
        return source;
    }

    private static ParsedRules parseRules(String source) {
        List<IdRule> idRules = new ArrayList<IdRule>();
        List<RegexRule> oreDictRules = new ArrayList<RegexRule>();
        List<RegexRule> displayNameRules = new ArrayList<RegexRule>();

        int index = 0;
        while (index < source.length()) {
            char ch = source.charAt(index);
            if (isSeparator(ch)) {
                index++;
                continue;
            }

            if (ch == '[') {
                int end = source.indexOf(']', index + 1);
                if (end < 0) {
                    return ParsedRules.empty();
                }

                IdRule rule = parseIdRule(source.substring(index + 1, end));
                if (rule == null) {
                    return ParsedRules.empty();
                }
                idRules.add(rule);
                index = end + 1;
                continue;
            }

            if (ch == '(') {
                ParseResult result = parseDelimitedToken(source, index, '(', ')');
                if (!result.valid) {
                    return ParsedRules.empty();
                }

                RegexRule rule = createRegexRule(result.content);
                if (rule != null) {
                    oreDictRules.add(rule);
                }
                index = result.nextIndex;
                continue;
            }

            if (ch == '{') {
                ParseResult result = parseDelimitedToken(source, index, '{', '}');
                if (!result.valid) {
                    return ParsedRules.empty();
                }

                RegexRule rule = createRegexRule(result.content);
                if (rule != null) {
                    displayNameRules.add(rule);
                }
                index = result.nextIndex;
                continue;
            }

            return ParsedRules.empty();
        }

        return new ParsedRules(idRules, oreDictRules, displayNameRules);
    }

    private static boolean isSeparator(char ch) {
        return Character.isWhitespace(ch) || ch == ',' || ch == ';' || ch == '|';
    }

    private static IdRule parseIdRule(String source) {
        String token = source != null ? source.trim() : "";
        if (token.isEmpty()) {
            return null;
        }

        String[] parts = token.split(":", -1);
        if (parts.length == 0 || parts.length > 2) {
            return null;
        }

        try {
            int itemId = Integer.parseInt(parts[0]);
            Integer meta = null;
            if (parts.length == 2) {
                if (parts[1].isEmpty()) {
                    return null;
                }
                meta = Integer.valueOf(Integer.parseInt(parts[1]));
            }
            return new IdRule(itemId, meta);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static RegexRule createRegexRule(String source) {
        String content = source != null ? source.trim() : "";
        if (content.isEmpty()) {
            return null;
        }

        try {
            return new RegexRule(content, Pattern.compile(content, Pattern.CASE_INSENSITIVE));
        } catch (PatternSyntaxException e) {
            return new RegexRule(content, Pattern.compile(Pattern.quote(content), Pattern.CASE_INSENSITIVE));
        }
    }

    private static ParseResult parseDelimitedToken(String source, int startIndex, char open, char close) {
        StringBuilder content = new StringBuilder();
        int depth = 1;

        for (int i = startIndex + 1; i < source.length(); i++) {
            char ch = source.charAt(i);

            if (ch == '\\' && i + 1 < source.length()) {
                content.append(ch);
                i++;
                content.append(source.charAt(i));
                continue;
            }

            if (ch == open) {
                depth++;
                content.append(ch);
                continue;
            }

            if (ch == close) {
                depth--;
                if (depth == 0) {
                    return new ParseResult(content.toString(), i + 1, true);
                }
                content.append(ch);
                continue;
            }

            content.append(ch);
        }

        return ParseResult.invalid();
    }

    static final class StackMatchCache {

        private final IdentityHashMap<ItemStack, StackMatchData> cache = new IdentityHashMap<ItemStack, StackMatchData>();

        private synchronized StackMatchData getOrCreate(ItemStack stack) {
            StackMatchData data = cache.get(stack);
            if (data == null) {
                data = new StackMatchData(stack);
                cache.put(stack, data);
            }
            return data;
        }
    }

    private static final class StackMatchData {

        private final ItemStack stack;
        private final int itemId;
        private final int meta;
        private String[] oreNames;
        private String displayName;
        private boolean oreNamesLoaded;
        private boolean displayNameLoaded;

        private StackMatchData(ItemStack stack) {
            this.stack = stack;

            Item item = stack != null ? stack.getItem() : null;
            if (item != null) {
                this.itemId = Item.getIdFromItem(item);
                this.meta = stack.getItemDamage();
            } else {
                this.itemId = -1;
                this.meta = -1;
            }
        }

        public int getItemId() {
            return itemId;
        }

        public int getMeta() {
            return meta;
        }

        public String[] getOreNames() {
            if (!oreNamesLoaded) {
                oreNames = OreDictUtil.getOreNamesSafe(stack);
                oreNamesLoaded = true;
            }
            return oreNames != null ? oreNames : EMPTY_STRINGS;
        }

        public String getDisplayName() {
            if (!displayNameLoaded) {
                displayName = ItemStackUtil.getSafeDisplayName(stack);
                displayNameLoaded = true;
            }
            return displayName != null ? displayName : "";
        }
    }

    private static final class ParsedRules {

        private static final ParsedRules EMPTY = new ParsedRules(
            Collections.<IdRule>emptyList(),
            Collections.<RegexRule>emptyList(),
            Collections.<RegexRule>emptyList());

        private final List<IdRule> idRules;
        private final List<RegexRule> oreDictRules;
        private final List<RegexRule> displayNameRules;

        private ParsedRules(List<IdRule> idRules, List<RegexRule> oreDictRules, List<RegexRule> displayNameRules) {
            this.idRules = idRules;
            this.oreDictRules = oreDictRules;
            this.displayNameRules = displayNameRules;
        }

        private static ParsedRules empty() {
            return EMPTY;
        }

        private boolean hasRules() {
            return !(idRules.isEmpty() && oreDictRules.isEmpty() && displayNameRules.isEmpty());
        }

        private MatchPlan toMatchPlan() {
            boolean hasId = !idRules.isEmpty();
            boolean hasOre = !oreDictRules.isEmpty();
            boolean hasDisplay = !displayNameRules.isEmpty();

            if (hasId && !hasOre && !hasDisplay) {
                return new IdOnlyMatchPlan(idRules);
            }
            if (!hasId && hasOre && !hasDisplay) {
                return new OreOnlyMatchPlan(oreDictRules);
            }
            if (!hasId && !hasOre && hasDisplay) {
                return new DisplayOnlyMatchPlan(displayNameRules);
            }
            return new MixedMatchPlan(idRules, oreDictRules, displayNameRules);
        }
    }

    private static final class IdRule {

        private final int itemId;
        private final Integer meta;

        private IdRule(int itemId, Integer meta) {
            this.itemId = itemId;
            this.meta = meta;
        }

        private boolean matches(int candidateId, int candidateMeta) {
            if (candidateId != itemId) {
                return false;
            }
            return meta == null || meta.intValue() == candidateMeta;
        }
    }

    private static final class RegexRule {

        private final Pattern pattern;

        private RegexRule(String source, Pattern pattern) {
            this.pattern = pattern;
        }

        private boolean matches(String candidate) {
            if (candidate == null || candidate.isEmpty()) {
                return false;
            }
            return pattern.matcher(candidate)
                .find();
        }

        private boolean matchesAny(String[] candidates) {
            if (candidates == null || candidates.length == 0) {
                return false;
            }

            for (String candidate : candidates) {
                if (matches(candidate)) {
                    return true;
                }
            }
            return false;
        }
    }

    private interface MatchPlan {

        MatchPlan NO_RULES = new MatchPlan() {

            @Override
            public boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier,
                Supplier<String> displayNameSupplier) {
                return false;
            }
        };

        boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier,
            Supplier<String> displayNameSupplier);
    }

    private abstract static class BaseMatchPlan implements MatchPlan {

        protected String[] getOreNames(Supplier<String[]> oreNamesSupplier) {
            if (oreNamesSupplier == null) {
                return EMPTY_STRINGS;
            }

            String[] oreNames = oreNamesSupplier.get();
            return oreNames != null ? oreNames : EMPTY_STRINGS;
        }

        protected String getDisplayName(Supplier<String> displayNameSupplier) {
            if (displayNameSupplier == null) {
                return "";
            }

            String displayName = displayNameSupplier.get();
            return displayName != null ? displayName : "";
        }
    }

    private static final class IdOnlyMatchPlan extends BaseMatchPlan {

        private final List<IdRule> idRules;

        private IdOnlyMatchPlan(List<IdRule> idRules) {
            this.idRules = idRules;
        }

        @Override
        public boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier,
            Supplier<String> displayNameSupplier) {
            for (IdRule rule : idRules) {
                if (rule.matches(itemId, meta)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class OreOnlyMatchPlan extends BaseMatchPlan {

        private final List<RegexRule> oreDictRules;

        private OreOnlyMatchPlan(List<RegexRule> oreDictRules) {
            this.oreDictRules = oreDictRules;
        }

        @Override
        public boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier,
            Supplier<String> displayNameSupplier) {
            String[] oreNames = getOreNames(oreNamesSupplier);
            for (RegexRule rule : oreDictRules) {
                if (rule.matchesAny(oreNames)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class DisplayOnlyMatchPlan extends BaseMatchPlan {

        private final List<RegexRule> displayNameRules;

        private DisplayOnlyMatchPlan(List<RegexRule> displayNameRules) {
            this.displayNameRules = displayNameRules;
        }

        @Override
        public boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier,
            Supplier<String> displayNameSupplier) {
            String displayName = getDisplayName(displayNameSupplier);
            for (RegexRule rule : displayNameRules) {
                if (rule.matches(displayName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class MixedMatchPlan extends BaseMatchPlan {

        private final List<IdRule> idRules;
        private final List<RegexRule> oreDictRules;
        private final List<RegexRule> displayNameRules;

        private MixedMatchPlan(List<IdRule> idRules, List<RegexRule> oreDictRules, List<RegexRule> displayNameRules) {
            this.idRules = idRules;
            this.oreDictRules = oreDictRules;
            this.displayNameRules = displayNameRules;
        }

        @Override
        public boolean matches(int itemId, int meta, Supplier<String[]> oreNamesSupplier,
            Supplier<String> displayNameSupplier) {
            for (IdRule rule : idRules) {
                if (rule.matches(itemId, meta)) {
                    return true;
                }
            }

            if (!oreDictRules.isEmpty()) {
                String[] oreNames = getOreNames(oreNamesSupplier);
                for (RegexRule rule : oreDictRules) {
                    if (rule.matchesAny(oreNames)) {
                        return true;
                    }
                }
            }

            if (!displayNameRules.isEmpty()) {
                String displayName = getDisplayName(displayNameSupplier);
                for (RegexRule rule : displayNameRules) {
                    if (rule.matches(displayName)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static final class ParseResult {

        private final String content;
        private final int nextIndex;
        private final boolean valid;

        private ParseResult(String content, int nextIndex, boolean valid) {
            this.content = content;
            this.nextIndex = nextIndex;
            this.valid = valid;
        }

        private static ParseResult invalid() {
            return new ParseResult("", -1, false);
        }
    }

    public static final class DegradeResult {

        private final boolean isExplicit;
        private final boolean isValid;
        private final String rawText;
        private final ExplicitFilterDropFormatter.DropChoiceSource category;
        private final String errorMessage;

        private DegradeResult(boolean isExplicit, boolean isValid, String rawText,
            ExplicitFilterDropFormatter.DropChoiceSource category, String errorMessage) {
            this.isExplicit = isExplicit;
            this.isValid = isValid;
            this.rawText = rawText;
            this.category = category;
            this.errorMessage = errorMessage;
        }

        static DegradeResult noBrackets(String text) {
            return new DegradeResult(false, true, text, null, null);
        }

        static DegradeResult degraded(String rawText, ExplicitFilterDropFormatter.DropChoiceSource category) {
            return new DegradeResult(false, true, rawText, category, null);
        }

        static DegradeResult explicit(String text) {
            return new DegradeResult(true, true, text, ExplicitFilterDropFormatter.DropChoiceSource.CUSTOM, null);
        }

        static DegradeResult error(String errorMessage) {
            return new DegradeResult(false, false, null, null, errorMessage);
        }

        public boolean isExplicit() {
            return isExplicit;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getRawText() {
            return rawText;
        }

        public ExplicitFilterDropFormatter.DropChoiceSource getCategory() {
            return category;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static DegradeResult degrade(String text) {
        if (text == null) {
            return DegradeResult.noBrackets("");
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            return DegradeResult.noBrackets(trimmed);
        }

        boolean hasBrackets = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '[' || ch == '(' || ch == '{' || ch == ']' || ch == ')' || ch == '}') {
                hasBrackets = true;
                break;
            }
        }

        if (!hasBrackets) {
            return DegradeResult.noBrackets(trimmed);
        }

        Map<ExplicitFilterDropFormatter.DropChoiceSource, String> extracted = new EnumMap<ExplicitFilterDropFormatter.DropChoiceSource, String>(
            ExplicitFilterDropFormatter.DropChoiceSource.class);
        boolean idFound = false;
        boolean oreFound = false;
        boolean nameFound = false;
        int index = 0;

        while (index < trimmed.length()) {
            char ch = trimmed.charAt(index);
            if (Character.isWhitespace(ch) || ch == ',' || ch == ';' || ch == '|') {
                index++;
                continue;
            }

            if (ch == '[') {
                int end = trimmed.indexOf(']', index + 1);
                if (end < 0) {
                    return DegradeResult.error("Unclosed ID bracket");
                }
                String content = trimmed.substring(index + 1, end)
                    .trim();
                if (content.isEmpty()) {
                    return DegradeResult.error("Empty ID bracket");
                }
                String[] parts = content.split(":", -1);
                if (parts.length == 0 || parts.length > 2) {
                    return DegradeResult.error("Invalid ID format: " + content);
                }
                try {
                    Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    return DegradeResult.error("Invalid ID number: " + parts[0]);
                }
                if (parts.length == 2) {
                    if (parts[1].isEmpty()) {
                        return DegradeResult.error("Invalid meta in ID: " + content);
                    }
                    try {
                        Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        return DegradeResult.error("Invalid meta number: " + parts[1]);
                    }
                }
                extracted.put(ExplicitFilterDropFormatter.DropChoiceSource.ITEM_ID, content);
                idFound = true;
                index = end + 1;
                continue;
            }

            if (ch == '(') {
                String content = extractDelimited(trimmed, index, '(', ')');
                if (content == null) {
                    return DegradeResult.error("Unclosed ore dict bracket");
                }
                if (content.trim()
                    .isEmpty()) {
                    return DegradeResult.error("Empty ore dict bracket");
                }
                String safeContent = content.trim();
                try {
                    Pattern.compile(safeContent);
                } catch (PatternSyntaxException e) {
                    return DegradeResult.error("Invalid regex in (): " + e.getMessage());
                }
                extracted.put(ExplicitFilterDropFormatter.DropChoiceSource.ORE_DICT, safeContent);
                oreFound = true;
                index += content.length() + 2;
                continue;
            }

            if (ch == '{') {
                String content = extractDelimited(trimmed, index, '{', '}');
                if (content == null) {
                    return DegradeResult.error("Unclosed display name bracket");
                }
                if (content.trim()
                    .isEmpty()) {
                    return DegradeResult.error("Empty display name bracket");
                }
                String safeContent = content.trim();
                try {
                    Pattern.compile(safeContent);
                } catch (PatternSyntaxException e) {
                    return DegradeResult.error("Invalid regex in {}: " + e.getMessage());
                }
                extracted.put(ExplicitFilterDropFormatter.DropChoiceSource.DISPLAY_NAME, safeContent);
                nameFound = true;
                index += content.length() + 2;
                continue;
            }

            return DegradeResult.error("Unexpected character '" + ch + "' in filter input");
        }

        int categoryCount = (idFound ? 1 : 0) + (oreFound ? 1 : 0) + (nameFound ? 1 : 0);
        if (categoryCount == 0) {
            return DegradeResult.noBrackets(trimmed);
        }

        if (categoryCount > 1) {
            return DegradeResult.explicit(trimmed);
        }

        if (idFound) {
            return DegradeResult.degraded(
                extracted.get(ExplicitFilterDropFormatter.DropChoiceSource.ITEM_ID),
                ExplicitFilterDropFormatter.DropChoiceSource.ITEM_ID);
        }
        if (oreFound) {
            return DegradeResult.degraded(
                extracted.get(ExplicitFilterDropFormatter.DropChoiceSource.ORE_DICT),
                ExplicitFilterDropFormatter.DropChoiceSource.ORE_DICT);
        }
        return DegradeResult.degraded(
            extracted.get(ExplicitFilterDropFormatter.DropChoiceSource.DISPLAY_NAME),
            ExplicitFilterDropFormatter.DropChoiceSource.DISPLAY_NAME);
    }

    private static String extractDelimited(String source, int startIndex, char open, char close) {
        int depth = 1;
        for (int i = startIndex + 1; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '\\' && i + 1 < source.length()) {
                i++;
                continue;
            }
            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return source.substring(startIndex + 1, i);
                }
            }
        }
        return null;
    }
}
