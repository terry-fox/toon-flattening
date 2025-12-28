package com.terryfox.toonflattening.integration;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback scaling provider that logs an error and performs no scaling.
 * Used when Pehkui or other scaling providers are unavailable.
 */
public class NoOpScalingProvider implements IScalingProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpScalingProvider.class);
    private boolean warningLogged = false;

    @Override
    public boolean canHandle(ServerPlayer player) {
        return true; // Always accepts as lowest priority fallback
    }

    @Override
    public void setScales(ServerPlayer player, float height, float width, float depth) {
        if (!warningLogged) {
            LOGGER.error("No scaling provider available! Pehkui is required for visual flattening.");
            LOGGER.error("Player {} will experience broken flattening (state changes but no visual scaling).",
                player.getName().getString());
            warningLogged = true;
        }
        // No-op: scales not applied
    }

    @Override
    public String getName() {
        return "NoOp (Fallback)";
    }
}
