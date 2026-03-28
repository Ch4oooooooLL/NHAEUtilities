package com.github.nhaeutilities.modules.superwirelesskit.tool;

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

    public static boolean isController(World world, int x, int y, int z) {
        return world != null && AEApi.instance()
            .definitions()
            .blocks()
            .controller()
            .isSameAs(world, x, y, z);
    }
}
