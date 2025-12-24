package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
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
        FlattenedStateAttachment state = FlattenedStateHelper.getState(serverPlayer);

        // Handle edge case: restoration animation should be complete on login
        if (state.isRestoring()) {
            long currentTime = serverPlayer.level().getGameTime();
            long elapsed = currentTime - state.restorationStartTime();
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();

            if (elapsed >= reformationTicks) {
                // Restoration complete, clear restoring flag
                state = FlattenedStateAttachment.DEFAULT;
                FlattenedStateHelper.setState(serverPlayer, state);
            }
        }

        if (state.isFlattened()) {
            // Restore flattened scale
            double heightScale = ToonFlatteningConfig.CONFIG.heightScale.get();
            double widthScale = ToonFlatteningConfig.CONFIG.widthScale.get();
            PehkuiIntegration.setPlayerScale(serverPlayer, (float) heightScale, (float) widthScale);

            // Sync flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, new FlattenedStateAttachment(true, state.flattenTime(), state.collisionType(), state.wallDirection(), false, 0L, state.ceilingBlockY(), state.frozenYaw(), state.wallSurfacePos()));

            ToonFlattening.LOGGER.debug("Restored flattened state for {} on login",
                serverPlayer.getName().getString());
        } else {
            // Ensure scale is reset if not flattened
            PehkuiIntegration.resetPlayerScale(serverPlayer);

            // Sync non-flattened state to client
            NetworkHandler.syncFlattenState(serverPlayer, new FlattenedStateAttachment(false, 0L, CollisionType.NONE, null, state.isRestoring(), state.restorationStartTime(), -1.0, 0.0f, -1.0));

            ToonFlattening.LOGGER.debug("Synced non-flattened state for {} on login",
                serverPlayer.getName().getString());
        }
    }
}
