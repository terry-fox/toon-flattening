package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.network.NetworkHandler;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
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
        FlattenedStateHelper.setState(serverPlayer, FlattenedStateAttachment.DEFAULT);

        // Sync to clients
        NetworkHandler.syncFlattenState(serverPlayer, FlattenedStateAttachment.DEFAULT);

        ToonFlattening.LOGGER.debug("Reset flattened state for {} on respawn",
            serverPlayer.getName().getString());
    }
}
