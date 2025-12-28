# Software Requirements Specification
## Toon Flattening

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
Toon Flattening implements cartoon-style flattening mechanics. When an anvil contacts a player from above (either falling entity or placed block), the player is progressively compressed based on the distance between the anvil and the floor. Once fully compressed (5% height), the player is locked in flattened state with blocked movement until reformation. If the anvil is removed before full compression, the player recovers automatically. The system supports re-flattening for both re-placed anvils and anvil stacks, persistent state across sessions, and resets on respawn.

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
- **Anvil Contact Detection**: Monitors player-anvil contact (falling entities and placed blocks) each tick
- **Progressive Compression**: Dynamically scales player height based on anvil-to-floor distance
- **Scale Calculation**: Height derived from physical distance; width/depth expand proportionally as height decreases
- **Movement Control**: Blocks player movement and item interaction only when fully flattened
- **Recovery Animation**: Automatic restoration when anvil removed before full compression
- **Stacking Mechanism**: Incrementally increases horizontal spread on repeated flattening (fully flattened only)
- **State Persistence**: Maintains flattened state and current scales across logout/reconnect
- **Reformation Control**: Player-initiated animation returning to normal scale, with anvil-blocking and fallback timeout
- **Visual/Audio Feedback**: Particles and sound effects when reaching fully flattened state
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
- **UI-2.2**: Particles shall use random cloud spread (0.5, 0.5, 0.5)

**UI-3**: The system shall play audio feedback during flattening.
- **UI-3.1**: Shall use custom sound file `assets/toonflattening/sounds/flatten.ogg`
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

### 4.1 Anvil Contact Detection

#### 4.1.1 Description
The system detects per-tick contact between anvils (both falling entities and placed blocks) and players to initiate progressive flattening.

#### 4.1.2 Functional Requirements

**FR-DETECT.1** [P0]: The system shall check for anvil contact each server tick per player.

**FR-DETECT.2** [P0]: The system shall detect falling anvil entities (FallingBlockEntity with anvil block) that intersect a player's bounding box from above.
- **FR-DETECT.2.1**: Anvil bottom must be at or above player's head position
- **FR-DETECT.2.2**: Anvil must match BlockTags.ANVIL

**FR-DETECT.3** [P0]: The system shall detect placed anvil blocks positioned above the player.
- **FR-DETECT.3.1**: Anvil block must intersect player's bounding box from above
- **FR-DETECT.3.2**: Block must match BlockTags.ANVIL

**FR-DETECT.4** [P0]: The system shall prioritize falling anvil entities over placed blocks when both are detected.

**FR-DETECT.5** [P0]: The system shall exclude spectator mode players from anvil contact detection.

**FR-DETECT.6** [P0]: The system shall cancel vanilla anvil damage events for players being progressively flattened.
- **FR-DETECT.6.1**: Custom damage shall be applied only at full compression per FR-PROG.6

### 4.2 Progressive Flattening

#### 4.2.1 Description
The system progressively compresses the player based on the physical distance between the contacting anvil and the floor surface, rather than applying instant scale changes.

#### 4.2.2 Functional Requirements

**FR-PROG.1** [P0]: The system shall calculate player height scale based on anvil-to-floor distance.
- **FR-PROG.1.1**: Height scale = (anvil bottom Y - floor Y) / original hitbox height
- **FR-PROG.1.2**: Height scale shall be clamped to minimum of 0.05 (5%)
- **FR-PROG.1.3**: Original hitbox height shall be captured when compression begins

**FR-PROG.2** [P0]: The system shall calculate width and depth scales proportionally to height compression.
- **FR-PROG.2.1**: Width scale = 1.0 + (1.0 - height scale) / 2
- **FR-PROG.2.2**: Depth scale shall equal width scale

**FR-PROG.3** [P0]: The system shall track four distinct phases of flattening:
- **FR-PROG.3.1**: Normal - no anvil contact, player at normal scale
- **FR-PROG.3.2**: ProgressiveFlattening - anvil compressing player, height > 0.05
- **FR-PROG.3.3**: FullyFlattened - height reached 0.05, player locked in flattened state
- **FR-PROG.3.4**: Recovering - anvil removed, player animating back to normal scale

**FR-PROG.4** [P0]: The system shall transition from Normal to ProgressiveFlattening when anvil contact is first detected.
- **FR-PROG.4.1**: System shall capture original hitbox height at transition

