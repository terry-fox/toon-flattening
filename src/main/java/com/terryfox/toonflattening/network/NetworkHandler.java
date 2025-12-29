package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FrozenPoseData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
            ReformHandler::handleRequestReform
        );
    }

    public static void syncFlattenState(ServerPlayer player, boolean isFlattened, long flattenTime, FrozenPoseData frozenPose) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new SyncFlattenStatePayload(player.getId(), isFlattened, flattenTime, java.util.Optional.ofNullable(frozenPose))
        );
    }

    public static void sendSquashAnimation(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            new TriggerSquashAnimationPayload(player.getId())
        );
    }
}
