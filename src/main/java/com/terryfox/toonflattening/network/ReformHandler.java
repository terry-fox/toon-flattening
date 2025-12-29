package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ReformHandler {
    public static void handleRequestReform(RequestReformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            FlatteningStateController.tryReform(serverPlayer);
        });
    }
}
