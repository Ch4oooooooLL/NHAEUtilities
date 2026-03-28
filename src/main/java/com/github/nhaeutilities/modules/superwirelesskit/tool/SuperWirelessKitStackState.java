package com.github.nhaeutilities.modules.superwirelesskit.tool;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;

public final class SuperWirelessKitStackState {

    private static final String MODE_KEY = "mode";
    private static final String CONTROLLER_KEY = "controller";
    private static final String QUEUED_TARGETS_KEY = "queuedTargets";
    private static final String PENDING_BINDINGS_KEY = "pendingBindings";
    private static final String LIST_ENTRY_KEY = "entry";
    private static final int COMPOUND_TAG_ID = 10;

    private SuperWirelessKitStackState() {}

    public static SuperWirelessKitMode getMode(ItemStack stack) {
        NBTTagCompound tag = getOrCreateTag(stack);
        if (!tag.hasKey(MODE_KEY)) {
            return SuperWirelessKitMode.QUEUE;
        }
        try {
            return SuperWirelessKitMode.valueOf(tag.getString(MODE_KEY));
        } catch (IllegalArgumentException ignored) {
            return SuperWirelessKitMode.QUEUE;
        }
    }

    public static void setMode(ItemStack stack, SuperWirelessKitMode mode) {
        getOrCreateTag(stack).setString(
            MODE_KEY,
            Objects.requireNonNull(mode, "mode")
                .name());
    }

    public static SuperWirelessKitMode toggleMode(ItemStack stack) {
        SuperWirelessKitMode next = getMode(stack).next();
        setMode(stack, next);
        return next;
    }

    public static boolean hasController(ItemStack stack) {
        return getController(stack) != null;
    }

    public static ControllerEndpointRef getController(ItemStack stack) {
        NBTTagCompound tag = getExistingTag(stack);
        if (tag == null || !tag.hasKey(CONTROLLER_KEY)) {
            return null;
        }
        return ControllerEndpointRef.fromNbt(tag.getCompoundTag(CONTROLLER_KEY));
    }

    public static void setController(ItemStack stack, ControllerEndpointRef controller) {
        NBTTagCompound tag = getOrCreateTag(stack);
        if (controller == null) {
            tag.removeTag(CONTROLLER_KEY);
        } else {
            tag.setTag(CONTROLLER_KEY, controller.toNbt());
        }
    }

    public static List<BindingTargetRef> getQueuedTargets(ItemStack stack) {
        return readTargetList(stack, QUEUED_TARGETS_KEY);
    }

    public static void clearQueuedTargets(ItemStack stack) {
        getOrCreateTag(stack).removeTag(QUEUED_TARGETS_KEY);
    }

    public static void addQueuedTarget(ItemStack stack, BindingTargetRef target) {
        Objects.requireNonNull(target, "target");
        List<BindingTargetRef> targets = new ArrayList<BindingTargetRef>(getQueuedTargets(stack));
        if (!targets.contains(target)) {
            targets.add(target);
            writeTargetList(stack, QUEUED_TARGETS_KEY, targets);
        }
    }

