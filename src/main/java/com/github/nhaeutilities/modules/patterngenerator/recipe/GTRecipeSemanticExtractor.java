package com.github.nhaeutilities.modules.patterngenerator.recipe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.FMLLog;

public final class GTRecipeSemanticExtractor {

    private static volatile Method GT_IS_ANY_INTEGRATED_CIRCUIT;
    private static volatile boolean circuitMethodInitialized;

    private GTRecipeSemanticExtractor() {}

    private static Method getGtCircuitMethod() {
        if (!circuitMethodInitialized) {
            synchronized (GTRecipeSemanticExtractor.class) {
                if (!circuitMethodInitialized) {
                    GT_IS_ANY_INTEGRATED_CIRCUIT = findGtCircuitMethod();
                    circuitMethodInitialized = true;
                }
            }
        }
        return GT_IS_ANY_INTEGRATED_CIRCUIT;
    }

    public static RecipeAnalysisSnapshot extract(RecipeEntry recipe) {
        if (recipe == null) {
            return new RecipeAnalysisSnapshot(
                RecipeAnalysisSnapshot.Status.UNRESOLVED,
                "",
                "",
                Collections.<RecipeInputSemanticSnapshot>emptyList(),
                "",
                "",
                -1,
                Collections.<String>emptyList(),
                0);
        }

        List<RecipeInputSemanticSnapshot> snapshots = new ArrayList<RecipeInputSemanticSnapshot>();
        ItemStack circuitStack = null;
        List<ItemStack> nonConsumables = new ArrayList<ItemStack>();
        int consumableCount = 0;

        int skipGetItem = 0;
        for (int i = 0; i < recipe.inputs.length; i++) {
            ItemStack stack = recipe.inputs[i];
            if (stack == null || stack.getItem() == null) {
                if (stack != null && skipGetItem == 0) {
                    logDiagnostic(
                        "[NHAEUtilities][SemanticCache] extract mapId=%s input[%d] stack!=null getItem=null stackSize=%d damage=%d",
                        recipe.recipeMapId,
                        i,
                        stack.stackSize,
                        stack.getItemDamage());
                }
                skipGetItem++;
                continue;
            }

            InputSemanticType type = classifyInput(stack);
            switch (type) {
                case PROGRAMMING_CIRCUIT:
                    if (circuitStack == null) {
                        circuitStack = stack.copy();
                    }
                    break;
                case NON_CONSUMABLE:
                    nonConsumables.add(stack.copy());
                    break;
                case CONSUMABLE:
                    consumableCount++;
                    break;
                default:
                    break;
            }
            snapshots.add(new RecipeInputSemanticSnapshot(type, i, stack));
        }

        for (FluidStack fluid : recipe.fluidInputs) {
            if (fluid == null) {
                continue;
            }
            snapshots.add(new RecipeInputSemanticSnapshot(InputSemanticType.FLUID, -1, null));
        }

        for (ItemStack special : recipe.specialItems) {
            if (special == null) {
                continue;
            }
            snapshots.add(new RecipeInputSemanticSnapshot(InputSemanticType.SPECIAL_SLOT, -1, special));
        }

        String circuitSignature = circuitStack != null ? itemSignature(circuitStack) : "";
        int circuitNumber = circuitStack != null ? circuitStack.getItemDamage() : -1;
        List<String> ncSignatures = new ArrayList<String>();
        for (ItemStack nc : nonConsumables) {
            String sig = itemSignature(nc);
            if (!sig.isEmpty()) {
                ncSignatures.add(sig);
            }
        }
        Collections.sort(ncSignatures);

        String groupingKey = buildGroupingKey(circuitSignature, ncSignatures);

        RecipeAnalysisSnapshot.Status status = RecipeAnalysisSnapshot.Status.COMPLETE;

        return new RecipeAnalysisSnapshot(
            status,
            recipe.recipeMapId,
            recipe.recipeId,
            snapshots,
            groupingKey,
            circuitSignature,
            circuitNumber,
            ncSignatures,
            consumableCount);
    }

    private static volatile boolean diagnosticsEnabled = true;

    public static void setDiagnosticsEnabled(boolean enabled) {
        diagnosticsEnabled = enabled;
    }

    private static void logDiagnostic(String format, Object... args) {
        if (!diagnosticsEnabled) return;
        try {
            FMLLog.info(format, args);
        } catch (Throwable ignored) {}
    }

    static InputSemanticType classifyInput(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return InputSemanticType.OTHER;
        }
        if (isIntegratedCircuit(stack)) {
            return InputSemanticType.PROGRAMMING_CIRCUIT;
        }
        if (stack.stackSize == 0) {
            return InputSemanticType.NON_CONSUMABLE;
        }
        return InputSemanticType.CONSUMABLE;
    }

    static boolean isIntegratedCircuit(ItemStack stack) {
        Method method = getGtCircuitMethod();
        if (stack == null || method == null) {
            return false;
        }
        try {
            Object result = method.invoke(null, stack);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    static String ncTypeKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        return registryName != null ? registryName.toString() : String.valueOf(Item.getIdFromItem(stack.getItem()));
    }

    static String buildGroupingKey(String circuitSignature, List<String> ncSignatures) {
        StringBuilder key = new StringBuilder();
        if (circuitSignature != null && !circuitSignature.isEmpty()) {
            key.append(circuitSignature);
        }
        if (ncSignatures != null) {
            for (String sig : ncSignatures) {
                if (sig == null || sig.isEmpty()) {
                    continue;
                }
                if (key.length() > 0) {
                    key.append("|");
                }
                key.append(sig);
            }
        }
        return key.toString();
    }

    public static String itemSignature(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }

        StringBuilder signature = new StringBuilder();
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        signature.append(registryName != null ? registryName : Item.getIdFromItem(stack.getItem()))
            .append("@")
            .append(stack.getItemDamage());

        if (stack.hasTagCompound()) {
            NBTTagCompound tagCopy = (NBTTagCompound) stack.getTagCompound()
                .copy();
            tagCopy.removeTag("Count");
            tagCopy.removeTag("Cnt");
            if (!tagCopy.hasNoTags()) {
                signature.append("@")
                    .append(tagCopy);
            }
        }

        return signature.toString();
    }

    public static void setCircuitMethod(Method method) {
        GT_IS_ANY_INTEGRATED_CIRCUIT = method;
        circuitMethodInitialized = true;
    }

    private static Method findGtCircuitMethod() {
        try {
            Class<?> gtUtility = Class.forName("gregtech.api.util.GTUtility");
            return gtUtility.getMethod("isAnyIntegratedCircuit", ItemStack.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
