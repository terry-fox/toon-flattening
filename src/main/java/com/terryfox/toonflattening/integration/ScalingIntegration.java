package com.terryfox.toonflattening.integration;

/**
 * Initialization class for scaling provider registration.
 * Registers built-in providers (Pehkui and NoOp fallback) at mod startup.
 */
public class ScalingIntegration {

    /**
     * Initialize scaling providers.
     * Called during mod initialization to register built-in providers.
     */
    public static void initialize() {
        // Register Pehkui provider with high priority
        ScalingProviderRegistry.registerProvider(new PehkuiScalingProvider(), 100);

        // Register NoOp fallback with minimum priority
        ScalingProviderRegistry.registerProvider(new NoOpScalingProvider(), Integer.MIN_VALUE);
    }
}
