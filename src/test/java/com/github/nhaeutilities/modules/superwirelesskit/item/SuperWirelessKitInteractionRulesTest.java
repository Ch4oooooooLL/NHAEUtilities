package com.github.nhaeutilities.modules.superwirelesskit.item;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SuperWirelessKitInteractionRulesTest {

    @Test
    public void onlySneakAirShouldToggleMode() {
        assertTrue(SuperWirelessKitInteractionRules.shouldToggleMode(true, false));
        assertFalse(SuperWirelessKitInteractionRules.shouldToggleMode(true, true));
        assertFalse(SuperWirelessKitInteractionRules.shouldToggleMode(false, false));
    }

    @Test
    public void batchCaptureRequiresSneakLeftClickAndNoPendingBindings() {
        assertTrue(SuperWirelessKitInteractionRules.shouldAttemptBatchCapture(true, true, false));
        assertFalse(SuperWirelessKitInteractionRules.shouldAttemptBatchCapture(false, true, false));
        assertFalse(SuperWirelessKitInteractionRules.shouldAttemptBatchCapture(true, false, false));
        assertFalse(SuperWirelessKitInteractionRules.shouldAttemptBatchCapture(true, true, true));
    }

    @Test
    public void pendingBindingsBlockNewTargets() {
        assertTrue(SuperWirelessKitInteractionRules.shouldRejectNewTarget(true));
        assertFalse(SuperWirelessKitInteractionRules.shouldRejectNewTarget(false));
    }
}
