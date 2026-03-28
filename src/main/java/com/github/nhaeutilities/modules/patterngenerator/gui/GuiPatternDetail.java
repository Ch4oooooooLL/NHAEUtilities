package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketStorageAction;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public class GuiPatternDetail {

    private static final int GUI_W = 260;
    private static final int GUI_H = 220;

    public static void open(EntityPlayer player, int patternIndex, List<String> inputNames, List<String> outputNames) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || player == null) {
            return;
        }
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow window = createWindow(buildContext, patternIndex, inputNames, outputNames);
        mc.displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, window)));
    }

    public static ModularWindow createWindow(UIBuildContext buildContext, int patternIndex, List<String> inputNames,
        List<String> outputNames) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget(
            EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_detail.title", "Pattern %s", patternIndex + 1));
        titleText.setScale(1.2f);
        titleText.setSize(GUI_W - 16, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 24);
        scrollable.setSize(GUI_W - 16, GUI_H - 24 - 32);

        int y = 0;
        TextWidget inTitle = new TextWidget(
            EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_detail.input.title", "Inputs (%s)", inputNames.size()));
        inTitle.setPos(4, y);
        scrollable.widget(inTitle);
        y += 12;

        if (inputNames.isEmpty()) {
            TextWidget emptyIn = new TextWidget(EnumChatFormatting.GRAY + t("nhaeutilities.gui.common.none", "None"));
            emptyIn.setPos(8, y);
            scrollable.widget(emptyIn);
            y += 12;
        } else {
            for (String name : inputNames) {
                TextWidget row = new TextWidget(EnumChatFormatting.GRAY + "- " + EnumChatFormatting.WHITE + name);
                row.setPos(8, y);
                scrollable.widget(row);
                y += 12;
            }
        }
        y += 8;

        TextWidget outTitle = new TextWidget(
            EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_detail.output.title", "Outputs (%s)", outputNames.size()));
        outTitle.setPos(4, y);
        scrollable.widget(outTitle);
        y += 12;

        if (outputNames.isEmpty()) {
            TextWidget emptyOut = new TextWidget(EnumChatFormatting.GRAY + t("nhaeutilities.gui.common.none", "None"));
            emptyOut.setPos(8, y);
            scrollable.widget(emptyOut);
            y += 12;
        } else {
            for (String name : outputNames) {
                TextWidget row = new TextWidget(EnumChatFormatting.GREEN + "- " + EnumChatFormatting.WHITE + name);
                row.setPos(8, y);
                scrollable.widget(row);
                y += 12;
            }
        }
        builder.widget(scrollable);

        int btnW = 90;
        int btnH = 20;
        int btnY = GUI_H - 28;

        ButtonWidget btnDelete = new ButtonWidget();
        btnDelete.setPos(GUI_W / 2 - btnW - 4, btnY);
        btnDelete.setSize(btnW, btnH);
        btnDelete.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnDelText = new TextWidget(t("nhaeutilities.gui.pattern_detail.button.delete", "Delete"));
        btnDelText.setPos(GUI_W / 2 - btnW - 4 + 16, btnY + 6);
        btnDelete.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_DELETE, patternIndex));
            GuiPatternStorage.open(Minecraft.getMinecraft().thePlayer);
        });
        builder.widget(btnDelete);
        builder.widget(btnDelText);

        ButtonWidget btnBack = new ButtonWidget();
        btnBack.setPos(GUI_W / 2 + 4, btnY);
        btnBack.setSize(btnW, btnH);
        btnBack.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnBackText = new TextWidget(t("nhaeutilities.gui.common.back", "Back"));
        btnBackText.setPos(GUI_W / 2 + 4 + 32, btnY + 6);
        btnBack.setOnClick((cd, w) -> GuiPatternStorage.open(Minecraft.getMinecraft().thePlayer));
        builder.widget(btnBack);
        builder.widget(btnBackText);

        return builder.build();
    }

    private static String t(String key, String fallback, Object... args) {
        return com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil.trOr(key, fallback, args);
    }
}
