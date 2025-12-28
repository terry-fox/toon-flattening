# TODO - Toon Flattening Implementation Plan

Sequential implementation tasks with acceptance criteria.

---

## Phase 1: Project Foundation

- [x] Add Pehkui 3.8.3 dependency to build.gradle (CurseMaven/JitPack)
- [x] Add Pehkui dependency declaration to mods.toml
- [x] Create package: `com.terryfox.toonflattening.core`
- [x] Create package: `com.terryfox.toonflattening.detection`
- [x] Create package: `com.terryfox.toonflattening.restriction`
- [x] Create package: `com.terryfox.toonflattening.reformation`
- [x] Create package: `com.terryfox.toonflattening.integration`
- [x] Create package: `com.terryfox.toonflattening.api`
- [x] Create package: `com.terryfox.toonflattening.infrastructure`
- [x] `./gradlew build` succeeds
- [x] `./gradlew runClient` launches with Pehkui loaded

---

## Phase 2: Core Module

- [ ] Create `core/FlattenPhase.java` enum (NORMAL, PROGRESSIVE_FLATTENING, FULLY_FLATTENED, RECOVERING)
- [ ] Add Codec to FlattenPhase for serialization
- [ ] Create `core/FlattenState.java` record with all fields per spec
- [ ] Implement `FlattenState.normal()` factory method
- [ ] Implement `FlattenState.withPhase()` copy method
- [ ] Implement `FlattenState.withScales()` copy method
- [ ] Create `core/ScaleCalculator.java` utility class
- [ ] Implement `ScaleCalculator.calculateHeightScale()`
- [ ] Implement `ScaleCalculator.calculateWidthScale()` (inverse relationship)
- [ ] Implement `ScaleCalculator.interpolateRecovery()`
- [ ] Implement `ScaleCalculator.applySpread()`
- [ ] Implement `ScaleCalculator.calculateSpread()`
- [ ] Implement `ScaleCalculator.calculateStackDamage()`
- [ ] Create `core/FlattenStateManager.java` singleton
- [ ] Implement state storage Map<UUID, FlattenState>
- [ ] Implement `beginCompression()`
- [ ] Implement `updateCompression()`
- [ ] Implement `lostContact()`
- [ ] Implement `applyReflatten()`
- [ ] Implement `beginReformation()`
- [ ] Implement `tick()`
- [ ] Implement `getState()` and `getPhase()` queries
- [ ] Implement `setSpreadMultiplier()`
- [ ] Implement `restoreState()`
- [ ] Implement `reset()`
- [ ] FlattenPhase has 4 values
- [ ] FlattenState.normal() returns correct defaults
- [ ] ScaleCalculator tests pass (height/width/interpolation)
- [ ] FlattenStateManager state machine transitions correctly
- [ ] `./gradlew build` succeeds

---

## Phase 3: Infrastructure (Config + Persistence)

- [ ] Create `infrastructure/ConfigSpec.java`
- [ ] Add all config values per spec with correct defaults
- [ ] Register ConfigSpec in mod constructor
- [ ] Create `infrastructure/PlayerDataAttachment.java`
- [ ] Implement FLATTEN_STATE_CODEC with RecordCodecBuilder
- [ ] Register AttachmentType with DeferredRegister
- [ ] Configure copyOnDeath() for respawn persistence
- [ ] Create `infrastructure/SoundRegistry.java`
- [ ] Register FLATTEN_SOUND SoundEvent
- [ ] Create `resources/assets/toonflattening/sounds.json`
- [ ] Config file generates on first run
- [ ] Config values load correctly
- [ ] PlayerDataAttachment serializes/deserializes correctly
- [ ] sounds.json is valid and references flatten.ogg
- [ ] `./gradlew build` succeeds

---

## Phase 4: Integration Module

