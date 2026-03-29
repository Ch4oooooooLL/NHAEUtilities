package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingBlockRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingFingerprint;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetKind;
import com.github.nhaeutilities.modules.superwirelesskit.data.BindingTargetRef;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.me.GridNode;
import appeng.parts.AEBasePart;
import appeng.tile.networking.TileController;

public class BindingNodeResolver {

    public ResolvedControllerEndpoint resolveController(World world, ControllerEndpointRef controllerRef) {
        if (!matchesWorld(world, controllerRef.getDimensionId())) {
            return null;
        }

        TileEntity tileEntity = world.getTileEntity(controllerRef.getX(), controllerRef.getY(), controllerRef.getZ());
        if (!(tileEntity instanceof TileController)) {
            return null;
        }

        TileController controller = (TileController) tileEntity;
        IGridNode node = controller.getGridNode(controllerRef.getFace());
        if (!(node instanceof GridNode)) {
            return null;
        }

        return new ResolvedControllerEndpoint(controllerRef, controller, (GridNode) node, controllerRef.getFace());
    }

    public ResolvedBindingTarget resolveTarget(World world, BindingTargetRef targetRef) {
        if (!matchesWorld(world, targetRef.getDimensionId())) {
            return null;
        }

        TileEntity hostTile = world.getTileEntity(targetRef.getX(), targetRef.getY(), targetRef.getZ());
        if (hostTile == null) {
            return null;
        }

        Object machine;
        IGridNode node;
        BindingFingerprint fingerprint;

        if (targetRef.getKind() == BindingTargetKind.PART) {
            if (!(hostTile instanceof IPartHost)) {
                return null;
            }

            IPart part = ((IPartHost) hostTile).getPart(targetRef.getSide());
            if (part == null) {
                return null;
            }

            machine = part;
            node = part.getGridNode();
            fingerprint = createFingerprint(hostTile, part);
        } else {
            if (!(hostTile instanceof IGridHost)) {
                return null;
            }

            machine = hostTile;
            node = ((IGridHost) hostTile).getGridNode(targetRef.getSide());
            fingerprint = createFingerprint(hostTile, hostTile);
        }

        if (!(node instanceof GridNode)) {
            return null;
        }
        if (!targetRef.getFingerprint()
            .equals(fingerprint)) {
            return null;
        }
        if (!node.hasFlag(GridFlags.REQUIRE_CHANNEL)) {
            return null;
        }

        return new ResolvedBindingTarget(targetRef, hostTile, machine, (GridNode) node);
    }

    public BindingBlockRef getNodeBlockRef(GridNode node) {
        if (node == null || node.getWorld() == null || node.getWorld().provider == null) {
            return null;
        }

        Object machine = node.getGridBlock()
            .getMachine();
        if (machine instanceof AEBasePart) {
            TileEntity tile = ((AEBasePart) machine).getTile();
            if (tile != null) {
                return new BindingBlockRef(node.getWorld().provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord);
            }
        }

        if (machine instanceof TileEntity) {
            TileEntity tile = (TileEntity) machine;
            return new BindingBlockRef(node.getWorld().provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord);
        }

        return null;
    }

    public static BindingFingerprint createFingerprint(TileEntity hostTile, Object target) {
        String hostBlockId = "unknown";
        if (hostTile != null) {
            Block block = hostTile.getBlockType();
            if (block != null) {
                Object registryName = Block.blockRegistry.getNameForObject(block);
                if (registryName != null) {
                    hostBlockId = registryName.toString();
                }
            }
        }

        return new BindingFingerprint(
            hostBlockId,
            target != null ? target.getClass()
                .getName() : "unknown");
    }

    private static boolean matchesWorld(World world, int dimensionId) {
        return world != null && world.provider != null && world.provider.dimensionId == dimensionId;
    }
}