**FR-PROG.5** [P0]: The system shall transition from ProgressiveFlattening to FullyFlattened when height scale reaches 0.05.
- **FR-PROG.5.1**: System shall capture frozen pose at transition
- **FR-PROG.5.2**: System shall start fallback timer at transition
- **FR-PROG.5.3**: System shall trigger visual and audio effects at transition

**FR-PROG.6** [P0]: The system shall apply damage only when transitioning to FullyFlattened phase.
- **FR-PROG.6.1**: Damage amount shall be 4.0 hearts (8.0 damage points) by default
- **FR-PROG.6.2**: Damage amount shall be configurable per CFG-ANVIL.1
- **FR-PROG.6.3**: Creative mode players shall not receive damage

**FR-PROG.7** [P0]: The system shall transition from ProgressiveFlattening to Recovering when anvil contact is lost before reaching FullyFlattened.
- **FR-PROG.7.1**: Current scale values shall be frozen at transition
- **FR-PROG.7.2**: Recovery animation shall begin immediately

**FR-PROG.8** [P0]: The system shall animate scale recovery over configurable duration during Recovering phase.
- **FR-PROG.8.1**: Scales shall interpolate linearly from frozen values to 1.0
- **FR-PROG.8.2**: Duration shall use reformation_ticks configuration
- **FR-PROG.8.3**: System shall transition to Normal when animation completes

**FR-PROG.9** [P1]: The system shall allow player movement during ProgressiveFlattening phase.
- **FR-PROG.9.1**: Compression shall track player's current bounding box per-tick
- **FR-PROG.9.2**: System shall not restart compression when player moves

**FR-PROG.10** [P1]: The system shall clamp height scale to minimum (0.05) immediately if anvil moves faster than tick rate.
- **FR-PROG.10.1**: Fast-moving anvils bypass progressive compression

**FR-PROG.11** [P1]: The system shall enable/disable progressive flattening via configuration.
- **FR-PROG.11.1**: When disabled, instant flattening behavior shall apply (legacy mode)

### 4.3 State Management

#### 4.3.1 Description
The system manages flattening phase and scale data persistence across game sessions and player lifecycle events.

#### 4.3.2 Functional Requirements

**FR-STATE.1** [P0]: The system shall persist all flattening state when a player logs out.
- **FR-STATE.1.1**: Persistence shall include current phase
- **FR-STATE.1.2**: Persistence shall include current scale values (height, width, depth)
- **FR-STATE.1.3**: Persistence shall include spread multiplier value
- **FR-STATE.1.4**: Persistence shall include frozen pose data
- **FR-STATE.1.5**: Persistence shall include anvil reference (entity UUID or block position)
- **FR-STATE.1.6**: Persistence shall include recovery ticks remaining
- **FR-STATE.1.7**: Persistence shall include fallback ticks remaining

**FR-STATE.2** [P0]: The system shall restore flattening state when a player logs back in.
- **FR-STATE.2.1**: Restoration shall resume at saved phase
- **FR-STATE.2.2**: If anvil reference is invalid on restore, system shall transition to Recovering phase

**FR-STATE.3** [P0]: The system shall reset to Normal phase when a player respawns after death.

**FR-STATE.4** [P1]: The system shall synchronize flattening state to all clients tracking the player.
- **FR-STATE.4.1**: Synchronization shall include current phase
- **FR-STATE.4.2**: Synchronization shall include current scale values
- **FR-STATE.4.3**: Synchronization shall include recovery ticks remaining

**FR-STATE.5** [P1]: The system shall store state using NeoForge attachment system with NBT serialization.

### 4.4 Re-Flatten Stacking

#### 4.4.1 Description
When a flattened or recovering player is contacted by another anvil, the system calculates horizontal spread based on the number of anvils in the stack. Two scenarios trigger re-flatten: (1) stacking additional anvils on top of an existing anvil flattening the player, or (2) replacing the anvil by removing it and dropping a new anvil before the player reaches Normal phase.

#### 4.4.2 Functional Requirements

**FR-REFL.1** [P0]: The system shall detect anvil contact on players in FullyFlattened or Recovering phases.
- **FR-REFL.1.1**: Re-flatten shall not apply during Normal phase
- **FR-REFL.1.2**: Re-flatten during Recovering phase shall restart progressive compression from current scale

