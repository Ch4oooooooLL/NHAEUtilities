package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import java.util.Objects;

import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;

import appeng.me.GridNode;
import appeng.tile.networking.TileController;

public final class ResolvedControllerEndpoint {

    private final ControllerEndpointRef ref;
    private final TileController controller;
    private final GridNode node;
    private final ForgeDirection face;

    public ResolvedControllerEndpoint(ControllerEndpointRef ref, TileController controller, GridNode node,
        ForgeDirection face) {
        this.ref = Objects.requireNonNull(ref, "ref");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.node = Objects.requireNonNull(node, "node");
        this.face = Objects.requireNonNull(face, "face");
    }

    public ControllerEndpointRef getRef() {
        return ref;
    }

    public TileController getController() {
        return controller;
    }

    public GridNode getNode() {
        return node;
    }

    public ForgeDirection getFace() {
        return face;
    }
}
