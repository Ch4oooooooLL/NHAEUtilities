package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;

public final class BindingFingerprint {

    private static final String HOST_BLOCK_ID_KEY = "hostBlockId";
    private static final String TARGET_TYPE_KEY = "targetType";

    private final String hostBlockId;
    private final String targetType;

    public BindingFingerprint(String hostBlockId, String targetType) {
        this.hostBlockId = Objects.requireNonNull(hostBlockId, "hostBlockId");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    public String getHostBlockId() {
        return hostBlockId;
    }

    public String getTargetType() {
        return targetType;
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(HOST_BLOCK_ID_KEY, hostBlockId);
        tag.setString(TARGET_TYPE_KEY, targetType);
        return tag;
    }

    public static BindingFingerprint fromNbt(NBTTagCompound tag) {
        return new BindingFingerprint(tag.getString(HOST_BLOCK_ID_KEY), tag.getString(TARGET_TYPE_KEY));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BindingFingerprint)) {
            return false;
        }
        BindingFingerprint that = (BindingFingerprint) o;
        return hostBlockId.equals(that.hostBlockId) && targetType.equals(that.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostBlockId, targetType);
    }
}
