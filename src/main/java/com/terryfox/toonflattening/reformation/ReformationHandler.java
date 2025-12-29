package com.terryfox.toonflattening.reformation;

import com.terryfox.toonflattening.core.FlattenPhase;
import com.terryfox.toonflattening.core.FlattenState;
import com.terryfox.toonflattening.core.FlattenStateManager;
import com.terryfox.toonflattening.infrastructure.ConfigSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Main entry point for reformation requests.
 * <p>
 * Per SRS FR-REFORM: Handles player-initiated recovery from FullyFlattened state
 * via SPACE keybind, with anvil-blocking detection and fallback timer.
 */
public final class ReformationHandler {
    private static final ReformationHandler INSTANCE = new ReformationHandler();

    private ReformationHandler() {
    }

    public static ReformationHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Called by infrastructure when reform keybind pressed.
     * <p>
     * Per SRS FR-REFORM.1: SPACE key event handler.
     *
     * @param player Target player
     */
    public void onKeyPress(ServerPlayer player) {
        if (!canReform(player)) {
            return; // Silently ignore
        }
        FlattenStateManager.getInstance().beginReformation(player);
    }

    /**
     * Check if reformation is allowed.
     * <p>
     * Per SRS FR-REFORM.4: Comprehensive validation.
     *
     * @param player Target player
     * @return True if reformation conditions met
     */
    public boolean canReform(Player player) {
        FlattenStateManager manager = FlattenStateManager.getInstance();
        FlattenPhase phase = manager.getPhase(player);

        if (phase != FlattenPhase.FULLY_FLATTENED) {
            return false;
        }

        FlattenState state = manager.getState(player);
        boolean fallbackExpired = state.fallbackTicksRemaining() <= 0;

        // Check anvil-blocking (only if fallback not expired)
        if (!fallbackExpired && ConfigSpec.anvil_blocking_enabled.get()) {
            if (ClearanceCalculator.hasAnvilAbove(player)) {
                return false; // Blocked by anvil
            }
        }

        // Check clearance (always required)
        if (!ClearanceCalculator.hasSufficientClearance(player, state.frozenPose())) {
            return false; // Blocked by insufficient space
        }

        return true;
    }

    /**
     * Called per-tick by infrastructure for fallback timer.
     * <p>
     * Per SRS FR-REFORM.9: Timer management (core handles actual decrement).
     * This method exists for future extension (sound effects, warnings, etc.).
     *
     * @param player Target player
     */
    public void tick(ServerPlayer player) {
        FlattenState state = FlattenStateManager.getInstance().getState(player);
        if (state.phase() == FlattenPhase.FULLY_FLATTENED && state.fallbackTicksRemaining() > 0) {
            // Core module handles timer decrement in FlattenStateManager.tick()
            // Future: Add warning sounds/particles when timer is low
        }
    }
}
