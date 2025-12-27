# Software Requirements Specification
## Toon Flattening

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) defines functional, nonfunctional, API, and configuration requirements for Toon Flattening, a NeoForge modification for Minecraft 1.21. The document establishes acceptance criteria for development, testing, and integration.

### 1.2 Document Conventions
- **Requirement IDs**: Hierarchical format (e.g., FR-ANVIL.1, API-EVENT.1, NFR-PERF.1)
- **Priority**: P0 (Critical), P1 (High), P2 (Medium)
- **Modality**: "shall" indicates mandatory requirement
- **TBD**: Marks unresolved specification items (see Appendix C)
- **Quantification**: All performance metrics use Planguage notation where applicable

### 1.3 Intended Audience
- **Development Team**: Implementation and testing reference
- **QA Engineers**: Validation and acceptance testing
- **Mod Developers**: API consumers requiring integration specifications
- **Server Administrators**: Configuration and deployment planning
- **Project Stakeholders**: Feature scope and acceptance criteria

### 1.4 Product Scope
Toon Flattening implements cartoon-style flattening mechanics triggered by anvil collision. Players experience height compression (5% vertical scale) and width expansion (180% horizontal scale) with blocked movement, reformation keybinding, and configurable stacking behavior. The system persists across sessions but resets on respawn.

### 1.5 References
- ISO/IEC/IEEE 29148:2018 – Systems and software engineering — Life cycle processes — Requirements engineering
- Wiegers, Karl; Beatty, Joy. *Software Requirements, 3rd Edition*. Microsoft Press, 2013
- NeoForge 21.1.214 Documentation: https://docs.neoforged.net/
- Pehkui API 3.8.3: https://github.com/Virtuoel/Pehkui
- Minecraft Java Edition 1.21 Documentation

---

## 2. Overall Description

### 2.1 Product Perspective
Toon Flattening is a standalone NeoForge mod with a hard dependency on Pehkui for entity scaling. It operates within Minecraft's entity tick and event systems, integrating with:
- NeoForge event bus for collision detection
- Pehkui API for scale manipulation
- Minecraft client/server synchronization for state persistence
- Keybinding system for player input

The mod exposes public APIs and events for third-party integration.

### 2.2 Product Functions
- **Anvil Collision Detection**: Monitors player-anvil collisions to trigger flattening
- **Scale Transformation**: Reduces height to 5% and expands width to 180% via Pehkui
- **Movement Control**: Blocks player movement and item interaction during flattened state
- **Stacking Mechanism**: Incrementally increases horizontal spread on repeated flattening
- **State Persistence**: Maintains flattened state across logout/reconnect
- **Reformation Control**: Player-initiated animation returning to normal scale, with anvil-blocking and fallback timeout
- **Visual/Audio Feedback**: Particles and sound effects for flatten events
- **Configuration Management**: Hot-reloadable TOML-based settings

### 2.3 User Classes and Characteristics
| User Class | Description | Technical Expertise | Usage Frequency |
|------------|-------------|---------------------|-----------------|
| Player | Experiences flattening mechanics during gameplay | Low | Continuous |
| Server Administrator | Configures mod settings via TOML files | Medium | Per-deployment |
| Mod Developer | Integrates flattening API into custom mods | High | Development phase |

### 2.4 Operating Environment
- **Platform**: Java 21 (LTS)
- **Mod Loader**: NeoForge 21.1.214 or later
- **Minecraft Version**: 1.21
- **Required Dependencies**: Pehkui 3.8.3 or later
- **Target Environment**: Client-side and server-side (dedicated servers supported)

### 2.5 Design and Implementation Constraints
- **CON-1**: Shall operate within NeoForge event lifecycle constraints
- **CON-2**: Shall not modify Minecraft base classes directly (mixin-based approach)
- **CON-3**: Shall maintain compatibility with Pehkui 3.8.3+ API contract
- **CON-4**: Shall synchronize state between client and server using NeoForge networking
- **CON-5**: Shall respect Minecraft's creative mode and spectator mode behavior

### 2.6 Assumptions and Dependencies
- **DEP-1**: Pehkui 3.8.3+ is installed and functioning correctly
- **DEP-2**: NeoForge 21.1.214+ provides stable event bus and networking APIs
- **DEP-3**: Server administrators have file system access for configuration
- **ASSUME-1**: Players have default keybinding availability (SPACE key)
- **ASSUME-2**: Server tick rate is standard (20 TPS)

