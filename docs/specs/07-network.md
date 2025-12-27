# Network Module Specification

**Package:** `com.terryfox.toonflattening.network`
**Side:** Both

## Purpose

Client-server synchronization via NeoForge networking.

## Files

```
network/
├── ModNetworking.java
└── packet/
    ├── FlattenSyncPacket.java
    └── ReformRequestPacket.java
```

## ModNetworking

Channel registration and packet handling.

```java
public class ModNetworking {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ToonFlattening.MODID)
            .versioned("1.0.0");

        registrar.playToClient(
            FlattenSyncPacket.TYPE,
            FlattenSyncPacket.STREAM_CODEC,
            FlattenSyncPacket::handleClient
        );

        registrar.playToServer(
            ReformRequestPacket.TYPE,
            ReformRequestPacket.STREAM_CODEC,
            ReformRequestPacket::handleServer
        );
    }
}
```

## Packets

### FlattenSyncPacket (Server → Client)

Synchronizes full flatten state to client.

```java
public record FlattenSyncPacket(
    int playerId,
    boolean isFlattened,
    float spreadMultiplier,
    int fallbackTicksRemaining,
    @Nullable FrozenPose frozenPose,
    int reformTicksRemaining
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FlattenSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "flatten_sync")
        );

    public static final StreamCodec<FriendlyByteBuf, FlattenSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, FlattenSyncPacket::playerId,
            ByteBufCodecs.BOOL, FlattenSyncPacket::isFlattened,
            ByteBufCodecs.FLOAT, FlattenSyncPacket::spreadMultiplier,
            ByteBufCodecs.INT, FlattenSyncPacket::fallbackTicksRemaining,
            // frozenPose optional encoding...
            ByteBufCodecs.INT, FlattenSyncPacket::reformTicksRemaining,
            FlattenSyncPacket::new
        );

    public static void handleClient(FlattenSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.level.getEntity(packet.playerId);
            if (player != null) {
                // Update client-side state cache
                ClientStateCache.update(player, packet);
            }
        });
    }
}
```

### ReformRequestPacket (Client → Server)

Requests reformation from server.

```java
public record ReformRequestPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReformRequestPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "reform_request")
        );

    public static final StreamCodec<FriendlyByteBuf, ReformRequestPacket> STREAM_CODEC =
        StreamCodec.unit(new ReformRequestPacket());

    public static void handleServer(ReformRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            FlattenStateManager.INSTANCE.initiateReform(player);
        });
    }
}
```

## Sync Strategy

- **Server-authoritative**: All state changes originate server-side
- **Broadcast on change**: Sent to all tracking clients when state changes
- **Initial sync**: Sent on player login and dimension change
- **No client prediction**: Client waits for server confirmation

## Sending Packets

```java
// Sync to all tracking clients
public static void syncToTracking(ServerPlayer target, FlattenState state) {
    FlattenSyncPacket packet = new FlattenSyncPacket(
        target.getId(),
        state.isFlattened(),
        state.spreadMultiplier(),
        state.fallbackTicksRemaining(),
        state.frozenPose(),
        state.reformTicksRemaining()
    );

    PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, packet);
}

// Client sends reform request
public static void sendReformRequest() {
    PacketDistributor.sendToServer(new ReformRequestPacket());
}
```

## Requirements Traced

- **SI-3**: NeoForge networking
- **CI-1.1**: Sync state to tracking clients
- **CI-1.2**: Reform requests from client
- **CI-1.3**: Sync spread multiplier (float)
- **FR-STATE.4**: Sync to all tracking clients
