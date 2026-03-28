package com.github.nhaeutilities.modules.patterngenerator.gui;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;

public class GuiPatternGenStatusBridgeTest {

    @After
    public void tearDown() {
        GuiPatternGenStatusBridge.clearStatus();
    }

    @Test
    public void clearStatusRestoresDefaultText() {
        GuiPatternGenStatusBridge.setStatus("Building cache");
        GuiPatternGenStatusBridge.clearStatus();

        assertEquals(I18nUtil.tr("nhaeutilities.gui.pattern_gen.status.ready"), GuiPatternGenStatusBridge.getStatus());
    }

    @Test
    public void setStatusReturnsTheAssignedValue() {
        GuiPatternGenStatusBridge.setStatus("Preview requested");

        assertEquals("Preview requested", GuiPatternGenStatusBridge.getStatus());
    }
}
