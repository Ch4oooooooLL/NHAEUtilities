package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Objects;

public final class BindingBlockRef {

    private final int dimensionId;
    private final int x;
    private final int y;
    private final int z;

    public BindingBlockRef(int dimensionId, int x, int y, int z) {
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BindingChunkRef toChunkRef() {
        return new BindingChunkRef(dimensionId, x >> 4, z >> 4);
    }

    public static BindingBlockRef of(ControllerEndpointRef controller) {
        return new BindingBlockRef(
            controller.getDimensionId(),
            controller.getX(),
            controller.getY(),
            controller.getZ());
    }

    public static BindingBlockRef of(BindingTargetRef target) {
        return new BindingBlockRef(target.getDimensionId(), target.getX(), target.getY(), target.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BindingBlockRef)) {
            return false;
        }
        BindingBlockRef that = (BindingBlockRef) o;
        return dimensionId == that.dimensionId && x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensionId, x, y, z);
    }
}
