package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RotationState(
    float yBodyRot,
    float yHeadRot,
    float xRot
) {
    public static final Codec<RotationState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("yBodyRot").forGetter(RotationState::yBodyRot),
            Codec.FLOAT.fieldOf("yHeadRot").forGetter(RotationState::yHeadRot),
            Codec.FLOAT.fieldOf("xRot").forGetter(RotationState::xRot)
        ).apply(instance, RotationState::new)
    );
}