**FR-REFL.2** [P0]: The system shall calculate width and depth spread based on anvil count in the contacting stack.
- **FR-REFL.2.1**: Spread added = (anvil_count × spread_increment)
- **FR-REFL.2.2**: Spread increment shall be configurable per CFG-SPREAD.1
- **FR-REFL.2.3**: Anvil count determined by number of vertically stacked anvil blocks above player
- **FR-REFL.2.4**: Spread shall be added to both width and depth scales equally

**FR-REFL.3** [P0]: The system shall enforce maximum spread limit (default: 6.0x width).
- **FR-REFL.3.1**: Maximum spread shall be configurable per CFG-SPREAD.2

**FR-REFL.4** [P0]: The system shall apply damage based on re-flatten scenario.
- **FR-REFL.4.1**: Stacking scenario (anvil contact present when new anvil lands): no damage
- **FR-REFL.4.2**: Replacement scenario (no anvil contact when new anvil lands): base_damage + (anvil_count - 1) × stack_damage_per_anvil
- **FR-REFL.4.3**: Initial flatten with N-anvil stack: base_damage + (N - 1) × stack_damage_per_anvil
- **FR-REFL.4.4**: Stack damage per anvil shall be configurable per CFG-REFL.3
- **FR-REFL.4.5**: Creative mode players shall not receive damage

**FR-REFL.5** [P1]: The system shall reset the fallback timer to initial duration on re-flatten.

**FR-REFL.6** [P0]: The system shall enforce a cooldown period between re-flatten events (default: 1 second).
- **FR-REFL.6.1**: Cooldown duration configurable per CFG-REFL.1
- **FR-REFL.6.2**: Spread calculation occurs once per cooldown period until max_spread_limit reached

**FR-REFL.7** [P0]: The system shall transition Recovering phase to ProgressiveFlattening when anvil contact occurs.
- **FR-REFL.7.1**: Progressive compression shall start from player's current scale values
- **FR-REFL.7.2**: Existing spread multiplier shall carry forward
- **FR-REFL.7.3**: Upon reaching FullyFlattened, new anvil spread and damage shall apply per FR-REFL.2 and FR-REFL.4

### 4.5 Movement and Interaction Restrictions

#### 4.5.1 Description
The system blocks specific player actions while in FullyFlattened phase to simulate cartoon physics. Restrictions do not apply during ProgressiveFlattening or Recovering phases.

#### 4.5.2 Functional Requirements

**FR-MOVE.1** [P0]: The system shall set player movement velocity to zero on each tick while in FullyFlattened phase only.
- **FR-MOVE.1.1**: Movement shall be allowed during ProgressiveFlattening phase
- **FR-MOVE.1.2**: Movement shall be allowed during Recovering phase
- **FR-MOVE.1.3**: System shall block external entity pushing while in FullyFlattened phase

**FR-MOVE.2** [P0]: The system shall cancel all item use actions (right-click, left-click) while in FullyFlattened phase only.
- **FR-MOVE.2.1**: Blocked interactions include eating, drinking, shield blocking, and attacking
- **FR-MOVE.2.2**: System shall cancel all PlayerInteractEvent types

**FR-MOVE.3** [P0]: The system shall allow chat message sending while in FullyFlattened phase.

**FR-MOVE.4** [P0]: The system shall allow command execution while in FullyFlattened phase.

**FR-MOVE.5** [P1]: The system shall freeze player pose when transitioning to FullyFlattened phase.
- **FR-MOVE.5.1**: Frozen pose shall persist until reformation completes
- **FR-MOVE.5.2**: Pose shall not be frozen during ProgressiveFlattening (player can still move/look)

**FR-MOVE.6** [P2]: The system shall prevent inventory changes while in FullyFlattened phase only.

**FR-MOVE.7** [P1]: The system shall disable the player's shadow rendering while in FullyFlattened phase only.
- **FR-MOVE.7.1**: Shadow shall be visible during ProgressiveFlattening and Recovering phases

**FR-MOVE.8** [P0]: The system shall disable creative flying while in FullyFlattened phase.
- **FR-MOVE.8.1**: Player shall be forced to ground state
- **FR-MOVE.8.2**: Creative flight shall be re-enabled upon transition to Normal phase

**FR-MOVE.9** [P0]: The system shall persist flattened state on gamemode switch.
- **FR-MOVE.9.1**: Switching to creative mode shall not clear flattened state
- **FR-MOVE.9.2**: Flattened state restrictions shall continue to apply in creative mode

