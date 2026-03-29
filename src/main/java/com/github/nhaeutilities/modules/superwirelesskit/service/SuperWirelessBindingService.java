package com.github.nhaeutilities.modules.superwirelesskit.service;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.github.nhaeutilities.modules.superwirelesskit.data.BindingRecord;
import com.github.nhaeutilities.modules.superwirelesskit.data.ControllerEndpointRef;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.ResolvedControllerEndpoint;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessDebugLog;
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

        SuperWirelessDebugLog.log(
            "BIND_SERVICE_DRAFTED",
            "player=%s controller=%s drafted=%d binderPlayerId=%d",
            player.getUniqueID(),
            formatController(controller),
            Integer.valueOf(draftedBindings.size()),
            Integer.valueOf(binderPlayerId));

        if (draftedBindings.isEmpty()) {
            SuperWirelessKitStackState.setPendingBindings(stack, Collections.<BindingRecord>emptyList());
            SuperWirelessDebugLog.log("BIND_SERVICE_EMPTY", "controller=%s", formatController(controller));
            return new BindingBatchResult(0, Collections.<BindingRecord>emptyList());
        }

        ResolvedControllerEndpoint resolvedController = SuperWirelessServices.resolver()
            .resolveController(world, controller);
        boolean permitted = resolvedController != null && canBuildOnController(player, resolvedController);
        if (!permitted) {
            SuperWirelessKitStackState.setPendingBindings(stack, draftedBindings);
            SuperWirelessDebugLog.log(
                "BIND_SERVICE_CONTROLLER_REJECT",
                "player=%s controller=%s resolved=%s permitted=%s drafted=%d",
                player.getUniqueID(),
                formatController(controller),
                Boolean.valueOf(resolvedController != null),
                Boolean.valueOf(permitted),
                Integer.valueOf(draftedBindings.size()));
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
        SuperWirelessDebugLog.log(
            "BIND_SERVICE_RESULT",
            "player=%s controller=%s success=%d failures=%d",
            player.getUniqueID(),
            formatController(controller),
            Integer.valueOf(result.getSuccessCount()),
            Integer.valueOf(result.getFailureCount()));
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

    private static String formatController(ControllerEndpointRef controller) {
        return controller.getDimensionId() + ":"
            + controller.getX()
            + ","
            + controller.getY()
            + ","
            + controller.getZ()
            + "/"
            + controller.getFace()
                .name();
    }
}
