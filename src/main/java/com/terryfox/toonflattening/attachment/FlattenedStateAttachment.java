package com.terryfox.toonflattening.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.network.SyncFlattenStatePayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Consumer;

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

    public static boolean isFlattened(Player player) {
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        return state != null && state.isFlattened();
    }

    /**
     * Executes action if entity is a flattened player.
     * Common pattern: if (!(entity instanceof Player player)) return; if (isFlattened(player)) action();
     */
    public static void ifFlattened(Entity entity, Runnable action) {
        if (entity instanceof Player player && isFlattened(player)) {
            action.run();
        }
    }

    /**
     * Executes action if entity is a flattened player, passing the player.
     * Common pattern: if (!(entity instanceof Player player)) return; if (isFlattened(player)) action(player);
     */
    public static void ifFlattened(Entity entity, Consumer<Player> action) {
        if (entity instanceof Player player && isFlattened(player)) {
            action.accept(player);
        }
    }
}
