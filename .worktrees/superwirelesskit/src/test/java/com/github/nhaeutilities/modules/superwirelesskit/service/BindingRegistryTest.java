package com.github.nhaeutilities.modules.superwirelesskit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingBlockRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingChunkRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.SuperWirelessSavedData;

public class BindingRegistryTest {

    @Test
    public void rejectsDuplicateTargetBindings() {
        BindingRegistry registry = new BindingRegistry(new SuperWirelessSavedData("test"));
        BindingRecord first = createRecord(0, ForgeDirection.UP);
        BindingRecord duplicateTarget = new BindingRecord(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            new ControllerEndpointRef(0, 1, 2, 3, ForgeDirection.DOWN, "controller"),
            first.getTarget(),
            99,
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
            2L);

        assertTrue(registry.add(first));
        assertFalse(registry.add(duplicateTarget));
        assertEquals(first, registry.findByTarget(first.getTarget()));
    }

    @Test
    public void enforcesThirtyTwoBindingsPerControllerFace() {
        BindingRegistry registry = new BindingRegistry(new SuperWirelessSavedData("test"));

        for (int i = 0; i < 32; i++) {
            assertTrue("binding " + i + " should fit", registry.add(createRecord(i, ForgeDirection.NORTH)));
        }

        assertFalse("33rd binding on same face must be rejected", registry.add(createRecord(32, ForgeDirection.NORTH)));
        assertEquals(
            32,
            registry.getBindingsForFace(new ControllerEndpointRef(0, 1, 2, 3, ForgeDirection.NORTH, "controller"))
                .size());
    }

    @Test
    public void indexesBindingsByBlockAndChunkForTargetedRefresh() {
        BindingRegistry registry = new BindingRegistry(new SuperWirelessSavedData("test"));
        BindingRecord north = createRecord(0, ForgeDirection.NORTH);
        BindingRecord south = createRecord(1, ForgeDirection.SOUTH);

        assertTrue(registry.add(north));
        assertTrue(registry.add(south));

        List<BindingRecord> blockMatches = registry.getBindingsTouchingBlock(new BindingBlockRef(0, 1, 2, 3));
        assertEquals(2, blockMatches.size());

        List<BindingRecord> targetMatches = registry.getBindingsTouchingBlock(new BindingBlockRef(0, 10, 20, 30));
        assertEquals(1, targetMatches.size());
        assertEquals(north, targetMatches.get(0));

        List<BindingRecord> chunkMatches = registry.getBindingsTouchingChunk(new BindingChunkRef(0, 0, 1));
        assertEquals(2, chunkMatches.size());
    }

    @Test
    public void removeUpdatesIndexes() {
        BindingRegistry registry = new BindingRegistry(new SuperWirelessSavedData("test"));
        BindingRecord record = createRecord(0, ForgeDirection.UP);

        assertTrue(registry.add(record));
        assertEquals(
            1,
            registry.getBindingsTouchingBlock(new BindingBlockRef(0, 10, 20, 30))
                .size());

        registry.remove(record.getBindingId());

        assertEquals(
            0,
            registry.getBindingsTouchingBlock(new BindingBlockRef(0, 10, 20, 30))
                .size());
        assertEquals(
            0,
            registry.getBindingsForFace(record.getController())
                .size());
    }

    private static BindingRecord createRecord(int offset, ForgeDirection face) {
        return new BindingRecord(
            UUID.nameUUIDFromBytes(("binding-" + offset).getBytes()),
            new ControllerEndpointRef(0, 1, 2, 3, face, "controller"),
            new BindingTargetRef(
                BindingTargetKind.TILE,
                0,
                10 + offset,
                20,
                30,
                ForgeDirection.UNKNOWN,
                new BindingFingerprint("mod:block", "tile.Class" + offset)),
            7,
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            offset);
    }
}
