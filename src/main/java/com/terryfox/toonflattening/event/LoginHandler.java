package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class LoginHandler {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Read persisted flattened state
        FlattenedStateAttachment state = serverPlayer.getData(ToonFlattening.FLATTENED_STATE.get());

        if (state.isFlattened()) {
            // Restore flattened scale
            double heightScale = ToonFlatteningConfig.CONFIG.heightScale.get();
            double widthScale = ToonFlatteningConfig.CONFIG.widthScale.get();
            PehkuiIntegration.setPlayerScale(serverPlayer, (float) heightScale, (float) widthScale);

            // Sync flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, true, state.flattenTime(), state.collisionType(), state.wallDirection());

            ToonFlattening.LOGGER.debug("Restored flattened state for {} on login",
                serverPlayer.getName().getString());
        } else {
            // Ensure scale is reset if not flattened
            PehkuiIntegration.resetPlayerScale(serverPlayer);

            // Sync non-flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, false, 0L, CollisionType.NONE, null);

            ToonFlattening.LOGGER.debug("Synced non-flattened state for {} on login",
                serverPlayer.getName().getString());
        }
    }
}
