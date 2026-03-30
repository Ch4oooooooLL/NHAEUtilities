package com.github.nhaeutilities.modules.superwirelesskit.service;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingNodeResolver;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingReconcileResult;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.ResolvedBindingTarget;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessDebugLog;

import appeng.me.GridNode;

final class BindingBatchProcessor {

    interface Reconciler {

        BindingReconcileResult reconcile(World world, BindingRecord record);
    }

    private final BindingNodeResolver resolver;
    private final Reconciler reconciler;

    BindingBatchProcessor(BindingNodeResolver resolver, Reconciler reconciler) {
        this.resolver = resolver;
        this.reconciler = reconciler;
    }

    BindingBatchResult bind(World world, ControllerEndpointRef controller, List<BindingRecord> draftedBindings,
        BindingRegistry registry) {
        List<BindingRecord> failures = new ArrayList<BindingRecord>();
        int successCount = 0;

        SuperWirelessDebugLog.log(
            "BIND_BATCH_START",
            "controller=%s drafted=%d existingRegistryBindings=%d",
            formatController(controller),
            Integer.valueOf(draftedBindings.size()),
            Integer.valueOf(
                registry.values()
                    .size()));

        Map<GridNode, BindingRecord> existingByNode = collectExistingBindingsByNode(world, registry);
        Map<GridNode, PendingBindingGroup> pendingGroups = new IdentityHashMap<GridNode, PendingBindingGroup>();

        for (BindingRecord record : draftedBindings) {
            if (record.getController()
                .getDimensionId()
                != record.getTarget()
                    .getDimensionId()
                || record.getController()
                    .getFace() == ForgeDirection.UNKNOWN) {
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_REJECT_CONTROLLER_ENDPOINT",
                    "bindingId=%s controller=%s target=%s",
                    record.getBindingId(),
                    formatController(record.getController()),
                    formatTarget(record));
                failures.add(record);
                continue;
            }

            BindingRecord exactExisting = registry.findByTarget(record.getTarget());
            if (exactExisting != null) {
                if (exactExisting.getController()
                    .equals(controller)) {
                    SuperWirelessDebugLog.log(
                        "BIND_BATCH_ALREADY_BOUND_TARGET",
                        "bindingId=%s existingBindingId=%s target=%s",
                        record.getBindingId(),
                        exactExisting.getBindingId(),
                        formatTarget(record));
                    successCount++;
                } else {
                    SuperWirelessDebugLog.log(
                        "BIND_BATCH_REJECT_TARGET_OWNED_BY_OTHER_CONTROLLER",
                        "bindingId=%s existingBindingId=%s target=%s existingController=%s requestedController=%s",
                        record.getBindingId(),
                        exactExisting.getBindingId(),
                        formatTarget(record),
                        formatController(exactExisting.getController()),
                        formatController(controller));
                    failures.add(record);
                }
                continue;
            }

            ResolvedBindingTarget resolvedTarget = resolver.resolveTarget(world, record.getTarget());
            if (resolvedTarget == null) {
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_RESOLVE_TARGET_MISS",
                    "bindingId=%s target=%s",
                    record.getBindingId(),
                    formatTarget(record));
                failures.add(record);
                continue;
            }

            GridNode targetNode = resolvedTarget.getNode();
            BindingRecord existing = existingByNode.get(targetNode);
            if (existing != null) {
                if (existing.getController()
                    .equals(controller)) {
                    SuperWirelessDebugLog.log(
                        "BIND_BATCH_ALREADY_BOUND_NODE",
                        "bindingId=%s existingBindingId=%s target=%s",
                        record.getBindingId(),
                        existing.getBindingId(),
                        formatTarget(record));
                    successCount++;
                } else {
                    SuperWirelessDebugLog.log(
                        "BIND_BATCH_REJECT_NODE_OWNED_BY_OTHER_CONTROLLER",
                        "bindingId=%s existingBindingId=%s target=%s existingController=%s requestedController=%s",
                        record.getBindingId(),
                        existing.getBindingId(),
                        formatTarget(record),
                        formatController(existing.getController()),
                        formatController(controller));
                    failures.add(record);
                }
                continue;
            }

