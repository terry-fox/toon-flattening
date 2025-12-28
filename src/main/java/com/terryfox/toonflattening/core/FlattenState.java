package com.terryfox.toonflattening.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Immutable snapshot of player flattening state.
 * <p>
 * Per SRS FR-STATE.1: Persists across logout/reconnect.
 * All mutation occurs via factory/copy methods.
 *
 * @param phase Current flattening phase (SRS FR-PROG.3)
 * @param heightScale Current height multiplier (SRS FR-PROG.1)
 * @param widthScale Current width multiplier (SRS FR-PROG.2)
 * @param depthScale Current depth multiplier (SRS FR-PROG.2)
 * @param spreadMultiplier Accumulated horizontal spread from re-flattening (SRS FR-REFL.2)
 * @param originalHitboxHeight Player's hitbox height when compression began (SRS FR-PROG.1.3)
 * @param frozenPose Player pose captured at FullyFlattened transition (SRS FR-MOVE.5)
 * @param recoveryTicksRemaining Ticks left in recovery animation (SRS FR-REFORM.3)
 * @param fallbackTicksRemaining Ticks until anvil-blocking bypassed (SRS FR-REFORM.9)
 * @param reflattenCooldownTicks Cooldown before next re-flatten spread calculation (SRS FR-REFL.6)
 * @param trackedAnvilCount Number of anvils in current stack (SRS FR-REFL.2.3)
 * @param hasContactingAnvil True if anvil currently detected above player
 * @param anvilEntityUUID UUID of contacting falling anvil entity, null if none (SRS FR-STATE.1.5)
 * @param anvilBlockPos Position of contacting anvil block, null if none (SRS FR-STATE.1.5)
 */
public record FlattenState(
        FlattenPhase phase,
        float heightScale,
        float widthScale,
        float depthScale,
        float spreadMultiplier,
        float originalHitboxHeight,
        Pose frozenPose,
        int recoveryTicksRemaining,
        int fallbackTicksRemaining,
        int reflattenCooldownTicks,
        int trackedAnvilCount,
        boolean hasContactingAnvil,
        @Nullable UUID anvilEntityUUID,
        @Nullable BlockPos anvilBlockPos
) {
    /**
     * Factory: Default unflatted state.
     * <p>
     * Per SRS FR-PROG.3.1: Normal phase with identity scales.
     *
     * @return New state with phase=Normal, scales=1.0
     */
    public static FlattenState normal() {
        return new FlattenState(
                FlattenPhase.NORMAL,
                1.0f,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                Pose.STANDING,
                0,
                0,
                0,
                0,
                false,
                null,
                null
        );
    }

    /**
     * Immutable copy with phase change.
     *
     * @param newPhase New phase value
     * @return New state instance with updated phase
     */
    public FlattenState withPhase(FlattenPhase newPhase) {
        return new FlattenState(
                newPhase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with scale changes.
     *
     * @param newHeight Height scale
     * @param newWidth Width scale
     * @param newDepth Depth scale
     * @return New state instance with updated scales
     */
    public FlattenState withScales(float newHeight, float newWidth, float newDepth) {
        return new FlattenState(
                this.phase,
                newHeight,
                newWidth,
                newDepth,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with spread multiplier change.
     *
     * @param newSpread New spread multiplier
     * @return New state instance with updated spread
     */
    public FlattenState withSpreadMultiplier(float newSpread) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                newSpread,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with original hitbox height change.
     *
     * @param newOriginalHeight New original hitbox height
     * @return New state instance with updated height
     */
    public FlattenState withOriginalHitboxHeight(float newOriginalHeight) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                newOriginalHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with frozen pose change.
     *
     * @param newPose New frozen pose
     * @return New state instance with updated pose
     */
    public FlattenState withFrozenPose(Pose newPose) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                newPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with recovery ticks change.
     *
     * @param newTicks New recovery ticks remaining
     * @return New state instance with updated ticks
     */
    public FlattenState withRecoveryTicksRemaining(int newTicks) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                newTicks,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with fallback ticks change.
     *
     * @param newTicks New fallback ticks remaining
     * @return New state instance with updated ticks
     */
    public FlattenState withFallbackTicksRemaining(int newTicks) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                newTicks,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with reflatten cooldown change.
     *
     * @param newTicks New reflatten cooldown ticks
     * @return New state instance with updated cooldown
     */
    public FlattenState withReflattenCooldownTicks(int newTicks) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                newTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with anvil count change.
     *
     * @param newCount New tracked anvil count
     * @return New state instance with updated count
     */
    public FlattenState withTrackedAnvilCount(int newCount) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                newCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with anvil contact status change.
     *
     * @param newHasContact New contact status
     * @return New state instance with updated status
     */
    public FlattenState withHasContactingAnvil(boolean newHasContact) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                newHasContact,
                this.anvilEntityUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with anvil entity UUID change.
     *
     * @param newUUID New anvil entity UUID
     * @return New state instance with updated UUID
     */
    public FlattenState withAnvilEntityUUID(@Nullable UUID newUUID) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                newUUID,
                this.anvilBlockPos
        );
    }

    /**
     * Immutable copy with anvil block position change.
     *
     * @param newPos New anvil block position
     * @return New state instance with updated position
     */
    public FlattenState withAnvilBlockPos(@Nullable BlockPos newPos) {
        return new FlattenState(
                this.phase,
                this.heightScale,
                this.widthScale,
                this.depthScale,
                this.spreadMultiplier,
                this.originalHitboxHeight,
                this.frozenPose,
                this.recoveryTicksRemaining,
                this.fallbackTicksRemaining,
                this.reflattenCooldownTicks,
                this.trackedAnvilCount,
                this.hasContactingAnvil,
                this.anvilEntityUUID,
                newPos
        );
    }
}
