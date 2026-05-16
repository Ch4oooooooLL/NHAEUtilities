package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketStorageAction;
import com.github.nhaeutilities.modules.shared.NHTextures;
import com.github.nhaeutilities.modules.shared.animation.ScreenHelper;

public class GuiPatternDetail {

    private static final int GUI_W = 260;
    private static final int GUI_H = 220;

    public static void open(EntityPlayer player, int patternIndex, List<String> inputNames, List<String> outputNames) {
        if (player == null) {
            return;
        }
        ScreenHelper.open(createWindow(patternIndex, inputNames, outputNames));
    }

    public static void openFromNetwork(int patternIndex, List<String> inputNames, List<String> outputNames) {
        ScreenHelper.open(createWindow(patternIndex, inputNames, outputNames));
    }

    public static ModularPanel createWindow(int patternIndex, List<String> inputNames, List<String> outputNames) {
        ModularPanel panel = ModularPanel.defaultPanel("pattern_detail", GUI_W, GUI_H)
            .background(GuiTextures.MC_BACKGROUND);

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_detail.title", "Pattern %s", patternIndex + 1))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(8, 8)
                        .size(GUI_W - 16, 20));

        ScrollWidget<?> scrollable = new ScrollWidget<>(new VerticalScrollData()).pos(8, 24)
            .size(GUI_W - 16, GUI_H - 24 - 32);

        int y = 0;
        TextWidget inTitle = new TextWidget(
            IKey.str(t("nhaeutilities.gui.pattern_detail.input.title", "Inputs (%s)", inputNames.size()))
                .style(EnumChatFormatting.BOLD));
        inTitle.pos(4, y);
        scrollable.child(inTitle);
        y += 12;

        if (inputNames.isEmpty()) {
            TextWidget emptyIn = new TextWidget(
                IKey.str(t("nhaeutilities.gui.common.none", "None"))
                    .style(EnumChatFormatting.GRAY));
            emptyIn.pos(8, y);
            scrollable.child(emptyIn);
            y += 12;
        } else {
            for (String name : inputNames) {
                TextWidget row = new TextWidget(
                    IKey.str("- " + name)
                        .style(EnumChatFormatting.GRAY));
                row.pos(8, y);
                scrollable.child(row);
                y += 12;
            }
        }
        y += 8;

        TextWidget outTitle = new TextWidget(
            IKey.str(t("nhaeutilities.gui.pattern_detail.output.title", "Outputs (%s)", outputNames.size()))
                .style(EnumChatFormatting.BOLD));
        outTitle.pos(4, y);
        scrollable.child(outTitle);
        y += 12;

        if (outputNames.isEmpty()) {
            TextWidget emptyOut = new TextWidget(
                IKey.str(t("nhaeutilities.gui.common.none", "None"))
                    .style(EnumChatFormatting.GRAY));
            emptyOut.pos(8, y);
            scrollable.child(emptyOut);
            y += 12;
        } else {
            for (String name : outputNames) {
                TextWidget row = new TextWidget(
                    IKey.str("- " + name)
                        .style(EnumChatFormatting.GREEN));
                row.pos(8, y);
                scrollable.child(row);
                y += 12;
            }
        }
        scrollable.getScrollArea()
            .getScrollY()
            .setScrollSize(y > 0 ? y : 12);
        panel.child(scrollable);

        int btnW = 90;
        int btnH = 20;
        int btnY = GUI_H - 28;

        panel.child(
            NHTextures.createButton()
                .pos(GUI_W / 2 - btnW - 4, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_detail.button.delete", "Delete"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    NetworkHandler.INSTANCE
                        .sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_DELETE, patternIndex));
                    GuiPatternStorage.open(null);
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(GUI_W / 2 + 4, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.common.back", "Back"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    GuiPatternStorage.open(null);
                    return true;
                }));

        return panel;
    }

    private static String t(String key, String fallback, Object... args) {
        return com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil.trOr(key, fallback, args);
    }
}
