package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import static org.junit.Assert.assertEquals;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

public class VirtualGridConnectionRulesTest {

    @Test
    public void runtimeVirtualLinksMustUseUnknownDirection() {
        assertEquals(ForgeDirection.UNKNOWN, VirtualGridConnectionRules.connectionDirectionForVirtualLink());
    }
}
