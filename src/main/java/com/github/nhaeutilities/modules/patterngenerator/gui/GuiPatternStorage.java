package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketRequestStorageDetail;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketRequestStorageSummary;
import com.github.nhaeutilities.modules.patterngenerator.network.PacketStorageAction;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;
import com.github.nhaeutilities.modules.shared.NHTextures;
import com.github.nhaeutilities.modules.shared.animation.ScreenHelper;

public class GuiPatternStorage {

    private static final int GUI_W = 260;
    private static final int GUI_H = 260;
    private static final Map<UUID, String> SEARCH_QUERY_BY_PLAYER = new HashMap<UUID, String>();
    private static UUID currentPlayerUUID;

    public static void open(EntityPlayer player) {
        if (player != null) {
            currentPlayerUUID = player.getUniqueID();
        }
        NetworkHandler.INSTANCE.sendToServer(new PacketRequestStorageSummary());
    }

    public static void rebuildWithSummary(PatternStorage.StorageSummary summary) {
        if (summary == null) {
            return;
        }
        ScreenHelper.open(createWindowWithSummary(summary));
    }

    public static ModularPanel createWindowWithSummary(PatternStorage.StorageSummary summary) {
        UUID playerUUID = getCurrentPlayerUUID();
        String searchQuery = getSearchQuery(playerUUID);

        ModularPanel panel = ModularPanel.defaultPanel("pattern_storage", GUI_W, GUI_H)
            .background(GuiTextures.MC_BACKGROUND);

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_storage.title", "Pattern Storage"))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(8, 8)
                        .size(GUI_W - 16, 20));

        if (summary.count == 0) {
            String msg = t("nhaeutilities.gui.pattern_storage.empty", "No stored patterns.");
            panel.child(new TextWidget(IKey.str(msg)).pos(GUI_W / 2 - approximateTextWidth(msg) / 2, 40));
        } else {
            int y = 24;
            panel.child(
                new TextWidget(
                    IKey.str(t("nhaeutilities.gui.pattern_storage.stats.title", "Storage details"))
                        .style(EnumChatFormatting.BOLD)).pos(8, y));
            y += 14;

            panel.child(
                new TextWidget(
                    IKey.str(t("nhaeutilities.gui.pattern_storage.stats.count", "Pattern count: %s", summary.count)))
                        .pos(14, y));
            y += 12;

            panel.child(
                new TextWidget(
                    IKey.str(t("nhaeutilities.gui.pattern_storage.stats.source", "Source: %s", summary.source)))
                        .pos(14, y));
            y += 12;

            String timeStr = summary.timestamp <= 0 ? t("nhaeutilities.gui.common.na", "N/A")
                : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(summary.timestamp));
            panel.child(
                new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_storage.stats.time", "Stored at: %s", timeStr)))
                    .pos(14, y));
            y += 16;

            int searchInputX = 46;
            int searchBtnW = 34;
            int resetBtnW = 34;
            int searchGap = 2;
            int searchFieldW = GUI_W - 16 - searchInputX - searchBtnW - resetBtnW - searchGap * 2;

            panel.child(
                new TextWidget(IKey.str(t("nhaeutilities.gui.pattern_storage.search.label", "Search"))).pos(14, y + 3));

            TextFieldWidget tfSearch = new TextFieldWidget();
            tfSearch.setText(searchQuery);
            tfSearch.pos(searchInputX, y);
            tfSearch.size(searchFieldW, 14);
            tfSearch.setTextColor(0xFFFFFF);
            tfSearch.background(new Rectangle().setColor(0xFF1E1E30));
            tfSearch.setTextAlignment(Alignment.CenterLeft);
            panel.child(tfSearch);

            int btnSearchX = searchInputX + searchFieldW + searchGap;
            panel.child(
                new ButtonWidget<>().pos(btnSearchX, y)
                    .size(searchBtnW, 14)
                    .background(new Rectangle().setColor(0xFF1E1E30))
                    .overlay(
                        IKey.str(t("nhaeutilities.gui.pattern_storage.search.button", "Go"))
                            .shadow(false))
                    .onMousePressed(mb -> {
                        setSearchQuery(playerUUID, tfSearch.getText());
                        open(null);
                        return true;
                    }));

