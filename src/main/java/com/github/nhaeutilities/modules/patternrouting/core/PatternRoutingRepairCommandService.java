package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;

public final class PatternRoutingRepairCommandService {

    interface TargetEvaluator {

        PatternRoutingRepairEvaluator.RepairTarget evaluate(Object controller);
    }

    interface TargetExecutor {

        PatternRoutingRepairExecutor.RepairResult repair(PatternRoutingRepairEvaluator.RepairTarget target);
    }

    private final PatternRoutingRepairScanner.ControllerScanner scanner;
    private final TargetEvaluator evaluator;
    private final TargetExecutor executor;

    public PatternRoutingRepairCommandService() {
        this(new PlayerWorldScanner(), new EvaluatorAdapter(), new ExecutorAdapter());
    }

    PatternRoutingRepairCommandService(PatternRoutingRepairScanner.ControllerScanner scanner, TargetEvaluator evaluator,
        TargetExecutor executor) {
        this.scanner = scanner;
        this.evaluator = evaluator;
        this.executor = executor;
    }

    public RepairSummary run(EntityPlayerMP player) {
        Object world = player != null ? player.worldObj : null;
        List<Object> scannedControllers = scanner.scan(world);
        if (scannedControllers == null || scannedControllers.isEmpty()) {
            return new RepairSummary(0, 0, 0, 0, 0, 0, 0);
        }

        Set<Object> matchedControllers = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        int repairedControllers = 0;
        int failedControllers = 0;
        int repairedHatches = 0;
        int failedHatches = 0;
        int recheckedHatches = 0;

        for (Object controller : scannedControllers) {
            PatternRoutingRepairEvaluator.RepairTarget target = evaluator.evaluate(controller);
            if (target == null || !matchedControllers.add(target.controller)) {
                continue;
            }
            PatternRoutingRepairExecutor.RepairResult result = executor.repair(target);
            if (result != null && result.controllerRepaired) {
                repairedControllers++;
            } else {
                failedControllers++;
            }
            if (result != null) {
                repairedHatches += result.repairedHatchCount;
                failedHatches += result.failedHatchCount;
                recheckedHatches += result.recheckedHatchCount;
            }
        }

        return new RepairSummary(
            scannedControllers.size(),
            matchedControllers.size(),
            repairedControllers,
            failedControllers,
            repairedHatches,
            failedHatches,
            recheckedHatches);
    }

    public static final class RepairSummary {

        public final int scannedControllerCount;
        public final int matchedControllerCount;
        public final int repairedControllerCount;
        public final int failedControllerCount;
        public final int repairedHatchCount;
        public final int failedHatchCount;
        public final int recheckedHatchCount;

        RepairSummary(int scannedControllerCount, int matchedControllerCount, int repairedControllerCount,
            int failedControllerCount, int repairedHatchCount, int failedHatchCount, int recheckedHatchCount) {
            this.scannedControllerCount = scannedControllerCount;
            this.matchedControllerCount = matchedControllerCount;
            this.repairedControllerCount = repairedControllerCount;
            this.failedControllerCount = failedControllerCount;
            this.repairedHatchCount = repairedHatchCount;
            this.failedHatchCount = failedHatchCount;
            this.recheckedHatchCount = recheckedHatchCount;
        }
    }

    private static final class PlayerWorldScanner implements PatternRoutingRepairScanner.ControllerScanner {

        @Override
        public List<Object> scan(Object world) {
            return world instanceof net.minecraft.world.World
                ? PatternRoutingRepairScanner.scanLoadedControllers((net.minecraft.world.World) world)
                : Collections.<Object>emptyList();
        }
    }

    private static final class EvaluatorAdapter implements TargetEvaluator {

        @Override
        public PatternRoutingRepairEvaluator.RepairTarget evaluate(Object controller) {
            return PatternRoutingRepairEvaluator.evaluate(controller);
        }
    }

    private static final class ExecutorAdapter implements TargetExecutor {

        @Override
        public PatternRoutingRepairExecutor.RepairResult repair(PatternRoutingRepairEvaluator.RepairTarget target) {
            return PatternRoutingRepairExecutor.repair(target);
        }
    }
}
