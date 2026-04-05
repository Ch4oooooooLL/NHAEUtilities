package com.github.nhaeutilities.modules.superwirelesskit.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingBlockRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingChunkRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.SuperWirelessSavedData;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessDebugLog;

public class BindingRegistry {

    public static final int MAX_BINDINGS_PER_FACE = 32;

    private final SuperWirelessSavedData savedData;
    private final Map<BindingTargetRef, UUID> bindingIdsByTarget = new LinkedHashMap<BindingTargetRef, UUID>();
    private final Map<ControllerEndpointRef, Set<UUID>> bindingIdsByFace = new LinkedHashMap<ControllerEndpointRef, Set<UUID>>();
    private final Map<BindingBlockRef, Set<UUID>> bindingIdsByControllerBlock = new LinkedHashMap<BindingBlockRef, Set<UUID>>();
    private final Map<BindingBlockRef, Set<UUID>> bindingIdsByTargetBlock = new LinkedHashMap<BindingBlockRef, Set<UUID>>();
    private final Map<BindingChunkRef, Set<UUID>> bindingIdsByControllerChunk = new LinkedHashMap<BindingChunkRef, Set<UUID>>();
    private final Map<BindingChunkRef, Set<UUID>> bindingIdsByTargetChunk = new LinkedHashMap<BindingChunkRef, Set<UUID>>();

    public BindingRegistry(SuperWirelessSavedData savedData) {
        this.savedData = Objects.requireNonNull(savedData, "savedData");
        for (BindingRecord record : savedData.values()) {
            index(record);
        }
    }

    public boolean add(BindingRecord record) {
        Objects.requireNonNull(record, "record");

        if (findByTarget(record.getTarget()) != null) {
            SuperWirelessDebugLog.log(
                "REGISTRY_ADD_REJECT_DUPLICATE_TARGET",
                "bindingId=%s controller=%s target=%s",
                record.getBindingId(),
                formatController(record.getController()),
                formatTarget(record.getTarget()));
            return false;
        }
        if (getBindingsForFace(record.getController()).size() >= MAX_BINDINGS_PER_FACE) {
            SuperWirelessDebugLog.log(
                "REGISTRY_ADD_REJECT_FACE_LIMIT",
                "bindingId=%s controller=%s limit=%d",
                record.getBindingId(),
                formatController(record.getController()),
                Integer.valueOf(MAX_BINDINGS_PER_FACE));
            return false;
        }
        savedData.put(record);
        index(record);
        SuperWirelessDebugLog.log(
            "REGISTRY_ADD",
            "bindingId=%s controller=%s target=%s",
            record.getBindingId(),
            formatController(record.getController()),
            formatTarget(record.getTarget()));
        return true;
    }

    public BindingRecord findByTarget(BindingTargetRef target) {
        UUID bindingId = bindingIdsByTarget.get(target);
        return bindingId != null ? savedData.get(bindingId) : null;
    }

    public BindingRecord findById(UUID bindingId) {
        return bindingId != null ? savedData.get(bindingId) : null;
    }

    public List<BindingRecord> getBindingsForFace(ControllerEndpointRef controller) {
        return resolveBindingIds(bindingIdsByFace.get(controller));
    }

    public BindingRecord remove(UUID bindingId) {
        BindingRecord removed = savedData.remove(bindingId);
        if (removed != null) {
            deindex(removed);
            SuperWirelessDebugLog.log(
                "REGISTRY_REMOVE",
                "bindingId=%s controller=%s target=%s",
                bindingId,
                formatController(removed.getController()),
                formatTarget(removed.getTarget()));
        } else {
            SuperWirelessDebugLog.log("REGISTRY_REMOVE_MISS", "bindingId=%s", bindingId);
        }
        return removed;
    }

    private static String formatController(ControllerEndpointRef controller) {
        return controller.getDimensionId() + ":"
            + controller.getX()
            + ","
            + controller.getY()
            + ","
            + controller.getZ()
            + "/"
            + controller.getFace()
                .name();
    }

