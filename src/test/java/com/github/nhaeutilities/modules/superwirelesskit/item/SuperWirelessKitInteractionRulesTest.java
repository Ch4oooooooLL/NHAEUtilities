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
}