- [ ] Create `integration/IScalingProvider.java` interface
- [ ] Create `integration/PehkuiScalingProvider.java`
- [ ] Implement canHandle() with Pehkui check
- [ ] Implement setScales() using ScaleTypes API
- [ ] Create `integration/NoOpScalingProvider.java` fallback
- [ ] Implement error logging (once per session)
- [ ] Create `integration/ScalingProviderRegistry.java`
- [ ] Implement priority-sorted provider list
- [ ] Implement per-player provider cache
- [ ] Implement `registerProvider()`
- [ ] Implement `getProvider()`
- [ ] Implement `invalidateCache()`
- [ ] Create `integration/ScalingIntegration.java`
- [ ] Register PehkuiScalingProvider (priority 100)
- [ ] Register NoOpScalingProvider (priority MIN_VALUE)
- [ ] Provider selection works correctly
- [ ] Cache invalidates on disconnect
- [ ] In-game: Player scales visually with Pehkui
- [ ] `./gradlew build` succeeds

---

## Phase 5: Detection Module

- [ ] Create `detection/IFlattenTrigger.java` interface
- [ ] Add default methods for damage/position/count
- [ ] Create `detection/TriggerRegistry.java`
- [ ] Implement priority-sorted trigger list
- [ ] Implement `registerTrigger()`
- [ ] Implement `getActiveTrigger()`
- [ ] Create `detection/AnvilContactDetector.java`
- [ ] Implement `tick()` main entry point
- [ ] Implement `detectFallingAnvil()` - entity scan
- [ ] Implement `detectPlacedAnvil()` - block scan
- [ ] Implement `countAnvilStack()` - max 5 blocks
- [ ] Implement `calculateFloorY()` - downward raycast
- [ ] Implement `handleAnvilContact()`
- [ ] Implement `handleNoContact()`
- [ ] Create `detection/AnvilDamageCanceller.java`
- [ ] Implement LivingIncomingDamageEvent handler (priority NORMAL)
- [ ] Register on FORGE bus
- [ ] Falling anvils detected correctly
- [ ] Placed anvils detected correctly
- [ ] Anvil stacks counted correctly (max 5)
- [ ] Floor raycast works correctly
- [ ] Vanilla anvil damage cancelled during PROGRESSIVE_FLATTENING
- [ ] In-game: Anvil drop triggers beginCompression
- [ ] `./gradlew build` succeeds

---

## Phase 6: Reformation Module

- [ ] Create `reformation/ClearanceCalculator.java`
- [ ] Implement `hasAnvilAbove()` - AABB scan
- [ ] Implement `hasSufficientClearance()` - ceiling raycast
- [ ] Implement `getPoseHeight()` - pose lookup table
- [ ] Create `reformation/FallbackTimer.java`
- [ ] Implement `initializeTimer()` - config to ticks
- [ ] Implement `resetTimer()`
- [ ] Implement `tick()` decrement
- [ ] Create `reformation/ReformationHandler.java`
- [ ] Implement `onKeyPress()`
- [ ] Implement `canReform()` validation
- [ ] Implement `tick()` timer management
- [ ] Clearance checks work correctly
- [ ] Fallback timer initializes/decrements correctly
- [ ] canReform() validates all conditions
- [ ] In-game: SPACE starts recovery when clear
- [ ] In-game: SPACE blocked when anvil above
- [ ] `./gradlew build` succeeds

---

## Phase 7: Restriction Module

- [ ] Create `restriction/PoseController.java`
- [ ] Implement `capturePose()`
- [ ] Implement `applyFrozenPose()`
- [ ] Implement `releasePose()`
- [ ] Create `restriction/MovementRestriction.java`
- [ ] Implement EntityTickEvent.Post handler (priority LOWEST)
- [ ] Set velocity to zero for FULLY_FLATTENED
- [ ] Disable creative flying for FULLY_FLATTENED
- [ ] Create `restriction/InteractionRestriction.java`
- [ ] Cancel all PlayerInteractEvent variants for FULLY_FLATTENED
- [ ] Create `restriction/mixin/EntityPushMixin.java`
- [ ] Inject into Entity.push() to cancel for FULLY_FLATTENED
- [ ] Create `restriction/mixin/ShadowRendererMixin.java` (client)
- [ ] Cancel shadow rendering for FULLY_FLATTENED
- [ ] Create `resources/toonflattening.mixins.json`
- [ ] Movement blocked for FULLY_FLATTENED only
- [ ] Movement allowed for PROGRESSIVE_FLATTENING/RECOVERING
- [ ] Interactions blocked for FULLY_FLATTENED
- [ ] Push cancelled for FULLY_FLATTENED
- [ ] Shadow hidden for FULLY_FLATTENED
- [ ] In-game: Flattened player cannot move/interact
- [ ] In-game: Player CAN move during compression
- [ ] `./gradlew build` succeeds

