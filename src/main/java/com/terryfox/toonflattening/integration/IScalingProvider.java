package com.terryfox.toonflattening.integration;

import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for scaling providers that abstract entity scaling APIs.
 * Providers are selected based on priority and capability to handle specific players.
 */
public interface IScalingProvider {
    /**
     * Check if this provider can handle scaling for the given player.
     *
     * @param player The player to check
     * @return true if this provider can apply scales to the player
     */
    boolean canHandle(ServerPlayer player);

    /**
     * Apply scale values to the player's hitbox and visual model.
     *
     * @param player The player to scale
     * @param height Height scale multiplier (1.0 = normal)
     * @param width Width scale multiplier (1.0 = normal)
     * @param depth Depth scale multiplier (1.0 = normal)
     */
    void setScales(ServerPlayer player, float height, float width, float depth);

    /**
     * Get the provider's name for logging purposes.
     *
     * @return Provider name
     */
    String getName();
}
