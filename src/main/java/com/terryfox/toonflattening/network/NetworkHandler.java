package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.event.CollisionType;
import com.terryfox.toonflattening.event.PlayerMovementHandler;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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

            if (!FlattenedStateHelper.isFlattened(serverPlayer)) {
                return;
            }

            // Enter restoration state
            long restorationStartTime = serverPlayer.level().getGameTime();
            FlattenedStateHelper.setState(
                serverPlayer,
                new FlattenedStateAttachment(false, 0L, CollisionType.NONE, null, true, restorationStartTime, -1.0, 0.0f)
            );

            // Reset Pehkui scale with animation
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
            PehkuiIntegration.resetPlayerScaleWithDelay(serverPlayer, reformationTicks);

            // Restore gravity and clear locked position
            serverPlayer.setNoGravity(false);
            PlayerMovementHandler.clearFlattenedPosition(serverPlayer);

            // Sync to all tracking clients
            FlattenedStateAttachment restoringState = new FlattenedStateAttachment(false, 0L, CollisionType.NONE, null, true, restorationStartTime, -1.0, 0.0f);
            syncFlattenState(serverPlayer, restoringState);

            ToonFlattening.LOGGER.info("Player {} started restoration", serverPlayer.getName().getString());
        });
    }

    public static void syncFlattenState(ServerPlayer player, FlattenedStateAttachment state) {
        ToonFlattening.LOGGER.info("SERVER: Syncing flatten state for {}: isFlattened={}, collisionType={}, wallDirection={}, isRestoring={}",
            player.getName().getString(), state.isFlattened(), state.collisionType(), state.wallDirection(), state.isRestoring());

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            SyncFlattenStatePayload.fromAttachment(player.getId(), state)
        );
    }

    public static void sendSquashAnimation(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new TriggerSquashAnimationPayload(player.getId())
        );
    }
}
