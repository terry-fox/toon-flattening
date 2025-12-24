package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.event.CollisionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncFlattenStatePayload(
    int playerId,
    boolean isFlattened,
    long flattenTime,
    int collisionTypeOrdinal,
    int wallDirectionId,
    boolean isRestoring,
    long restorationStartTime,
    double ceilingBlockY,
    float frozenYaw
) implements CustomPacketPayload {
    public static final Type<SyncFlattenStatePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "sync_flatten_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncFlattenStatePayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, SyncFlattenStatePayload payload) {
            buf.writeVarInt(payload.playerId);
            buf.writeBoolean(payload.isFlattened);
            buf.writeVarLong(payload.flattenTime);
            buf.writeVarInt(payload.collisionTypeOrdinal);
            buf.writeVarInt(payload.wallDirectionId);
            buf.writeBoolean(payload.isRestoring);
            buf.writeVarLong(payload.restorationStartTime);
            buf.writeDouble(payload.ceilingBlockY);
            buf.writeFloat(payload.frozenYaw);
        }

        @Override
        public SyncFlattenStatePayload decode(FriendlyByteBuf buf) {
            return new SyncFlattenStatePayload(
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarLong(),
                buf.readDouble(),
                buf.readFloat()
            );
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static SyncFlattenStatePayload fromAttachment(int playerId, FlattenedStateAttachment state) {
        return new SyncFlattenStatePayload(
            playerId,
            state.isFlattened(),
            state.flattenTime(),
            state.collisionType().ordinal(),
            state.wallDirection() != null ? state.wallDirection().get3DDataValue() : -1,
            state.isRestoring(),
            state.restorationStartTime(),
            state.ceilingBlockY(),
            state.frozenYaw()
        );
    }

    public FlattenedStateAttachment toAttachment() {
        return new FlattenedStateAttachment(
            isFlattened,
            flattenTime,
            CollisionType.values()[collisionTypeOrdinal],
            wallDirectionId == -1 ? null : Direction.from3DDataValue(wallDirectionId),
            isRestoring,
            restorationStartTime,
            ceilingBlockY,
            frozenYaw
        );
    }

    public static void handle(SyncFlattenStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ToonFlattening.LOGGER.info("CLIENT: Received sync packet: playerId={}, isFlattened={}, collisionTypeOrdinal={}, wallDirectionId={}",
                payload.playerId(), payload.isFlattened(), payload.collisionTypeOrdinal(), payload.wallDirectionId());

            var level = Minecraft.getInstance().level;
            if (level == null) {
                ToonFlattening.LOGGER.warn("CLIENT: Level is null, cannot sync");
                return;
            }
            var entity = level.getEntity(payload.playerId());
            if (!(entity instanceof Player player)) {
                ToonFlattening.LOGGER.warn("CLIENT: Entity {} is not a player", payload.playerId());
                return;
            }

            FlattenedStateAttachment attachment = payload.toAttachment();
            ToonFlattening.LOGGER.info("CLIENT: Syncing to player {}: collisionType={}, wallDirection={}",
                player.getName().getString(), attachment.collisionType(), attachment.wallDirection());

            player.setData(ToonFlattening.FLATTENED_STATE.get(), attachment);

            ToonFlattening.LOGGER.info("CLIENT: Sync complete for player {}", player.getName().getString());
        });
    }
}
