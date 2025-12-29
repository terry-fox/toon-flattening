package com.terryfox.toonflattening.infrastructure.client;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.infrastructure.NetworkPackets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side input handling (FORGE bus).
 * <p>
 * Sends reformation packet when SPACE pressed.
 */
@EventBusSubscriber(modid = ToonFlattening.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientInputHandler {
    private ClientInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (KeybindHandler.REFORM_KEY.consumeClick()) {
            PacketDistributor.sendToServer(new NetworkPackets.ReformRequestPacket());
        }
    }
}