    private static String formatTarget(BindingTargetRef target) {
        return target.getKind()
            .name() + "@"
            + target.getDimensionId()
            + ":"
            + target.getX()
            + ","
            + target.getY()
            + ","
            + target.getZ()
            + "/"
            + target.getSide()
                .name();
    }

    public List<BindingRecord> getBindingsTouchingBlock(BindingBlockRef block) {
        LinkedHashSet<UUID> bindingIds = new LinkedHashSet<UUID>();
        addAll(bindingIds, bindingIdsByControllerBlock.get(block));
        addAll(bindingIds, bindingIdsByTargetBlock.get(block));
        return resolveBindingIds(bindingIds);
    }

    public List<BindingRecord> getBindingsTouchingChunk(BindingChunkRef chunk) {
        LinkedHashSet<UUID> bindingIds = new LinkedHashSet<UUID>();
        addAll(bindingIds, bindingIdsByControllerChunk.get(chunk));
        addAll(bindingIds, bindingIdsByTargetChunk.get(chunk));
        return resolveBindingIds(bindingIds);
    }

    public Collection<BindingRecord> values() {
        return savedData.values();
    }

    private void index(BindingRecord record) {
        bindingIdsByTarget.put(record.getTarget(), record.getBindingId());
        index(bindingIdsByFace, record.getController(), record.getBindingId());

        BindingBlockRef controllerBlock = BindingBlockRef.of(record.getController());
        BindingBlockRef targetBlock = BindingBlockRef.of(record.getTarget());
        index(bindingIdsByControllerBlock, controllerBlock, record.getBindingId());
        index(bindingIdsByTargetBlock, targetBlock, record.getBindingId());
        index(bindingIdsByControllerChunk, controllerBlock.toChunkRef(), record.getBindingId());
        index(bindingIdsByTargetChunk, targetBlock.toChunkRef(), record.getBindingId());
    }

    private void deindex(BindingRecord record) {
        bindingIdsByTarget.remove(record.getTarget());
        remove(bindingIdsByFace, record.getController(), record.getBindingId());

        BindingBlockRef controllerBlock = BindingBlockRef.of(record.getController());
        BindingBlockRef targetBlock = BindingBlockRef.of(record.getTarget());
        remove(bindingIdsByControllerBlock, controllerBlock, record.getBindingId());
        remove(bindingIdsByTargetBlock, targetBlock, record.getBindingId());
        remove(bindingIdsByControllerChunk, controllerBlock.toChunkRef(), record.getBindingId());
        remove(bindingIdsByTargetChunk, targetBlock.toChunkRef(), record.getBindingId());
    }

    private static <K> void index(Map<K, Set<UUID>> index, K key, UUID bindingId) {
        Set<UUID> bindingIds = index.get(key);
        if (bindingIds == null) {
            bindingIds = new LinkedHashSet<UUID>();
            index.put(key, bindingIds);
        }
        bindingIds.add(bindingId);
    }

    private static <K> void remove(Map<K, Set<UUID>> index, K key, UUID bindingId) {
        Set<UUID> bindingIds = index.get(key);
        if (bindingIds == null) {
            return;
        }
        bindingIds.remove(bindingId);
        if (bindingIds.isEmpty()) {
            index.remove(key);
        }
    }

    private List<BindingRecord> resolveBindingIds(Set<UUID> bindingIds) {
        List<BindingRecord> matches = new ArrayList<BindingRecord>();
        addResolved(matches, bindingIds);
        return matches;
    }

    private void addResolved(List<BindingRecord> matches, Iterable<UUID> bindingIds) {
        if (bindingIds == null) {
            return;
        }
        for (UUID bindingId : bindingIds) {
            BindingRecord record = savedData.get(bindingId);
            if (record != null) {
                matches.add(record);
            }
        }
    }

    private static void addAll(Set<UUID> target, Set<UUID> source) {
        if (source != null) {
            target.addAll(source);
        }
    }
}
