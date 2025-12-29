package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.core.ScaleDimensions;
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
            // Handle backwards compatibility: if spreadLevel is 0, treat as 1
            int spreadLevel = state.spreadLevel() == 0 ? 1 : state.spreadLevel();

            // Restore flattened scale
            PehkuiIntegration.setPlayerScale(serverPlayer, ScaleDimensions.fromConfig(spreadLevel));

            // Sync flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, true, state.flattenTime(), state.frozenPose(), spreadLevel);

            ToonFlattening.LOGGER.debug("Restored flattened state for {} on login (spread level: {})",
                serverPlayer.getName().getString(), spreadLevel);
        } else {
            // Ensure scale is reset if not flattened
            PehkuiIntegration.resetPlayerScale(serverPlayer);

            // Sync non-flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, false, 0L, null, 0);

            ToonFlattening.LOGGER.debug("Synced non-flattened state for {} on login",
                serverPlayer.getName().getString());
        }
    }
}