---

## Phase 8: API Module

- [ ] Create `api/Scales.java` record
- [ ] Implement `Scales.normal()` factory
- [ ] Create `api/event/PreFlattenEvent.java` (@Cancelable)
- [ ] Create `api/event/PostFlattenEvent.java` (with anvilCount)
- [ ] Create `api/event/PreReformEvent.java` (@Cancelable)
- [ ] Create `api/event/PostReformEvent.java`
- [ ] Create `api/ToonFlatteningAPI.java` singleton
- [ ] Implement `getInstance()`
- [ ] Implement query methods (getPhase, isFlattened, etc.)
- [ ] Implement command methods (flatten, reform, setSpreadMultiplier)
- [ ] Implement extension methods (registerFlattenTrigger, registerScalingProvider)
- [ ] Implement `validateServerThread()` helper
- [ ] API methods throw on non-server thread
- [ ] Event cancellation prevents transitions
- [ ] API queries return correct values
- [ ] `./gradlew build` succeeds

---

## Phase 9: Infrastructure (Networking + Effects + Tick)

- [ ] Create `infrastructure/NetworkPackets.java`
- [ ] Implement SyncStatePacket record with StreamCodec
- [ ] Implement ReformRequestPacket record with StreamCodec
- [ ] Implement packet handle methods
- [ ] Create `infrastructure/EffectHandler.java`
- [ ] Implement `playFlattenEffects()` - particles + sound
- [ ] Implement `playReformEffects()` - empty
- [ ] Implement `playReflattenEffects()` - empty
- [ ] Create `infrastructure/TickOrchestrator.java`
- [ ] Implement PlayerTickEvent.Post handler
- [ ] Call all tick methods (detector, state manager, reformation)
- [ ] Create `infrastructure/LifecycleHandler.java`
- [ ] Implement PlayerLoggedInEvent - restore + sync
- [ ] Implement PlayerLoggedOutEvent - invalidate cache
- [ ] Implement PlayerRespawnEvent - reset state
- [ ] Packets serialize/deserialize correctly
- [ ] Effects spawn at correct times (initial only)
- [ ] Tick orchestrator calls all modules
- [ ] Lifecycle events handled correctly
- [ ] In-game: Particles/sound on initial flatten
- [ ] In-game: NO particles/sound on re-flatten
- [ ] `./gradlew build` succeeds

---

## Phase 10: Client-Side Infrastructure

- [ ] Create `infrastructure/client/KeybindHandler.java`
- [ ] Register SPACE key mapping
- [ ] Send ReformRequestPacket on press (FULLY_FLATTENED only)
- [ ] Create `infrastructure/client/ClientStateManager.java`
- [ ] Implement state storage per player UUID
- [ ] Implement `updateClientState()`
- [ ] Keybind sends packet only when FULLY_FLATTENED
- [ ] Client state updates on packet receive
- [ ] In-game: SPACE sends packet (check logs)
- [ ] In-game: Multiplayer sync works
- [ ] `./gradlew build` succeeds

---

## Phase 11: Mod Initialization Wiring

