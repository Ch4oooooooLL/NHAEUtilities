package com.github.nhaeutilities.modules.superwirelesskit.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;

public class SuperWirelessKitBatchCollectorTest {

    @Test
    public void collectsWholeConnectedComponentRecursively() {
        BindingTargetRef root = createTarget(0);
        BindingTargetRef next = createTarget(1);
        BindingTargetRef tail = createTarget(2);

        SuperWirelessKitBatchCollector collector = new SuperWirelessKitBatchCollector(new GraphLookup(mapOf(
            root,
            Arrays.asList(next),
            next,
            Arrays.asList(root, tail),
            tail,
            Arrays.asList(next))));

        List<BindingTargetRef> collected = collector.collect(root, Collections.<BindingTargetRef>emptyList());

        assertEquals(3, collected.size());
        assertEquals(root, collected.get(0));
        assertTrue(collected.contains(next));
        assertTrue(collected.contains(tail));
    }

    @Test
    public void skipsAlreadyKnownTargetsAndDoesNotLoopForever() {
        BindingTargetRef root = createTarget(0);
        BindingTargetRef known = createTarget(1);
        BindingTargetRef fresh = createTarget(2);

        SuperWirelessKitBatchCollector collector = new SuperWirelessKitBatchCollector(new GraphLookup(mapOf(
            root,
            Arrays.asList(known, fresh),
            known,
            Arrays.asList(root, fresh),
            fresh,
            Arrays.asList(root))));

        List<BindingTargetRef> collected = collector.collect(root, Collections.singleton(known));

        assertEquals(2, collected.size());
        assertTrue(collected.contains(root));
        assertTrue(collected.contains(fresh));
        assertFalse(collected.contains(known));
    }

    @Test
    public void deduplicatesTileTargetsThatOnlyDifferByFace() {
        BindingTargetRef rootUp = createTarget(0, ForgeDirection.UP);
        BindingTargetRef rootNorth = createTarget(0, ForgeDirection.NORTH);
        BindingTargetRef nextSouth = createTarget(1, ForgeDirection.SOUTH);
        BindingTargetRef nextEast = createTarget(1, ForgeDirection.EAST);
        BindingTargetRef tailDown = createTarget(2, ForgeDirection.DOWN);
        BindingTargetRef tailWest = createTarget(2, ForgeDirection.WEST);

        SuperWirelessKitBatchCollector collector = new SuperWirelessKitBatchCollector(new GraphLookup(mapOf(
            rootUp,
            Arrays.asList(rootNorth, nextSouth, nextEast),
            rootNorth,
            Arrays.asList(rootUp, nextSouth),
            nextSouth,
            Arrays.asList(rootUp, rootNorth, nextEast, tailDown, tailWest),
            nextEast,
            Arrays.asList(rootUp, nextSouth, tailDown),
            tailDown,
            Arrays.asList(nextSouth, nextEast, tailWest),
            tailWest,
            Arrays.asList(nextSouth, tailDown))));

        List<BindingTargetRef> collected = collector.collect(rootUp, Collections.<BindingTargetRef>emptyList());

        assertEquals(3, collected.size());
        assertTrue(collected.contains(rootUp));
        assertTrue(collected.contains(nextSouth));
        assertTrue(collected.contains(tailDown));
        assertFalse(collected.contains(rootNorth));
        assertFalse(collected.contains(nextEast));
        assertFalse(collected.contains(tailWest));
    }

    @Test
    public void treatsKnownTileTargetsOnOtherFacesAsAlreadyQueued() {
        BindingTargetRef root = createTarget(0, ForgeDirection.UP);
        BindingTargetRef knownNorth = createTarget(1, ForgeDirection.NORTH);
        BindingTargetRef knownEast = createTarget(1, ForgeDirection.EAST);
        BindingTargetRef fresh = createTarget(2, ForgeDirection.DOWN);

        SuperWirelessKitBatchCollector collector = new SuperWirelessKitBatchCollector(new GraphLookup(mapOf(
            root,
            Arrays.asList(knownEast, fresh),
            knownEast,
            Arrays.asList(root),
            fresh,
            Arrays.asList(root))));

        List<BindingTargetRef> collected = collector.collect(root, Collections.singleton(knownNorth));

        assertEquals(2, collected.size());
        assertTrue(collected.contains(root));
        assertTrue(collected.contains(fresh));
        assertFalse(collected.contains(knownEast));
    }

    private static BindingTargetRef createTarget(int offset) {
        return createTarget(offset, ForgeDirection.UP);
    }

    private static BindingTargetRef createTarget(int offset, ForgeDirection side) {
        return new BindingTargetRef(
            BindingTargetKind.TILE,
            0,
            10 + offset,
            20,
            30,
            side,
            new BindingFingerprint("mod:block", "tile.Target" + offset));
    }

    private static Map<BindingTargetRef, List<BindingTargetRef>> mapOf(Object... entries) {
        Map<BindingTargetRef, List<BindingTargetRef>> map = new LinkedHashMap<BindingTargetRef, List<BindingTargetRef>>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((BindingTargetRef) entries[i], (List<BindingTargetRef>) entries[i + 1]);
        }
        return map;
    }

    private static final class GraphLookup implements SuperWirelessKitBatchCollector.NeighborLookup {

        private final Map<BindingTargetRef, List<BindingTargetRef>> adjacency;

        private GraphLookup(Map<BindingTargetRef, List<BindingTargetRef>> adjacency) {
            this.adjacency = adjacency;
        }

        @Override
        public List<BindingTargetRef> getAdjacentTargets(BindingTargetRef target) {
            List<BindingTargetRef> targets = adjacency.get(target);
            return targets != null ? targets : Collections.<BindingTargetRef>emptyList();
        }
    }
}
