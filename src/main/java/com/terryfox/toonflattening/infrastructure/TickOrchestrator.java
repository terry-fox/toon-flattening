package com.terryfox.toonflattening.infrastructure;

import com.terryfox.toonflattening.core.FlattenPhase;
import com.terryfox.toonflattening.core.FlattenState;
import com.terryfox.toonflattening.core.FlattenStateManager;
import com.terryfox.toonflattening.core.ScaleCalculator;
import com.terryfox.toonflattening.detection.AnvilContactDetector;
import com.terryfox.toonflattening.integration.ScalingProviderRegistry;
import com.terryfox.toonflattening.reformation.ReformationHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TickOrchestrator {
    // Config reference (injected during initialization)
    private static int reformationTicks = 5;

    // Track previous states to detect phase transitions
    private final Map<UUID, FlattenState> previousStates = new HashMap<>();

    public static void setReformationTicks(int ticks) {
        reformationTicks = ticks;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Orchestrate per-tick processing (runs after player tick work)
        AnvilContactDetector.getInstance().tick(serverPlayer);
        FlattenStateManager.getInstance().tick(serverPlayer);
        ReformationHandler.getInstance().tick(serverPlayer);

        // Only update Pehkui on phase transitions
        FlattenState state = FlattenStateManager.getInstance().getState(serverPlayer);
        FlattenState prevState = previousStates.get(serverPlayer.getUUID());

        if (shouldUpdatePehkui(prevState, state)) {
            applyPehkuiTransition(serverPlayer, state);
        }

        previousStates.put(serverPlayer.getUUID(), state);
    }

    /**
     * Determine if Pehkui needs to be updated.
     * Only returns true on phase transitions or significant velocity changes.
     *
     * @param prev Previous state (null on first tick)
     * @param curr Current state
     * @return True if Pehkui setScales should be called
     */
    private boolean shouldUpdatePehkui(FlattenState prev, FlattenState curr) {
        if (prev == null) {
            // First tick - update if not NORMAL
            return curr.phase() != FlattenPhase.NORMAL;
        }

        // Phase transition detected
        if (prev.phase() != curr.phase()) {
            return true;
        }

        // For PROGRESSIVE_FLATTENING, update every tick
        if (curr.phase() == FlattenPhase.PROGRESSIVE_FLATTENING) {
            return true;
        }

        return false;
    }

    /**
     * Apply Pehkui transition based on current phase.
     * Sets target scales and duration for Pehkui to interpolate.
     *
     * @param player Target player
     * @param state Current flatten state
     */
    private void applyPehkuiTransition(ServerPlayer player, FlattenState state) {
        var provider = ScalingProviderRegistry.getProvider(player);
        switch (state.phase()) {
            case PROGRESSIVE_FLATTENING:
                // Instant update to current scale state (0 duration for immediate response)
                provider.setScales(player, state.heightScale(), state.widthScale(), state.depthScale(), 0);
                break;

            case RECOVERING:
                // Target = normal (1.0), duration = reformation ticks
                ScalingProviderRegistry.getProvider(player)
                        .setScales(player, 1.0f, 1.0f, 1.0f, reformationTicks);
                break;

            case FULLY_FLATTENED:
                // Instant snap to current scale (already at minimum from progressive)
                ScalingProviderRegistry.getProvider(player)
                        .setScales(player, state.heightScale(), state.widthScale(), state.depthScale(), 0);
                break;

            case NORMAL:
                // Ensure reset to 1.0
                ScalingProviderRegistry.getProvider(player)
                        .setScales(player, 1.0f, 1.0f, 1.0f, 0);
                break;
        }
    }
}
