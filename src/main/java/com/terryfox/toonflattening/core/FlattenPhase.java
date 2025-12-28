package com.terryfox.toonflattening.core;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Four-state enum representing player flattening lifecycle.
 * <p>
 * State machine per SRS Appendix B:
 * <ul>
 *   <li>Normal → ProgressiveFlattening (anvil contact detected)</li>
 *   <li>ProgressiveFlattening → FullyFlattened (height scale ≤ 0.05)</li>
 *   <li>ProgressiveFlattening → Recovering (anvil lost before 0.05)</li>
 *   <li>FullyFlattened → Recovering (player-initiated reform)</li>
 *   <li>Recovering → ProgressiveFlattening (re-flatten during recovery)</li>
 *   <li>Recovering → Normal (animation complete)</li>
 * </ul>
 *
 * @see StringRepresentable
 */
public enum FlattenPhase implements StringRepresentable {
    /**
     * No anvil contact, player at normal scale.
     * <p>
     * Per SRS FR-PROG.3.1: Normal phase baseline.
     */
    NORMAL("normal"),

    /**
     * Anvil compressing player, height scale > 0.05.
     * <p>
     * Per SRS FR-PROG.3.2: Progressive compression phase.
     */
    PROGRESSIVE_FLATTENING("progressive_flattening"),

    /**
     * Height reached 0.05, player locked in flattened state.
     * <p>
     * Per SRS FR-PROG.3.3: Fully flattened phase with movement restrictions.
     * Per SRS FR-MOVE.1: Movement velocity set to zero.
     */
    FULLY_FLATTENED("fully_flattened"),

    /**
     * Anvil removed or reform started, animating back to normal scale.
     * <p>
     * Per SRS FR-PROG.3.4: Recovery animation phase.
     */
    RECOVERING("recovering");

    /**
     * NeoForge serialization codec.
     * <p>
     * Uses snake_case string representation for NBT and network serialization.
     */
    public static final Codec<FlattenPhase> CODEC = StringRepresentable.fromEnum(FlattenPhase::values);

    private final String name;

    FlattenPhase(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
