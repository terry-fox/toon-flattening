package com.terryfox.toonflattening.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton state machine managing all player flattening state.
 * <p>
 * Per SRS Appendix B: Implements 6 state transitions.
 * Infrastructure module injects config via setters during initialization.
 */
public final class FlattenStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlattenStateManager.class);
    private static final FlattenStateManager INSTANCE = new FlattenStateManager();

    // Per-player state storage
    private final Map<UUID, FlattenState> states = new HashMap<>();

    // Config values (injected by infrastructure module)
    private float minHeightScale = 0.05f;
    private float spreadIncrement = 0.8f;
    private float maxSpreadLimit = 6.0f;
    private float baseDamage = 8.0f; // 4.0 hearts × 2.0 points per heart
    private float stackDamagePerAnvil = 2.0f; // 1.0 heart × 2.0 points per heart
    private int reformationTicks = 5;
    private int fallbackTimeoutTicks = 6000; // 300 seconds × 20 ticks
    private int reflattenCooldownTicks = 20;

    private FlattenStateManager() {
    }

    public static FlattenStateManager getInstance() {
        return INSTANCE;
    }

    // ==================== Config Injection ====================

    public void setMinHeightScale(float minHeightScale) {
        this.minHeightScale = minHeightScale;
    }

    public void setSpreadIncrement(float spreadIncrement) {
        this.spreadIncrement = spreadIncrement;
    }

    public void setMaxSpreadLimit(float maxSpreadLimit) {
        this.maxSpreadLimit = maxSpreadLimit;
    }

    public void setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
    }

    public void setStackDamagePerAnvil(float stackDamagePerAnvil) {
        this.stackDamagePerAnvil = stackDamagePerAnvil;
    }

    public void setReformationTicks(int reformationTicks) {
        this.reformationTicks = reformationTicks;
    }

    public void setFallbackTimeoutTicks(int fallbackTimeoutTicks) {
        this.fallbackTimeoutTicks = fallbackTimeoutTicks;
    }

    public void setReflattenCooldownTicks(int reflattenCooldownTicks) {
        this.reflattenCooldownTicks = reflattenCooldownTicks;
    }

    // ==================== State Queries ====================

    /**
     * Get immutable snapshot of player state.
     * <p>
     * Returns normal state if no entry exists.
     *
     * @param player Target player
     * @return Immutable state snapshot
     */
    public FlattenState getState(Player player) {
        return states.getOrDefault(player.getUUID(), FlattenState.normal());
    }

    /**
     * Get current phase for player.
     * <p>
     * Per SRS API-METHOD.8.1: Returns current FlattenPhase enum.
     *
     * @param player Target player
     * @return Current phase
     */
    public FlattenPhase getPhase(Player player) {
        return getState(player).phase();
    }

    // ==================== State Transitions ====================

    /**
     * Transition: Normal → ProgressiveFlattening.
     * <p>
     * Per SRS FR-PROG.4: First anvil contact detected.
     *
     * @param player Target player
     * @param anvilY Anvil bottom Y coordinate
     * @param floorY Floor surface Y coordinate
     * @param anvilCount Number of anvils in stack
     * @param velocityY Anvil Y velocity (negative = falling)
     */
    public void beginCompression(ServerPlayer player, double anvilY, double floorY, int anvilCount, double velocityY) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.NORMAL) {
            return; // Already compressing
        }

        float originalHeight = (float) player.getBoundingBox().getYsize();
        float heightScale = ScaleCalculator.calculateHeightScale(anvilY, floorY, originalHeight, minHeightScale);
        float widthScale = ScaleCalculator.calculateWidthScale(heightScale);

        FlattenState newState = current
                .withPhase(FlattenPhase.PROGRESSIVE_FLATTENING)
                .withOriginalHitboxHeight(originalHeight)
                .withScales(heightScale, widthScale, widthScale)
                .withTrackedAnvilCount(anvilCount)
                .withHasContactingAnvil(true)
                .withTrackedAnvilVelocityY(velocityY)
                .withTrackedAnvilY(anvilY)
                .withTrackedFloorY(floorY);

        states.put(player.getUUID(), newState);

        // Check immediate FullyFlattened transition
        if (heightScale <= minHeightScale) {
            transitionToFullyFlattened(player);
        }
    }

    /**
     * Update compression during ProgressiveFlattening phase.
     * <p>
     * Per SRS FR-PROG.1: Recalculate scales based on current anvil position.
     *
     * @param player Target player
     * @param anvilY Anvil bottom Y coordinate
     * @param floorY Floor surface Y coordinate
     * @param anvilCount Number of anvils in stack
     * @param velocityY Anvil Y velocity (negative = falling)
     */
    public void updateCompression(ServerPlayer player, double anvilY, double floorY, int anvilCount, double velocityY) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.PROGRESSIVE_FLATTENING) {
            return;
        }

        float heightScale = ScaleCalculator.calculateHeightScale(anvilY, floorY, current.originalHitboxHeight(), minHeightScale);
        float widthScale = ScaleCalculator.calculateWidthScale(heightScale);

        // Check if velocity changed significantly (>20%)
        boolean velocityChanged = false;
        if (current.trackedAnvilVelocityY() != 0.0) {
            double velocityChange = Math.abs(velocityY - current.trackedAnvilVelocityY());
            double velocityChangePercent = velocityChange / Math.abs(current.trackedAnvilVelocityY());
            velocityChanged = velocityChangePercent > 0.2;
        }

        FlattenState newState = current
                .withScales(heightScale, widthScale, widthScale)
                .withTrackedAnvilCount(anvilCount)
                .withHasContactingAnvil(true)
                .withTrackedAnvilVelocityY(velocityChanged ? velocityY : current.trackedAnvilVelocityY());

        states.put(player.getUUID(), newState);

        // Check FullyFlattened transition
        if (heightScale <= minHeightScale) {
            transitionToFullyFlattened(player);
        }
    }

    /**
     * Transition: ProgressiveFlattening → FullyFlattened.
     * <p>
     * Per SRS FR-PROG.5: Height scale reached minimum (0.05).
     * Per SRS FR-PROG.5.1: Capture frozen pose.
     * Per SRS FR-PROG.5.2: Start fallback timer.
     * Per SRS FR-PROG.6: Apply damage.
     *
     * @param player Target player
     */
    private void transitionToFullyFlattened(ServerPlayer player) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.PROGRESSIVE_FLATTENING) {
            return;
        }

        // Calculate damage
        float damage = ScaleCalculator.calculateStackDamage(current.trackedAnvilCount(), baseDamage, stackDamagePerAnvil);

        // Apply damage (creative mode check per SRS FR-PROG.6.3)
        if (!player.isCreative() && damage > 0.0f) {
            DamageSource damageSource = player.damageSources().anvil(player);
            player.hurt(damageSource, damage);
        }

        // Transition state
        FlattenState newState = current
                .withPhase(FlattenPhase.FULLY_FLATTENED)
                .withFrozenPose(player.getPose())
                .withFallbackTicksRemaining(fallbackTimeoutTicks);

        states.put(player.getUUID(), newState);

        LOGGER.debug("Player {} transitioned to FullyFlattened (damage={}, anvilCount={})", player.getName().getString(), damage, current.trackedAnvilCount());
    }

    /**
     * Transition: ProgressiveFlattening → Recovering.
     * <p>
     * Per SRS FR-PROG.7: Anvil contact lost before reaching FullyFlattened.
     *
     * @param player Target player
     */
    public void lostContact(ServerPlayer player) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.PROGRESSIVE_FLATTENING) {
            return;
        }

        FlattenState newState = current
                .withPhase(FlattenPhase.RECOVERING)
                .withRecoveryTicksRemaining(reformationTicks)
                .withHasContactingAnvil(false)
                .withAnvilEntityUUID(null)
                .withAnvilBlockPos(null);

        states.put(player.getUUID(), newState);
        LOGGER.debug("Player {} lost anvil contact, transitioning to Recovering", player.getName().getString());
    }

    /**
     * Handle re-flatten logic for FullyFlattened or Recovering players.
     * <p>
     * Per SRS FR-REFL.1: Detect anvil contact on flattened/recovering players.
     * Per SRS FR-REFL.2: Calculate spread based on anvil count.
     * Per SRS FR-REFL.4: Apply damage based on scenario.
     *
     * @param player Target player
     * @param anvilCount Number of anvils in current stack
     * @param isReplacement True if no prior contact (replacement scenario), false if stacking
     */
    public void applyReflatten(ServerPlayer player, int anvilCount, boolean isReplacement) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.FULLY_FLATTENED && current.phase() != FlattenPhase.RECOVERING) {
            return;
        }

        // Cooldown check (per SRS FR-REFL.6)
        if (current.reflattenCooldownTicks() > 0) {
            return;
        }

        // Calculate new spread
        float spreadAdded = ScaleCalculator.calculateSpread(anvilCount, spreadIncrement);
        float newSpread = Math.min(current.spreadMultiplier() + spreadAdded, maxSpreadLimit);

        // Calculate damage (per SRS FR-REFL.4)
        float damage = 0.0f;
        if (isReplacement) {
            damage = ScaleCalculator.calculateStackDamage(anvilCount, baseDamage, stackDamagePerAnvil);
        }

        // Apply damage (creative mode check)
        if (!player.isCreative() && damage > 0.0f) {
            DamageSource damageSource = player.damageSources().anvil(player);
            player.hurt(damageSource, damage);
        }

        // Update state
        FlattenState newState;
        if (current.phase() == FlattenPhase.RECOVERING) {
            // Transition Recovering → ProgressiveFlattening (per SRS FR-REFL.7)
            newState = current
                    .withPhase(FlattenPhase.PROGRESSIVE_FLATTENING)
                    .withSpreadMultiplier(newSpread)
                    .withTrackedAnvilCount(anvilCount)
                    .withHasContactingAnvil(true)
                    .withReflattenCooldownTicks(reflattenCooldownTicks);
        } else {
            // FullyFlattened remains, update spread and reset fallback timer
            newState = current
                    .withSpreadMultiplier(newSpread)
                    .withTrackedAnvilCount(anvilCount)
                    .withHasContactingAnvil(true)
                    .withFallbackTicksRemaining(fallbackTimeoutTicks)
                    .withReflattenCooldownTicks(reflattenCooldownTicks);
        }

        states.put(player.getUUID(), newState);
        LOGGER.debug("Player {} re-flattened (spread={}, damage={}, replacement={})", player.getName().getString(), newSpread, damage, isReplacement);
    }

    /**
     * Transition: FullyFlattened → Recovering.
     * <p>
     * Per SRS FR-REFORM.3: Player-initiated reformation.
     *
     * @param player Target player
     */
    public void beginReformation(ServerPlayer player) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.FULLY_FLATTENED) {
            return;
        }

        FlattenState newState = current
                .withPhase(FlattenPhase.RECOVERING)
                .withRecoveryTicksRemaining(reformationTicks);

        states.put(player.getUUID(), newState);
        LOGGER.debug("Player {} began reformation", player.getName().getString());
    }

    /**
     * Tick recovery animation and decrement timers.
     * <p>
     * Per SRS FR-PROG.8: Interpolate scales during Recovering phase.
     * Per SRS FR-REFORM.9: Decrement fallback timer.
     *
     * @param player Target player
     */
    public void tick(ServerPlayer player) {
        FlattenState current = getState(player);

        // Decrement reflatten cooldown
        if (current.reflattenCooldownTicks() > 0) {
            FlattenState newState = current.withReflattenCooldownTicks(current.reflattenCooldownTicks() - 1);
            states.put(player.getUUID(), newState);
            current = newState;
        }

        // Decrement fallback timer (only in FullyFlattened phase)
        if (current.phase() == FlattenPhase.FULLY_FLATTENED && current.fallbackTicksRemaining() > 0) {
            FlattenState newState = current.withFallbackTicksRemaining(current.fallbackTicksRemaining() - 1);
            states.put(player.getUUID(), newState);
            current = newState;
        }

        // Handle recovery animation
        if (current.phase() == FlattenPhase.RECOVERING) {
            if (current.recoveryTicksRemaining() <= 0) {
                // Transition Recovering → Normal
                FlattenState newState = FlattenState.normal();
                states.put(player.getUUID(), newState);
                LOGGER.debug("Player {} completed recovery animation", player.getName().getString());
            } else {
                // Decrement recovery ticks (Pehkui handles interpolation)
                FlattenState newState = current.withRecoveryTicksRemaining(current.recoveryTicksRemaining() - 1);
                states.put(player.getUUID(), newState);
            }
        }
    }

    // ==================== API Methods ====================

    /**
     * API: Set spread multiplier for player.
     * <p>
     * Per SRS API-METHOD.5: Only applies when player is in FullyFlattened phase.
     *
     * @param player Target player
     * @param spread New spread multiplier
     */
    public void setSpreadMultiplier(ServerPlayer player, float spread) {
        FlattenState current = getState(player);
        if (current.phase() != FlattenPhase.FULLY_FLATTENED) {
            return;
        }

        float clampedSpread = Math.min(spread, maxSpreadLimit);
        FlattenState newState = current.withSpreadMultiplier(clampedSpread);
        states.put(player.getUUID(), newState);
    }

    /**
     * API: Restore state from persistence.
     * <p>
     * Per SRS FR-STATE.2: Restore flattening state on login.
     *
     * @param player Target player
     * @param state Restored state
     */
    public void restoreState(Player player, FlattenState state) {
        states.put(player.getUUID(), state);
        LOGGER.debug("Restored state for player {} (phase={})", player.getName().getString(), state.phase());
    }

    /**
     * API: Reset player to Normal phase.
     * <p>
     * Per SRS FR-STATE.3: Reset on respawn.
     *
     * @param player Target player
     */
    public void reset(Player player) {
        states.put(player.getUUID(), FlattenState.normal());
        LOGGER.debug("Reset state for player {}", player.getName().getString());
    }

    /**
     * API: Clear state on logout.
     * <p>
     * Per SRS FR-STATE.1: Persistence handled by attachment system.
     *
     * @param player Target player
     */
    public void clearState(Player player) {
        states.remove(player.getUUID());
    }

    // ==================== Helper Methods ====================

    /**
     * Estimate fall duration in ticks based on anvil velocity and distance to floor.
     *
     * @param anvilY Anvil bottom Y coordinate
     * @param floorY Floor surface Y coordinate
     * @param velocityY Anvil Y velocity (negative = falling)
     * @return Estimated ticks to reach floor (clamped to 1-100)
     */
    public static int estimateFallTicks(double anvilY, double floorY, double velocityY) {
        if (velocityY >= 0) {
            // Anvil moving up or stationary - use default
            return 20; // 1 second fallback
        }
        double distance = anvilY - floorY;
        int ticks = (int) Math.ceil(distance / Math.abs(velocityY));
        return Math.max(1, Math.min(ticks, 100));
    }
}
