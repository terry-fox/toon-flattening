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
    float yBodyRot,
    float yHeadRot,
    float xRot,
    float walkAnimPos,
    float walkAnimSpeed,
    float attackAnim,
    int swingTime,
    boolean swinging,
    float swimAmount,
    boolean crouching
) {
    public static final Codec<FrozenPoseData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("yBodyRot").forGetter(FrozenPoseData::yBodyRot),
            Codec.FLOAT.fieldOf("yHeadRot").forGetter(FrozenPoseData::yHeadRot),
            Codec.FLOAT.fieldOf("xRot").forGetter(FrozenPoseData::xRot),
            Codec.FLOAT.fieldOf("walkAnimPos").forGetter(FrozenPoseData::walkAnimPos),
            Codec.FLOAT.fieldOf("walkAnimSpeed").forGetter(FrozenPoseData::walkAnimSpeed),
            Codec.FLOAT.fieldOf("attackAnim").forGetter(FrozenPoseData::attackAnim),
            Codec.INT.fieldOf("swingTime").forGetter(FrozenPoseData::swingTime),
            Codec.BOOL.fieldOf("swinging").forGetter(FrozenPoseData::swinging),
            Codec.FLOAT.fieldOf("swimAmount").forGetter(FrozenPoseData::swimAmount),
            Codec.BOOL.fieldOf("crouching").forGetter(FrozenPoseData::crouching)
        ).apply(instance, FrozenPoseData::new)
    );

    /**
     * Captures the current pose state from a player.
     */
    public static FrozenPoseData capture(Player player) {
        return new FrozenPoseData(
            player.yBodyRot,
            player.yHeadRot,
            player.getXRot(),
            ((WalkAnimationStateAccessor) player.walkAnimation).getPosition(),
            player.walkAnimation.speed(),
            player.getAttackAnim(1.0f),
            player.swingTime,
            player.swinging,
            player.getSwimAmount(1.0f),
            player.isCrouching()
        );
    }
}
