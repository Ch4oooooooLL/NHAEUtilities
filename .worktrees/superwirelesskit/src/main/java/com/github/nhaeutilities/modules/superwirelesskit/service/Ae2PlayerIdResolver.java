package com.github.nhaeutilities.modules.superwirelesskit.service;

import com.mojang.authlib.GameProfile;

import appeng.core.worlddata.IWorldData;

final class Ae2PlayerIdResolver {

    private Ae2PlayerIdResolver() {}

    static int resolvePlayerId(GameProfile profile, int fallbackPlayerId, IWorldData worldData) {
        if (profile == null || worldData == null) {
            return fallbackPlayerId;
        }

        try {
            return worldData.playerData()
                .getPlayerID(profile);
        } catch (RuntimeException ignored) {
            return fallbackPlayerId;
        }
    }
}
