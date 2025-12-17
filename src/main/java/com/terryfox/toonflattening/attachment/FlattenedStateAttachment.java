package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FlattenedStateAttachment(boolean isFlattened, long flattenTime) {

    public static final FlattenedStateAttachment DEFAULT = new FlattenedStateAttachment(false, 0L);

    public static final Codec<FlattenedStateAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenedStateAttachment::isFlattened),
            Codec.LONG.fieldOf("flattenTime").forGetter(FlattenedStateAttachment::flattenTime)
        ).apply(instance, FlattenedStateAttachment::new)
    );
}
