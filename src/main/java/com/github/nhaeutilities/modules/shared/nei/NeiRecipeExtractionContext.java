package com.github.nhaeutilities.modules.shared.nei;

import com.github.nhaeutilities.modules.shared.DebugLog;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NeiRecipeExtractionContext {

    private static final NeiRecipeExtractionContext INSTANCE = new NeiRecipeExtractionContext();

    private volatile boolean active;
    private volatile NeiRecipeExtractionCallback callback;

    private NeiRecipeExtractionContext() {}

    public static NeiRecipeExtractionContext instance() {
        return INSTANCE;
    }

    public void activate(NeiRecipeExtractionCallback callback) {
        DebugLog.info("[NHAE] NeiRecipeExtractionContext.activate called");
        this.callback = callback;
        this.active = true;
    }

    public void deactivate() {
        DebugLog.info("[NHAE] NeiRecipeExtractionContext.deactivate called");
        this.active = false;
        this.callback = null;
    }

    public boolean isActive() {
        return active;
    }

    public NeiRecipeExtractionCallback getCallback() {
        return callback;
    }
}
