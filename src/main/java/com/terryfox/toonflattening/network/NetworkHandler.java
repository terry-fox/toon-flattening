package com.terryfox.toonflattening.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.terryfox.toonflattening.ToonFlattening;

@EventBusSubscriber(modid = ToonFlattening.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
            SyncFlattenStatePayload.TYPE,
            SyncFlattenStatePayload.CODEC,
            SyncFlattenStatePayload::handle
        );
    }

    public static void syncFlattenState(ServerPlayer player, boolean isFlattened, long flattenTime) {
        player.connection.send(new SyncFlattenStatePayload(player.getId(), isFlattened, flattenTime));
    }
}
