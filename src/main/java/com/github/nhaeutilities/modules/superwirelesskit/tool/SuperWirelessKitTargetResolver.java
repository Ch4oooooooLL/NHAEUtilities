package com.github.nhaeutilities.modules.superwirelesskit.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingNodeResolver;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridHost;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;

public final class SuperWirelessKitTargetResolver {

    private SuperWirelessKitTargetResolver() {}

    public static ControllerEndpointRef resolveController(World world, int x, int y, int z, ForgeDirection side) {
        if (world == null || !AEApi.instance()
            .definitions()
            .blocks()
            .controller()
            .isSameAs(world, x, y, z)) {
            return null;
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        String controllerType = tile != null ? tile.getClass()
            .getName() : "appeng.block.networking.BlockController";
        return new ControllerEndpointRef(world.provider.dimensionId, x, y, z, side, controllerType);
    }

    public static BindingTargetRef resolveTarget(World world, int x, int y, int z, ForgeDirection side, float hitX,
        float hitY, float hitZ) {
        if (world == null) {
            return null;
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) {
            return null;
        }

        if (tile instanceof IPartHost) {
            SelectedPart selectedPart = ((IPartHost) tile).selectPart(Vec3.createVectorHelper(hitX, hitY, hitZ));
            if (selectedPart != null && selectedPart.part != null
                && selectedPart.part.getGridNode() != null
                && selectedPart.part.getGridNode()
                    .hasFlag(GridFlags.REQUIRE_CHANNEL)) {
                return new BindingTargetRef(
                    BindingTargetKind.PART,
                    world.provider.dimensionId,
                    x,
                    y,
                    z,
                    selectedPart.side,
                    BindingNodeResolver.createFingerprint(tile, selectedPart.part));
            }
        }

        if (!(tile instanceof IGridHost)) {
            return null;
        }

        IGridHost host = (IGridHost) tile;
        if (host.getGridNode(side) == null || !host.getGridNode(side)
            .hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            return null;
        }

        return new BindingTargetRef(
            BindingTargetKind.TILE,
            world.provider.dimensionId,
            x,
            y,
            z,
            side,
            BindingNodeResolver.createFingerprint(tile, tile));
    }

    public static List<BindingTargetRef> resolveTargetsAtBlock(World world, int x, int y, int z) {
        if (world == null) {
            return Collections.emptyList();
        }

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) {
            return Collections.emptyList();
        }

        List<BindingTargetRef> targets = new ArrayList<BindingTargetRef>();
        appendPartTargets(world, x, y, z, tile, targets);
        appendTileTargets(world, x, y, z, tile, targets);
        return Collections.unmodifiableList(targets);
    }

    public static List<BindingTargetRef> findAdjacentTargets(World world, BindingTargetRef target) {
        if (world == null || target == null) {
            return Collections.emptyList();
        }

        List<BindingTargetRef> adjacent = new ArrayList<BindingTargetRef>();
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            int nx = target.getX() + direction.offsetX;
            int ny = target.getY() + direction.offsetY;
            int nz = target.getZ() + direction.offsetZ;
            for (BindingTargetRef neighbor : resolveTargetsAtBlock(world, nx, ny, nz)) {
                if (!adjacent.contains(neighbor)) {
                    adjacent.add(neighbor);
                }
            }
        }
        return Collections.unmodifiableList(adjacent);
    }

    public static boolean isController(World world, int x, int y, int z) {
        return world != null && AEApi.instance()
            .definitions()
            .blocks()
            .controller()
            .isSameAs(world, x, y, z);
    }

    private static void appendPartTargets(World world, int x, int y, int z, TileEntity tile, List<BindingTargetRef> targets) {
        if (!(tile instanceof IPartHost)) {
            return;
        }

        IPartHost partHost = (IPartHost) tile;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (partHost.getPart(side) == null || partHost.getPart(side).getGridNode() == null
                || !partHost.getPart(side).getGridNode().hasFlag(GridFlags.REQUIRE_CHANNEL)) {
                continue;
            }

            BindingTargetRef target = new BindingTargetRef(
                BindingTargetKind.PART,
                world.provider.dimensionId,
                x,
                y,
                z,
                side,
                BindingNodeResolver.createFingerprint(tile, partHost.getPart(side)));
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }
    }

    private static void appendTileTargets(World world, int x, int y, int z, TileEntity tile, List<BindingTargetRef> targets) {
        if (!(tile instanceof IGridHost)) {
            return;
        }

        IGridHost host = (IGridHost) tile;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (host.getGridNode(side) == null || !host.getGridNode(side).hasFlag(GridFlags.REQUIRE_CHANNEL)) {
                continue;
            }

            BindingTargetRef target = new BindingTargetRef(
                BindingTargetKind.TILE,
                world.provider.dimensionId,
                x,
                y,
                z,
                side,
                BindingNodeResolver.createFingerprint(tile, tile));
            if (!targets.contains(target)) {
                targets.add(target);
            }
        }
    }
}
