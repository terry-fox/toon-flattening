package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terryfox.toonflattening.mixin.accessor.WalkAnimationStateAccessor;
import net.minecraft.world.entity.player.Player;

/**
 * Captures and stores player pose data at the moment of flattening.
 * Used to freeze body/head rotations and animation states for third-person rendering.
 */
public record FrozenPoseData(
    RotationState rotation,
    AnimationState animation
) {
    public static final Codec<FrozenPoseData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            RotationState.CODEC.fieldOf("rotation").forGetter(FrozenPoseData::rotation),
            AnimationState.CODEC.fieldOf("animation").forGetter(FrozenPoseData::animation)
        ).apply(instance, FrozenPoseData::new)
    );

    /**
     * Captures the current pose state from a player.
     */
    public static FrozenPoseData capture(Player player) {
        RotationState rotation = new RotationState(
            player.yBodyRot,
            player.yHeadRot,
            player.getXRot()
        );

        AnimationState animation = new AnimationState(
            ((WalkAnimationStateAccessor) player.walkAnimation).getPosition(),
            player.walkAnimation.speed(),
            player.getAttackAnim(1.0f),
            player.swingTime,
            player.swinging,
            player.getSwimAmount(1.0f),
            player.isCrouching()
        );

        return new FrozenPoseData(rotation, animation);
    }
}
