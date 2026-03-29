package com.github.nhaeutilities.modules.patterngenerator.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class RecipeEntry {

    public final String sourceType;
    public final String recipeMapId;
    public final String machineDisplayName;
    public final ItemStack[] inputs;
    public final ItemStack[] outputs;
    public final FluidStack[] fluidInputs;
    public final FluidStack[] fluidOutputs;
    public final ItemStack[] specialItems;
    public final int duration;
    public final int euPerTick;

    public RecipeEntry(String sourceType, String recipeMapId, String machineDisplayName, ItemStack[] inputs,
        ItemStack[] outputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs, ItemStack[] specialItems,
        int duration, int euPerTick) {
        this.sourceType = sourceType;
        this.recipeMapId = recipeMapId;
        this.machineDisplayName = machineDisplayName;
        this.inputs = inputs != null ? inputs : new ItemStack[0];
        this.outputs = outputs != null ? outputs : new ItemStack[0];
        this.fluidInputs = fluidInputs != null ? fluidInputs : new FluidStack[0];
        this.fluidOutputs = fluidOutputs != null ? fluidOutputs : new FluidStack[0];
        this.specialItems = specialItems != null ? specialItems : new ItemStack[0];
        this.duration = duration;
        this.euPerTick = euPerTick;
    }
}
