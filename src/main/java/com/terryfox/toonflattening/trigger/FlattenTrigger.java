package com.terryfox.toonflattening.trigger;

import com.terryfox.toonflattening.api.FlattenContext;
import com.terryfox.toonflattening.event.FlattenCause;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;

/**
 * Interface for flatten triggers.
 */
public interface FlattenTrigger {
    /**
     * @return the cause associated with this trigger
     */
    FlattenCause getCause();

    /**
     * @return the resource location ID for this trigger
     */
    ResourceLocation getTriggerId();

    /**
     * Check if event should trigger flattening.
     * @param event the damage event
     * @return flatten context if should trigger, null otherwise
     */
    @Nullable
    FlattenContext shouldTrigger(LivingIncomingDamageEvent event);
}
