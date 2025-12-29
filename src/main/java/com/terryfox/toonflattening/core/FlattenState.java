package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.attachment.FrozenPoseData;

record FlattenState(
    int spreadLevel,
    long flattenTime,
    FrozenPoseData frozenPose,
    int animationTicks,
    boolean sendSquashAnimation
) {
}