### 4.6 Reformation Mechanism

#### 4.6.1 Description
Players trigger reformation via keybinding from FullyFlattened phase, initiating an animated transition back to normal scale. Reformation is blocked while an anvil block rests above the player or when vertical clearance is insufficient. A configurable fallback timer bypasses anvil-blocking after timeout.

#### 4.6.2 Functional Requirements

**FR-REFORM.1** [P0]: The system shall provide a keybinding (default: SPACE) to trigger reformation.

**FR-REFORM.2** [P0]: The system shall ignore reformation keybinding input when player is not in FullyFlattened phase.
- **FR-REFORM.2.1**: Input shall be ignored during Normal phase
- **FR-REFORM.2.2**: Input shall be ignored during ProgressiveFlattening phase
- **FR-REFORM.2.3**: Input shall be ignored during Recovering phase

**FR-REFORM.8** [P0]: The system shall block reformation while a placed anvil block occupies any block position intersecting the player's bounding box.
- **FR-REFORM.8.1**: Detection shall check for BlockTags.ANVIL within player AABB
- **FR-REFORM.8.2**: Keybinding input shall be silently ignored while blocked
- **FR-REFORM.8.3**: API reform() calls shall return false while blocked
- **FR-REFORM.8.4**: Anvil-blocking shall be enabled/disabled per CFG-REFORM.2

**FR-REFORM.10** [P0]: The system shall block reformation when vertical clearance is insufficient.
- **FR-REFORM.10.1**: Minimum clearance shall be 0.75 × player's frozen pose hitbox height
- **FR-REFORM.10.2**: Pose hitbox height shall be determined by frozen pose state (standing, sneaking, swimming, sleeping)
- **FR-REFORM.10.3**: Clearance shall be measured from floor surface to lowest obstruction above player
- **FR-REFORM.10.4**: Anvil blocks (BlockTags.ANVIL) shall be treated as ceiling for clearance calculation
- **FR-REFORM.10.5**: Keybinding input shall be silently ignored while insufficient clearance
- **FR-REFORM.10.6**: API reform() calls shall return false while insufficient clearance

**FR-REFORM.9** [P0]: The system shall disable anvil-blocking after a fallback timeout (default: 5 minutes), allowing player to reform via keybind.
- **FR-REFORM.9.1**: Fallback timer shall start when player enters FullyFlattened phase
- **FR-REFORM.9.2**: Fallback timer shall reset on re-flatten events
- **FR-REFORM.9.3**: Upon timeout expiry, anvil-blocking (FR-REFORM.8) shall be bypassed for that player
- **FR-REFORM.9.4**: Player shall remain in FullyFlattened phase until pressing reform keybind after timeout
- **FR-REFORM.9.5**: Fallback timeout duration shall be configurable per CFG-REFORM.3
- **FR-REFORM.9.6**: When CFG-REFORM.3 = 0, fallback timer shall be disabled (anvil blocks indefinitely)

**FR-REFORM.3** [P0]: The system shall transition from FullyFlattened to Recovering phase upon successful reformation request.
- **FR-REFORM.3.1**: Animation duration shall be configurable per CFG-REFORM.1

**FR-REFORM.4** [P0]: The system shall interpolate scale values linearly from flattened to normal over animation duration.

**FR-REFORM.5** [P0]: The system shall restore normal movement and interaction capabilities upon transition to Recovering phase.

**FR-REFORM.6** [P0]: The system shall reset spread multiplier to 0.0 upon transition to Normal phase.

**FR-REFORM.7** [P1]: The system shall unfreeze player pose upon transition to Normal phase.

### 4.7 Visual and Audio Feedback

#### 4.7.1 Description
The system provides particle and sound effects when transitioning to FullyFlattened phase.

#### 4.7.2 Functional Requirements

**FR-VFX.1** [P1]: The system shall spawn 25 POOF particles at player location upon transition to FullyFlattened phase.
- **FR-VFX.1.1**: Particles shall use Minecraft's ParticleTypes.POOF
- **FR-VFX.1.2**: Particles shall spread randomly (0.5, 0.5, 0.5)
- **FR-VFX.1.3**: Particles shall not spawn during ProgressiveFlattening

