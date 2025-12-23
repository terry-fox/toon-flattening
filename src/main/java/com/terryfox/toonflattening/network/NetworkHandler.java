package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.event.CollisionType;
import com.terryfox.toonflattening.event.PlayerMovementHandler;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
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

            FlattenedStateAttachment state = serverPlayer.getData(ToonFlattening.FLATTENED_STATE.get());
            if (!state.isFlattened()) {
                return;
            }

            // Enter restoration state
            long restorationStartTime = serverPlayer.level().getGameTime();
            serverPlayer.setData(
                ToonFlattening.FLATTENED_STATE.get(),
                new FlattenedStateAttachment(false, 0L, CollisionType.NONE, null, true, restorationStartTime, -1.0)
            );

            // Reset Pehkui scale with animation
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
            PehkuiIntegration.resetPlayerScaleWithDelay(serverPlayer, reformationTicks);

            // Restore gravity and clear locked position
            serverPlayer.setNoGravity(false);
            PlayerMovementHandler.clearFlattenedPosition(serverPlayer);

            // Sync to all tracking clients
            syncFlattenState(serverPlayer, false, 0L, CollisionType.NONE, null, true, restorationStartTime, -1.0);

            ToonFlattening.LOGGER.info("Player {} started restoration", serverPlayer.getName().getString());
        });
    }

    public static void syncFlattenState(ServerPlayer player, boolean isFlattened, long flattenTime, CollisionType collisionType, Direction wallDirection, boolean isRestoring, long restorationStartTime, double ceilingBlockY) {
        int collisionTypeOrdinal = collisionType.ordinal();
        int wallDirectionId = (wallDirection != null) ? wallDirection.get3DDataValue() : -1;

        ToonFlattening.LOGGER.info("SERVER: Syncing flatten state for {}: isFlattened={}, collisionType={} (ordinal={}), wallDirection={} (id={}), isRestoring={}, restorationStartTime={}, ceilingBlockY={}",
            player.getName().getString(), isFlattened, collisionType, collisionTypeOrdinal, wallDirection, wallDirectionId, isRestoring, restorationStartTime, ceilingBlockY);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new SyncFlattenStatePayload(player.getId(), isFlattened, flattenTime, collisionTypeOrdinal, wallDirectionId, isRestoring, restorationStartTime, ceilingBlockY)
        );
    }

    public static void sendSquashAnimation(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new TriggerSquashAnimationPayload(player.getId())
        );
    }
}
