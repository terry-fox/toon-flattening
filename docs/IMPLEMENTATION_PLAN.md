# Toon Flattening Mod - Multi-Phase Implementation Plan

## Overview

NeoForge 1.21.1 mod: When an anvil lands on a player, they are flattened to 1/20th their original height, become completely immobilized, and remain flattened until they initiate a reform action by pressing the jump/space key. Damage taken upon flattening is configurable via server configuration files.

**Architecture**: Data Attachments + Event Handlers + Network Sync + Pehkui Integration

**Key Dependency**: Pehkui (required) - handles entity scaling, hitbox adjustments, camera positioning

---

## Phase 1: Project Setup & Foundational Infrastructure

### Objectives
- Update project to NeoForge 21.1.214
- Establish Pehkui dependency
- Create configuration system
- Create data attachment system
- Initialize central registration points

### Tasks

#### 1.1 Update Gradle Configuration
**File**: `gradle.properties`
- Change `neo_version=21.0.167` → `21.1.214`

**File**: `build.gradle`
- Add Pehkui Maven repository:
  ```gradle
  repositories {
      maven {
          name = "Virtuoel"
          url = "https://maven.virtuoel.net/"
      }
  }
  ```
- Add Pehkui as implementation dependency:
  ```gradle
  dependencies {
      implementation "com.github.Virtuoel:Pehkui:${pehkui_version}"
  }
  ```
- Add property to gradle.properties: `pehkui_version=1.3.6` (or latest compatible)

#### 1.2 Update Mod Metadata
**File**: `src/main/templates/META-INF/neoforge.mods.toml`
- Add Pehkui as required dependency in `[[dependencies.toonflattening]]` section:
  ```toml
  [[dependencies.toonflattening]]
  modId = "pehkui"
  mandatory = true
  versionRange = "[1.3,)"
  ordering = "BEFORE"
  side = "BOTH"
  ```

#### 1.3 Create Configuration System
**File**: `src/main/java/com/terryfox/toonflattening/config/ToonFlatteningConfig.java`

**Requirements**:
- SERVER type config (per-world overrides)
- Float value: `flattenDamage` (0.0-20.0, default 4.0)
- Float value: `heightScale` (0.01-1.0, default 0.05)

**Key Points**:
- Use ModConfigSpec.Builder pattern
- Provide translation keys for config GUI
- Config location: `saves/<world>/serverconfig/toonflattening-server.toml`

#### 1.4 Create Data Attachment
**File**: `src/main/java/com/terryfox/toonflattening/attachment/FlattenedStateAttachment.java`

**Requirements**:
- Record class with fields: `boolean isFlattened`, `long flattenTime`
- Implement `ValueIOSerializable` interface
- Create Codec for serialization
- Static `DEFAULT` instance: `new FlattenedState(false, 0L)`

**Key Points**:
- flattenTime stores game tick when flattening occurred (for animations/debugging)
- Must be serializable for world save/load

#### 1.5 Register Core Systems in Main Class
**File**: `src/main/java/com/terryfox/toonflattening/ToonFlattening.java`

**Add to class**:
- DeferredRegister for AttachmentType: `ATTACHMENT_TYPES`
  - Register FLATTENED_STATE attachment with `.copyOnDeath()`
- DeferredRegister for SoundEvent: `SOUND_EVENTS`
  - Register FLATTEN custom sound event
- Constructor: register config with modContainer
- Constructor: register all DeferredRegisters to mod event bus

**Registration Pattern**:
```java
public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
    DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

public static final Supplier<AttachmentType<FlattenedState>> FLATTENED_STATE =
    ATTACHMENT_TYPES.register("flattened_state", () =>
        AttachmentType.builder(() => FlattenedState.DEFAULT)
            .serialize(FlattenedState.CODEC)
            .copyOnDeath()
            .build()
    );
```

### Deliverables
- ✓ Project builds with NeoForge 21.1.214
- ✓ Pehkui dependency resolved and compiles
- ✓ Configuration loads without errors (can verify in test world)
- ✓ Data attachment system registered
- ✓ All future modules have registration points ready

### Testing
- `./gradlew build` succeeds
- Run client: verify no startup errors
- Create world: check `/config` directory for `toonflattening-server.toml`
- Verify config keys present with correct defaults

---

## Phase 2: Core Flattening Mechanics

