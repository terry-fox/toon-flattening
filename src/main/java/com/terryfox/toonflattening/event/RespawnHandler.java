package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class RespawnHandler {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Reset flattened state on respawn
        serverPlayer.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            FlattenedStateAttachment.DEFAULT
        );

        // Reset Pehkui scale
        PehkuiIntegration.resetPlayerScale(serverPlayer);

        // Sync to clients
        NetworkHandler.syncFlattenState(serverPlayer, false, 0L, CollisionType.NONE, null, false, 0L, -1.0, 0.0f);

        ToonFlattening.LOGGER.debug("Reset flattened state for {} on respawn",
            serverPlayer.getName().getString());
    }
}
