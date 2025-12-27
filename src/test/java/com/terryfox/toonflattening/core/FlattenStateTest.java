package com.terryfox.toonflattening.core;

import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlattenStateTest {

    @Test
    void empty_createsDefaultState() {
        FlattenState state = FlattenState.empty();

        assertFalse(state.isFlattened());
        assertEquals(0f, state.spreadMultiplier());
        assertEquals(-1, state.fallbackTicksRemaining());
        assertNull(state.frozenPose());
        assertEquals(0, state.reformTicksRemaining());
        assertEquals(0, state.flattenedAtTick());
    }

    @Test
    void isReforming_returnsTrueWhenTicksGreaterThanZero() {
        FlattenState reforming = new FlattenState(true, 1.8f, 6000, null, 5, 1000L);
        FlattenState notReforming = new FlattenState(true, 1.8f, 6000, null, 0, 1000L);

        assertTrue(reforming.isReforming());
        assertFalse(notReforming.isReforming());
    }

    @Test
    void constructor_createsStateWithAllFields() {
        FrozenPose pose = new FrozenPose(Pose.CROUCHING, 45f, 30f, 50f);
        FlattenState state = new FlattenState(true, 2.5f, 100, pose, 3, 500L);

        assertTrue(state.isFlattened());
        assertEquals(2.5f, state.spreadMultiplier());
        assertEquals(100, state.fallbackTicksRemaining());
        assertEquals(pose, state.frozenPose());
        assertEquals(3, state.reformTicksRemaining());
        assertEquals(500L, state.flattenedAtTick());
    }

    @Test
    void stateIsImmutable() {
        FlattenState original = new FlattenState(true, 1.8f, 6000, null, 0, 1000L);
        FlattenState modified = new FlattenState(
            original.isFlattened(),
            original.spreadMultiplier() + 0.8f,
            original.fallbackTicksRemaining(),
            original.frozenPose(),
            5,
            original.flattenedAtTick()
        );

        assertEquals(1.8f, original.spreadMultiplier());
        assertEquals(2.6f, modified.spreadMultiplier(), 0.001f);
        assertEquals(0, original.reformTicksRemaining());
        assertEquals(5, modified.reformTicksRemaining());
    }
}