**FR-VFX.2** [P1]: The system shall play a sound effect at player location upon transition to FullyFlattened phase.
- **FR-VFX.2.1**: Sound shall use custom file `assets/toonflattening/sounds/flatten.ogg`
- **FR-VFX.2.2**: Sound category shall be PLAYERS
- **FR-VFX.2.3**: Sound shall not play during ProgressiveFlattening

**FR-VFX.3** [P2]: The system shall not display visual or audio effects during reformation animation.

---

## 5. API Specification

### 5.1 Public Methods

**API-METHOD.1** [P0]: The system shall expose a public method `flatten(ServerPlayer player, float damage)`.
- **API-METHOD.1.1**: When progressive_enabled=true, method shall transition to ProgressiveFlattening if not already compressing
- **API-METHOD.1.2**: When progressive_enabled=false, method shall immediately transition to FullyFlattened (legacy mode)
- **API-METHOD.1.3**: Method shall return boolean indicating success

**API-METHOD.2** [P0]: The system shall expose a public method `reform(ServerPlayer player)`.
- **API-METHOD.2.1**: Method shall transition from FullyFlattened to Recovering phase
- **API-METHOD.2.2**: Method shall return boolean indicating success (false if not in FullyFlattened phase or blocked)

**API-METHOD.3** [P0]: The system shall expose a public method `isFlattened(Player player)`.
- **API-METHOD.3.1**: Method shall return true only when phase is FullyFlattened
- **API-METHOD.3.2**: Method shall return false for ProgressiveFlattening, Recovering, and Normal phases

**API-METHOD.8** [P1]: The system shall expose a public method `getPhase(Player player)`.
- **API-METHOD.8.1**: Method shall return current FlattenPhase enum value (Normal, ProgressiveFlattening, FullyFlattened, Recovering)

**API-METHOD.9** [P1]: The system shall expose a public method `getCurrentScales(Player player)`.
- **API-METHOD.9.1**: Method shall return current height, width, and depth scale values
- **API-METHOD.9.2**: Method shall return (1.0, 1.0, 1.0) for Normal phase

**API-METHOD.4** [P1]: The system shall expose a public method `getSpreadMultiplier(Player player)`.
- **API-METHOD.4.1**: Method shall return float value representing accumulated horizontal spread
- **API-METHOD.4.2**: Method shall return 0.0 for non-FullyFlattened players

**API-METHOD.5** [P1]: The system shall expose a public method `setSpreadMultiplier(ServerPlayer player, float spread)`.
- **API-METHOD.5.1**: Method shall clamp spread to configured maximum per CFG-SPREAD.2
- **API-METHOD.5.2**: Method shall only apply when player is in FullyFlattened phase

**API-METHOD.6** [P1]: The system shall expose a public method `isReformBlocked(Player player)`.
- **API-METHOD.6.1**: Method shall return true if anvil block detected above player per FR-REFORM.8
- **API-METHOD.6.2**: Method shall return true if insufficient vertical clearance per FR-REFORM.10
- **API-METHOD.6.3**: Method shall return false if not in FullyFlattened phase
- **API-METHOD.6.4**: Method shall return false if anvil-blocking is disabled (CFG-REFORM.2 = false), but clearance check still applies

**API-METHOD.7** [P2]: The system shall expose a public method `getRemainingFallbackTicks(Player player)`.
- **API-METHOD.7.1**: Method shall return remaining ticks until anvil-blocking bypass is enabled
- **API-METHOD.7.2**: Method shall return -1 if fallback timer is disabled (CFG-REFORM.3 = 0)
- **API-METHOD.7.3**: Method shall return 0 if fallback timeout has already expired

### 5.2 Event System

**API-EVENT.1** [P0]: The system shall post a cancellable `PreFlattenEvent` before transitioning to FullyFlattened phase.
- **API-EVENT.1.1**: Event shall extend NeoForge's Event class
- **API-EVENT.1.2**: Event shall expose player, damage, and source (anvil reference)
- **API-EVENT.1.3**: Cancellation shall prevent transition to FullyFlattened (returns to Normal)
- **API-EVENT.1.4**: Event fires only once when height scale reaches 0.05, not during progressive compression

**API-EVENT.2** [P0]: The system shall post a `PostFlattenEvent` after transitioning to FullyFlattened phase.
- **API-EVENT.2.1**: Event shall extend NeoForge's Event class
- **API-EVENT.2.2**: Event shall expose player, applied damage, and spread multiplier
- **API-EVENT.2.3**: Event fires only once per flatten cycle (not on re-flatten)