---

## 3. External Interface Requirements

### 3.1 User Interfaces
**UI-1**: The system shall provide a keybinding configuration in Minecraft's Options > Controls menu.
- **UI-1.1**: Default binding shall be SPACE key
- **UI-1.2**: Keybinding label shall read "Reform from Flattened State"
- **UI-1.3**: Keybinding shall support rebinding to any valid key

**UI-2**: The system shall display visual feedback during flattening.
- **UI-2.1**: Shall spawn 25 POOF particles at player location
- **UI-2.2**: TBD: Particle spawn pattern (sphere/cloud distribution)

**UI-3**: The system shall play audio feedback during flattening.
- **UI-3.1**: TBD: Sound source (custom sound file vs vanilla sound effect)
- **UI-3.2**: Sound volume shall respect player's sound settings

### 3.2 Software Interfaces
**SI-1**: Pehkui Integration
- **SI-1.1**: Shall use Pehkui's ScaleType API for width, height, and model scale modifications
- **SI-1.2**: Shall maintain scale data persistence through Pehkui's data synchronization
- **SI-1.3**: Shall query Pehkui version at runtime and fail gracefully if incompatible

**SI-2**: NeoForge Event Bus
- **SI-2.1**: Shall register event handlers on MOD event bus for lifecycle events
- **SI-2.2**: Shall register event handlers on FORGE event bus for entity collision
- **SI-2.3**: Shall post custom events to FORGE event bus for third-party consumption

**SI-3**: Minecraft Networking
- **SI-3.1**: Shall synchronize flattened state using NeoForge's networking API
- **SI-3.2**: Shall handle client-side prediction for reform animation
- **SI-3.3**: Shall validate server authority for state transitions

### 3.3 Communications Interfaces
**CI-1**: Client-Server Synchronization
- **CI-1.1**: Shall transmit flatten state changes from server to all tracking clients
- **CI-1.2**: Shall transmit reform requests from client to server
- **CI-1.3**: Shall synchronize spread multiplier as floating-point value (4 bytes)

---

## 4. System Features

### 4.1 Anvil Collision Trigger

#### 4.1.1 Description
The system detects when an anvil entity collides with a player entity and initiates the flattening transformation.

#### 4.1.2 Functional Requirements

**FR-ANVIL.1** [P0]: The system shall detect when a falling anvil entity intersects a player's bounding box.

**FR-ANVIL.2** [P0]: The system shall exclude spectator mode players from anvil collision detection.

**FR-ANVIL.3** [P0]: The system shall apply flattening transformation to creative mode players without applying damage.

**FR-ANVIL.4** [P0]: The system shall apply flattening transformation to survival/adventure mode players and apply damage per FR-ANVIL.5.

**FR-ANVIL.5** [P0]: The system shall apply 4.0 hearts of damage (8.0 damage points) to non-creative players upon flattening.
- **FR-ANVIL.5.1**: Damage amount shall be configurable per CFG-ANVIL.1

**FR-ANVIL.6** [P1]: The system shall consume the anvil collision event to prevent vanilla damage mechanics from applying.

### 4.2 State Management

#### 4.2.1 Description
The system manages flattened state persistence across game sessions and player lifecycle events.

#### 4.2.2 Functional Requirements

**FR-STATE.1** [P0]: The system shall persist flattened state when a player logs out.

**FR-STATE.2** [P0]: The system shall restore flattened state when a player logs back in.
- **FR-STATE.2.1**: Restoration shall include scale values (height, width, model)
- **FR-STATE.2.2**: Restoration shall include spread multiplier value
- **FR-STATE.2.3**: Restoration shall include frozen pose data

**FR-STATE.3** [P0]: The system shall reset flattened state to normal when a player respawns after death.

**FR-STATE.4** [P1]: The system shall synchronize flattened state to all clients tracking the player.

**FR-STATE.5** [P1]: The system shall store state using Minecraft's persistent entity data (NBT).

### 4.3 Re-Flatten Stacking

#### 4.3.1 Description
When a flattened player is struck by another anvil, the system incrementally increases horizontal spread without resetting the flattened state.

