package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public final class ControllerEndpointRef {

    private static final String DIMENSION_ID_KEY = "dimensionId";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String FACE_KEY = "face";
    private static final String CONTROLLER_TYPE_KEY = "controllerType";

    private final int dimensionId;
    private final int x;
    private final int y;
    private final int z;
    private final ForgeDirection face;
    private final String controllerType;

    public ControllerEndpointRef(int dimensionId, int x, int y, int z, ForgeDirection face, String controllerType) {
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = Objects.requireNonNull(face, "face");
        this.controllerType = Objects.requireNonNull(controllerType, "controllerType");
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

    public ForgeDirection getFace() {
        return face;
    }

    public String getControllerType() {
        return controllerType;
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(DIMENSION_ID_KEY, dimensionId);
        tag.setInteger(X_KEY, x);
        tag.setInteger(Y_KEY, y);
        tag.setInteger(Z_KEY, z);
        tag.setInteger(FACE_KEY, face.ordinal());
        tag.setString(CONTROLLER_TYPE_KEY, controllerType);
        return tag;
    }

    public static ControllerEndpointRef fromNbt(NBTTagCompound tag) {
        return new ControllerEndpointRef(
            tag.getInteger(DIMENSION_ID_KEY),
            tag.getInteger(X_KEY),
            tag.getInteger(Y_KEY),
            tag.getInteger(Z_KEY),
            ForgeDirection.getOrientation(tag.getInteger(FACE_KEY)),
            tag.getString(CONTROLLER_TYPE_KEY));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ControllerEndpointRef)) {
            return false;
        }
        ControllerEndpointRef that = (ControllerEndpointRef) o;
        return dimensionId == that.dimensionId && x == that.x
            && y == that.y
            && z == that.z
            && face == that.face
            && controllerType.equals(that.controllerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensionId, x, y, z, face, controllerType);
    }
}