**API-EVENT.3** [P1]: The system shall post a cancellable `PreReformEvent` before transitioning from FullyFlattened to Recovering phase.
- **API-EVENT.3.1**: Event shall extend NeoForge's Event class
- **API-EVENT.3.2**: Event shall expose player
- **API-EVENT.3.3**: Cancellation shall prevent transition to Recovering phase

**API-EVENT.4** [P1]: The system shall post a `PostReformEvent` after transitioning from Recovering to Normal phase.
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

**NFR-COMPAT.5** [P0]: The system shall respect Pehkui-modified player bounding boxes for anvil detection.
- **Plan**: Use vanilla Entity.getBoundingBox() to automatically handle pre-scaled players

**NFR-COMPAT.6** [P0]: The system shall overwrite Pehkui scales every tick during flattening phases.
- **Plan**: Toon Flattening takes priority over other Pehkui scale modifications

### 6.4 Configurability Requirements

**NFR-CFG.1** [P0]: The system shall reload all configuration values within 5 seconds of TOML file modification without server restart.

**NFR-CFG.2** [P1]: The system shall validate configuration values and log warnings for out-of-range values.

**NFR-CFG.3** [P1]: The system shall apply default values if configuration file is missing or corrupted.

---

## 7. Configuration Requirements

**CFG-PROG.1** [P0]: The system shall provide configuration property `progressive_enabled`.
- **Type**: Boolean
- **Default**: true
- **Description**: When true, anvil contact causes progressive compression. When false, instant flattening (legacy mode)

**CFG-ANVIL.1** [P0]: The system shall provide configuration property `damage_amount`.
- **Type**: Float
- **Range**: 0.0 to 20.0
- **Default**: 4.0
- **Unit**: Hearts (2.0 damage points per heart)

**CFG-SCALE.1** [P0]: The system shall provide configuration property `height_scale`.
- **Type**: Float
- **Range**: 0.01 to 1.0
- **Default**: 0.05
- **Unit**: Minimum height multiplier when fully flattened

**CFG-SPREAD.1** [P0]: The system shall provide configuration property `spread_increment`.
- **Type**: Float
- **Range**: 0.1 to 2.0
- **Default**: 0.8
- **Unit**: Width/depth multiplier added per re-flatten

**CFG-SPREAD.2** [P0]: The system shall provide configuration property `max_spread_limit`.
- **Type**: Float
- **Range**: 1.0 to 6.0
- **Default**: 6.0
- **Unit**: Maximum total width/depth multiplier

**CFG-REFORM.1** [P0]: The system shall provide configuration property `reformation_ticks`.
- **Type**: Integer
- **Range**: 1 to 100
- **Default**: 5
- **Unit**: Game ticks (20 ticks = 1 second)
- **Description**: Duration of reformation animation and recovery animation

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

**CFG-REFL.1** [P0]: The system shall provide configuration property `reflatten_cooldown_ticks`.
- **Type**: Integer
- **Range**: 1 to 100
- **Default**: 20
- **Unit**: Game ticks (20 ticks = 1 second)
- **Description**: Cooldown period between re-flatten events

**CFG-REFL.3** [P0]: The system shall provide configuration property `stack_damage_per_anvil`.
- **Type**: Float
- **Range**: 0.0 to 10.0
- **Default**: 1.0
- **Unit**: Hearts (2.0 damage points per heart)
- **Description**: Additional damage per anvil beyond the first in a stack

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Progressive Flattening | Gradual compression based on anvil-to-floor distance rather than instant transformation |
| FlattenPhase | Enum representing player state: Normal, ProgressiveFlattening, FullyFlattened, Recovering |
| Normal Phase | Player has no anvil contact, at normal scale |
| ProgressiveFlattening Phase | Anvil is compressing player, height > 0.05 |
| FullyFlattened Phase | Player reached minimum height (0.05), locked state with restrictions |
| Recovering Phase | Anvil removed before full flatten or reformation started, animating to normal |
| Anvil Contact | Per-tick detection of anvil (falling entity or placed block) above player |
| Height Scale | Calculated as (anvil bottom Y - floor Y) / original hitbox height |
| Width/Depth Scale | Calculated as 1.0 + (1.0 - height scale) / 2 |
| Re-Flatten | Subsequent anvil contact on FullyFlattened or Recovering player; spread calculated by anvil stack count |
| Spread Multiplier | Accumulated horizontal scale increase based on total anvils encountered |
| Stacking Scenario | Additional anvil dropped on existing anvil currently flattening player; adds spread, no damage |
| Replacement Scenario | Anvil removed and new anvil dropped before player reaches Normal phase; adds spread and damage |
| Reformation | Player-initiated transition from FullyFlattened to Recovering phase |
| Recovery Animation | Automatic scale interpolation to normal when anvil removed before full flatten |
| Anvil Blocking | State where reformation is prevented due to placed anvil block above player |
| Clearance Check | Validation that sufficient vertical space (75% of frozen pose height) exists |
| Fallback Timer | Configurable timeout after which anvil-blocking is bypassed |
| Pehkui | Third-party mod providing entity scaling API |
| NeoForge | Minecraft mod loader and API framework |
| POOF Particle | Vanilla Minecraft particle type (white smoke cloud) |
| Hot-Reload | Configuration update without server restart |
| Frozen Pose | Player animation state captured when transitioning to FullyFlattened |
| Original Hitbox Height | Player's hitbox height captured when progressive compression begins |