### Objectives
- Implement anvil detection
- Create extensible flattening system
- Integrate with Pehkui for entity scaling
- Establish sound effects infrastructure

### Tasks

#### 2.1 Create Pehkui Integration Layer
**File**: `src/main/java/com/terryfox/toonflattening/integration/PehkuiIntegration.java`

**Requirements**:
- Static method: `setPlayerScale(Player player, float heightScale, float widthScale)`
- Static method: `resetPlayerScale(Player player)`
- No fallback or soft dependency handling (required dependency)

**Implementation**:
```java
public static void setPlayerScale(Player player, float heightScale, float widthScale) {
    ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(heightScale);
    ScaleTypes.WIDTH.getScaleData(player).setTargetScale(widthScale);
}

public static void resetPlayerScale(Player player) {
    setPlayerScale(player, 1.0f, 1.0f);
}
```

**Key Points**:
- Methods designed for future expansion (width scaling configurable)
- Currently width always 1.0f (height-only flatten)
- Direct API calls, no error handling needed

#### 2.2 Create Extensible Flattening Handler
**File**: `src/main/java/com/terryfox/toonflattening/event/FlatteningHandler.java`

**Requirements**:
- Static method: `flattenPlayer(Player player, float damage, FlattenCause cause)`
- Event listener for LivingHurtEvent (anvil detection)
- Support for future causes via enum

**Enum**: `FlattenCause` (ANVIL, DRIPSTONE_future, etc.)

**flattenPlayer Logic**:
1. Check if already flattened (prevent double-flattening)
2. Set attachment: `player.setData(FLATTENED_STATE, new FlattenedState(true, gameTime))`
3. Apply Pehkui scaling: `PehkuiIntegration.setPlayerScale(player, heightScale, 1.0f)`
4. Apply damage: `player.hurt(DamageSource.GENERIC, damage)`
5. Play sound: `level.playSound(..., SoundEvents.FLATTEN, ...)`
6. Send sync packet: `NetworkHandler.syncFlattenState(player, true)`
7. Trigger animation packet: `NetworkHandler.sendSquashAnimation(player)`

**Anvil Detection Handler**:
- Event: LivingHurtEvent (priority: HIGH)
- Check `event.getSource().is(DamageTypes.FALLING_BLOCK)`
- Check entity: `source.getDirectEntity() instanceof FallingBlockEntity`
- Check block: `blockState.is(Blocks.ANVIL | CHIPPED_ANVIL | DAMAGED_ANVIL)`
- Call `flattenPlayer(player, config.flattenDamage, FlattenCause.ANVIL)`
- Override damage: `event.setAmount(config.flattenDamage)`

**Key Points**:
- Double-flattening check: `if (state.isFlattened()) return;`
- Always override damage to configured value (prevents other mods interfering)
- EventPriority.HIGH ensures processing before most mods
- Architecture allows adding dripstone/other triggers in future

#### 2.3 Create Sound Event Registration
**File**: `assets/toonflattening/sounds.json` (new)

```json
{
  "flatten": {
    "sounds": ["toonflattening:flatten"],
    "subtitle": "subtitles.toonflattening.flatten"
  }
}
```

**Add Sound File**: `assets/toonflattening/sounds/flatten.ogg`
- Placeholder audio initially (user to replace with custom sound)
- Suggested: short impact/squash sound effect

**Register in ToonFlattening.java**:
```java
public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
    DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

public static final Supplier<SoundEvent> FLATTEN =
    SOUND_EVENTS.register("flatten", () =>
        SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(MODID, "flatten")
        )
    );
```

### Deliverables
- ✓ Pehkui integration layer working (no compilation errors)
- ✓ Anvil landing triggers flattening
- ✓ Player scales to 1/20 height when flattened
- ✓ Custom sound plays on flatten
- ✓ Extensible architecture ready for future triggers

### Testing
- Single-player: Drop anvil on player → player should scale down (verify visually)
- Check console: no errors from Pehkui calls
- Listen: custom flatten sound should play
- Verify: player still moves (immobilization in Phase 3)

---

## Phase 3: Player Immobilization & Movement Control

### Objectives
- Prevent player movement while flattened
- Prevent jumping while flattened
- Block input on client-side for smooth UX
- Ensure spectator mode immunity

### Tasks

