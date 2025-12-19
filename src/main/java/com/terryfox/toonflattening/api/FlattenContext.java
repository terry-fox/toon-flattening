package com.terryfox.toonflattening.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public record FlattenContext(
    ResourceLocation triggerId,
    double impactVelocity,
    FlattenDirection direction,
    @Nullable Entity sourceEntity
) {
}