    public static List<BindingRecord> getPendingBindings(ItemStack stack) {
        NBTTagCompound tag = getExistingTag(stack);
        if (tag == null || !tag.hasKey(PENDING_BINDINGS_KEY)) {
            return Collections.emptyList();
        }
        NBTTagList list = tag.getTagList(PENDING_BINDINGS_KEY, COMPOUND_TAG_ID);
        List<BindingRecord> records = new ArrayList<BindingRecord>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            records.add(
                BindingRecord.fromNbt(
                    list.getCompoundTagAt(i)
                        .getCompoundTag(LIST_ENTRY_KEY)));
        }
        return Collections.unmodifiableList(records);
    }

    public static void clearPendingBindings(ItemStack stack) {
        getOrCreateTag(stack).removeTag(PENDING_BINDINGS_KEY);
    }

    public static void addPendingBinding(ItemStack stack, BindingRecord record) {
        Objects.requireNonNull(record, "record");
        List<BindingRecord> records = new ArrayList<BindingRecord>(getPendingBindings(stack));
        if (!records.contains(record)) {
            records.add(record);
            writeBindingList(stack, PENDING_BINDINGS_KEY, records);
        }
    }

    public static void setPendingBindings(ItemStack stack, List<BindingRecord> records) {
        Objects.requireNonNull(records, "records");
        if (records.isEmpty()) {
            clearPendingBindings(stack);
        } else {
            writeBindingList(stack, PENDING_BINDINGS_KEY, records);
        }
    }

    public static List<BindingRecord> promoteQueuedTargetsToBindings(ItemStack stack, ControllerEndpointRef controller,
        int binderPlayerId, UUID binderUuid, long createdAt) {
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(binderUuid, "binderUuid");

        List<BindingTargetRef> queuedTargets = getQueuedTargets(stack);
        if (queuedTargets.isEmpty()) {
            setController(stack, controller);
            return Collections.emptyList();
        }

        List<BindingRecord> promoted = new ArrayList<BindingRecord>(queuedTargets.size());
        for (BindingTargetRef target : queuedTargets) {
            promoted.add(
                new BindingRecord(
                    createBindingId(controller, target, binderUuid, createdAt),
                    controller,
                    target,
                    binderPlayerId,
                    binderUuid,
                    createdAt));
        }

        setController(stack, controller);
        clearQueuedTargets(stack);
        List<BindingRecord> existing = new ArrayList<BindingRecord>(getPendingBindings(stack));
        for (BindingRecord record : promoted) {
            if (!existing.contains(record)) {
                existing.add(record);
            }
        }
        writeBindingList(stack, PENDING_BINDINGS_KEY, existing);
        return Collections.unmodifiableList(promoted);
    }

    private static UUID createBindingId(ControllerEndpointRef controller, BindingTargetRef target, UUID binderUuid,
        long createdAt) {
        String seed = controller.getDimensionId() + ":"
            + controller.getX()
            + ":"
            + controller.getY()
            + ":"
            + controller.getZ()
            + ":"
            + controller.getFace()
                .ordinal()
            + ":"
            + controller.getControllerType()
            + "|"
            + target.getKind()
                .name()
            + ":"
            + target.getDimensionId()
            + ":"
            + target.getX()
            + ":"
            + target.getY()
            + ":"
            + target.getZ()
            + ":"
            + target.getSide()
                .ordinal()
            + ":"
            + target.getFingerprint()
                .getHostBlockId()
            + ":"
            + target.getFingerprint()
                .getTargetType()
            + "|"
            + binderUuid
            + "|"
            + createdAt;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static List<BindingTargetRef> readTargetList(ItemStack stack, String key) {
        NBTTagCompound tag = getExistingTag(stack);
        if (tag == null || !tag.hasKey(key)) {
            return Collections.emptyList();
        }
        NBTTagList list = tag.getTagList(key, COMPOUND_TAG_ID);
        List<BindingTargetRef> targets = new ArrayList<BindingTargetRef>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            targets.add(
                BindingTargetRef.fromNbt(
                    list.getCompoundTagAt(i)
                        .getCompoundTag(LIST_ENTRY_KEY)));
        }
        return Collections.unmodifiableList(targets);
    }

    private static void writeTargetList(ItemStack stack, String key, List<BindingTargetRef> targets) {
        NBTTagList list = new NBTTagList();
        for (BindingTargetRef target : targets) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag(LIST_ENTRY_KEY, target.toNbt());
            list.appendTag(entry);
        }
        getOrCreateTag(stack).setTag(key, list);
    }

    private static void writeBindingList(ItemStack stack, String key, List<BindingRecord> records) {
        NBTTagList list = new NBTTagList();
        for (BindingRecord record : records) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag(LIST_ENTRY_KEY, record.toNbt());
            list.appendTag(entry);
        }
        getOrCreateTag(stack).setTag(key, list);
    }

    private static NBTTagCompound getExistingTag(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        return stack.getTagCompound();
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (stack == null) {
            throw new IllegalArgumentException("stack");
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
