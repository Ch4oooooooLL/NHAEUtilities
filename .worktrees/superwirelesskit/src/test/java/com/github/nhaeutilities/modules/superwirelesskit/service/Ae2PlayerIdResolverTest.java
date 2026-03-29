package com.github.nhaeutilities.modules.superwirelesskit.service;

import static org.junit.Assert.assertEquals;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;

import com.mojang.authlib.GameProfile;

import appeng.core.worlddata.IWorldCompassData;
import appeng.core.worlddata.IWorldData;
import appeng.core.worlddata.IWorldDimensionData;
import appeng.core.worlddata.IWorldGridStorageData;
import appeng.core.worlddata.IWorldPlayerData;
import appeng.core.worlddata.IWorldSpawnData;

public class Ae2PlayerIdResolverTest {

    @Test
    public void prefersAe2PlayerIdWhenWorldDataIsAvailable() {
        GameProfile profile = new GameProfile(
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "tester");

        assertEquals(4242, Ae2PlayerIdResolver.resolvePlayerId(profile, 99, createWorldData(4242)));
    }

    @Test
    public void fallsBackWhenWorldDataIsUnavailable() {
        GameProfile profile = new GameProfile(
            java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "tester");

        assertEquals(99, Ae2PlayerIdResolver.resolvePlayerId(profile, 99, null));
    }

    private static IWorldData createWorldData(final int playerId) {
        return new IWorldData() {

            @Override
            public void onServerStopping() {}

            @Override
            public void onServerStoppped() {}

            @Override
            public @Nonnull IWorldGridStorageData storageData() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nonnull IWorldPlayerData playerData() {
                return new IWorldPlayerData() {

                    @Override
                    public EntityPlayer getPlayerFromID(int playerID) {
                        return null;
                    }

                    @Override
                    public int getPlayerID(GameProfile profile) {
                        return playerId;
                    }
                };
            }

            @Override
            public @Nonnull IWorldDimensionData dimensionData() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nonnull IWorldCompassData compassData() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nonnull IWorldSpawnData spawnData() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
