package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record FlattenedStateAttachment(boolean isFlattened, long flattenTime, FrozenPoseData frozenPose) {

    public static final FlattenedStateAttachment DEFAULT = new FlattenedStateAttachment(false, 0L, null);

    public static final Codec<FlattenedStateAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenedStateAttachment::isFlattened),
            Codec.LONG.fieldOf("flattenTime").forGetter(FlattenedStateAttachment::flattenTime),
            FrozenPoseData.CODEC.optionalFieldOf("frozenPose").forGetter(attachment ->
                Optional.ofNullable(attachment.frozenPose()))
        ).apply(instance, (isFlattened, flattenTime, frozenPose) ->
            new FlattenedStateAttachment(isFlattened, flattenTime, frozenPose.orElse(null)))
    );
}
