package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import java.util.Objects;

import net.minecraft.tileentity.TileEntity;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;

import appeng.me.GridNode;

public final class ResolvedBindingTarget {

    private final BindingTargetRef ref;
    private final TileEntity hostTile;
    private final Object machine;
    private final GridNode node;

    public ResolvedBindingTarget(BindingTargetRef ref, TileEntity hostTile, Object machine, GridNode node) {
        this.ref = Objects.requireNonNull(ref, "ref");
        this.hostTile = Objects.requireNonNull(hostTile, "hostTile");
        this.machine = Objects.requireNonNull(machine, "machine");
        this.node = Objects.requireNonNull(node, "node");
    }

    public BindingTargetRef getRef() {
        return ref;
    }

    public TileEntity getHostTile() {
        return hostTile;
    }

    public Object getMachine() {
        return machine;
    }

    public GridNode getNode() {
        return node;
    }
}
