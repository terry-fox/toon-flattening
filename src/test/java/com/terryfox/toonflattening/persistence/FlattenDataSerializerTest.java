package com.terryfox.toonflattening.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.terryfox.toonflattening.core.FlattenState;
import com.terryfox.toonflattening.core.FrozenPose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlattenDataSerializerTest {

    @Test
    void frozenPoseCodec_serializesAndDeserializes() {
        FrozenPose original = new FrozenPose(Pose.CROUCHING, 45.5f, -30.2f, 60.8f);

        DataResult<Tag> encoded = FlattenDataSerializer.FROZEN_POSE_CODEC.encodeStart(NbtOps.INSTANCE, original);
        assertTrue(encoded.result().isPresent());

        Tag nbt = encoded.result().get();
        DataResult<FrozenPose> decoded = FlattenDataSerializer.FROZEN_POSE_CODEC.parse(NbtOps.INSTANCE, nbt);
        assertTrue(decoded.result().isPresent());

        FrozenPose deserialized = decoded.result().get();
        assertEquals(original, deserialized);
    }

    @Test
    void frozenPoseCodec_containsAllFields() {
        FrozenPose pose = new FrozenPose(Pose.SWIMMING, 90f, -45f, 80f);

        DataResult<Tag> result = FlattenDataSerializer.FROZEN_POSE_CODEC.encodeStart(NbtOps.INSTANCE, pose);
        CompoundTag nbt = (CompoundTag) result.result().get();

        assertTrue(nbt.contains("pose"));
        assertTrue(nbt.contains("yRot"));
        assertTrue(nbt.contains("xRot"));
        assertTrue(nbt.contains("bodyYRot"));

        assertEquals("swimming", nbt.getString("pose"));
        assertEquals(90f, nbt.getFloat("yRot"), 0.001f);
        assertEquals(-45f, nbt.getFloat("xRot"), 0.001f);
        assertEquals(80f, nbt.getFloat("bodyYRot"), 0.001f);
    }

    @Test
    void flattenStateCodec_serializesWithoutFrozenPose() {
        FlattenState original = new FlattenState(false, 0f, -1, null, 0, 0L);

        DataResult<Tag> encoded = FlattenDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, original);
        assertTrue(encoded.result().isPresent());

        Tag nbt = encoded.result().get();
        DataResult<FlattenState> decoded = FlattenDataSerializer.CODEC.parse(NbtOps.INSTANCE, nbt);
        assertTrue(decoded.result().isPresent());

        FlattenState deserialized = decoded.result().get();
        assertEquals(original, deserialized);
    }

    @Test
    void flattenStateCodec_serializesWithFrozenPose() {
        FrozenPose pose = new FrozenPose(Pose.STANDING, 0f, 0f, 0f);
        FlattenState original = new FlattenState(true, 2.6f, 6000, pose, 3, 12345L);

        DataResult<Tag> encoded = FlattenDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, original);
        assertTrue(encoded.result().isPresent());

        Tag nbt = encoded.result().get();
        DataResult<FlattenState> decoded = FlattenDataSerializer.CODEC.parse(NbtOps.INSTANCE, nbt);
        assertTrue(decoded.result().isPresent());

        FlattenState deserialized = decoded.result().get();
        assertEquals(original, deserialized);
    }

    @Test
    void flattenStateCodec_containsAllMandatoryFields() {
        FlattenState state = new FlattenState(true, 1.8f, 5000, null, 5, 9999L);

        DataResult<Tag> result = FlattenDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, state);
        CompoundTag nbt = (CompoundTag) result.result().get();

        assertTrue(nbt.contains("isFlattened"));
        assertTrue(nbt.contains("spreadMultiplier"));
        assertTrue(nbt.contains("fallbackTicksRemaining"));
        assertTrue(nbt.contains("reformTicksRemaining"));
        assertTrue(nbt.contains("flattenedAtTick"));

        assertTrue(nbt.getBoolean("isFlattened"));
        assertEquals(1.8f, nbt.getFloat("spreadMultiplier"), 0.001f);
        assertEquals(5000, nbt.getInt("fallbackTicksRemaining"));
        assertEquals(5, nbt.getInt("reformTicksRemaining"));
        assertEquals(9999L, nbt.getLong("flattenedAtTick"));
    }

    @Test
    void flattenStateCodec_optionalFrozenPoseHandlesNull() {
        FlattenState state = new FlattenState(true, 1.8f, 6000, null, 0, 1000L);

        DataResult<Tag> result = FlattenDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, state);
        CompoundTag nbt = (CompoundTag) result.result().get();

        assertFalse(nbt.contains("frozenPose"));
    }

    @Test
    void flattenStateCodec_roundtripPreservesData() {
        FrozenPose pose = new FrozenPose(Pose.FALL_FLYING, 120f, 15f, 110f);
        FlattenState original = new FlattenState(true, 4.2f, 3000, pose, 10, 54321L);

        Tag nbt = FlattenDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, original)
            .result()
            .orElseThrow();

        FlattenState deserialized = FlattenDataSerializer.CODEC.parse(NbtOps.INSTANCE, nbt)
            .result()
            .orElseThrow();

        assertEquals(original.isFlattened(), deserialized.isFlattened());
        assertEquals(original.spreadMultiplier(), deserialized.spreadMultiplier(), 0.001f);
        assertEquals(original.fallbackTicksRemaining(), deserialized.fallbackTicksRemaining());
        assertEquals(original.frozenPose(), deserialized.frozenPose());
        assertEquals(original.reformTicksRemaining(), deserialized.reformTicksRemaining());
        assertEquals(original.flattenedAtTick(), deserialized.flattenedAtTick());
    }

    @Test
    void flattenStateCodec_emptyStateRoundtrip() {
        FlattenState empty = FlattenState.empty();

        Tag nbt = FlattenDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, empty)
            .result()
            .orElseThrow();

        FlattenState deserialized = FlattenDataSerializer.CODEC.parse(NbtOps.INSTANCE, nbt)
            .result()
            .orElseThrow();

        assertEquals(empty, deserialized);
    }

    @Test
    void codec_handlesInvalidData() {
        CompoundTag invalidNbt = new CompoundTag();
        // Missing required fields

        DataResult<FlattenState> result = FlattenDataSerializer.CODEC.parse(NbtOps.INSTANCE, invalidNbt);

        assertTrue(result.error().isPresent(), "Should fail to parse incomplete data");
    }
}
