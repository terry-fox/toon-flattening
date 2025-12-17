package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.client.SquashAnimationRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TriggerSquashAnimationPayload(int playerId) implements CustomPacketPayload {
    public static final Type<TriggerSquashAnimationPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "trigger_squash_animation"));

    public static final StreamCodec<FriendlyByteBuf, TriggerSquashAnimationPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TriggerSquashAnimationPayload::playerId,
            TriggerSquashAnimationPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TriggerSquashAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            SquashAnimationRenderer.playSquashEffect(payload.playerId());
        });
    }
}
