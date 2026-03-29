package com.github.nhaeutilities.modules.superwirelesskit.service;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingNodeResolver;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingReconcileResult;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.ResolvedBindingTarget;

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

        Map<GridNode, BindingRecord> existingByNode = collectExistingBindingsByNode(world, registry);
        Map<GridNode, PendingBindingGroup> pendingGroups = new IdentityHashMap<GridNode, PendingBindingGroup>();

        for (BindingRecord record : draftedBindings) {
            if (record.getController()
                .getDimensionId()
                != record.getTarget()
                    .getDimensionId()
                || record.getController()
                    .getFace() == ForgeDirection.UNKNOWN) {
                failures.add(record);
                continue;
            }

            BindingRecord exactExisting = registry.findByTarget(record.getTarget());
            if (exactExisting != null) {
                if (exactExisting.getController().equals(controller)) {
                    successCount++;
                } else {
                    failures.add(record);
                }
                continue;
            }

            ResolvedBindingTarget resolvedTarget = resolver.resolveTarget(world, record.getTarget());
            if (resolvedTarget == null) {
                failures.add(record);
                continue;
            }

            GridNode targetNode = resolvedTarget.getNode();
            BindingRecord existing = existingByNode.get(targetNode);
            if (existing != null) {
                if (existing.getController().equals(controller)) {
                    successCount++;
                } else {
                    failures.add(record);
                }
                continue;
            }

            PendingBindingGroup group = pendingGroups.get(targetNode);
            if (group == null) {
                group = new PendingBindingGroup(record, targetNode);
                pendingGroups.put(targetNode, group);
            } else {
                group.members.add(record);
            }
        }

        for (PendingBindingGroup group : pendingGroups.values()) {
            if (!registry.add(group.canonicalRecord)) {
                failures.addAll(group.members);
                continue;
            }

            BindingReconcileResult reconcileResult = reconciler.reconcile(world, group.canonicalRecord);
            if (reconcileResult == BindingReconcileResult.CONNECTED
                || reconcileResult == BindingReconcileResult.ALREADY_CONNECTED) {
                successCount += group.members.size();
                existingByNode.put(group.targetNode, group.canonicalRecord);
            } else {
                registry.remove(group.canonicalRecord.getBindingId());
                failures.addAll(group.members);
            }
        }

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
}
