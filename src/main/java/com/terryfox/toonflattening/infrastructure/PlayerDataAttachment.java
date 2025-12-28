package com.terryfox.toonflattening.infrastructure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terryfox.toonflattening.core.FlattenPhase;
import com.terryfox.toonflattening.core.FlattenState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.UUID;

/**
 * Player data attachment for persisting FlattenState across logout/login.
 * <p>
 * Per SRS FR-STATE.1: Uses NeoForge AttachmentType with Codec serialization.
 * Per SRS FR-STATE.3: copyOnDeath() enables respawn state reset.
 */
public final class PlayerDataAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "toonflattening");

    /**
     * Codec for serializing FlattenState to NBT.
     * <p>
     * Encodes all 14 fields. UUID and BlockPos use Optional for nullable fields.
     */
    public static final Codec<FlattenState> FLATTEN_STATE_CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.fieldOf("phase").forGetter(s -> s.phase().name()),
                            Codec.FLOAT.fieldOf("heightScale").forGetter(FlattenState::heightScale),
                            Codec.FLOAT.fieldOf("widthScale").forGetter(FlattenState::widthScale),
                            Codec.FLOAT.fieldOf("depthScale").forGetter(FlattenState::depthScale),
                            Codec.FLOAT.fieldOf("spreadMultiplier").forGetter(FlattenState::spreadMultiplier),
                            Codec.FLOAT.fieldOf("originalHitboxHeight").forGetter(FlattenState::originalHitboxHeight),
                            Codec.STRING.fieldOf("frozenPose").forGetter(s -> s.frozenPose().name()),
                            Codec.INT.fieldOf("recoveryTicksRemaining").forGetter(FlattenState::recoveryTicksRemaining),
                            Codec.INT.fieldOf("fallbackTicksRemaining").forGetter(FlattenState::fallbackTicksRemaining),
                            Codec.INT.fieldOf("reflattenCooldownTicks").forGetter(FlattenState::reflattenCooldownTicks),
                            Codec.INT.fieldOf("trackedAnvilCount").forGetter(FlattenState::trackedAnvilCount),
                            Codec.BOOL.fieldOf("hasContactingAnvil").forGetter(FlattenState::hasContactingAnvil),
                            UUIDUtil.CODEC.optionalFieldOf("anvilEntityUUID").forGetter(s -> Optional.ofNullable(s.anvilEntityUUID())),
                            BlockPos.CODEC.optionalFieldOf("anvilBlockPos").forGetter(s -> Optional.ofNullable(s.anvilBlockPos()))
                    ).apply(instance, PlayerDataAttachment::reconstructState)
            );

    /**
     * AttachmentType registration.
     * <p>
     * Uses copyOnDeath() to preserve state across respawn.
     * Respawn event handler will reset state to Normal per SRS FR-STATE.3.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FlattenState>> FLATTEN_STATE =
            ATTACHMENT_TYPES.register("flatten_state", () ->
                    AttachmentType.builder(() -> FlattenState.normal())
                            .serialize(FLATTEN_STATE_CODEC)
                            .copyOnDeath()
                            .build()
            );

    /**
     * Helper to reconstruct FlattenState from codec fields.
     * <p>
     * Converts String enum names to enums and unwraps Optional nullable fields.
     *
     * @return Reconstructed FlattenState instance
     */
    private static FlattenState reconstructState(
            String phase, float heightScale, float widthScale, float depthScale,
            float spreadMultiplier, float originalHitboxHeight, String frozenPose,
            int recoveryTicksRemaining, int fallbackTicksRemaining, int reflattenCooldownTicks,
            int trackedAnvilCount, boolean hasContactingAnvil,
            Optional<UUID> anvilEntityUUID, Optional<BlockPos> anvilBlockPos
    ) {
        return new FlattenState(
                FlattenPhase.valueOf(phase), heightScale, widthScale, depthScale,
                spreadMultiplier, originalHitboxHeight, Pose.valueOf(frozenPose),
                recoveryTicksRemaining, fallbackTicksRemaining, reflattenCooldownTicks,
                trackedAnvilCount, hasContactingAnvil,
                anvilEntityUUID.orElse(null), anvilBlockPos.orElse(null)
        );
    }

    private PlayerDataAttachment() {
    }
}