#### 4.3.2 Functional Requirements

**FR-REFL.1** [P0]: The system shall detect anvil collision on already-flattened players.

**FR-REFL.2** [P0]: The system shall increase width scale by the configured spread increment (default: 0.8).
- **FR-REFL.2.1**: Spread increment shall be configurable per CFG-SPREAD.1

**FR-REFL.3** [P0]: The system shall enforce maximum spread limit (default: 6.0x width).
- **FR-REFL.3.1**: Maximum spread shall be configurable per CFG-SPREAD.2

**FR-REFL.4** [P0]: The system shall apply damage per FR-ANVIL.5 on each re-flatten event.

**FR-REFL.5** [P1]: The system shall reset the fallback timer to initial duration on re-flatten per FR-REFORM.9.2.

**FR-REFL.6** [P1]: The system shall play visual and audio feedback per FR-VFX.1 and FR-VFX.2 on re-flatten.

### 4.4 Movement and Interaction Restrictions

#### 4.4.1 Description
The system blocks specific player actions while in flattened state to simulate cartoon physics.

#### 4.4.2 Functional Requirements

**FR-MOVE.1** [P0]: The system shall set player movement velocity to zero on each tick while flattened.

**FR-MOVE.2** [P0]: The system shall cancel all item use actions (right-click, left-click) while flattened.

**FR-MOVE.3** [P0]: The system shall allow chat message sending while flattened.

**FR-MOVE.4** [P0]: The system shall allow command execution while flattened.

**FR-MOVE.5** [P1]: The system shall freeze player pose at the moment of anvil impact.
- **FR-MOVE.5.1**: Frozen pose shall persist until reformation completes

**FR-MOVE.6** [P2]: The system shall prevent inventory changes while flattened.

**FR-MOVE.7** [P1]: The system shall disable the player's shadow rendering while flattened.
- **FR-MOVE.7.1**: Shadow shall be restored upon reformation completion

### 4.5 Reformation Mechanism

#### 4.5.1 Description
Players trigger reformation via keybinding, initiating an animated transition back to normal scale. Reformation is blocked while an anvil block rests above the player or when vertical clearance is insufficient (< 75% of frozen pose hitbox height). A configurable fallback timer bypasses anvil-blocking after timeout, allowing the player to reform via keybind.

#### 4.5.2 Functional Requirements

**FR-REFORM.1** [P0]: The system shall provide a keybinding (default: SPACE) to trigger reformation.

**FR-REFORM.2** [P0]: The system shall ignore reformation keybinding input when player is not flattened.

**FR-REFORM.8** [P0]: The system shall block reformation while a placed anvil block occupies any block position intersecting the player's bounding box.
- **FR-REFORM.8.1**: Detection shall check for BlockTags.ANVIL within player AABB
- **FR-REFORM.8.2**: Keybinding input shall be silently ignored while blocked
- **FR-REFORM.8.3**: API reform() calls shall return false while blocked
- **FR-REFORM.8.4**: Anvil-blocking shall be enabled/disabled per CFG-REFORM.2

**FR-REFORM.10** [P0]: The system shall block reformation when vertical clearance is insufficient.
- **FR-REFORM.10.1**: Minimum clearance shall be 0.75 × player's current pose hitbox height
- **FR-REFORM.10.2**: Pose hitbox height shall be determined by frozen pose state (standing, sneaking, swimming, sleeping)
- **FR-REFORM.10.3**: Clearance shall be measured from floor surface to lowest obstruction above player
- **FR-REFORM.10.4**: Anvil blocks (BlockTags.ANVIL) shall be treated as ceiling for clearance calculation
- **FR-REFORM.10.5**: Keybinding input shall be silently ignored while insufficient clearance
- **FR-REFORM.10.6**: API reform() calls shall return false while insufficient clearance

**FR-REFORM.9** [P0]: The system shall disable anvil-blocking after a fallback timeout (default: 5 minutes), allowing player to reform via keybind.
- **FR-REFORM.9.1**: Fallback timer shall start when player enters flattened state
- **FR-REFORM.9.2**: Fallback timer shall reset on re-flatten events
- **FR-REFORM.9.3**: Upon timeout expiry, anvil-blocking (FR-REFORM.8) shall be bypassed for that player
- **FR-REFORM.9.4**: Player shall remain flattened until pressing reform keybind after timeout
- **FR-REFORM.9.5**: Fallback timeout duration shall be configurable per CFG-REFORM.3
- **FR-REFORM.9.6**: When CFG-REFORM.3 = 0, fallback timer shall be disabled (anvil blocks indefinitely)

