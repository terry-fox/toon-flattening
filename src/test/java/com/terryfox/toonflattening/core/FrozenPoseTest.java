package com.terryfox.toonflattening.core;

import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class FrozenPoseTest {

    @Test
    void constructor_storesAllFields() {
        FrozenPose pose = new FrozenPose(Pose.SWIMMING, 90f, -45f, 80f);

        assertEquals(Pose.SWIMMING, pose.pose());
        assertEquals(90f, pose.yRot());
        assertEquals(-45f, pose.xRot());
        assertEquals(80f, pose.bodyYRot());
    }

    @ParameterizedTest
    @CsvSource({
        "STANDING, 1.8",
        "CROUCHING, 1.5",
        "SWIMMING, 0.6",
        "FALL_FLYING, 0.6",
        "SLEEPING, 0.2"
    })
    void getHitboxHeight_returnsCorrectHeightForPose(String poseName, float expectedHeight) {
        Pose pose = Pose.valueOf(poseName);
        FrozenPose frozenPose = new FrozenPose(pose, 0f, 0f, 0f);

        assertEquals(expectedHeight, frozenPose.getHitboxHeight(), 0.001f);
    }

    @Test
    void getHitboxHeight_unknownPoseDefaultsToStanding() {
        // DYING is not a typical pose for flatten, should default to 1.8
        FrozenPose pose = new FrozenPose(Pose.DYING, 0f, 0f, 0f);

        assertEquals(1.8f, pose.getHitboxHeight(), 0.001f);
    }

    @Test
    void recordEquality() {
        FrozenPose pose1 = new FrozenPose(Pose.STANDING, 45f, 30f, 40f);
        FrozenPose pose2 = new FrozenPose(Pose.STANDING, 45f, 30f, 40f);
        FrozenPose pose3 = new FrozenPose(Pose.CROUCHING, 45f, 30f, 40f);

        assertEquals(pose1, pose2);
        assertNotEquals(pose1, pose3);
        assertEquals(pose1.hashCode(), pose2.hashCode());
    }
}
