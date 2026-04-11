package com.github.nhaeutilities.modules.patterngenerator.encoder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingNbt;

/**
 * AE2 ????????????RecipeEntry ??? AE2 ?????? ItemStack
 * <p>
 * ?????????????????????? AE2FC ??ItemFluidDrop ???????????
 */
public class PatternEncoder {

    /** ??? AE2FC ?????? */
    private static Boolean ae2fcAvailable = null;
    private static volatile PatternItemResolver patternItemResolver = new DefaultPatternItemResolver();

    /**
     * ????????? AE2 Processing Pattern (??????)
     *
     * @param recipe ???
     * @return ????????? ItemStack
     */
    public static ItemStack encode(RecipeEntry recipe) {
        // ???????????Item
        Item patternItem = findEncodedPatternItem();
        if (patternItem == null) {
            return null;
        }

        // ?????????
        ItemStack patternStack = new ItemStack(patternItem, 1, 0);

        NBTTagCompound tag = new NBTTagCompound();

        // ---- ?????? ----
        NBTTagList inList = new NBTTagList();

        // ??????
        for (ItemStack input : recipe.inputs) {
            appendStackTag(inList, input);
        }

        // ?????? -> ItemFluidDrop
        for (FluidStack fluid : recipe.fluidInputs) {
            if (fluid == null || fluid.amount <= 0) continue;
            ItemStack fluidItem = convertFluidToItem(fluid);
            appendStackTag(inList, fluidItem);
        }

        // ---- ?????? ----
        NBTTagList outList = new NBTTagList();

        // ??????
        for (ItemStack output : recipe.outputs) {
            appendStackTag(outList, output);
        }

        // ?????? -> ItemFluidDrop
        for (FluidStack fluid : recipe.fluidOutputs) {
            if (fluid == null || fluid.amount <= 0) continue;
            ItemStack fluidItem = convertFluidToItem(fluid);
            appendStackTag(outList, fluidItem);
        }

        // ?????????????????????????
        if (inList.tagCount() == 0 || outList.tagCount() == 0) {
            return null;
        }

        tag.setTag("in", inList);
        tag.setTag("out", outList);
        tag.setBoolean("crafting", false); // ??????
        tag.setBoolean("substitute", false);
        tag.setBoolean("beSubstitute", false);
        tag.setBoolean("isStandard", true);

        patternStack.setTagCompound(tag);
        PatternRoutingNbt.writeRoutingData(
            patternStack,
            new PatternRoutingNbt.RoutingMetadata(
                PatternRoutingKeys.CURRENT_VERSION,
                recipe.recipeId,
                "",
                "",
                "",
                PatternRoutingKeys.SOURCE_GENERATOR,
                false));
        return patternStack;
    }

    public static List<ItemStack> encodeBatch(List<RecipeEntry> recipes) {
        List<ItemStack> patterns = new ArrayList<>();
        for (RecipeEntry recipe : recipes) {
            ItemStack pattern = encode(recipe);
            if (pattern != null) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    /**
     * ??ItemStack ??AE2 ???????????????????????NBT??
     * <p>
     * AE2 ??Pattern ???????????? int ?????"Count"?????"Cnt"??
     */
    private static void appendStackTag(NBTTagList targetList, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) {
            return;
        }

        NBTTagCompound itemTag = new NBTTagCompound();
        stack.writeToNBT(itemTag);

        // ??? Count ??int?????>127 ??? byte ????????? 1 ?????
        itemTag.setInteger("Count", stack.stackSize);

        // ?????? Cnt ?????????????????????????????
        itemTag.setLong("Cnt", stack.stackSize);

        targetList.appendTag(itemTag);
    }

    /**
     * ??FluidStack ??? AE2FC ??ItemFluidDrop ItemStack
     * <p>
     * ??? AE2FC ????????? null?????????
     */
    private static ItemStack convertFluidToItem(FluidStack fluid) {
        if (!isAe2fcAvailable()) {
            return null;
        }
        try {
            return com.glodblock.github.common.item.ItemFluidDrop.newStack(fluid);
        } catch (Throwable e) {
            // AE2FC ????????????????????
            ae2fcAvailable = false;
            return null;
        }
    }

    /**
     * ????AE2FC ??????
     */
    private static boolean isAe2fcAvailable() {
        if (ae2fcAvailable != null) {
            return ae2fcAvailable;
        }
        try {
            Class.forName("com.glodblock.github.common.item.ItemFluidDrop");
            ae2fcAvailable = true;
        } catch (ClassNotFoundException e) {
            ae2fcAvailable = false;
        }
        return ae2fcAvailable;
    }

    private static Item findEncodedPatternItem() {
        return patternItemResolver.resolve();
    }

    static void setPatternItemResolver(PatternItemResolver resolver) {
        patternItemResolver = resolver != null ? resolver : patternItemResolver;
    }

    static void resetPatternItemResolver() {
        patternItemResolver = new DefaultPatternItemResolver();
    }

    interface PatternItemResolver {

        Item resolve();
    }

    private static final class DefaultPatternItemResolver implements PatternItemResolver {

        @Override
        public Item resolve() {
            try {
                for (ItemStack stack : appeng.api.AEApi.instance()
                    .definitions()
                    .items()
                    .encodedPattern()
                    .maybeStack(1)
                    .asSet()) {
                    if (stack != null && stack.getItem() != null) {
                        return stack.getItem();
                    }
                }
            } catch (Throwable e) {}

            String patternId = ForgeConfig.getEncodedPatternId();
            return (Item) Item.itemRegistry.getObject(patternId);
        }
    }
}
