package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

final class PatternRoutingRepairScanner {

    interface ControllerScanner {

        List<Object> scan(Object world);
    }

    private PatternRoutingRepairScanner() {}

    static List<Object> scanLoadedControllers(World world) {
        if (world == null || world.loadedTileEntityList == null || world.loadedTileEntityList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> controllers = new ArrayList<Object>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (Object loaded : world.loadedTileEntityList) {
            Object controller = resolveController(loaded);
            if (controller != null && seen.add(controller)) {
                controllers.add(controller);
            }
        }
        return controllers;
    }

    private static Object resolveController(Object loaded) {
        if (loaded == null) {
            return null;
        }
        if (looksLikeController(loaded)) {
            return loaded;
        }
        Object metaTile = invokeNoArg(loaded, "getMetaTileEntity");
        return looksLikeController(metaTile) ? metaTile : null;
    }

    private static boolean looksLikeController(Object candidate) {
        if (candidate == null) {
            return false;
        }
        if (candidate instanceof MTEMultiBlockBase) {
            return true;
        }
        if (candidate instanceof TileEntity || candidate instanceof IGregTechTileEntity) {
            return false;
        }
        return findNoArgMethod(candidate.getClass(), "getDualInputHatches") != null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        Method method = findNoArgMethod(target != null ? target.getClass() : null, methodName);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