#### 3.1 Server-Side Movement Prevention
**File**: `src/main/java/com/terryfox/toonflattening/event/PlayerMovementHandler.java`

**Event Listener**: LivingEvent.LivingTickEvent (server-side)
- Check if player is flattened via attachment
- If flattened:
  - `player.setDeltaMovement(Vec3.ZERO)` - cancel all velocity
  - `player.setOnGround(true)` - ensure grounded state
  - Cancel sprinting: `if (player.isSprinting()) player.setSprinting(false)`
  - Cancel swimming: `if (player.isSwimming()) player.setSwimming(false)`
  - Skip if spectator mode: `if (player.isSpectator()) return;`

**Key Points**:
- Runs every tick to reset any movement state changes
- Spectators immune (check before flattening too)
- Prevents exploits (flying, swimming, etc.)

#### 3.2 Client-Side Input Blocking
**File**: `src/main/java/com/terryfox/toonflattening/client/ClientEventHandler.java`

**Event Listener**: ClientTickEvent.Post (CLIENT dist only)
- Get local player from Minecraft instance
- Check if flattened via attachment
- If flattened, clear all movement input:
  - `player.input.leftImpulse = 0`
  - `player.input.forwardImpulse = 0`
  - `player.input.up = false` (W)
  - `player.input.down = false` (S)
  - `player.input.left = false` (A)
  - `player.input.right = false` (D)
  - `player.input.jumping = false`

**Key Points**:
- Provides immediate feedback (no input lag)
- Server still validates (belt and suspenders)
- Prevents visual desync

#### 3.3 Spectator Mode Check
**Add to FlatteningHandler.flattenPlayer()**:
- Before flattening, check: `if (player.isSpectator()) return;`
- Spectators should not be flattenable

### Deliverables
- ✓ Flattened player cannot move (WASD blocked)
- ✓ Flattened player cannot jump
- ✓ No movement input displayed to client
- ✓ Spectator mode immune
- ✓ Server validates immobility

### Testing
- Single-player: Flatten → try WASD → no movement
- Single-player: Flatten → try spacebar → no jump
- Multiplayer: Observer mode → not affected by anvil
- Verify: player visually stationary while flattened

---

## Phase 4: Reform Mechanics & Network Synchronization

### Objectives
- Create keybinding for reform action
- Implement network packets for state sync
- Handle reform request on server
- Sync animation triggers across clients

### Tasks

#### 4.1 Create Custom Keybinding
**File**: `src/main/java/com/terryfox/toonflattening/client/KeyBindings.java`

**Requirements**:
- Static KeyMapping: `reformKey`
- Bind to GLFW.GLFW_KEY_SPACE (jump key)
- Category: "key.categories.toonflattening"

**Registration**:
- Use @EventBusSubscriber(modid=MODID, bus=MOD, value=CLIENT)
- RegisterKeyMappingsEvent handler calls registration

**Code Pattern**:
```java
public static KeyMapping reformKey;

public static void register(RegisterKeyMappingsEvent event) {
    reformKey = new KeyMapping(
        "key.toonflattening.reform",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_SPACE,
        "key.categories.toonflattening"
    );
    event.register(reformKey);
}
```

**Key Points**:
- Stored in static field for access from ClientEventHandler
- Translation key must be added to localization file
- Space key default (can be rebound in controls)

#### 4.2 Create Network Payloads
**File**: `src/main/java/com/terryfox/toonflattening/network/payload/RequestReformPayload.java`

- Type: CustomPacketPayload
- No data fields (just a signal)
- StreamCodec.unit() pattern

**File**: `src/main/java/com/terryfox/toonflattening/network/payload/SyncFlattenStatePayload.java`

- Type: CustomPacketPayload
- Fields:
  - `int playerId` (VAR_INT)
  - `boolean isFlattened` (BOOL)
  - `long flattenTime` (VAR_LONG)
- StreamCodec composite pattern

**File**: `src/main/java/com/terryfox/toonflattening/network/payload/TriggerSquashAnimationPayload.java`

- Type: CustomPacketPayload
- Fields:
  - `int playerId` (VAR_INT)
- StreamCodec unit or simple pattern

#### 4.3 Create Network Handler
**File**: `src/main/java/com/terryfox/toonflattening/network/NetworkHandler.java`