**FR-REFORM.3** [P0]: The system shall initiate reformation animation over configurable duration (default: 5 ticks).
- **FR-REFORM.3.1**: Animation duration shall be configurable per CFG-REFORM.1

**FR-REFORM.4** [P0]: The system shall interpolate scale values linearly from flattened to normal over animation duration.

**FR-REFORM.5** [P0]: The system shall restore normal movement and interaction capabilities upon animation completion.

**FR-REFORM.6** [P0]: The system shall reset spread multiplier to 0.0 upon reformation completion.

**FR-REFORM.7** [P1]: The system shall unfreeze player pose upon reformation completion.

### 4.6 Visual and Audio Feedback

#### 4.6.1 Description
The system provides particle and sound effects to communicate flattening events.

#### 4.6.2 Functional Requirements

**FR-VFX.1** [P1]: The system shall spawn 25 POOF particles at player location upon flattening.
- **FR-VFX.1.1**: TBD: Particle count configurability
- **FR-VFX.1.2**: Particles shall use Minecraft's ParticleTypes.POOF

**FR-VFX.2** [P1]: The system shall play a sound effect at player location upon flattening.
- **FR-VFX.2.1**: TBD: Sound source identification (custom vs vanilla)
- **FR-VFX.2.2**: Sound category shall be PLAYERS

**FR-VFX.3** [P2]: TBD: Visual feedback for reformation event (particles/sound)

---

## 5. API Specification

### 5.1 Public Methods

**API-METHOD.1** [P0]: The system shall expose a public method `flatten(ServerPlayer player, float damage)`.
- **API-METHOD.1.1**: Method shall trigger flattening state on specified player
- **API-METHOD.1.2**: Method shall apply specified damage amount
- **API-METHOD.1.3**: Method shall return boolean indicating success

**API-METHOD.2** [P0]: The system shall expose a public method `reform(ServerPlayer player)`.
- **API-METHOD.2.1**: Method shall initiate reformation animation
- **API-METHOD.2.2**: Method shall return boolean indicating success (false if not flattened)

**API-METHOD.3** [P0]: The system shall expose a public method `isFlattened(Player player)`.
- **API-METHOD.3.1**: Method shall return boolean indicating current flattened state

**API-METHOD.4** [P1]: The system shall expose a public method `getSpreadMultiplier(Player player)`.
- **API-METHOD.4.1**: Method shall return float value representing current horizontal spread
- **API-METHOD.4.2**: Method shall return 0.0 for non-flattened players

**API-METHOD.5** [P1]: The system shall expose a public method `setSpreadMultiplier(ServerPlayer player, float spread)`.
- **API-METHOD.5.1**: Method shall clamp spread to configured maximum per CFG-SPREAD.2

**API-METHOD.6** [P1]: The system shall expose a public method `isReformBlocked(Player player)`.
- **API-METHOD.6.1**: Method shall return true if anvil block detected above player per FR-REFORM.8
- **API-METHOD.6.2**: Method shall return true if insufficient vertical clearance per FR-REFORM.10
- **API-METHOD.6.3**: Method shall return false for non-flattened players
- **API-METHOD.6.4**: Method shall return false if anvil-blocking is disabled (CFG-REFORM.2 = false), but clearance check still applies

**API-METHOD.7** [P2]: The system shall expose a public method `getRemainingFallbackTicks(Player player)`.
- **API-METHOD.7.1**: Method shall return remaining ticks until anvil-blocking bypass is enabled
- **API-METHOD.7.2**: Method shall return -1 if fallback timer is disabled (CFG-REFORM.3 = 0)
- **API-METHOD.7.3**: Method shall return 0 if fallback timeout has already expired

### 5.2 Event System

**API-EVENT.1** [P0]: The system shall post a cancellable `PreFlattenEvent` before applying flattening transformation.
- **API-EVENT.1.1**: Event shall extend NeoForge's Event class
- **API-EVENT.1.2**: Event shall expose player, damage, and source (anvil entity)
- **API-EVENT.1.3**: Cancellation shall prevent flattening and damage