            PendingBindingGroup group = pendingGroups.get(targetNode);
            if (group == null) {
                group = new PendingBindingGroup(record, targetNode);
                pendingGroups.put(targetNode, group);
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_GROUP_CANONICAL",
                    "bindingId=%s target=%s targetNode=%d",
                    record.getBindingId(),
                    formatTarget(record),
                    Integer.valueOf(System.identityHashCode(targetNode)));
            } else {
                group.members.add(record);
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_GROUP_ALIAS",
                    "bindingId=%s canonicalBindingId=%s target=%s targetNode=%d",
                    record.getBindingId(),
                    group.canonicalRecord.getBindingId(),
                    formatTarget(record),
                    Integer.valueOf(System.identityHashCode(targetNode)));
            }
        }

        for (PendingBindingGroup group : pendingGroups.values()) {
            if (!registry.add(group.canonicalRecord)) {
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_REJECT_REGISTRY_ADD",
                    "bindingId=%s aliases=%d controller=%s",
                    group.canonicalRecord.getBindingId(),
                    Integer.valueOf(group.members.size() - 1),
                    formatController(group.canonicalRecord.getController()));
                failures.addAll(group.members);
                continue;
            }

            BindingReconcileResult reconcileResult = reconciler.reconcile(world, group.canonicalRecord);
            if (reconcileResult == BindingReconcileResult.CONNECTED
                || reconcileResult == BindingReconcileResult.ALREADY_CONNECTED) {
                successCount += group.members.size();
                existingByNode.put(group.targetNode, group.canonicalRecord);
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_GROUP_CONNECTED",
                    "bindingId=%s aliases=%d result=%s",
                    group.canonicalRecord.getBindingId(),
                    Integer.valueOf(group.members.size() - 1),
                    reconcileResult.name());
            } else {
                registry.remove(group.canonicalRecord.getBindingId());
                failures.addAll(group.members);
                SuperWirelessDebugLog.log(
                    "BIND_BATCH_GROUP_FAILED",
                    "bindingId=%s aliases=%d result=%s",
                    group.canonicalRecord.getBindingId(),
                    Integer.valueOf(group.members.size() - 1),
                    reconcileResult.name());
            }
        }

        SuperWirelessDebugLog.log(
            "BIND_BATCH_RESULT",
            "controller=%s success=%d failures=%d pendingGroups=%d",
            formatController(controller),
            Integer.valueOf(successCount),
            Integer.valueOf(failures.size()),
            Integer.valueOf(pendingGroups.size()));
        return new BindingBatchResult(successCount, failures);
    }

    private Map<GridNode, BindingRecord> collectExistingBindingsByNode(World world, BindingRegistry registry) {
        Map<GridNode, BindingRecord> existingByNode = new IdentityHashMap<GridNode, BindingRecord>();
        for (BindingRecord existing : registry.values()) {
            ResolvedBindingTarget resolvedTarget = resolver.resolveTarget(world, existing.getTarget());
            if (resolvedTarget != null) {
                existingByNode.put(resolvedTarget.getNode(), existing);
            }
        }
        return existingByNode;
    }

    private static final class PendingBindingGroup {

        private final BindingRecord canonicalRecord;
        private final GridNode targetNode;
        private final List<BindingRecord> members = new ArrayList<BindingRecord>();

        private PendingBindingGroup(BindingRecord canonicalRecord, GridNode targetNode) {
            this.canonicalRecord = canonicalRecord;
            this.targetNode = targetNode;
            this.members.add(canonicalRecord);
        }
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

    private static String formatTarget(BindingRecord record) {
        return record.getTarget()
            .getKind()
            .name() + "@"
            + record.getTarget()
                .getDimensionId()
            + ":"
            + record.getTarget()
                .getX()
            + ","
            + record.getTarget()
                .getY()
            + ","
            + record.getTarget()
                .getZ()
            + "/"
            + record.getTarget()
                .getSide()
                .name();
    }
}
