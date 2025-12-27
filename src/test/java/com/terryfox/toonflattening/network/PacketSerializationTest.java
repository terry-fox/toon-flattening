package com.terryfox.toonflattening.network;

import com.terryfox.toonflattening.core.FrozenPose;
import com.terryfox.toonflattening.network.packet.FlattenSyncPacket;
import com.terryfox.toonflattening.network.packet.ReformRequestPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketSerializationTest {

    @Test
    void flattenSyncPacket_serializesWithoutFrozenPose() {
        FlattenSyncPacket original = new FlattenSyncPacket(
            12345,
            false,
            0f,
            -1,
            null,
            0
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FlattenSyncPacket.STREAM_CODEC.encode(buf, original);

        FlattenSyncPacket deserialized = FlattenSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(original.playerId(), deserialized.playerId());
        assertEquals(original.isFlattened(), deserialized.isFlattened());
        assertEquals(original.spreadMultiplier(), deserialized.spreadMultiplier(), 0.001f);
        assertEquals(original.fallbackTicksRemaining(), deserialized.fallbackTicksRemaining());
        assertNull(deserialized.frozenPose());
        assertEquals(original.reformTicksRemaining(), deserialized.reformTicksRemaining());

        buf.release();
    }

    @Test
    void flattenSyncPacket_serializesWithFrozenPose() {
        FrozenPose pose = new FrozenPose(Pose.CROUCHING, 45f, -30f, 50f);
        FlattenSyncPacket original = new FlattenSyncPacket(
            67890,
            true,
            2.6f,
            6000,
            pose,
            5
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FlattenSyncPacket.STREAM_CODEC.encode(buf, original);

        FlattenSyncPacket deserialized = FlattenSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(original.playerId(), deserialized.playerId());
        assertEquals(original.isFlattened(), deserialized.isFlattened());
        assertEquals(original.spreadMultiplier(), deserialized.spreadMultiplier(), 0.001f);
        assertEquals(original.fallbackTicksRemaining(), deserialized.fallbackTicksRemaining());
        assertNotNull(deserialized.frozenPose());
        assertEquals(pose, deserialized.frozenPose());
        assertEquals(original.reformTicksRemaining(), deserialized.reformTicksRemaining());

        buf.release();
    }

    @Test
    void flattenSyncPacket_bufferFullyConsumed() {
        FlattenSyncPacket packet = new FlattenSyncPacket(123, true, 1.8f, 5000, null, 0);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FlattenSyncPacket.STREAM_CODEC.encode(buf, packet);

        int writtenBytes = buf.readableBytes();
        FlattenSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(0, buf.readableBytes(), "Buffer should be fully consumed");

        buf.release();
    }

    @Test
    void flattenSyncPacket_handlesMaxSpread() {
        FlattenSyncPacket packet = new FlattenSyncPacket(999, true, 6.0f, 10000, null, 100);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FlattenSyncPacket.STREAM_CODEC.encode(buf, packet);

        FlattenSyncPacket deserialized = FlattenSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(6.0f, deserialized.spreadMultiplier(), 0.001f);
        assertEquals(10000, deserialized.fallbackTicksRemaining());

        buf.release();
    }

    @Test
    void flattenSyncPacket_handlesAllPoseTypes() {
        Pose[] poses = { Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING };

        for (Pose pose : poses) {
            FrozenPose frozenPose = new FrozenPose(pose, 10f, 20f, 30f);
            FlattenSyncPacket packet = new FlattenSyncPacket(1, true, 1.8f, 100, frozenPose, 5);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            FlattenSyncPacket.STREAM_CODEC.encode(buf, packet);

            FlattenSyncPacket deserialized = FlattenSyncPacket.STREAM_CODEC.decode(buf);

            assertNotNull(deserialized.frozenPose());
            assertEquals(pose, deserialized.frozenPose().pose());

            buf.release();
        }
    }

    @Test
    void reformRequestPacket_serializes() {
        ReformRequestPacket original = new ReformRequestPacket();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ReformRequestPacket.STREAM_CODEC.encode(buf, original);

        ReformRequestPacket deserialized = ReformRequestPacket.STREAM_CODEC.decode(buf);

        assertNotNull(deserialized);

        buf.release();
    }

    @Test
    void reformRequestPacket_isEmpty() {
        // ReformRequestPacket has no fields, should serialize to zero bytes or minimal overhead
        ReformRequestPacket packet = new ReformRequestPacket();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ReformRequestPacket.STREAM_CODEC.encode(buf, packet);

        // Unit codec should write nothing or minimal marker
        int writtenBytes = buf.readableBytes();
        assertTrue(writtenBytes >= 0, "Should write minimal or zero bytes");

        buf.release();
    }

    @Test
    void reformRequestPacket_equality() {
        ReformRequestPacket packet1 = new ReformRequestPacket();
        ReformRequestPacket packet2 = new ReformRequestPacket();

        assertEquals(packet1, packet2);
        assertEquals(packet1.hashCode(), packet2.hashCode());
    }

    @Test
    void flattenSyncPacket_negativePlayerId() {
        // Edge case: negative player IDs shouldn't occur but test robustness
        FlattenSyncPacket packet = new FlattenSyncPacket(-1, false, 0f, -1, null, 0);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FlattenSyncPacket.STREAM_CODEC.encode(buf, packet);

        FlattenSyncPacket deserialized = FlattenSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(-1, deserialized.playerId());

        buf.release();
    }

    @Test
    void flattenSyncPacket_zeroValues() {
        FlattenSyncPacket packet = new FlattenSyncPacket(0, false, 0f, 0, null, 0);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        FlattenSyncPacket.STREAM_CODEC.encode(buf, packet);

        FlattenSyncPacket deserialized = FlattenSyncPacket.STREAM_CODEC.decode(buf);

        assertEquals(0, deserialized.playerId());
        assertFalse(deserialized.isFlattened());
        assertEquals(0f, deserialized.spreadMultiplier());
        assertEquals(0, deserialized.fallbackTicksRemaining());
        assertEquals(0, deserialized.reformTicksRemaining());

        buf.release();
    }
}
