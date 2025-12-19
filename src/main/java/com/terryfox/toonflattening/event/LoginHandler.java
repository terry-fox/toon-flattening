package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.config.TriggerConfigSpec;
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
            // Restore flattened scale using trigger config
            TriggerConfigSpec config = ToonFlatteningConfig.CONFIG.getTriggerConfig(
                state.causeId() != null
                    ? FlattenCause.ANVIL  // Default to ANVIL for now
                    : FlattenCause.ANVIL
            );
            PehkuiIntegration.setPlayerScale(serverPlayer, (float) config.getHeightScale(), (float) config.getWidthScale());

            // Sync flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, true, state.flattenTime(), state.causeId(), state.direction());

            ToonFlattening.LOGGER.debug("Restored flattened state for {} on login",
                serverPlayer.getName().getString());
        } else {
            // Ensure scale is reset if not flattened
            PehkuiIntegration.resetPlayerScale(serverPlayer);

            // Sync non-flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, false, 0L, null, null);

            ToonFlattening.LOGGER.debug("Synced non-flattened state for {} on login",
                serverPlayer.getName().getString());
        }
    }
}
