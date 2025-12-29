package com.terryfox.toonflattening.infrastructure;

import com.terryfox.toonflattening.reformation.ReformationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network packet registration and definitions.
 * <p>
 * Handles client→server communication for reformation keybind.
 */
public final class NetworkPackets {
    private NetworkPackets() {
    }

    /**
     * Register all network packets.
     *
     * @param registrar Payload registrar from RegisterPayloadHandlersEvent
     */
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                ReformRequestPacket.TYPE,
                ReformRequestPacket.STREAM_CODEC,
                ReformRequestPacket::handle
        );
    }

    /**
     * Client→Server: Request reformation when SPACE pressed.
     * <p>
     * Empty payload - player identity inferred from network context.
     */
    public record ReformRequestPacket() implements CustomPacketPayload {
        public static final Type<ReformRequestPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath("toonflattening", "reform_request"));

        public static final StreamCodec<FriendlyByteBuf, ReformRequestPacket> STREAM_CODEC =
                StreamCodec.unit(new ReformRequestPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /**
         * Server-side packet handler.
         * <p>
         * Delegates to ReformationHandler for validation + state transition.
         */
        public static void handle(ReformRequestPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    ReformationHandler.getInstance().onKeyPress(serverPlayer);
                }
            });
        }
    }
}
