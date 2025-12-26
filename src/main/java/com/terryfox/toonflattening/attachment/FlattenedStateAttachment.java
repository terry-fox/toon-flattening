package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terryfox.toonflattening.api.FlattenDirection;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record FlattenedStateAttachment(
    boolean isFlattened,
    long flattenTime,
    @Nullable ResourceLocation causeId,
    @Nullable FlattenDirection direction
) {

    public static final FlattenedStateAttachment DEFAULT = new FlattenedStateAttachment(false, 0L, null, null);

    public static final Codec<FlattenedStateAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenedStateAttachment::isFlattened),
            Codec.LONG.fieldOf("flattenTime").forGetter(FlattenedStateAttachment::flattenTime),
            ResourceLocation.CODEC.optionalFieldOf("causeId").forGetter(a -> java.util.Optional.ofNullable(a.causeId())),
            FlattenDirection.CODEC.optionalFieldOf("direction").forGetter(a -> java.util.Optional.ofNullable(a.direction()))
        ).apply(instance, (isFlattened, flattenTime, causeId, direction) ->
            new FlattenedStateAttachment(isFlattened, flattenTime, causeId.orElse(null), direction.orElse(null))
        )
    );
}