---

## Appendix B: State Diagrams

### B.1 Four-Phase State Machine

```
                              anvil contact detected
                                      |
                                      v
+--------+                  +----------------------+
| Normal | ---------------> | ProgressiveFlattening |
+--------+                  +----------------------+
    ^                              |           |
    |                   anvil lost |           | height <= 0.05
    |                   before 0.05|           |
    |                              v           v
    |           anvil contact +------------+  +----------------+
    |           <-------------| Recovering |  | FullyFlattened |
    |           |             +------------+  +----------------+
    |           v                    |               |
    |    +----------------------+    |               | keybind + no block
    |    | ProgressiveFlattening|    |               |
    |    +----------------------+    |               v
    |           |                    |        +------------+
    |           | height <= 0.05     |        | Recovering |
    |           v                    |        +------------+
    |    +----------------+          |               |
    |    | FullyFlattened |          |               |
    |    +----------------+          |               |
    |           |                    |               |
    |           | animation complete | animation     |
    |           v                    v   complete    v
    +<------[Normal]<----------------+<--------------+
```

### B.2 Phase Transitions with Events

```
                                    anvil contact
                                         |
                                         v
+--------+                     +----------------------+
| Normal | ------------------> | ProgressiveFlattening |
+--------+                     +----------------------+
                                         |
                            height scale reaches 0.05
                                         |
                              PreFlattenEvent (cancellable)
                                         |
                            +------------+------------+
                            |                         |
                       cancelled                   allowed
                            |                         |
                            v                         v
                      +-----------+          +----------------+
                      | Recovering |         | FullyFlattened |
                      +-----------+          +----------------+
                                                     |
                                              PostFlattenEvent
                                                     |
                                                     v
                                            [restrictions active]
                                                     |
                                           keybind + clearance ok
                                                     |
                                        PreReformEvent (cancellable)
                                                     |
                                                     v
                                             +------------+
                                             | Recovering |
                                             +------------+
                                                     |
                                          animation complete
                                                     |
                                              PostReformEvent
                                                     |
                                                     v
                                               +--------+
                                               | Normal |
                                               +--------+
```

### B.3 Re-Flatten Flow

```
                     anvil contact detected
                              |
                 +------------+------------+
                 |                         |
          no prior contact         prior contact exists
          (replacement)              (stacking)
                 |                         |
                 v                         v
      +----------------------+    +----------------+
      | ProgressiveFlattening|    | FullyFlattened |
      | (from current scale) |    +----------------+
      +----------------------+             |
                 |                         |
       height <= 0.05                     |
                 |                         |
                 v                         |
      +----------------+                   |
      | FullyFlattened |                   |
      +----------------+                   |
                 |                         |
   - Spread += anvil_count × increment    |
   - Damage = base + (count-1) × stack    |
   - Reset fallback timer                 |
   - Play effects                         |
                 |                         |
                 +-------------------------+
                              |
                              v
                      +----------------+
                      | FullyFlattened |
                      +----------------+
```

---

## Appendix C: TBD List

| ID | Description | Priority | Target Resolution |
|----|-------------|----------|-------------------|
| TBD-3 | Reference hardware specification for performance testing | P1 | v0.5.0 |

---