**Registration Method**: `register(RegisterPayloadHandlersEvent event)`
- Called from ToonFlattening.java mod event bus listener
- Register all three payload types:
  - playToServer(RequestReformPayload, ...)
  - playToClient(SyncFlattenStatePayload, ...)
  - playToClient(TriggerSquashAnimationPayload, ...)

**Handler: RequestReformPayload**
- Check if player is flattened
- If yes:
  - Reset attachment: `player.setData(FLATTENED_STATE, FlattenedState.DEFAULT)`
  - Reset Pehkui: `PehkuiIntegration.resetPlayerScale(player)`
  - Sync to clients: `syncFlattenState(player, false)`
  - Play reform sound: `player.level().playSound(..., SoundEvents.UI_BUTTON_CLICK, ...)`

**Handler: SyncFlattenStatePayload** (client-side)
- Get entity by playerId from client world
- Update attachment: `player.setData(FLATTENED_STATE, new FlattenedState(...))`
- If isFlattened: apply scale
- If not flattened: reset scale

**Handler: TriggerSquashAnimationPayload** (client-side)
- Get entity by playerId
- Call `SquashAnimationRenderer.playSquashEffect(player)`

**Static Method: syncFlattenState(Player player, boolean isFlattened)**
- Create SyncFlattenStatePayload
- Send via `PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, payload)`

**Static Method: sendSquashAnimation(Player player)**
- Create TriggerSquashAnimationPayload
- Send via `PacketDistributor.sendToPlayersTrackingEntityAndSelf(...)`

**Key Points**:
- All handlers use `context.enqueueWork()` for main thread execution
- Sync sent to all players tracking entity (multiplayer visibility)
- Uses NEW NeoForge networking API (not FML channels)

#### 4.4 Add Key Polling to ClientEventHandler
**File**: `src/main/java/com/terryfox/toonflattening/client/ClientEventHandler.java` (expanded)

**In ClientTickEvent.Post handler**:
```java
// Poll for reform key press (after input blocking section)
while (KeyBindings.reformKey.consumeClick()) {
    FlattenedState state = mc.player.getData(FLATTENED_STATE);
    if (state.isFlattened()) {
        PacketDistributor.sendToServer(new RequestReformPayload());
    }
}
```

**Key Points**:
- Must use `while(consumeClick())` pattern per NeoForge docs
- Only send if actually flattened (prevents spam)
- Client-side, so input is immediate feedback

### Deliverables
- ✓ Space key bound to reform action
- ✓ Pressing space while flattened sends server request
- ✓ Server processes reform and resets player state
- ✓ All clients see other players reform via sync packet
- ✓ Sound plays on reform
- ✓ Network communication stable in multiplayer

### Testing
- Single-player: Flatten → press spacebar → player returns to normal
- Single-player: Flatten → verify sound plays on reform
- Multiplayer: Player A flattens → Player B sees it → Player A reforms → Player B sees reform
- Rebind key: Verify new key works for reform
- Config: Verify damage value applied correctly

---

## Phase 5: Visual Effects & Polish

### Objectives
- Implement squash animation on flatten
- Create localization strings
- Rename asset namespace
- Polish visual feedback

### Tasks

#### 5.1 Create Squash Animation Renderer
**File**: `src/main/java/com/terryfox/toonflattening/client/SquashAnimationRenderer.java`

**Requirements**:
- Static method: `playSquashEffect(Player player)`
- Triggered by TriggerSquashAnimationPayload
- Creates visual/particle effect at player position

**Implementation Options**:
1. **Particle Burst** (Recommended for MVP):
   - Use vanilla particle types (CRIT, CRIT_EMITTERS, etc.)
   - Spawn 5-10 particles in burst pattern around player
   - Set position to player position

2. **Scale Pulse Animation**:
   - Briefly scale player to 0.9x then back
   - Requires tracking animation state per player

3. **Combination** (Polish):
   - Particles + brief scale pulse
   - More polished but more complex

**Initial Implementation (Particle Burst)**:
```java
public static void playSquashEffect(Player player) {
    Level level = player.level();
    Vec3 pos = player.getEyePosition();

    for (int i = 0; i < 8; i++) {
        double vx = (Math.random() - 0.5) * 2;
        double vy = (Math.random() - 0.5) * 0.5; // Less vertical spread
        double vz = (Math.random() - 0.5) * 2;

        level.addParticle(
            ParticleTypes.CRIT,
            pos.x, pos.y, pos.z,
            vx, vy, vz
        );
    }
}
```

