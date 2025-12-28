# Infrastructure Module Specification

## Responsibilities

- **Persistence**: Serialize/deserialize FlattenState to NBT via NeoForge attachments
- **Networking**: Synchronize state changes between server and clients
- **Configuration**: Hot-reloadable TOML-based settings with validation
- **Effects**: Spawn particles and play sounds for initial flatten transition (NOT re-flatten)
- **Tick Orchestration**: Per-player tick handler coordinating detection, core, reformation
- **Lifecycle Hooks**: Player join/leave, respawn, dimension change

## Data Ownership

### Exclusive Data
- NBT serialization format (attachment schema)
- Network packet formats
- Configuration file structure
- Effect rendering parameters

### Read-Only Dependencies
- FlattenState (from core)
- Configuration values (owned by this module)

## Communication

### Incoming (Afferent Coupling)
- **NeoForge Event Bus** → Player tick, join/leave, respawn events
- **core** → `EffectHandler.playFlattenEffects(ServerPlayer)` (called on initial FullyFlattened transition only, NOT on re-flatten)

### Outgoing (Efferent Coupling)
- **core** → `FlattenStateManager.tick(ServerPlayer)` (tick orchestration)
- **detection** → `AnvilContactDetector.tick(ServerPlayer)` (tick orchestration)
- **reformation** → `ReformationHandler.tick(ServerPlayer)` (tick orchestration)
- **core** → `FlattenStateManager.getState(Player)` (for serialization)
- **core** → `FlattenStateManager.restoreState(ServerPlayer, FlattenState)` (deserialization)

## Key Classes

### PlayerDataAttachment (Persistence)
```java
public class PlayerDataAttachment {
    // Codec for FlattenState serialization
    public static final Codec<FlattenState> FLATTEN_STATE_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("phase").forGetter(s -> s.phase().name()),
            Codec.FLOAT.fieldOf("heightScale").forGetter(FlattenState::heightScale),
            Codec.FLOAT.fieldOf("widthScale").forGetter(FlattenState::widthScale),
            Codec.FLOAT.fieldOf("depthScale").forGetter(FlattenState::depthScale),
            Codec.FLOAT.fieldOf("spreadMultiplier").forGetter(FlattenState::spreadMultiplier),
            Codec.FLOAT.fieldOf("originalHitboxHeight").forGetter(FlattenState::originalHitboxHeight),
            Codec.STRING.fieldOf("frozenPose").forGetter(s -> s.frozenPose().name()),
            Codec.INT.fieldOf("recoveryTicksRemaining").forGetter(FlattenState::recoveryTicksRemaining),
            Codec.INT.fieldOf("fallbackTicksRemaining").forGetter(FlattenState::fallbackTicksRemaining),
            Codec.INT.fieldOf("reflattenCooldownTicks").forGetter(FlattenState::reflattenCooldownTicks),
            Codec.INT.fieldOf("trackedAnvilCount").forGetter(FlattenState::trackedAnvilCount),
            Codec.BOOL.fieldOf("hasContactingAnvil").forGetter(FlattenState::hasContactingAnvil),
            UUIDUtil.CODEC.optionalFieldOf("anvilEntityUUID").forGetter(s -> Optional.ofNullable(s.anvilEntityUUID())),
            BlockPos.CODEC.optionalFieldOf("anvilBlockPos").forGetter(s -> Optional.ofNullable(s.anvilBlockPos()))
        ).apply(instance, PlayerDataAttachment::createState)
    );

    public static final Supplier<AttachmentType<FlattenState>> FLATTEN_STATE =
        ATTACHMENT_TYPES.register("flatten_state", () ->
            AttachmentType.builder(() -> FlattenState.normal())
                .serialize(FLATTEN_STATE_CODEC)
                .copyOnDeath()
                .build()
        );

    // Helper method for Codec reconstruction
    private static FlattenState createState(
        String phase, float heightScale, float widthScale, float depthScale,
        float spreadMultiplier, float originalHitboxHeight, String frozenPose,
        int recoveryTicksRemaining, int fallbackTicksRemaining, int reflattenCooldownTicks,
        int trackedAnvilCount, boolean hasContactingAnvil,
        Optional<UUID> anvilEntityUUID, Optional<BlockPos> anvilBlockPos
    ) {
        return new FlattenState(
            FlattenPhase.valueOf(phase), heightScale, widthScale, depthScale,
            spreadMultiplier, originalHitboxHeight, Pose.valueOf(frozenPose),
            recoveryTicksRemaining, fallbackTicksRemaining, reflattenCooldownTicks,
            trackedAnvilCount, hasContactingAnvil,
            anvilEntityUUID.orElse(null), anvilBlockPos.orElse(null)
        );
    }
}
```

