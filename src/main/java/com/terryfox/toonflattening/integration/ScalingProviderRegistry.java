package com.terryfox.toonflattening.integration;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for scaling providers with priority-based selection and per-player caching.
 * Providers are selected based on priority (higher = preferred) and capability to handle players.
 */
public class ScalingProviderRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingProviderRegistry.class);
    private static final List<ProviderEntry> PROVIDERS = new ArrayList<>();
    private static final Map<UUID, IScalingProvider> CACHE = new ConcurrentHashMap<>();

    /**
     * Register a scaling provider with the given priority.
     * Higher priority providers are selected first.
     *
     * @param provider The provider to register
     * @param priority Priority level (higher = preferred, Integer.MIN_VALUE = fallback)
     */
    public static void registerProvider(IScalingProvider provider, int priority) {
        PROVIDERS.add(new ProviderEntry(provider, priority));
        PROVIDERS.sort(Comparator.comparingInt(ProviderEntry::priority).reversed());
        LOGGER.info("Registered scaling provider '{}' with priority {}",
            provider.getName(), priority);
    }

    /**
     * Get the scaling provider for the given player.
     * Uses cached provider if available, otherwise selects first capable provider.
     *
     * @param player The player to get provider for
     * @return The selected scaling provider
     */
    public static IScalingProvider getProvider(ServerPlayer player) {
        UUID uuid = player.getUUID();
        return CACHE.computeIfAbsent(uuid, k -> selectProvider(player));
    }

    /**
     * Select the first provider that can handle the player.
     *
     * @param player The player to select provider for
     * @return The selected provider, or NoOpScalingProvider if none available
     */
    private static IScalingProvider selectProvider(ServerPlayer player) {
        for (ProviderEntry entry : PROVIDERS) {
            if (entry.provider().canHandle(player)) {
                LOGGER.debug("Selected scaling provider '{}' for player {}",
                    entry.provider().getName(), player.getName().getString());
                return entry.provider();
            }
        }

        LOGGER.warn("No scaling provider found for player {}, using NoOp fallback",
            player.getName().getString());
        return new NoOpScalingProvider(); // Should never happen if NoOp registered
    }

    /**
     * Invalidate cached provider for the given player.
     * Called when player disconnects to free memory.
     *
     * @param playerUUID The player's UUID
     */
    public static void invalidateCache(UUID playerUUID) {
        CACHE.remove(playerUUID);
    }

    /**
     * Internal record for storing provider with priority.
     */
    private record ProviderEntry(IScalingProvider provider, int priority) {}
}
