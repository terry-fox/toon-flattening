package com.terryfox.toonflattening.infrastructure;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for custom sound events.
 * <p>
 * Registers sound played when player becomes fully flattened.
 */
public final class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "toonflattening");

    public static final DeferredHolder<SoundEvent, SoundEvent> FLATTEN_SOUND =
            SOUNDS.register("flatten", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath("toonflattening", "flatten")
                    )
            );

    private SoundRegistry() {
    }
}