**Call Location**: TriggerSquashAnimationPayload handler (client-side)

**Key Points**:
- ClientOnly annotation on file
- Only adds particles clientside (no network overhead)
- Called on all clients receiving sync packet
- Can be expanded to more complex animation in future

#### 5.2 Rename Asset Namespace
**Current**: `src/main/resources/assets/examplemod/`
**New**: `src/main/resources/assets/toonflattening/`

**Files to Move**:
- `assets/examplemod/lang/en_us.json` → `assets/toonflattening/lang/en_us.json`

**Files to Create**:
- `assets/toonflattening/sounds.json` (created in Phase 2, now move if not there)
- `assets/toonflattening/sounds/flatten.ogg` (placeholder initially)

**Update Mod Config** (if not done):
- Verify sounds.json has correct namespace references

#### 5.3 Localization File
**File**: `src/main/resources/assets/toonflattening/lang/en_us.json`

**Contents**:
```json
{
  "key.categories.toonflattening": "Toon Flattening",
  "key.toonflattening.reform": "Reform from Flattened",

  "config.toonflattening.flatten_damage": "Flatten Damage",
  "config.toonflattening.height_scale": "Flattened Height Scale",

  "subtitles.toonflattening.flatten": "Player flattens",
  "sound.toonflattening.flatten": "Flatten"
}
```

**Key Points**:
- Keys match usage in code
- Subtitles for accessibility
- Can expand with more languages in future

#### 5.4 Update Mod Description (Optional)
**File**: `src/main/templates/META-INF/neoforge.mods.toml`
- Update description field with brief feature description
- Ensure Pehkui dependency clearly documented

### Deliverables
- ✓ Visual effect plays when player flattens
- ✓ All localization strings present
- ✓ Asset namespace renamed to toonflattening
- ✓ Sound effect integrated with localization
- ✓ Mod description accurate

### Testing
- Single-player: Flatten → particles burst around player
- Multiplayer: Flatten → all clients see animation
- Verify: localization strings display correctly in menus
- Check: sound subtitle appears (if subtitles enabled)
- Config GUI: verify readable config descriptions

---

## Phase 6: Testing & Validation

### Objectives
- Comprehensive single-player testing
- Multiplayer testing and sync validation
- Edge case handling
- Performance verification

### Test Scenarios

#### Single-Player Tests
- [ ] Basic flatten: Drop anvil on player → flattens to 1/20 height
- [ ] Damage applied: Config damage amount matches health reduction
- [ ] Immobilization: Cannot move while flattened
- [ ] Jump blocked: Cannot jump while flattened
- [ ] Reform: Press space → player returns to normal height
- [ ] Sound plays: Flatten sound on flatten event
- [ ] Animation: Particle burst visible on flatten
- [ ] Multiple anvils: Drop second anvil on flattened player → works
- [ ] Death while flattened: Player respawns still flattened
- [ ] Config changes: Modify flattenDamage in .toml → takes effect on new flatten
- [ ] Height scale config: Verify 0.05 (1/20) calculation matches visual

#### Multiplayer Tests
- [ ] Initial sync: Join world while other player flattened → see flattened state
- [ ] Flatten sync: Flatten player → visible to other clients
- [ ] Reform sync: Reform → other clients see return to normal
- [ ] Animation visible: Other players see squash animation on flatten
- [ ] Sound heard: Other players hear flatten sound
- [ ] Multiple players: Both flatten independently → both work

#### Edge Cases
- [ ] Creative mode: Can still be flattened
- [ ] Spectator mode: Cannot be flattened
- [ ] Rapid anvils: Drop multiple anvils quickly → all work
- [ ] Different damage configs: Change config, re-flatten → correct damage
- [ ] Config per-world: Different worlds with different configs
- [ ] Other mods: Verify compatibility (test with common mods)

#### Performance
- [ ] No lag spikes on flatten
- [ ] Network packets reasonable size
- [ ] Animation smooth (no stutter)
- [ ] No memory leaks (play extended session)

### Issue Tracking
If issues found, note in format:
- **Issue**: [Description]
- **Reproduction**: [Steps to reproduce]
- **Expected**: [Correct behavior]
- **Actual**: [Current behavior]
- **Fix**: [Solution or phase to address in]

