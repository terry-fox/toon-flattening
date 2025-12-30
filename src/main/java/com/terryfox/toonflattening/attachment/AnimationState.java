package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AnimationState(
    float walkAnimPos,
    float walkAnimSpeed,
    float attackAnim,
    int swingTime,
    boolean swinging,
    float swimAmount,
    boolean crouching
) {
    public static final Codec<AnimationState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("walkAnimPos").forGetter(AnimationState::walkAnimPos),
            Codec.FLOAT.fieldOf("walkAnimSpeed").forGetter(AnimationState::walkAnimSpeed),
            Codec.FLOAT.fieldOf("attackAnim").forGetter(AnimationState::attackAnim),
            Codec.INT.fieldOf("swingTime").forGetter(AnimationState::swingTime),
            Codec.BOOL.fieldOf("swinging").forGetter(AnimationState::swinging),
            Codec.FLOAT.fieldOf("swimAmount").forGetter(AnimationState::swimAmount),
            Codec.BOOL.fieldOf("crouching").forGetter(AnimationState::crouching)
        ).apply(instance, AnimationState::new)
    );
}
