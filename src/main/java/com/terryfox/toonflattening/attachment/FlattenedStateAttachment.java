package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terryfox.toonflattening.network.SyncFlattenStatePayload;

import java.util.Optional;

public record FlattenedStateAttachment(boolean isFlattened, long flattenTime, FrozenPoseData frozenPose, double accumulatedSpread, String flatteningSource) {

    public static final FlattenedStateAttachment DEFAULT = new FlattenedStateAttachment(false, 0L, null, 0.0, "");

    public static final Codec<FlattenedStateAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenedStateAttachment::isFlattened),
            Codec.LONG.fieldOf("flattenTime").forGetter(FlattenedStateAttachment::flattenTime),
            FrozenPoseData.CODEC.optionalFieldOf("frozenPose").forGetter(attachment ->
                Optional.ofNullable(attachment.frozenPose())),
            Codec.DOUBLE.optionalFieldOf("accumulatedSpread", 0.0).forGetter(FlattenedStateAttachment::accumulatedSpread),
            Codec.STRING.optionalFieldOf("flatteningSource", "").forGetter(FlattenedStateAttachment::flatteningSource)
        ).apply(instance, (isFlattened, flattenTime, frozenPose, accumulatedSpread, flatteningSource) ->
            new FlattenedStateAttachment(isFlattened, flattenTime, frozenPose.orElse(null), accumulatedSpread, flatteningSource))
    );

    public FlattenedStateAttachment withSpread(double newSpread) {
        return new FlattenedStateAttachment(isFlattened, flattenTime, frozenPose, newSpread, flatteningSource);
    }

    public SyncFlattenStatePayload toSyncPayload(int playerId) {
        return new SyncFlattenStatePayload(playerId, isFlattened, flattenTime, Optional.ofNullable(frozenPose), accumulatedSpread, flatteningSource);
    }
}
