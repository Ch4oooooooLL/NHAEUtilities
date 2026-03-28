package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public final class BindingTargetRef {

    private static final String TARGET_KIND_KEY = "targetKind";
    private static final String DIMENSION_ID_KEY = "dimensionId";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String SIDE_KEY = "side";
    private static final String FINGERPRINT_KEY = "fingerprint";

    private final BindingTargetKind kind;
    private final int dimensionId;
    private final int x;
    private final int y;
    private final int z;
    private final ForgeDirection side;
    private final BindingFingerprint fingerprint;

    public BindingTargetRef(BindingTargetKind kind, int dimensionId, int x, int y, int z, ForgeDirection side,
        BindingFingerprint fingerprint) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.side = Objects.requireNonNull(side, "side");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public BindingTargetKind getKind() {
        return kind;
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

    public ForgeDirection getSide() {
        return side;
    }

    public BindingFingerprint getFingerprint() {
        return fingerprint;
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(TARGET_KIND_KEY, kind.name());
        tag.setInteger(DIMENSION_ID_KEY, dimensionId);
        tag.setInteger(X_KEY, x);
        tag.setInteger(Y_KEY, y);
        tag.setInteger(Z_KEY, z);
        tag.setInteger(SIDE_KEY, side.ordinal());
        tag.setTag(FINGERPRINT_KEY, fingerprint.toNbt());
        return tag;
    }

    public static BindingTargetRef fromNbt(NBTTagCompound tag) {
        return new BindingTargetRef(
            BindingTargetKind.valueOf(tag.getString(TARGET_KIND_KEY)),
            tag.getInteger(DIMENSION_ID_KEY),
            tag.getInteger(X_KEY),
            tag.getInteger(Y_KEY),
            tag.getInteger(Z_KEY),
            ForgeDirection.getOrientation(tag.getInteger(SIDE_KEY)),
            BindingFingerprint.fromNbt(tag.getCompoundTag(FINGERPRINT_KEY)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BindingTargetRef)) {
            return false;
        }
        BindingTargetRef that = (BindingTargetRef) o;
        return dimensionId == that.dimensionId && x == that.x
            && y == that.y
            && z == that.z
            && kind == that.kind
            && side == that.side
            && fingerprint.equals(that.fingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, dimensionId, x, y, z, side, fingerprint);
    }
}
