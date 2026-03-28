package com.github.nhaeutilities.modules.patterngenerator.encoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.github.nhaeutilities.modules.patterngenerator.util.OreDictUtil;

/**
 * ??????????????????? ItemStack ??????
 * <p>
 * ??????: {@code ???????????} (?????{@code ;} ???)
 * <p>
 * ?? {@code ingotCopper=dustCopper;ingotTin=dustTin}
 */
public class OreDictReplacer {

    private final Map<String, String> rules;

    public OreDictReplacer(String rulesStr) {
        this.rules = new LinkedHashMap<>();
        if (rulesStr == null || rulesStr.trim()
            .isEmpty()) return;

        for (String rule : rulesStr.split(";")) {
            rule = rule.trim();
            if (rule.isEmpty()) continue;
            int eq = rule.indexOf('=');
            if (eq <= 0 || eq >= rule.length() - 1) continue;
            String src = rule.substring(0, eq)
                .trim();
            String dst = rule.substring(eq + 1)
                .trim();
            if (!src.isEmpty() && !dst.isEmpty()) {
                rules.put(src, dst);
            }
        }
    }

    /**
     * @return ??????????????
     */
    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /**
     * ?????????????????
     *
     * @param items ?????????
     * @return ???????????(?????????)
     */
    public ItemStack[] apply(ItemStack[] items) {
        if (!hasRules() || items == null) return items;

        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = tryReplace(items[i]);
        }
        return result;
    }

    private ItemStack tryReplace(ItemStack original) {
        if (original == null) return null;

        // ???????????????????????????????
        String[] oreNames = OreDictUtil.getOreNamesSafe(original);
        if (oreNames.length == 0) return original;

        for (String oreName : oreNames) {
            if (rules.containsKey(oreName)) {
                String targetOre = rules.get(oreName);
                List<ItemStack> candidates = OreDictionary.getOres(targetOre);
                if (!candidates.isEmpty()) {
                    // ????????????????????
                    ItemStack replacement = candidates.get(0)
                        .copy();
                    replacement.stackSize = original.stackSize;
                    return replacement;
                }
            }
        }

        return original;
    }
}