            int btnResetX = btnSearchX + searchBtnW + searchGap;
            panel.child(
                new ButtonWidget<>().pos(btnResetX, y)
                    .size(resetBtnW, 14)
                    .background(new Rectangle().setColor(0xFF1E1E30))
                    .overlay(
                        IKey.str(t("nhaeutilities.gui.pattern_storage.search.reset", "Reset"))
                            .shadow(false))
                    .onMousePressed(mb -> {
                        setSearchQuery(playerUUID, "");
                        open(null);
                        return true;
                    }));

            y += 20;

            List<Integer> filteredIndices = filterIndices(summary.previews, searchQuery);
            panel.child(
                new TextWidget(
                    IKey.str(
                        t(
                            "nhaeutilities.gui.pattern_storage.preview.title",
                            "Patterns: %s / %s",
                            filteredIndices.size(),
                            summary.count))
                        .style(EnumChatFormatting.BOLD)).pos(8, y));
            y += 14;

            ScrollWidget<?> scrollable = new ScrollWidget<>(new VerticalScrollData()).pos(8, y)
                .size(GUI_W - 16, GUI_H - y - 40);

            int listY = 0;
            if (filteredIndices.isEmpty()) {
                scrollable.child(
                    new TextWidget(
                        IKey.str(t("nhaeutilities.gui.pattern_storage.preview.empty", "No results."))
                            .style(EnumChatFormatting.GRAY)).pos(4, listY + 3));
            } else {
                for (int i = 0; i < filteredIndices.size(); i++) {
                    int actualIndex = filteredIndices.get(i);
                    String name = summary.previews.get(actualIndex);

                    ButtonWidget<?> rowBtn = new ButtonWidget<>().pos(0, listY)
                        .size(GUI_W - 28, 14)
                        .background(new Rectangle().setColor(0xFF1E1E30))
                        .overlay(
                            IKey.str("#" + (actualIndex + 1) + "  " + name)
                                .shadow(false))
                        .onMousePressed(mb -> {
                            NetworkHandler.INSTANCE.sendToServer(new PacketRequestStorageDetail(actualIndex));
                            return true;
                        });
                    scrollable.child(rowBtn);
                    listY += 16;
                }
            }

            scrollable.getScrollArea()
                .getScrollY()
                .setScrollSize(listY > 0 ? listY : 12);
            panel.child(scrollable);
        }

        int btnW = 90;
        int btnH = 20;
        int btnY = GUI_H - 32;

        panel.child(
            NHTextures.createButton()
                .pos(GUI_W / 2 - btnW - 4, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_storage.button.extract", "Extract"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    NetworkHandler.INSTANCE.sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_EXTRACT));
                    ClientGUI.close();
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(GUI_W / 2 + 4, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_storage.button.clear", "Clear"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    NetworkHandler.INSTANCE.sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_CLEAR));
                    ClientGUI.close();
                    return true;
                }));

        panel.child(
            new TextWidget(
                IKey.str(
                    t(
                        "nhaeutilities.gui.pattern_storage.footer.export_hint",
                        "Sneak-right-click a container to export stored patterns."))
                    .style(EnumChatFormatting.GRAY)).pos(8, GUI_H - 12));

        return panel;
    }

    private static UUID getCurrentPlayerUUID() {
        if (currentPlayerUUID == null) {
            net.minecraft.entity.player.EntityPlayer p = net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            if (p != null) {
                currentPlayerUUID = p.getUniqueID();
            }
        }
        return currentPlayerUUID;
    }

    private static String getSearchQuery(UUID playerUUID) {
        String query = SEARCH_QUERY_BY_PLAYER.get(playerUUID);
        return query == null ? "" : query;
    }

    private static void setSearchQuery(UUID playerUUID, String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            SEARCH_QUERY_BY_PLAYER.remove(playerUUID);
            return;
        }
        SEARCH_QUERY_BY_PLAYER.put(playerUUID, normalized);
    }

    static List<Integer> filterIndices(List<String> previews, String query) {
        List<Integer> result = new ArrayList<Integer>();
        if (previews == null || previews.isEmpty()) {
            return result;
        }

        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            for (int i = 0; i < previews.size(); i++) {
                result.add(i);
            }
            return result;
        }

        String loweredQuery = normalized.toLowerCase(Locale.ROOT);
        for (int i = 0; i < previews.size(); i++) {
            String preview = previews.get(i);
            if (preview != null && preview.toLowerCase(Locale.ROOT)
                .contains(loweredQuery)) {
                result.add(i);
            }
        }

        return result;
    }

    static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static int approximateTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() * 6;
    }

    private static String t(String key, String fallback, Object... args) {
        return com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil.trOr(key, fallback, args);
    }
}
