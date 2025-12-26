package com.terryfox.toonflattening.registry;

import com.terryfox.toonflattening.event.FlattenCause;
import com.terryfox.toonflattening.trigger.FlattenTrigger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for flatten triggers.
 */
public class FlattenTriggerRegistry {
    private static final Map<FlattenCause, FlattenTrigger> TRIGGERS = new HashMap<>();

    public static void register(FlattenTrigger trigger) {
        TRIGGERS.put(trigger.getCause(), trigger);
    }

    public static Optional<FlattenTrigger> get(FlattenCause cause) {
        return Optional.ofNullable(TRIGGERS.get(cause));
    }

    public static Iterable<FlattenTrigger> getAll() {
        return TRIGGERS.values();
    }
}
