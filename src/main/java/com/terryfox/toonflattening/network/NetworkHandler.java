package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.api.FlattenDirection;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.attachment.FrozenPoseData;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = ToonFlattening.MODID)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client-bound packets
        registrar.playToClient(
            SyncFlattenStatePayload.TYPE,
            SyncFlattenStatePayload.CODEC,
            SyncFlattenStatePayload::handle
        );
        registrar.playToClient(
            TriggerSquashAnimationPayload.TYPE,
            TriggerSquashAnimationPayload.CODEC,
            TriggerSquashAnimationPayload::handle
        );

        // Server-bound packets
        registrar.playToServer(
            RequestReformPayload.TYPE,
            RequestReformPayload.CODEC,
            NetworkHandler::handleRequestReform
        );
    }

    public static void handleRequestReform(RequestReformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            FlattenedStateAttachment state = serverPlayer.getData(ToonFlattening.FLATTENED_STATE.get());
            if (!state.isFlattened()) {
                return;
            }

            // Reset flattened state
            serverPlayer.setData(
                ToonFlattening.FLATTENED_STATE.get(),
                FlattenedStateAttachment.DEFAULT
            );

            // Reset Pehkui scale
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
            PehkuiIntegration.resetPlayerScaleWithDelay(serverPlayer, reformationTicks);

            // Sync to all tracking clients
            syncFlattenState(serverPlayer, false, 0L, null, null, null);

            ToonFlattening.LOGGER.info("Player {} reformed", serverPlayer.getName().getString());
        });
    }

    public static void syncFlattenState(
        ServerPlayer player,
        boolean isFlattened,
        long flattenTime,
        @Nullable ResourceLocation causeId,
        @Nullable FlattenDirection direction,
        @Nullable FrozenPoseData frozenPose
    ) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new SyncFlattenStatePayload(player.getId(), isFlattened, flattenTime, causeId, direction, frozenPose)
        );
    }

    public static void sendSquashAnimation(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new TriggerSquashAnimationPayload(player.getId())
        );
    }
}
