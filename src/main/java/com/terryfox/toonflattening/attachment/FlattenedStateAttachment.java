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
    @Nullable Direction wallDirection,
    boolean isRestoring,
    long restorationStartTime,
    double ceilingBlockY
) {

    public static final FlattenedStateAttachment DEFAULT = new FlattenedStateAttachment(
        false,
        0L,
        CollisionType.NONE,
        null,
        false,
        0L,
        -1.0
    );

    public static final Codec<FlattenedStateAttachment> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenedStateAttachment::isFlattened),
            Codec.LONG.fieldOf("flattenTime").forGetter(FlattenedStateAttachment::flattenTime),
            Codec.STRING.fieldOf("collisionType").forGetter(state -> state.collisionType().name()),
            Codec.STRING.fieldOf("wallDirection").forGetter(state ->
                state.wallDirection() != null ? state.wallDirection().getName() : ""
            ),
            Codec.BOOL.fieldOf("isRestoring").forGetter(FlattenedStateAttachment::isRestoring),
            Codec.LONG.fieldOf("restorationStartTime").forGetter(FlattenedStateAttachment::restorationStartTime),
            Codec.DOUBLE.fieldOf("ceilingBlockY").forGetter(FlattenedStateAttachment::ceilingBlockY)
        ).apply(instance, (isFlattened, flattenTime, collisionTypeName, wallDirectionName, isRestoring, restorationStartTime, ceilingBlockY) ->
            new FlattenedStateAttachment(
                isFlattened,
                flattenTime,
                CollisionType.valueOf(collisionTypeName),
                wallDirectionName.isEmpty() ? null : Direction.byName(wallDirectionName),
                isRestoring,
                restorationStartTime,
                ceilingBlockY
            )
        )
    );
}
