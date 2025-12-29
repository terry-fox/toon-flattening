package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class RespawnHandler {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        FlatteningStateController.resetPlayer(serverPlayer);

        ToonFlattening.LOGGER.debug("Reset flattened state for {} on respawn",
            serverPlayer.getName().getString());
    }
}
