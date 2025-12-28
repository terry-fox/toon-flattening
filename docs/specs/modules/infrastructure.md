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
    public static final AttachmentType<FlattenState> FLATTEN_STATE =
        AttachmentType.<FlattenState>builder()
            .serialize(PlayerDataAttachment::write)
            .deserialize(PlayerDataAttachment::read)
            .build();

    // Serialize FlattenState to NBT
    private static void write(FlattenState state, CompoundTag tag) {
        tag.putString("phase", state.phase().name());
        tag.putFloat("heightScale", state.heightScale());
        tag.putFloat("widthScale", state.widthScale());
        tag.putFloat("depthScale", state.depthScale());
        tag.putFloat("spreadMultiplier", state.spreadMultiplier());
        tag.putFloat("originalHitboxHeight", state.originalHitboxHeight());
        tag.putString("frozenPose", state.frozenPose().name());
        tag.putInt("recoveryTicksRemaining", state.recoveryTicksRemaining());
        tag.putInt("fallbackTicksRemaining", state.fallbackTicksRemaining());
        tag.putInt("reflattenCooldownTicks", state.reflattenCooldownTicks());
        tag.putInt("trackedAnvilCount", state.trackedAnvilCount()); // NEW
        tag.putBoolean("hasContactingAnvil", state.hasContactingAnvil()); // NEW

        if (state.anvilEntityUUID() != null) {
            tag.putUUID("anvilEntityUUID", state.anvilEntityUUID());
        }
        if (state.anvilBlockPos() != null) {
            tag.putLong("anvilBlockPos", state.anvilBlockPos().asLong());
        }
    }

    // Deserialize FlattenState from NBT
    private static FlattenState read(CompoundTag tag) {
        FlattenPhase phase = FlattenPhase.valueOf(tag.getString("phase"));
        float heightScale = tag.getFloat("heightScale");
        float widthScale = tag.getFloat("widthScale");
        float depthScale = tag.getFloat("depthScale");
        float spreadMultiplier = tag.getFloat("spreadMultiplier");
        float originalHitboxHeight = tag.getFloat("originalHitboxHeight");
        Pose frozenPose = Pose.valueOf(tag.getString("frozenPose"));
        int recoveryTicksRemaining = tag.getInt("recoveryTicksRemaining");
        int fallbackTicksRemaining = tag.getInt("fallbackTicksRemaining");
        int reflattenCooldownTicks = tag.getInt("reflattenCooldownTicks");
        int trackedAnvilCount = tag.getInt("trackedAnvilCount"); // NEW
        boolean hasContactingAnvil = tag.getBoolean("hasContactingAnvil"); // NEW

        UUID anvilEntityUUID = tag.contains("anvilEntityUUID")
            ? tag.getUUID("anvilEntityUUID")
            : null;
        BlockPos anvilBlockPos = tag.contains("anvilBlockPos")
            ? BlockPos.of(tag.getLong("anvilBlockPos"))
            : null;

        return new FlattenState(phase, heightScale, widthScale, depthScale,
            spreadMultiplier, originalHitboxHeight, frozenPose,
            recoveryTicksRemaining, fallbackTicksRemaining,
            reflattenCooldownTicks, trackedAnvilCount, hasContactingAnvil,
            anvilEntityUUID, anvilBlockPos);
    }
}
```

### NetworkPackets
```java
public class NetworkPackets {
    public static final ResourceLocation SYNC_STATE_ID =
        new ResourceLocation("toonflattening", "sync_state");

    // Packet: Server → Client (state sync)
    public record SyncStatePacket(
        UUID playerUUID,
        FlattenPhase phase,
        float heightScale,
        float widthScale,
        float depthScale,
        int recoveryTicksRemaining
    ) implements CustomPacketPayload {
        public static void encode(SyncStatePacket packet, FriendlyByteBuf buf) {
            buf.writeUUID(packet.playerUUID);
            buf.writeEnum(packet.phase);
            buf.writeFloat(packet.heightScale);
            buf.writeFloat(packet.widthScale);
            buf.writeFloat(packet.depthScale);
            buf.writeInt(packet.recoveryTicksRemaining);
        }

        public static SyncStatePacket decode(FriendlyByteBuf buf) {
            return new SyncStatePacket(
                buf.readUUID(),
                buf.readEnum(FlattenPhase.class),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt()
            );
        }

        public static void handle(SyncStatePacket packet, PlayPayloadContext ctx) {
            ctx.workHandler().execute(() -> {
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
        public static void handle(ReformRequestPacket packet, PlayPayloadContext ctx) {
            ctx.workHandler().execute(() -> {
                ServerPlayer player = (ServerPlayer) ctx.player().orElse(null);
                if (player != null) {
                    ReformationHandler.onKeyPress(player);
                }
            });
        }
    }
}
```

### ConfigSpec (TOML)
```java
public class ConfigSpec {
    public static final ForgeConfigSpec SPEC;

    public static final BooleanValue progressive_enabled;
    public static final DoubleValue damage_amount;
    public static final DoubleValue height_scale;
    public static final DoubleValue spread_increment;
    public static final DoubleValue max_spread_limit;
    public static final IntValue reformation_ticks;
    public static final BooleanValue anvil_blocking_enabled;
    public static final IntValue fallback_timeout_seconds;
    public static final IntValue reflatten_cooldown_ticks;
    public static final DoubleValue stack_damage_per_anvil; // NEW: Replaces reflatten_damage_threshold

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Orchestrate per-tick processing
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

## Testing Strategy

### Unit Tests
- **Serialization**:
  - `testFlattenStateSerializesToNBT()` - All fields written correctly
  - `testFlattenStateDeserializesFromNBT()` - All fields read correctly
  - `testAnvilReferenceSerialization()` - Entity UUID and BlockPos handled

- **Configuration**:
  - `testConfigDefaultValues()` - All defaults match SRS
  - `testConfigRangeValidation()` - Out-of-range values clamped
  - `testConfigHotReload()` - File modification triggers reload

- **Network Packets**:
  - `testSyncStatePacketEncoding()` - FlattenState → bytes
  - `testSyncStatePacketDecoding()` - Bytes → FlattenState
  - `testReformRequestPacket()` - Client → server request

### Integration Tests
- **Persistence**:
  - `testStatePersistedAcrossLogout()` - Logout → login restores FullyFlattened
  - `testStateResetOnRespawn()` - Death → respawn resets to Normal

- **Networking**:
  - `testStateChangeSyncsToClient()` - Server transition → client receives packet
  - `testReformKeybindSendsPacket()` - Client keybind → server receives request

- **Cross-Module**:
  - `testTickOrchestratorCallsModules()` - Tick event → detection, core, reformation ticked
  - `testEffectsPlayedOnTransition()` - FullyFlattened transition → particles/sound

## Coupling Metrics

- **Afferent Coupling (Ca)**: 1 (core - for effect triggering)
- **Efferent Coupling (Ce)**: 3 (core, detection, reformation)
- **Instability (I)**: 0.75 (unstable by design - orchestration layer)

## Performance Constraints

- **NBT Serialization**: ≤0.5ms per player on logout
- **Network Packet Size**: ≤64 bytes per state sync (avoid large payloads)
- **Config Reload**: ≤100ms for full TOML parse + validation
- **Tick Orchestration**: ≤0.01ms overhead (direct method calls)
