package com.terryfox.toonflattening.detection;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Priority-sorted registry for custom flattening triggers.
 * <p>
 * Per SRS FR-DETECT.7: Third-party mods register triggers with priority values.
 * Higher priority triggers are evaluated first.
 * <p>
 * Singleton pattern ensures single source of truth for trigger evaluation.
 */
public final class TriggerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerRegistry.class);
    private static final TriggerRegistry INSTANCE = new TriggerRegistry();

    /**
     * TreeMap with reverse order (highest priority first).
     * Each priority maps to a list of triggers registered at that priority.
     */
    private final TreeMap<Integer, List<IFlattenTrigger>> triggers = new TreeMap<>(Collections.reverseOrder());

    private TriggerRegistry() {
    }

    public static TriggerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a custom flattening trigger.
     * <p>
     * Per SRS FR-DETECT.7.1: Triggers are evaluated in descending priority order.
     *
     * @param trigger Trigger implementation
     * @param priority Priority value (higher = evaluated first)
     */
    public void registerTrigger(IFlattenTrigger trigger, int priority) {
        triggers.computeIfAbsent(priority, k -> new ArrayList<>()).add(trigger);
        LOGGER.info("Registered custom flatten trigger: {} (priority={})", trigger.getClass().getSimpleName(), priority);
    }

    /**
     * Get the first active trigger for the given player.
     * <p>
     * Iterates through triggers in priority order, returning the first
     * trigger where shouldTrigger() returns true.
     *
     * @param player Target player
     * @return First matching trigger, or null if none match
     */
    @Nullable
    public IFlattenTrigger getActiveTrigger(ServerPlayer player) {
        for (List<IFlattenTrigger> triggerList : triggers.values()) {
            for (IFlattenTrigger trigger : triggerList) {
                if (trigger.shouldTrigger(player)) {
                    return trigger;
                }
            }
        }
        return null;
    }

    /**
     * Clear all registered triggers.
     * <p>
     * Used for testing or mod reload scenarios.
     */
    public void clearAll() {
        triggers.clear();
        LOGGER.info("Cleared all custom flatten triggers");
    }
}
