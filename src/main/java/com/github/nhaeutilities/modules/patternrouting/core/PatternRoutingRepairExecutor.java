package com.github.nhaeutilities.modules.patternrouting.core;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

final class PatternRoutingRepairExecutor {

    private PatternRoutingRepairExecutor() {}

    static RepairResult repair(PatternRoutingRepairEvaluator.RepairTarget target) {
        if (target == null || target.controller == null || target.brokenHatches.isEmpty()) {
            return new RepairResult(false, 0, 0, 0);
        }

        HatchAssignmentService.refreshAssignments(target.controller);

        int repaired = 0;
        int failed = 0;
        int rechecked = 0;
        for (PatternRoutingRepairEvaluator.BrokenHatch brokenHatch : target.brokenHatches) {
            if (matchesExpected(brokenHatch.hatch, brokenHatch.expectedAssignment)) {
                repaired++;
                continue;
            }
            HatchControllerRecheckService.RecheckResult recheck = HatchControllerRecheckService
                .recheckAndVerify(brokenHatch.hatch, brokenHatch.expectedAssignment);
            rechecked++;
            if (recheck.success && matchesExpected(brokenHatch.hatch, brokenHatch.expectedAssignment)) {
                repaired++;
            } else {
                failed++;
            }
        }
        return new RepairResult(repaired > 0 && failed == 0, repaired, failed, rechecked);
    }

    private static boolean matchesExpected(Object hatch, HatchAssignmentData expectedAssignment) {
        Object writable = CraftingInputHatchAccess.resolveWritableHatch(hatch);
        if (writable instanceof HatchAssignmentHolder) {
            HatchAssignmentData actual = ((HatchAssignmentHolder) writable).nhaeutilities$getAssignmentData();
            return PatternRoutingRepairEvaluator.matchesExpected(actual, expectedAssignment);
        }
        if (hatch instanceof HatchAssignmentHolder) {
            HatchAssignmentData actual = ((HatchAssignmentHolder) hatch).nhaeutilities$getAssignmentData();
            return PatternRoutingRepairEvaluator.matchesExpected(actual, expectedAssignment);
        }
        return false;
    }

    static final class RepairResult {

        final boolean controllerRepaired;
        final int repairedHatchCount;
        final int failedHatchCount;
        final int recheckedHatchCount;

        RepairResult(boolean controllerRepaired, int repairedHatchCount, int failedHatchCount,
            int recheckedHatchCount) {
            this.controllerRepaired = controllerRepaired;
            this.repairedHatchCount = repairedHatchCount;
            this.failedHatchCount = failedHatchCount;
            this.recheckedHatchCount = recheckedHatchCount;
        }
    }
}
