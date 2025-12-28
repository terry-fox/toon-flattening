package com.terryfox.toonflattening.core;

/**
 * Stateless utility for pure scale calculation functions.
 * <p>
 * All methods are static and side-effect free.
 * Per SRS FR-PROG.1/FR-PROG.2: Scale formulas.
 */
public final class ScaleCalculator {
    private ScaleCalculator() {
        throw new AssertionError("Utility class");
    }

    /**
     * Calculate height scale based on anvil-to-floor distance.
     * <p>
     * Per SRS FR-PROG.1.1: Height scale = (anvil bottom Y - floor Y) / original height.
     * Per SRS FR-PROG.1.2: Clamped to minimum height scale.
     *
     * @param anvilY Anvil bottom Y coordinate
     * @param floorY Floor surface Y coordinate
     * @param originalHeight Player's original hitbox height
     * @param minHeightScale Minimum allowed height scale (config)
     * @return Calculated height scale, clamped to [minHeightScale, 1.0]
     */
    public static float calculateHeightScale(double anvilY, double floorY, float originalHeight, float minHeightScale) {
        if (originalHeight <= 0.0f) {
            return minHeightScale;
        }
        float raw = (float) ((anvilY - floorY) / originalHeight);
        return Math.max(minHeightScale, Math.min(1.0f, raw));
    }

    /**
     * Calculate width/depth scale proportional to height compression.
     * <p>
     * Per SRS FR-PROG.2.1: Width scale = 1.0 + (1.0 - height scale) / 2.
     * Per SRS FR-PROG.2.2: Depth scale equals width scale.
     *
     * @param heightScale Current height scale
     * @return Calculated width/depth scale
     */
    public static float calculateWidthScale(float heightScale) {
        return 1.0f + (1.0f - heightScale) / 2.0f;
    }

    /**
     * Interpolate recovery animation from current to target scale.
     * <p>
     * Per SRS FR-PROG.8.1: Linear interpolation.
     *
     * @param current Current scale value
     * @param target Target scale value (typically 1.0)
     * @param progress Animation progress [0.0, 1.0]
     * @return Interpolated scale value
     */
    public static float interpolateRecovery(float current, float target, float progress) {
        return current + (target - current) * progress;
    }

    /**
     * Apply spread multiplier to width/depth scales.
     * <p>
     * Per SRS FR-REFL.2.4: Spread added to both width and depth equally.
     *
     * @param baseScale Base width/depth scale
     * @param spreadMultiplier Accumulated spread from re-flattening
     * @return Final scale with spread applied
     */
    public static float applySpread(float baseScale, float spreadMultiplier) {
        return baseScale + spreadMultiplier;
    }

    /**
     * Calculate spread based on anvil count.
     * <p>
     * Per SRS FR-REFL.2.1: Spread added = anvil_count × spread_increment.
     *
     * @param anvilCount Number of anvils in stack
     * @param spreadIncrement Spread per anvil (config)
     * @return Calculated spread multiplier
     */
    public static float calculateSpread(int anvilCount, float spreadIncrement) {
        return anvilCount * spreadIncrement;
    }

    /**
     * Calculate damage for anvil stack.
     * <p>
     * Per SRS FR-REFL.4.2: base_damage + (anvil_count - 1) × stack_damage_per_anvil.
     * Per SRS FR-REFL.4.3: Initial flatten with N-anvil stack uses same formula.
     *
     * @param anvilCount Number of anvils in stack
     * @param baseDamage Base damage amount (config)
     * @param stackDamagePerAnvil Additional damage per anvil beyond first (config)
     * @return Calculated total damage
     */
    public static float calculateStackDamage(int anvilCount, float baseDamage, float stackDamagePerAnvil) {
        if (anvilCount <= 0) {
            return 0.0f;
        }
        return baseDamage + Math.max(0, anvilCount - 1) * stackDamagePerAnvil;
    }
}
