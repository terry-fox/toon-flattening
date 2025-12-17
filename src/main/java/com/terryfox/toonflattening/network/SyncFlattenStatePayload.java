package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncFlattenStatePayload(int playerId, boolean isFlattened, long flattenTime) implements CustomPacketPayload {
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
            SyncFlattenStatePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncFlattenStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            player.setData(
                ToonFlattening.FLATTENED_STATE.get(),
                new FlattenedStateAttachment(payload.isFlattened(), payload.flattenTime())
            );
        });
    }
}
