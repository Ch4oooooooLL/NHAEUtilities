package com.github.nhaeutilities.modules.superwirelesskit.item;

final class SuperWirelessKitInteractionRules {

    private SuperWirelessKitInteractionRules() {}

    static boolean shouldToggleMode(boolean sneaking, boolean targetingBlock) {
        return sneaking && !targetingBlock;
    }
}