### NetworkPackets
```java
public class NetworkPackets {
    // Packet: Server → Client (state sync)
    public record SyncStatePacket(
        UUID playerUUID,
        FlattenPhase phase,
        float heightScale,
        float widthScale,
        float depthScale,
        int recoveryTicksRemaining
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncStatePacket> TYPE =
            new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath("toonflattening", "sync_state")
            );

        public static final StreamCodec<FriendlyByteBuf, SyncStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, SyncStatePacket::playerUUID,
                ByteBufCodecs.fromCodec(FlattenPhase.CODEC), SyncStatePacket::phase,
                ByteBufCodecs.FLOAT, SyncStatePacket::heightScale,
                ByteBufCodecs.FLOAT, SyncStatePacket::widthScale,
                ByteBufCodecs.FLOAT, SyncStatePacket::depthScale,
                ByteBufCodecs.INT, SyncStatePacket::recoveryTicksRemaining,
                SyncStatePacket::new
            );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SyncStatePacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                // Update client-side state (for rendering)
                Player player = Minecraft.getInstance().level.getPlayerByUUID(packet.playerUUID);
                if (player != null) {
                    // Apply to client-side state manager
                    FlattenStateManager.updateClientState(player, packet);
                }
            });
        }
    }

    // Packet: Client → Server (reform request)
    public record ReformRequestPacket() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ReformRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath("toonflattening", "reform_request")
            );

        public static final StreamCodec<FriendlyByteBuf, ReformRequestPacket> STREAM_CODEC =
            StreamCodec.unit(new ReformRequestPacket()); // Empty packet

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ReformRequestPacket packet, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) ctx.player();
                ReformationHandler.onKeyPress(player);
            });
        }
    }

    // Registration (in mod constructor):
    // PayloadRegistrar registrar = event.registrar("toonflattening");
    // registrar.playToClient(SyncStatePacket.TYPE, SyncStatePacket.STREAM_CODEC, SyncStatePacket::handle);
    // registrar.playToServer(ReformRequestPacket.TYPE, ReformRequestPacket.STREAM_CODEC, ReformRequestPacket::handle);
}
```

### ConfigSpec (TOML)
```java
public class ConfigSpec {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue progressive_enabled;
    public static final ModConfigSpec.DoubleValue damage_amount;
    public static final ModConfigSpec.DoubleValue height_scale;
    public static final ModConfigSpec.DoubleValue spread_increment;
    public static final ModConfigSpec.DoubleValue max_spread_limit;
    public static final ModConfigSpec.IntValue reformation_ticks;
    public static final ModConfigSpec.BooleanValue anvil_blocking_enabled;
    public static final ModConfigSpec.IntValue fallback_timeout_seconds;
    public static final ModConfigSpec.IntValue reflatten_cooldown_ticks;
    public static final ModConfigSpec.DoubleValue stack_damage_per_anvil;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Progressive Flattening Settings").push("flattening");
        progressive_enabled = builder
            .comment("Enable progressive compression (true) or instant flatten (false)")
            .define("progressive_enabled", true);
        damage_amount = builder
            .comment("Damage in hearts when reaching FullyFlattened (0.0-20.0)")
            .defineInRange("damage_amount", 4.0, 0.0, 20.0);
        height_scale = builder
            .comment("Minimum height multiplier when fully flattened (0.01-1.0)")
            .defineInRange("height_scale", 0.05, 0.01, 1.0);
        builder.pop();

        builder.comment("Spread Stacking Settings").push("stacking");
        spread_increment = builder
            .comment("Width/depth added per spread (0.1-2.0)")
            .defineInRange("spread_increment", 0.8, 0.1, 2.0);
        max_spread_limit = builder
            .comment("Maximum total spread multiplier (1.0-6.0)")
            .defineInRange("max_spread_limit", 6.0, 1.0, 6.0);
        builder.pop();

        builder.comment("Reformation Settings").push("reformation");
        reformation_ticks = builder
            .comment("Animation duration in ticks (1-100, 20 ticks = 1 second)")
            .defineInRange("reformation_ticks", 5, 1, 100);
        anvil_blocking_enabled = builder
            .comment("Block reformation while anvil above player")
            .define("anvil_blocking_enabled", true);
        fallback_timeout_seconds = builder
            .comment("Seconds until anvil-blocking bypassed (0 = disabled, 1-3600)")
            .defineInRange("fallback_timeout_seconds", 300, 0, 3600);
        builder.pop();

        builder.comment("Re-Flatten Settings").push("reflatten");
        reflatten_cooldown_ticks = builder
            .comment("Cooldown between re-flatten events in ticks (1-100, 20 ticks = 1 second)")
            .defineInRange("reflatten_cooldown_ticks", 20, 1, 100);
        stack_damage_per_anvil = builder
            .comment("Additional damage per anvil beyond first in stack (0.0-10.0 hearts)")
            .defineInRange("stack_damage_per_anvil", 1.0, 0.0, 10.0);
        builder.pop();

        SPEC = builder.build();
    }
}
```

