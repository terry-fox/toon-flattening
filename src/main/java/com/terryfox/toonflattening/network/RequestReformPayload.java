package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.ToonFlattening;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestReformPayload() implements CustomPacketPayload {
    public static final Type<RequestReformPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "request_reform"));

    public static final StreamCodec<FriendlyByteBuf, RequestReformPayload> CODEC =
        StreamCodec.unit(new RequestReformPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
