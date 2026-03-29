package com.github.nhaeutilities.modules.superwirelesskit.item;

final class SuperWirelessKitInteractionRules {

    private SuperWirelessKitInteractionRules() {}

    static boolean shouldToggleMode(boolean sneaking, boolean targetingBlock) {
        return sneaking && !targetingBlock;
    }

    static boolean shouldAttemptBatchCapture(boolean sneaking, boolean leftClickBlock, boolean hasPendingBindings) {
        return sneaking && leftClickBlock && !hasPendingBindings;
    }

    static boolean shouldRejectNewTarget(boolean hasPendingBindings) {
        return hasPendingBindings;
    }
}
