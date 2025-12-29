package com.terryfox.toonflattening.reformation;

import com.terryfox.toonflattening.infrastructure.ConfigSpec;

/**
 * Utility class for fallback timer initialization and management.
 * <p>
 * Per SRS FR-REFORM.9: Fallback timer allows bypassing anvil-blocking after timeout.
 * Core FlattenStateManager owns fallback ticks in state, this class provides initialization logic.
 */
public final class FallbackTimer {
    private static final int TICKS_PER_SECOND = 20;

    private FallbackTimer() {
    }

    /**
     * Start timer when entering FullyFlattened phase.
     * <p>
     * Per SRS FR-REFORM.9.1: Convert config seconds to ticks.
     *
     * @return Timer ticks, or -1 if disabled (config = 0)
     */
    public static int initializeTimer() {
        int seconds = ConfigSpec.fallback_timeout_seconds.get();
        if (seconds == 0) {
            return -1; // Disabled
        }
        return seconds * TICKS_PER_SECOND;
    }

    /**
     * Reset timer on re-flatten.
     * <p>
     * Per SRS FR-REFORM.9.2: Re-read config on each reset.
     *
     * @return Timer ticks, or -1 if disabled
     */
    public static int resetTimer() {
        return initializeTimer();
    }

    /**
     * Decrement timer each tick (called by core).
     * <p>
     * Per SRS FR-REFORM.9.3: Tick logic (core module handles actual call).
     *
     * @param currentTicks Current timer value
     * @return Decremented value, or 0 if already expired
     */
    public static int tick(int currentTicks) {
        if (currentTicks <= 0) {
            return 0; // Already expired or disabled
        }
        return currentTicks - 1;
    }
}
