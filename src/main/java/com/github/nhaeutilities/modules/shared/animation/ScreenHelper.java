package com.github.nhaeutilities.modules.shared.animation;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;

/**
 * Helpers for opening MUI2 screens with NEI enabled.
 */
public final class ScreenHelper {

    private ScreenHelper() {}

    /**
     * Opens a modular screen with NEI enabled (required for NEI item panel, drag-and-drop, etc.)
     */
    public static void open(ModularPanel panel) {
        ClientGUI.open(new ModularScreen(panel), createNeiSettings());
    }

    public static void open(ModularScreen screen) {
        ClientGUI.open(screen, createNeiSettings());
    }

    private static UISettings createNeiSettings() {
        UISettings settings = new UISettings();
        settings.customContainer(ModularContainer::new);
        settings.getNEISettings()
            .enableNEI();
        return settings;
    }
}
