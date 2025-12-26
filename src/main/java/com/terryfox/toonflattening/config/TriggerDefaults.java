package com.terryfox.toonflattening.config;

import com.terryfox.toonflattening.event.FlattenCause;

/**
 * Default configuration values per trigger cause.
 */
public record TriggerDefaults(boolean enabled, double damage, double heightScale, double widthScale) {

    public static TriggerDefaults forCause(FlattenCause cause) {
        return switch (cause) {
            case ANVIL -> new TriggerDefaults(true, 4.0, 0.05, 1.8);
        };
    }
}
