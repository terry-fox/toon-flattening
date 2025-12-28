package com.terryfox.toonflattening.detection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Extension interface for custom flattening triggers.
 * <p>
 * Per SRS FR-DETECT.7: Third-party mods can register triggers to bypass
 * standard anvil detection and instantly transition players to FullyFlattened phase.
 * <p>
 * Triggers are evaluated before standard anvil detection during each tick.
 * The first active trigger (by priority order) determines the flattening behavior.
 */
public interface IFlattenTrigger {
    /**
     * Check if this trigger should activate for the given player.
     * <p>
     * Called once per tick per player. Implementations should be performant.
     *
     * @param player Target player
     * @return True if trigger should activate and flatten the player
     */
    boolean shouldTrigger(ServerPlayer player);

    /**
     * Get damage amount to apply when trigger activates.
     * <p>
     * Default implementation returns 0.0 (no damage).
     *
     * @return Damage in damage points (not hearts)
     */
    default float getDamage() {
        return 0.0f;
    }

    /**
     * Get anvil position for this trigger.
     * <p>
     * Default implementation returns null, which signals instant flattening
     * without progressive compression animation.
     *
     * @return Anvil position, or null for instant flatten
     */
    @Nullable
    default BlockPos getAnvilPosition() {
        return null;
    }

    /**
     * Get anvil count for spread calculation.
     * <p>
     * Default implementation returns 1 (single anvil).
     *
     * @return Number of anvils for damage/spread calculation
     */
    default int getAnvilCount() {
        return 1;
    }
}
