package com.github.nhaeutilities.modules.superwirelesskit.data;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

public final class BindingRecord {

    private static final String BINDING_ID_KEY = "bindingId";
    private static final String CONTROLLER_KEY = "controller";
    private static final String TARGET_KEY = "target";
    private static final String BINDER_PLAYER_ID_KEY = "binderPlayerId";
    private static final String BINDER_UUID_MOST_KEY = "binderUuidMost";
    private static final String BINDER_UUID_LEAST_KEY = "binderUuidLeast";
    private static final String CREATED_AT_KEY = "createdAt";

    private final UUID bindingId;
    private final ControllerEndpointRef controller;
    private final BindingTargetRef target;
    private final int binderPlayerId;
    private final UUID binderUuid;
    private final long createdAt;

    public BindingRecord(UUID bindingId, ControllerEndpointRef controller, BindingTargetRef target, int binderPlayerId,
        UUID binderUuid, long createdAt) {
        this.bindingId = Objects.requireNonNull(bindingId, "bindingId");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.target = Objects.requireNonNull(target, "target");
        this.binderPlayerId = binderPlayerId;
        this.binderUuid = Objects.requireNonNull(binderUuid, "binderUuid");
        this.createdAt = createdAt;
    }

    public UUID getBindingId() {
        return bindingId;
    }

    public ControllerEndpointRef getController() {
        return controller;
    }

    public BindingTargetRef getTarget() {
        return target;
    }

    public int getBinderPlayerId() {
        return binderPlayerId;
    }

    public UUID getBinderUuid() {
        return binderUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(BINDING_ID_KEY, bindingId.toString());
        tag.setTag(CONTROLLER_KEY, controller.toNbt());
        tag.setTag(TARGET_KEY, target.toNbt());
        tag.setInteger(BINDER_PLAYER_ID_KEY, binderPlayerId);
        tag.setLong(BINDER_UUID_MOST_KEY, binderUuid.getMostSignificantBits());
        tag.setLong(BINDER_UUID_LEAST_KEY, binderUuid.getLeastSignificantBits());
        tag.setLong(CREATED_AT_KEY, createdAt);
        return tag;
    }

    public static BindingRecord fromNbt(NBTTagCompound tag) {
        return new BindingRecord(
            UUID.fromString(tag.getString(BINDING_ID_KEY)),
            ControllerEndpointRef.fromNbt(tag.getCompoundTag(CONTROLLER_KEY)),
            BindingTargetRef.fromNbt(tag.getCompoundTag(TARGET_KEY)),
            tag.getInteger(BINDER_PLAYER_ID_KEY),
            new UUID(tag.getLong(BINDER_UUID_MOST_KEY), tag.getLong(BINDER_UUID_LEAST_KEY)),
            tag.getLong(CREATED_AT_KEY));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BindingRecord)) {
            return false;
        }
        BindingRecord that = (BindingRecord) o;
        return binderPlayerId == that.binderPlayerId && createdAt == that.createdAt
            && bindingId.equals(that.bindingId)
            && controller.equals(that.controller)
            && target.equals(that.target)
            && binderUuid.equals(that.binderUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindingId, controller, target, binderPlayerId, binderUuid, createdAt);
    }
}
