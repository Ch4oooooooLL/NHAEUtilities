package com.github.nhaeutilities.modules.patternrouting.util;

import java.lang.reflect.Method;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.recipe.RecipeMap;

public final class MachineRecipeMapResolver {

    private MachineRecipeMapResolver() {}

    public static RecipeMap<?> resolve(Object metaTile) {
        if (!(metaTile instanceof IMetaTileEntity)) {
            return null;
        }

        if (metaTile instanceof RecipeMapWorkable) {
            return ((RecipeMapWorkable) metaTile).getRecipeMap();
        }

        try {
            Method method = metaTile.getClass()
                .getMethod("getRecipeMap");
            Object result = method.invoke(metaTile);
            return result instanceof RecipeMap<?> ? (RecipeMap<?>) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