**API-EVENT.2** [P0]: The system shall post a `PostFlattenEvent` after applying flattening transformation.
- **API-EVENT.2.1**: Event shall extend NeoForge's Event class
- **API-EVENT.2.2**: Event shall expose player, applied damage, and spread multiplier

**API-EVENT.3** [P1]: The system shall post a cancellable `PreReformEvent` before initiating reformation.
- **API-EVENT.3.1**: Event shall extend NeoForge's Event class
- **API-EVENT.3.2**: Event shall expose player
- **API-EVENT.3.3**: Cancellation shall prevent reformation

**API-EVENT.4** [P1]: The system shall post a `PostReformEvent` after reformation completes.
- **API-EVENT.4.1**: Event shall extend NeoForge's Event class
- **API-EVENT.4.2**: Event shall expose player

### 5.3 Extension Points

**API-EXT.1** [P2]: The system shall provide an interface `IFlattenTrigger` for custom flatten triggers.
- **API-EXT.1.1**: Interface shall define method `shouldTriggerFlatten(Player player)`
- **API-EXT.1.2**: System shall query registered triggers on entity tick

**API-EXT.2** [P2]: The system shall provide a registration method `registerFlattenTrigger(IFlattenTrigger trigger)`.

---

## 6. Nonfunctional Requirements

### 6.1 Performance Requirements

**NFR-PERF.1** [P0]: The system shall process flattened player tick logic with mean execution time ≤ 0.1 milliseconds per player.
- **Scale**: Tick processing time per flattened player
- **Meter**: Server profiler (Spark/timings)
- **Must**: ≤ 0.1 ms mean
- **Plan**: 0.05 ms mean
- **Wish**: ≤ 0.02 ms mean
- **TBD**: Reference hardware specification

**NFR-PERF.2** [P1]: The system shall complete reformation animation without client frame drops below 60 FPS on reference hardware.
- **TBD**: Reference hardware specification (GPU, CPU)

**NFR-PERF.3** [P1]: The system shall handle 100 simultaneously flattened players with server TPS ≥ 19.5.

### 6.2 Reliability Requirements

**NFR-REL.1** [P0]: The system shall persist flattened state with 100% reliability across server restart.

**NFR-REL.2** [P0]: The system shall prevent state corruption if Pehkui fails to apply scale changes.
- **Plan**: Rollback to pre-flatten state on failure

**NFR-REL.3** [P1]: The system shall log error messages for all API misuse (invalid player, null parameters).

### 6.3 Compatibility Requirements

**NFR-COMPAT.1** [P0]: The system shall operate without errors on NeoForge 21.1.214 through 21.1.x minor versions.

**NFR-COMPAT.2** [P0]: The system shall operate without errors with Pehkui 3.8.3 through 3.8.x minor versions.

**NFR-COMPAT.3** [P1]: The system shall not conflict with mods modifying player movement (e.g., speed mods, flight mods).

**NFR-COMPAT.4** [P1]: The system shall respect other mods' cancellation of damage events.

### 6.4 Configurability Requirements

**NFR-CFG.1** [P0]: The system shall reload all configuration values within 5 seconds of TOML file modification without server restart.

**NFR-CFG.2** [P1]: The system shall validate configuration values and log warnings for out-of-range values.

**NFR-CFG.3** [P1]: The system shall apply default values if configuration file is missing or corrupted.

---

## 7. Configuration Requirements

**CFG-ANVIL.1** [P0]: The system shall provide configuration property `damage_amount`.
- **Type**: Float
- **Range**: 0.0 to 20.0
- **Default**: 4.0
- **Unit**: Hearts (2.0 damage points per heart)

**CFG-SCALE.1** [P0]: The system shall provide configuration property `height_scale`.
- **Type**: Float
- **Range**: 0.01 to 1.0
- **Default**: 0.05
- **Unit**: Multiplier (1.0 = normal height)

**CFG-SCALE.2** [P0]: The system shall provide configuration property `width_scale`.
- **Type**: Float
- **Range**: 1.0 to 6.0
- **Default**: 1.8
- **Unit**: Multiplier (1.0 = normal width)

