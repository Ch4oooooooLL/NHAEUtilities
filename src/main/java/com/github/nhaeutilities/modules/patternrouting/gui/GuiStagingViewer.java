package com.github.nhaeutilities.modules.patternrouting.gui;

import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.gtnewhorizons.modularui.api.ModularUITextures;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public final class GuiStagingViewer {

    private static final int GUI_W = 240;
    private static final int GUI_H = 200;

    private GuiStagingViewer() {}

    public static ModularWindow createWindow(UIBuildContext buildContext) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(ModularUITextures.VANILLA_BACKGROUND);

        TextWidget title = new TextWidget(
            EnumChatFormatting.BOLD + t("nhaeutilities.gui.staging_viewer.title", "Staging Storage"));
        title.setScale(1.2f);
        title.setSize(GUI_W - 16, 20);
        title.setPos(8, 8);
        builder.widget(title);

        TextWidget info = new TextWidget(
            t("nhaeutilities.gui.staging_viewer.info", "Stored patterns that failed routing are listed here."));
        info.setPos(8, 34);
        builder.widget(info);

        TextWidget placeholder = new TextWidget(
            EnumChatFormatting.GRAY
                + t("nhaeutilities.gui.staging_viewer.placeholder", "Details coming in a future update."));
        placeholder.setPos(8, 54);
        builder.widget(placeholder);

        int buttonW = 80;
        int buttonH = 18;
        int startX = (GUI_W - buttonW) / 2;

        ButtonWidget closeButton = new ButtonWidget();
        closeButton.setPos(startX, GUI_H - 38);
        closeButton.setSize(buttonW, buttonH);
        closeButton.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        closeButton.setSynced(false, false);
        closeButton.setOnClick((cd, w) -> NHAEUtilities.proxy.closeCurrentScreen());
        builder.widget(closeButton);

        TextWidget closeText = new TextWidget(t("nhaeutilities.gui.pattern_routing_analysis.button.close", "Close"));
        closeText.setPos(startX + 25, GUI_H - 35);
        builder.widget(closeText);

        return builder.build();
    }

    private static String t(String key, String fallback, Object... args) {
        return I18nUtil.trOr(key, fallback, args);
    }
}
