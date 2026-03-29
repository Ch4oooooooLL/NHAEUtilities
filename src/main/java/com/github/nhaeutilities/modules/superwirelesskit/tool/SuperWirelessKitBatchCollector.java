package com.github.nhaeutilities.modules.superwirelesskit.tool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;

public final class SuperWirelessKitBatchCollector {

    public interface NeighborLookup {

        List<BindingTargetRef> getAdjacentTargets(BindingTargetRef target);
    }

    private final NeighborLookup neighborLookup;

    public SuperWirelessKitBatchCollector(NeighborLookup neighborLookup) {
        this.neighborLookup = Objects.requireNonNull(neighborLookup, "neighborLookup");
    }

    public List<BindingTargetRef> collect(BindingTargetRef root, Collection<BindingTargetRef> knownTargets) {
        Objects.requireNonNull(root, "root");
        Set<TargetIdentity> known = new LinkedHashSet<TargetIdentity>();
        if (knownTargets != null) {
            for (BindingTargetRef knownTarget : knownTargets) {
                if (knownTarget != null) {
                    known.add(TargetIdentity.of(knownTarget));
                }
            }
        }

        Set<TargetIdentity> visited = new LinkedHashSet<TargetIdentity>();
        Map<TargetIdentity, BindingTargetRef> collected = new LinkedHashMap<TargetIdentity, BindingTargetRef>();
        ArrayDeque<BindingTargetRef> pending = new ArrayDeque<BindingTargetRef>();
        pending.add(root);

        while (!pending.isEmpty()) {
            BindingTargetRef current = pending.removeFirst();
            TargetIdentity currentIdentity = TargetIdentity.of(current);
            if (!visited.add(currentIdentity)) {
                continue;
            }

            if (!known.contains(currentIdentity)) {
                collected.put(currentIdentity, current);
            }

            List<BindingTargetRef> neighbors = neighborLookup.getAdjacentTargets(current);
            if (neighbors == null) {
                continue;
            }
            for (BindingTargetRef neighbor : neighbors) {
                if (neighbor != null && !visited.contains(TargetIdentity.of(neighbor))) {
                    pending.addLast(neighbor);
                }
            }
        }

        return Collections.unmodifiableList(new ArrayList<BindingTargetRef>(collected.values()));
    }

    private static final class TargetIdentity {

        private final BindingTargetKind kind;
        private final int dimensionId;
        private final int x;
        private final int y;
        private final int z;
        private final ForgeDirection side;
        private final BindingFingerprint fingerprint;

        private TargetIdentity(BindingTargetKind kind, int dimensionId, int x, int y, int z, ForgeDirection side,
            BindingFingerprint fingerprint) {
            this.kind = kind;
            this.dimensionId = dimensionId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.side = side;
            this.fingerprint = fingerprint;
        }

        private static TargetIdentity of(BindingTargetRef target) {
            ForgeDirection identitySide = target.getKind() == BindingTargetKind.PART ? target.getSide() : null;
            return new TargetIdentity(
                target.getKind(),
                target.getDimensionId(),
                target.getX(),
                target.getY(),
                target.getZ(),
                identitySide,
                target.getFingerprint());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TargetIdentity)) {
                return false;
            }
            TargetIdentity that = (TargetIdentity) o;
            return dimensionId == that.dimensionId && x == that.x
                && y == that.y
                && z == that.z
                && kind == that.kind
                && side == that.side
                && fingerprint.equals(that.fingerprint);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, dimensionId, x, y, z, side, fingerprint);
        }
    }
}