**CFG-SPREAD.1** [P0]: The system shall provide configuration property `spread_increment`.
- **Type**: Float
- **Range**: 0.1 to 2.0
- **Default**: 0.8
- **Unit**: Width multiplier added per re-flatten

**CFG-SPREAD.2** [P0]: The system shall provide configuration property `max_spread_limit`.
- **Type**: Float
- **Range**: 1.0 to 6.0
- **Default**: 6.0
- **Unit**: Maximum width multiplier

**CFG-REFORM.1** [P0]: The system shall provide configuration property `reformation_ticks`.
- **Type**: Integer
- **Range**: 1 to 100
- **Default**: 5
- **Unit**: Game ticks (20 ticks = 1 second)

**CFG-REFORM.2** [P0]: The system shall provide configuration property `anvil_blocking_enabled`.
- **Type**: Boolean
- **Default**: true
- **Description**: When true, reformation is blocked while anvil block rests above player

**CFG-REFORM.3** [P0]: The system shall provide configuration property `fallback_timeout_seconds`.
- **Type**: Integer
- **Range**: 0 to 3600
- **Default**: 300
- **Unit**: Seconds (0 = disabled, anvil blocks indefinitely)
- **Description**: Time until anvil-blocking is bypassed; player must still press keybind to reform

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Flattening | Transformation applied to player reducing height to 5% and expanding width to 180% |
| Re-Flatten | Subsequent anvil collision on already-flattened player, increasing horizontal spread |
| Spread Multiplier | Accumulated horizontal scale increase from re-flatten events |
| Reformation | Player-initiated animation returning entity to normal scale |
| Anvil Blocking | State where reformation is prevented due to placed anvil block above player |
| Clearance Check | Validation that sufficient vertical space (75% of frozen pose height) exists between floor and lowest obstruction (including anvils) |
| Fallback Timer | Configurable timeout after which anvil-blocking is bypassed, allowing keybind reformation |
| Pehkui | Third-party mod providing entity scaling API |
| NeoForge | Minecraft mod loader and API framework |
| POOF Particle | Vanilla Minecraft particle type (white smoke cloud) |
| Hot-Reload | Configuration update without server restart |
| Frozen Pose | Player animation state captured at moment of anvil impact |

---

## Appendix B: State Diagrams

### B.1 Flattened State Lifecycle

```
[Normal] --anvil collision--> [Flattening] --apply scales--> [Flattened]
   ^                                                              |
   |                                                   keybinding |
   |                                                              v
   |                                                   [Reform Check]
   |                                                    /    |     \
   |                                       anvil above /     |      \ clearance ok
   |                                                  v      v       \
   |                                            [Blocked] [Low       v
   |                                                 |    Clearance] [Reforming]
   |                               fallback expires  |        |          |
   |                                                 v        |          |
   |                                         [Bypass Enabled] |          |
   |                                                 |        |          |
   |                                          keybinding      |          |
   |                                                 v        |          |
   |                                          [Clearance Check]          |
   |                                                 |                   |
   +<---------------reformation complete-------------+-------------------+
                                      ^
                                      |
                              re-flatten (anvil collision, resets timer)
                                      |
                                      +------[Flattened]
```

### B.2 State Transitions with Events

```
                    PreFlattenEvent (cancellable)
                             |
                             v
[Normal] -----------> [Transition Check] --------> [Flattened]
                             |                          |
                        cancelled                  PostFlattenEvent
                             |                          |
                             v                          v
                         [Normal]                  [Active State]
                                                         |
                                                    PreReformEvent
                                                         |
                                                    [Reforming]
                                                         |
                                                    PostReformEvent
                                                         |
                                                         v
                                                     [Normal]
```

---

## Appendix C: TBD List

| ID | Description | Priority | Target Resolution |
|----|-------------|----------|-------------------|
| TBD-1 | Sound source specification (custom sound file vs vanilla sound effect) | P1 | v0.5.0 |
| TBD-2 | Particle configurability (count, pattern) | P2 | v0.6.0 |
| TBD-3 | Reference hardware specification for performance testing | P1 | v0.5.0 |
| TBD-4 | Particle spawn pattern (sphere/cloud/random distribution) | P2 | v0.6.0 |
| TBD-5 | Visual/audio feedback for reformation event | P2 | v0.6.0 |

---
