package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.attachment.FrozenPoseData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record SyncFlattenStatePayload(int playerId, boolean isFlattened, long flattenTime, Optional<FrozenPoseData> frozenPose) implements CustomPacketPayload {
    public static final Type<SyncFlattenStatePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "sync_flatten_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncFlattenStatePayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncFlattenStatePayload::playerId,
            ByteBufCodecs.BOOL,
            SyncFlattenStatePayload::isFlattened,
            ByteBufCodecs.VAR_LONG,
            SyncFlattenStatePayload::flattenTime,
            ByteBufCodecs.optional(ByteBufCodecs.fromCodec(FrozenPoseData.CODEC)),
            SyncFlattenStatePayload::frozenPose,
            SyncFlattenStatePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncFlattenStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            var entity = level.getEntity(payload.playerId());
            if (!(entity instanceof Player player)) {
                return;
            }
            player.setData(
                ToonFlattening.FLATTENED_STATE.get(),
                new FlattenedStateAttachment(payload.isFlattened(), payload.flattenTime(), payload.frozenPose().orElse(null))
            );
        });
    }
}
