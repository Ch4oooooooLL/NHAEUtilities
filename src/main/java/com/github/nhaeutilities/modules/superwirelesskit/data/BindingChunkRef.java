package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Objects;

public final class BindingChunkRef {

    private final int dimensionId;
    private final int chunkX;
    private final int chunkZ;

    public BindingChunkRef(int dimensionId, int chunkX, int chunkZ) {
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BindingChunkRef)) {
            return false;
        }
        BindingChunkRef that = (BindingChunkRef) o;
        return dimensionId == that.dimensionId && chunkX == that.chunkX && chunkZ == that.chunkZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensionId, chunkX, chunkZ);
    }
}
