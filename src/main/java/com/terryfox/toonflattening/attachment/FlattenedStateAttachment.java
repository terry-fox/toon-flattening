package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terryfox.toonflattening.event.CollisionType;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public record FlattenedStateAttachment(
    boolean isFlattened,
    long flattenTime,
    CollisionType collisionType,
    @Nullable Direction wallDirection
) {

    public static final FlattenedStateAttachment DEFAULT = new FlattenedStateAttachment(
        false,
        0L,
        CollisionType.NONE,
        null
    );

    public static final Codec<FlattenedStateAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenedStateAttachment::isFlattened),
            Codec.LONG.fieldOf("flattenTime").forGetter(FlattenedStateAttachment::flattenTime),
            Codec.STRING.fieldOf("collisionType").forGetter(state -> state.collisionType().name()),
            Codec.STRING.fieldOf("wallDirection").forGetter(state ->
                state.wallDirection() != null ? state.wallDirection().getName() : ""
            )
        ).apply(instance, (isFlattened, flattenTime, collisionTypeName, wallDirectionName) ->
            new FlattenedStateAttachment(
                isFlattened,
                flattenTime,
                CollisionType.valueOf(collisionTypeName),
                wallDirectionName.isEmpty() ? null : Direction.byName(wallDirectionName)
            )
        )
    );
}
