package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.shared.NHTextures;

public final class GuiStagingViewer {

    private static final int GUI_W = 240;
    private static final int GUI_H = 200;

    private GuiStagingViewer() {}

    public static ModularPanel createWindow(EntityPlayer player) {
        ModularPanel panel = ModularPanel.defaultPanel("staging_viewer", GUI_W, GUI_H)
            .background(GuiTextures.MC_BACKGROUND);

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.staging_viewer.title", "Staging Storage"))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(8, 8)
                        .size(GUI_W - 16, 20));

        panel.child(
            new TextWidget(
                IKey.str(
                    t("nhaeutilities.gui.staging_viewer.info", "Stored patterns that failed routing are listed here.")))
                        .pos(8, 36));

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.staging_viewer.placeholder", "Details coming in a future update."))
                    .style(EnumChatFormatting.GRAY)).pos(8, 56));

        int buttonW = 64;
        int buttonH = 16;
        int startX = (GUI_W - buttonW) / 2;

        panel.child(
            NHTextures.createButton()
                .pos(startX, GUI_H - 34)
                .size(buttonW, buttonH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.common.close", "Close"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    NHAEUtilities.proxy.closeCurrentScreen();
                    return true;
                }));

        return panel;
    }

    private static String t(String key, String fallback, Object... args) {
        return I18nUtil.trOr(key, fallback, args);
    }
}