- [ ] Update `ToonFlattening.java` constructor
- [ ] Register ConfigSpec
- [ ] Register PlayerDataAttachment DeferredRegister
- [ ] Register SoundRegistry DeferredRegister
- [ ] Call ScalingIntegration.initialize()
- [ ] Register MovementRestriction on FORGE bus
- [ ] Register InteractionRestriction on FORGE bus
- [ ] Register AnvilDamageCanceller on FORGE bus
- [ ] Register TickOrchestrator on FORGE bus
- [ ] Register LifecycleHandler on FORGE bus
- [ ] Register network packets (both directions)
- [ ] Update `resources/toonflattening.mixins.json`
- [ ] List EntityPushMixin
- [ ] List ShadowRendererMixin (client)
- [ ] Update `META-INF/mods.toml`
- [ ] Add Pehkui dependency (required)
- [ ] Add NeoForge 21.1.214+ requirement
- [ ] All registrations complete
- [ ] Mixin config valid JSON
- [ ] `./gradlew build` succeeds
- [ ] `./gradlew runClient` launches without errors
- [ ] Log shows mod initialization
- [ ] Log shows provider registration

---

## Phase 12: Wire Core State Transitions

- [ ] Implement NORMAL → PROGRESSIVE_FLATTENING transition
- [ ] Implement PROGRESSIVE_FLATTENING → FULLY_FLATTENED transition (height ≤ 0.05)
- [ ] Implement FULLY_FLATTENED → RECOVERING transition
- [ ] Implement RECOVERING → NORMAL transition (recoveryTicks = 0)
- [ ] Implement RECOVERING → PROGRESSIVE_FLATTENING transition (re-flatten)
- [ ] Wire IScalingProvider.setScales() in tick() for non-NORMAL phases
- [ ] Post PreFlattenEvent before FULLY_FLATTENED
- [ ] Post PostFlattenEvent after FULLY_FLATTENED
- [ ] Post PreReformEvent before RECOVERING
- [ ] Post PostReformEvent after NORMAL
- [ ] Call EffectHandler.playFlattenEffects() on initial flatten only
- [ ] Track isInitialFlatten flag to prevent re-flatten effects
- [ ] All state transitions work correctly
- [ ] Events post at correct times
- [ ] Scales apply every tick during non-NORMAL
- [ ] Effects play only on initial flatten
- [ ] In-game: Full cycle works (drop → flatten → SPACE → recover)
- [ ] In-game: Visual compression during progression
- [ ] In-game: Player squashed at 5% height
- [ ] In-game: Recovery animation smooth
- [ ] `./gradlew build` succeeds

---

## Phase 13: Integration Testing

**Basic Flatten Flow:**
- [ ] Drop anvil from 5+ blocks → progressive compression
- [ ] Player reaches 5% height when anvil at floor
- [ ] Damage applied when FULLY_FLATTENED reached
- [ ] Particles spawn at flatten moment
- [ ] Sound plays at flatten moment
- [ ] Player width/depth increases (spread)

**Restrictions:**
- [ ] Flattened player cannot move (WASD)
- [ ] Flattened player cannot jump (SPACE = reform)
- [ ] Flattened player cannot use items
- [ ] Flattened player cannot attack
- [ ] Flattened player cannot be pushed
- [ ] Flattened player has no shadow
- [ ] Creative flight disabled while flattened

**Reformation:**
- [ ] SPACE starts recovery when clear
- [ ] SPACE blocked when anvil above
- [ ] SPACE works after fallback timeout
- [ ] Recovery animation interpolates smoothly
- [ ] Player returns to NORMAL
- [ ] Player can move/interact after reform

**Re-Flatten:**
- [ ] Second anvil increases spread
- [ ] Stacking anvil = no damage, spread only
- [ ] Replacing anvil = damage + spread
- [ ] Anvil during recovery → restart compression

**Persistence:**
- [ ] Relog while flattened → state restored
- [ ] Relog while recovering → state restored
- [ ] Respawn → state reset to NORMAL

**Multiplayer:**
- [ ] Other players see flattened state
- [ ] State syncs on player join

**Config:**
- [ ] Config changes affect behavior
- [ ] Disabling anvil_blocking allows reform under anvil

---

## Design Notes

- **Keybind**: SPACE key (only active during FULLY_FLATTENED)
- **Compression**: Distance-based (anvil-to-floor gap)
- **Client sync**: Phase change only
- **Shadow mixin**: Target EntityRenderDispatcher.renderShadow
- **sounds.json**: Must be created for flatten.ogg
