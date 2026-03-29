package com.github.nhaeutilities.modules.superwirelesskit.service;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.ResolvedControllerEndpoint;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessServices;
import com.github.nhaeutilities.modules.superwirelesskit.tool.SuperWirelessKitStackState;

import appeng.api.config.SecurityPermissions;
import appeng.core.worlddata.WorldData;
import appeng.me.GridAccessException;

public final class SuperWirelessBindingService {

    private SuperWirelessBindingService() {}

    public static BindingBatchResult bindQueuedTargets(World world, EntityPlayer player, ItemStack stack,
        ControllerEndpointRef controller, long createdAt) {
        int binderPlayerId = Ae2PlayerIdResolver
            .resolvePlayerId(player.getGameProfile(), player.getEntityId(), WorldData.instance());
        List<BindingRecord> draftedBindings = SuperWirelessKitStackState
            .promoteQueuedTargetsToBindings(stack, controller, binderPlayerId, player.getUniqueID(), createdAt);

        if (draftedBindings.isEmpty()) {
            SuperWirelessKitStackState.setPendingBindings(stack, Collections.<BindingRecord>emptyList());
            return new BindingBatchResult(0, Collections.<BindingRecord>emptyList());
        }

        ResolvedControllerEndpoint resolvedController = SuperWirelessServices.resolver()
            .resolveController(world, controller);
        if (resolvedController == null || !canBuildOnController(player, resolvedController)) {
            SuperWirelessKitStackState.setPendingBindings(stack, draftedBindings);
            return new BindingBatchResult(0, draftedBindings);
        }

        BindingRegistry registry = SuperWirelessServices.runtimeManager()
            .getRegistry(world);
        BindingBatchResult result = new BindingBatchProcessor(
            SuperWirelessServices.resolver(),
            new BindingBatchProcessor.Reconciler() {

                @Override
                public com.github.nhaeutilities.modules.superwirelesskit.runtime.BindingReconcileResult reconcile(
                    World bindingWorld, BindingRecord record) {
                    return SuperWirelessServices.runtimeManager()
                        .reconcile(bindingWorld, record);
                }
            }).bind(world, controller, draftedBindings, registry);

        SuperWirelessKitStackState.setPendingBindings(stack, result.getFailedRecords());
        return result;
    }

    private static boolean canBuildOnController(EntityPlayer player, ResolvedControllerEndpoint controller) {
        try {
            return controller.getController()
                .getProxy()
                .getSecurity()
                .hasPermission(player, SecurityPermissions.BUILD);
        } catch (GridAccessException ignored) {
            return true;
        }
    }
}