### Deliverables
- ✓ All test scenarios pass
- ✓ No critical bugs
- ✓ Multiplayer works correctly
- ✓ Performance acceptable
- ✓ Documentation updated with findings

### Success Criteria
- All "Basic" tests pass
- All "Multiple" multiplayer tests pass
- No game-breaking bugs
- No performance issues
- Mod description accurate

---

## Files Summary

### Phase 1 (Setup)
- gradle.properties (modify)
- build.gradle (modify)
- neoforge.mods.toml (modify)
- config/ToonFlatteningConfig.java (create)
- attachment/FlattenedStateAttachment.java (create)
- ToonFlattening.java (modify)

### Phase 2 (Core)
- integration/PehkuiIntegration.java (create)
- event/FlatteningHandler.java (create)
- assets/toonflattening/sounds.json (create)
- assets/toonflattening/sounds/flatten.ogg (create)
- ToonFlattening.java (modify - sound registration)

### Phase 3 (Immobilization)
- event/PlayerMovementHandler.java (create)
- client/ClientEventHandler.java (create - partial)

### Phase 4 (Reform)
- client/KeyBindings.java (create)
- network/payload/RequestReformPayload.java (create)
- network/payload/SyncFlattenStatePayload.java (create)
- network/payload/TriggerSquashAnimationPayload.java (create)
- network/NetworkHandler.java (create)
- client/ClientEventHandler.java (modify - add key polling)
- ToonFlattening.java (modify - register network)

### Phase 5 (Polish)
- client/SquashAnimationRenderer.java (create)
- assets/examplemod/ (move to assets/toonflattening/)
- assets/toonflattening/lang/en_us.json (create)

### Phase 6 (Testing)
- Various test saves/worlds

---

## Critical Dependencies & Notes

### Pehkui API
- `ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(scale)`
- `ScaleTypes.WIDTH.getScaleData(player).setTargetScale(scale)`
- Available from mod initialization onward

### NeoForge Concepts
- Data Attachments: API for per-entity custom data
- Events: Game and Mod buses, event priority
- Network Payloads: New streaming codec system (1.21.1+)
- Registries: DeferredRegister pattern for all registrable objects

### Minecraft Structure
- Damage sources: DamageTypes enum + DamageSource class
- Falling blocks: FallingBlockEntity type
- Players: extends LivingEntity, has level reference
- Sound: SoundEvent registry, playSound on Level

### Best Practices
- All network handlers use `context.enqueueWork()` for thread safety
- Event priorities set to HIGH to ensure processing
- Config values cached in local variables (not called repeatedly)
- Spectator mode checked early to prevent state changes
- Double-flattening prevented at entry point

---

## Implementation Notes

### Architecture Rationale
1. **Pehkui Required**: No Pehkui = broken scaling. Intentional crash signals missing dependency.
2. **Data Attachments**: Modern replacement for old capability system, cleaner API
3. **Extensible Flattening**: FlattenCause enum allows future triggers without modifying core handler
4. **Network Packets**: Essential for multiplayer visibility and animations
5. **High Priority Events**: Ensures mod actions before other damage mods interfere

### Future Expansion Points
1. **Width Scaling**: Change `widthScale` parameter from 1.0f to 0.5f for squashed width
2. **Additional Triggers**: Add dripstone handler calling `flattenPlayer()` with `FlattenCause.DRIPSTONE`
3. **Animation Variety**: Expand SquashAnimationRenderer with particle types, sounds
4. **Effects**: Add screen shake, particles, advanced animations
5. **Mechanics**: Helper reformation (right-click flattened player), cooldowns, duration timeouts

### Common Pitfalls to Avoid
- Forgetting `.copyOnDeath()` on attachment → state lost on death
- Forgetting `context.enqueueWork()` in packet handlers → threadcrash
- Checking `isFlattened` without null check → potential NPE
- Not overriding damage → other mods may change it
- Forgetting spectator check → broken spectator mode
- Not using EventPriority.HIGH → other mods process first

---

## Success Indicators
✓ Mod builds successfully
✓ Anvil flattens player to 1/20 height
✓ Player frozen while flattened
✓ Space key reforms player
✓ Damage configurable
✓ Works in multiplayer
✓ Sound effect plays
✓ Animation visible
✓ State persists through death
✓ Performance acceptable
