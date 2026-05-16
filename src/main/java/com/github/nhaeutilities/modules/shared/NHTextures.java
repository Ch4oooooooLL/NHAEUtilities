package com.github.nhaeutilities.modules.shared;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.widgets.ButtonWidget;

/**
 * Custom textures that fix MUI2 rendering artifacts.
 */
public final class NHTextures {

    /**
     * Vanilla button background with corrected UV to avoid the 1px line artifact
     * that occurs with MC_BUTTON at the UV=0.5 boundary between normal and hover states.
     */
    public static final UITexture BUTTON = UITexture.builder()
        .location("modularui2", "gui/widgets/mc_button")
        .imageSize(16, 32)
        .uv(0f, 0f, 1f, 0.49f)
        .adaptable(2)
        .name("nh_button")
        .build();

    /**
     * Creates a ButtonWidget pre-configured with NH button background and hover disabled.
     * Without disabling hover, MUI2 auto-swaps to the theme's MC_BUTTON_HOVERED texture
     * which causes visual stripes/artifacts on hover.
     */
    public static ButtonWidget<?> createButton() {
        ButtonWidget<?> btn = new ButtonWidget<>();
        btn.background(BUTTON);
        btn.disableHoverBackground();
        return btn;
    }

    private NHTextures() {}
}
