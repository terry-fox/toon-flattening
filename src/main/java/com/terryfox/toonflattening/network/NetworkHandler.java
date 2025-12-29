package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.core.FlattenStateManager;
import com.terryfox.toonflattening.core.AnvilPinningHelper;
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

            // Check if anvil pinning is enabled and player is pinned
            if (ToonFlatteningConfig.CONFIG.anvilPinningEnabled.get() &&
                AnvilPinningHelper.isPlayerPinnedByAnvil(serverPlayer)) {

                int timeoutSeconds = ToonFlatteningConfig.CONFIG.anvilPinningTimeoutSeconds.get();

                // If timeout is 0, infinite pinning - deny reform
                if (timeoutSeconds == 0) {
                    return;
                }

                // Calculate elapsed time since flattening
                long currentGameTime = serverPlayer.level().getGameTime();
                long elapsedSeconds = (currentGameTime - state.flattenTime()) / 20;

                // If timeout hasn't elapsed, deny reform
                if (elapsedSeconds < timeoutSeconds) {
                    return;
                }
            }

            FlattenStateManager.resetPlayer(serverPlayer);

            ToonFlattening.LOGGER.info("Player {} reformed", serverPlayer.getName().getString());
        });
    }

    public static void syncFlattenState(ServerPlayer player, boolean isFlattened, long flattenTime) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new SyncFlattenStatePayload(player.getId(), isFlattened, flattenTime)
        );
    }

    public static void sendSquashAnimation(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new TriggerSquashAnimationPayload(player.getId())
        );
    }
}
