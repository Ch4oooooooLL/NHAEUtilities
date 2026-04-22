package com.github.nhaeutilities.modules.patternrouting.mixin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

public class MixinPackageBoundaryTest {

    @Test
    public void directlyReferencedAccessorTypesStayOutsideOwnedPatternMixinPackage() {
        String mixinPackage = MixinMTEHatchCraftingInputME.class.getPackage()
            .getName();
        String holderPackage = HatchAssignmentHolder.class.getPackage()
            .getName();

        assertFalse(holderPackage.equals(mixinPackage));
        assertFalse(holderPackage.startsWith(mixinPackage + "."));
    }

    @Test
    public void directPatternRoutingHelpersStayOutsideOwnedPatternMixinPackage() {
        String mixinPackage = MixinMTEHatchCraftingInputME.class.getPackage()
            .getName();
        String helperPackage = com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentPersistenceSupport.class
            .getPackage()
            .getName();

        assertFalse(helperPackage.equals(mixinPackage));
        assertFalse(helperPackage.startsWith(mixinPackage + "."));
    }

    @Test
    public void directPatternRoutingTerminalReflectionHelpersStayOutsideOwnedPatternMixinPackage()
        throws ClassNotFoundException {
        String mixinPackage = MixinMTEHatchCraftingInputME.class.getPackage()
            .getName();
        String helperPackage = Class
            .forName("com.github.nhaeutilities.modules.patternrouting.core.PatternTerminalReflectionSupport")
            .getPackage()
            .getName();

        assertFalse(helperPackage.equals(mixinPackage));
        assertFalse(helperPackage.startsWith(mixinPackage + "."));
    }

    @Test
    public void activeMixinConfigKeepsBothPatternRoutingAndSuperwirelessMixinsReachable() throws IOException {
        String activeConfig = readResource("/mixins.nhaeutilities.json");
        String patternRoutingConfig = readResource("/mixins.nhaeutilities.patternrouting.json");
        String superWirelessConfig = readResource("/mixins.nhaeutilities.superwirelesskit.json");
        String manifest = new String(Files.readAllBytes(Paths.get("META-INF", "MANIFEST.MF")), StandardCharsets.UTF_8);

        assertTrue(activeConfig.contains("\"package\": \"com.github.nhaeutilities.modules.patterngenerator.mixin\""));
        assertTrue(activeConfig.contains("\"mixins\": []"));
        assertTrue(
            patternRoutingConfig.contains("\"package\": \"com.github.nhaeutilities.modules.patternrouting.mixin\""));
        assertTrue(patternRoutingConfig.contains("\"MixinMTEMultiBlockBase\""));
        assertTrue(patternRoutingConfig.contains("\"MixinMTEMultiBlockBaseModeRefresh\""));
        assertTrue(patternRoutingConfig.contains("\"MixinGTNLSuperCraftingInputHatchME\""));
        assertTrue(patternRoutingConfig.contains("\"MixinGTNLSuperDualInputHatchME\""));
        assertTrue(patternRoutingConfig.contains("\"MixinCPacketFluidPatternTermBtnsHandler\""));
        assertTrue(
            superWirelessConfig.contains("\"package\": \"com.github.nhaeutilities.modules.superwirelesskit.mixin\""));
        assertTrue(superWirelessConfig.contains("\"MixinGridNode\""));
        assertTrue(
            manifest.contains(
                "MixinConfigs: mixins.nhaeutilities.json,mixins.nhaeutilities.patternrouting.json,mixins.nhaeutilities.superwirelesskit.json"));
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = MixinPackageBoundaryTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + path);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