### EffectHandler
```java
public class EffectHandler {
    // Play effects when transitioning to FullyFlattened (ONLY on initial flatten, NOT on re-flatten)
    public static void playFlattenEffects(ServerPlayer player) {
        Level level = player.level();
        Vec3 pos = player.position();

        // Spawn 25 POOF particles with random cloud spread
        ((ServerLevel) level).sendParticles(
            ParticleTypes.POOF,
            pos.x, pos.y + 0.5, pos.z,
            25, // count
            0.5, 0.5, 0.5, // random cloud spread
            0.05 // speed
        );

        // Play custom sound file
        level.playSound(
            null,
            pos.x, pos.y, pos.z,
            SoundRegistry.FLATTEN_SOUND, // assets/toonflattening/sounds/flatten.ogg
            SoundSource.PLAYERS,
            1.0f, // volume
            1.0f  // pitch
        );
    }

    // No effects for reformation (animation only)
    public static void playReformEffects(ServerPlayer player) {
        // Intentionally empty - no visual/audio feedback for reform
    }

    // No effects for re-flatten (silent spread increase)
    public static void playReflattenEffects(ServerPlayer player) {
        // Intentionally empty - no visual/audio feedback for re-flatten
    }
}
```

### TickOrchestrator (Event Handler)
```java
public class TickOrchestrator {
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Orchestrate per-tick processing (runs after player tick work)
        AnvilContactDetector.tick(serverPlayer);
        FlattenStateManager.tick(serverPlayer);
        ReformationHandler.tick(serverPlayer);
    }
}
```

### LifecycleHandler (Event Handler)
```java
public class LifecycleHandler {
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Restore state from attachment
            FlattenState state = serverPlayer.getData(PlayerDataAttachment.FLATTEN_STATE);
            FlattenStateManager.restoreState(serverPlayer, state);

            // Sync to client
            NetworkPackets.sendSyncState(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Save state to attachment (automatic via NeoForge)
            ScalingProviderRegistry.invalidateCache(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Reset to Normal phase on respawn
            FlattenStateManager.reset(serverPlayer);
        }
    }
}
```

## File Locations

- **Config**: `config/toonflattening-common.toml` (server-side, synced to clients)
- **Attachments**: Stored in player NBT data (world save files)

## Configuration Hot-Reload

```
File watcher detects TOML modification
        |
        v
ConfigSpec reloads values
        |
        v
Validation: Check ranges, log warnings for invalid values
        |
        v
Notify modules of config change (optional event)
```

## Coupling Metrics

- **Afferent Coupling (Ca)**: 1 (core - for effect triggering)
- **Efferent Coupling (Ce)**: 3 (core, detection, reformation)
- **Instability (I)**: 0.75 (unstable by design - orchestration layer)

## Performance Constraints

- **NBT Serialization**: ≤0.5ms per player on logout
- **Network Packet Size**: ≤64 bytes per state sync (avoid large payloads)
- **Config Reload**: ≤100ms for full TOML parse + validation
- **Tick Orchestration**: ≤0.01ms overhead (direct method calls)
