package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class PatternRoutingRepairCommandServiceTest {

    @Test
    public void runDeduplicatesMatchedControllersBeforeRepairing() {
        Object controller = new Object();
        PatternRoutingRepairEvaluator.RepairTarget target = new PatternRoutingRepairEvaluator.RepairTarget(
            controller,
            Collections.singletonList(
                new PatternRoutingRepairEvaluator.BrokenHatch(
                    new Object(),
                    new HatchAssignmentData("k", "cat", "", ""))));
        PatternRoutingRepairCommandService service = new PatternRoutingRepairCommandService(
            new StubScanner(Arrays.asList(controller, controller)),
            new StubEvaluator(Collections.singletonMap(controller, target)),
            new StubExecutor(
                Collections.singletonMap(controller, new PatternRoutingRepairExecutor.RepairResult(true, 1, 0, 0))));

        PatternRoutingRepairCommandService.RepairSummary summary = service.run(null);

        assertEquals(2, summary.scannedControllerCount);
        assertEquals(1, summary.matchedControllerCount);
        assertEquals(1, summary.repairedControllerCount);
        assertEquals(1, summary.repairedHatchCount);
    }

    @Test
    public void runCountsControllersWithoutRepairTargetsAsScannedOnly() {
        Object controller = new Object();
        PatternRoutingRepairCommandService service = new PatternRoutingRepairCommandService(
            new StubScanner(Collections.singletonList(controller)),
            new StubEvaluator(Collections.<Object, PatternRoutingRepairEvaluator.RepairTarget>emptyMap()),
            new StubExecutor(Collections.<Object, PatternRoutingRepairExecutor.RepairResult>emptyMap()));

        PatternRoutingRepairCommandService.RepairSummary summary = service.run(null);

        assertEquals(1, summary.scannedControllerCount);
        assertEquals(0, summary.matchedControllerCount);
        assertEquals(0, summary.repairedControllerCount);
        assertEquals(0, summary.failedControllerCount);
        assertEquals(0, summary.repairedHatchCount);
        assertEquals(0, summary.failedHatchCount);
    }

    @Test
    public void runAccumulatesFailedRepairCounts() {
        Object controller = new Object();
        PatternRoutingRepairEvaluator.RepairTarget target = new PatternRoutingRepairEvaluator.RepairTarget(
            controller,
            Collections.singletonList(
                new PatternRoutingRepairEvaluator.BrokenHatch(
                    new Object(),
                    new HatchAssignmentData("k", "cat", "", ""))));
        PatternRoutingRepairCommandService service = new PatternRoutingRepairCommandService(
            new StubScanner(Collections.singletonList(controller)),
            new StubEvaluator(Collections.singletonMap(controller, target)),
            new StubExecutor(
                Collections.singletonMap(controller, new PatternRoutingRepairExecutor.RepairResult(false, 0, 1, 1))));

        PatternRoutingRepairCommandService.RepairSummary summary = service.run(null);

        assertEquals(1, summary.matchedControllerCount);
        assertEquals(0, summary.repairedControllerCount);
        assertEquals(1, summary.failedControllerCount);
        assertEquals(0, summary.repairedHatchCount);
        assertEquals(1, summary.failedHatchCount);
        assertEquals(1, summary.recheckedHatchCount);
    }

    private static final class StubScanner implements PatternRoutingRepairScanner.ControllerScanner {

        private final List<Object> controllers;

        private StubScanner(List<Object> controllers) {
            this.controllers = controllers;
        }

        @Override
        public List<Object> scan(Object world) {
            return controllers;
        }
    }

    private static final class StubEvaluator implements PatternRoutingRepairCommandService.TargetEvaluator {

        private final java.util.Map<Object, PatternRoutingRepairEvaluator.RepairTarget> targets;

        private StubEvaluator(java.util.Map<Object, PatternRoutingRepairEvaluator.RepairTarget> targets) {
            this.targets = targets;
        }

        @Override
        public PatternRoutingRepairEvaluator.RepairTarget evaluate(Object controller) {
            return targets.get(controller);
        }
    }

    private static final class StubExecutor implements PatternRoutingRepairCommandService.TargetExecutor {

        private final java.util.Map<Object, PatternRoutingRepairExecutor.RepairResult> results;

        private StubExecutor(java.util.Map<Object, PatternRoutingRepairExecutor.RepairResult> results) {
            this.results = results;
        }

        @Override
        public PatternRoutingRepairExecutor.RepairResult repair(PatternRoutingRepairEvaluator.RepairTarget target) {
            return results.get(target.controller);
        }
    }
}
