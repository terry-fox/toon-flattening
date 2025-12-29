# Software Requirements Specification
## Cartoon Flattening Mod — Hammer Feature
**Version:** 1.0.0-DRAFT  
**Date:** 2025-01-01  
**Target Platform:** Minecraft 1.21.1, NeoForge 21.1.214  
**Document Standard:** Wiegers & Beatty, Software Requirements 3rd Edition

---

## 1. Introduction

### 1.1 Purpose
This document defines the software requirements for the **Hammer** feature within the Cartoon Flattening mod. The Hammer is a new weapon item that extends current flattening mechanics to flatten player targets upon a successful melee hit.

### 1.2 Scope
This specification covers:
- The Hammer item (crafting, properties, behavior)
- Integration with the existing flattening system
- Player state changes when flattened by the Hammer
- Configuration parameters

This specification excludes:
- Mob flattening (deferred to future release)
- Anvil flattening mechanics (existing feature, referenced only)
- Custom death messages (planned future version)

### 1.3 Definitions and Acronyms

| Term | Definition |
|------|------------|
| Flattened State | A player capability where the entity model is down on the Y-axis with increased X/Z dimensions |
| Spread Value | The cumulative X/Z scale multiplier applied to a flattened player |
| Reform | The action of returning a flattened player to normal dimensions |
| TPS | Ticks Per Second; Minecraft server performance metric (target: 20 TPS) |
| BMC5 | Better Minecraft 5 modpack |

### 1.4 References
- NeoForge 1.21.1 Documentation
- Existing `toonflattening` mod codebase (anvil flattening implementation)

---

## 2. Business Requirements

### BR-1: Project Vision
The Cartoon Flattening mod shall provide roleplay-oriented multiplayer servers with tools to enact cartoon-style transformation scenarios in a stable, multiplayer-safe manner.

### BR-2: Success Criteria
The Hammer feature shall be considered complete when:
- **BR-2.1:** The Hammer item is craftable and functional in survival mode.
- **BR-2.2:** The flattening effect synchronizes correctly across all connected clients.
- **BR-2.3:** The feature operates without server crashes or TPS degradation under normal load.
- **BR-2.4:** The feature is compatible with the target modpack (BMC5) and NeoForge 1.21.1.

### BR-3: Stakeholders

| Stakeholder | Role | Concern |
|-------------|------|---------|
| Server Operators | Deployer | Stability, configurability, performance |
| Players | End User | Visual fidelity, responsive controls, fair gameplay |
| Mod Developer | Implementer | Clean architecture, maintainability, extensibility |

---

## 3. User Requirements

### 3.1 Actor Definitions

| Actor | Description |
|-------|-------------|
| Attacker | A player wielding the Hammer who initiates a flattening attack |
| Target | A player who receives a Hammer hit and becomes flattened |
| Flattened Player | A player currently in the Flattened State |
| Server | The authoritative game server managing state |

### 3.2 Use Cases

#### UC-1: Flatten a Player with the Hammer
**Primary Actor:** Attacker  
**Preconditions:**
- PvP is enabled on the server.
- The Attacker is holding a Hammer item.
- The Target is within melee range (≤3.0 blocks).
- The Hammer's attack cooldown has elapsed.

**Main Success Scenario:**
1. The Attacker performs a melee attack on the Target.
2. The Server validates hit registration and PvP rules.
3. The Server flattens the Target.
4. The Server sets the Target's Y-scale to the configured height value and applies the configured initial X/Z spread.
5. The Server plays the flattening sound (`toonflattening:flatten`) at the Target's location.
6. The Server spawns 25 `minecraft:poof` particles in a circular burst at the Target's location.
7. The Server synchronizes the updated state to all tracking clients within 100ms.
8. The Target's movement is restricted per the Flattened State rules.

**Extensions:**
- **3a.** Target is already flattened:
  - 3a.1. The Server increments the Target's Spread Value by the configured per-hit increment.
  - 3a.2. The Server does NOT play the sound or spawn particles.
  - 3a.3. The Server synchronizes the updated Spread Value to all tracking clients.
- **3b.** Target's Spread Value is at the configured maximum:
  - 3b.1. The Server does NOT modify the Spread Value.
  - 3b.2. The Server does NOT play sound or spawn particles.

**Postconditions:**
- The Target is in the Flattened State with updated scale values.
- The flattening source is recorded as "Hammer."

---

#### UC-2: Reform from Flattened State
**Primary Actor:** Flattened Player  
**Preconditions:**
- The Flattened Player is currently in the Flattened State.

**Main Success Scenario:**
1. The Flattened Player presses the Reform keybind (default: Space).
2. The Server receives the reform request.
3. The Server initiates the reform animation over the configured duration.
4. The Server interpolates the player's scale from the current flattened dimensions to normal (1.0, 1.0, 1.0).
5. The player is no longer considered flattened.
7. The Server synchronizes the restored state to all tracking clients.

**Extensions:**
- None.

**Postconditions:**
- The player is no longer in the Flattened State.
- The player has full movement capabilities restored.

---

#### UC-3: Craft a Hammer
**Primary Actor:** Player  
**Preconditions:**
- The Player has access to a crafting table.
- The Player possesses the required materials.

**Main Success Scenario:**
1. The Player opens a crafting table interface.
2. The Player places 2 Iron Ingots in positions (1,1) and (1,3) [top-left and top-right].
3. The Player places 3 Sticks in positions (1,2), (2,2), and (3,2) [middle column].
4. The crafting output displays a Hammer item.
5. The Player retrieves the Hammer from the output slot.

**Postconditions:**
- The Player possesses one Hammer item.

---

#### UC-4: Persist Flattened State Across Logout
**Primary Actor:** Server  
**Trigger:** A Flattened Player disconnects from the server.

**Main Success Scenario:**
1. The Server detects the player disconnect event.
2. The Server serializes the player's flattened state (including Spread Value and source).
3. The Server stores the serialized state in the player's persistent data.
4. Upon reconnection, the Server deserializes and reapplies the flattened state.
5. The Server synchronizes the flattened state to the reconnecting client and all tracking clients.

**Postconditions:**
- The player remains in the Flattened State with the same parameters as before logout.

---

#### UC-5: Clear Flattened State on Death
**Primary Actor:** Server  
**Trigger:** A Flattened Player dies.

**Main Success Scenario:**
1. The Server detects the player death event.
2. The Server removes the flattened state from the player.
3. The Server does NOT serialize the flattened state to persistent data.
4. Upon respawn, the player has normal dimensions.

**Postconditions:**
- The respawned player is not in the Flattened State.

---

## 4. Functional Requirements

### 4.1 Hammer Item

#### FR-1: Hammer Item Registration
The system shall register a new item with the identifier `toonflattening:hammer`.

#### FR-2: Hammer Crafting Recipe
The system shall register a shaped crafting recipe for the Hammer with the following pattern:
```
I S I
_ S _
_ S _
```
Where `I` = `minecraft:iron_ingot` and `S` = `minecraft:stick`.

*Note: Pattern represents a 3x3 crafting grid; top-left (1,1), top-right (1,3), middle column (1,2), (2,2), (3,2).*

#### FR-3: Hammer Durability
The Hammer shall have infinite durability and shall not consume durability on use.

#### FR-4: Hammer Attack Damage
The Hammer shall deal 0.0 attack damage to entities.

#### FR-5: Hammer Attack Speed
The Hammer shall have an attack speed of 0.8 (equivalent to vanilla axes).

#### FR-6: Hammer Enchantability
The Hammer shall not be enchantable. The system shall prevent the Hammer from being enchanted via enchanting table, anvil, or commands.

#### FR-7: Hammer Creative Tab
The Hammer shall appear in the mod's creative tab (or a relevant vanilla combat/tools tab).

---

### 4.2 Flattening Trigger

#### FR-8: Hammer Hit Detection
The system shall detect when a player holding a Hammer successfully lands a melee attack on another player entity.

#### FR-9: PvP Rule Enforcement
The system shall apply flattening only when the server's PvP rules permit damage between the Attacker and Target.

#### FR-10: Melee Range Constraint
The system shall use the standard vanilla melee reach distance (approximately 3.0 blocks) for Hammer hit registration.

#### FR-11: Target State Independence
The system shall allow flattening regardless of the Target's current state (grounded, airborne, swimming, etc.).

#### FR-12: Attack Cooldown Enforcement
The system shall respect the Hammer's attack speed cooldown (0.8) before allowing subsequent flattening hits.

---

### 4.3 Flattening Effect Application

#### FR-13: Initial Flattening — Y-Scale
When a non-flattened player is hit by the Hammer, the system shall set the Target's Y-axis scale to the configured flatten height value.

#### FR-14: Initial Flattening — X/Z Scale
When a non-flattened player is hit by the Hammer, the system shall increment the Target's X-axis and Z-axis scale by the configured per-hit spread increment.

#### FR-15: Spread Increment on Subsequent Hits
When an already-flattened player is hit by the Hammer, the system shall increment the Target's X-axis and Z-axis scale by the configured per-hit spread increment.

#### FR-16: Maximum Spread Enforcement
The system shall not increase a player's X/Z scale beyond the configured maximum spread value.

#### FR-17: Flattening Sound — Initial Only
When a non-flattened player is hit by the Hammer, the system shall play the sound `toonflattening:flatten` at the Target's location.

#### FR-18: Flattening Sound — Suppression on Subsequent Hits
When an already-flattened player is hit by the Hammer, the system shall NOT play the flattening sound.

#### FR-19: Flattening Particles — Initial Only
When a non-flattened player is hit by the Hammer, the system shall spawn 25 `minecraft:poof` particles in a circular burst pattern centered on the Target.

#### FR-20: Flattening Particles — Suppression on Subsequent Hits
When an already-flattened player is hit by the Hammer, the system shall NOT spawn particles.

#### FR-21: Source Tracking
The system shall record "Hammer" as the flattening source when flattening is triggered by a Hammer hit.

---

### 4.4 Flattened State Mechanics

#### FR-22: Hitbox Scaling
The system shall scale the flattened player's collision hitbox to match the visual Y-height (5% of normal).

#### FR-23: Camera Height Adjustment
The system shall adjust the first-person camera view height for flattened players to prevent camera clipping into the floor.

#### FR-24: Movement Immobilization
The system shall prevent flattened players from moving via standard movement inputs (WASD or equivalent).

#### FR-25: Jump Prevention
The system shall prevent flattened players from jumping.

#### FR-26: Sprint Prevention
The system shall prevent flattened players from sprinting.

#### FR-27: Block Interaction Prevention
The system shall prevent flattened players from interacting with blocks (placing, breaking, using).

#### FR-28: Item Holding Permitted
The system shall allow flattened players to hold items in their hand.

#### FR-29: Item Switching Permitted
The system shall allow flattened players to switch between hotbar items.

#### FR-30: Shared Mechanics Reference
The movement restrictions (FR-24 through FR-29) shall use the same implementation logic and configuration parameters as the existing anvil flattening mechanic.

---

### 4.5 Reform Mechanic

#### FR-31: Reform Keybind Registration
The system shall register a new keybind action named "Reform" in the Controls menu.

#### FR-32: Reform Keybind Default
The Reform keybind shall default to the Space key.

#### FR-33: Reform Keybind Rebindable
The system shall allow players to rebind the Reform keybind to any available key.

#### FR-34: Reform Trigger
When a flattened player presses the Reform keybind, the system shall initiate the reform process.

#### FR-35: Reform Animation
The system shall animate the player's scale from the flattened dimensions to normal (1.0, 1.0, 1.0) over the configured reform duration.

#### FR-36: Reform Animation Style
The reform animation shall use interpolation (not instant snap) to smoothly transition scale values.

#### FR-37: Reform Sound Suppression
The system shall NOT play a sound effect during or after the reform animation.

#### FR-38: Reform Particle Suppression
The system shall NOT spawn particles during or after the reform animation.

#### FR-39: Reform Completion — State Removal
Upon completion of the reform animation, the system shall remove the `FlattenedCapability` from the player.

#### FR-40: Reform Completion — Movement Restoration
Upon completion of the reform animation, the system shall restore all movement capabilities to the player.

---

### 4.6 State Persistence

#### FR-41: Logout Persistence
When a flattened player disconnects, the system shall serialize and persist the player's flattened state (including Spread Value and source) to the player's persistent data store.

#### FR-42: Login Restoration
When a player with persisted flattened state reconnects, the system shall deserialize and reapply the `FlattenedCapability` with the saved parameters.

#### FR-43: Death Reset
When a flattened player dies, the system shall remove the `FlattenedCapability` and shall NOT persist the flattened state.

#### FR-44: Respawn Normal State
When a player respawns after death, the system shall ensure the player is in a normal, unflattened state.

#### FR-45: Dimension Change Persistence
When a flattened player changes dimensions (via portal or teleportation), the system shall maintain the `FlattenedCapability` and all associated parameters.

---

### 4.7 Multi-Source Interaction

#### FR-46: Unified Spread Tracking
The system shall maintain a single, unified Spread Value per player regardless of the flattening source (Hammer or Anvil).

#### FR-47: Cross-Source Spread Stacking
When a player flattened by one source (e.g., Anvil) is hit by another source (e.g., Hammer), the system shall apply the hitting source's spread increment to the unified Spread Value.

#### FR-48: Source History Retention
The system shall retain a record of the most recent flattening source for potential future use (death messages, statistics).

---

## 5. Nonfunctional Requirements

### 5.1 Performance

#### NFR-1: Visual Synchronization Latency
**Attribute:** Responsiveness  
**Scale:** Time from server hit registration to client visual update  
**Meter:** Measured via network packet timestamps under controlled LAN conditions  
**Target:** ≤100ms (approximately 2 server ticks)  
**Constraint:** Must not exceed 150ms under normal network conditions

#### NFR-2: Concurrent Flattened Player Capacity
**Attribute:** Scalability  
**Scale:** Number of simultaneously flattened players  
**Meter:** Server TPS measured via `/tick` or equivalent while N players are flattened  
**Target:** 20 concurrent flattened players with TPS ≥19.0  
**Constraint:** TPS shall not drop below 18.0 with 20 concurrent flattened players

#### NFR-3: Memory Overhead per Flattened Player
**Attribute:** Resource Efficiency  
**Scale:** Additional heap memory consumed per flattened player  
**Meter:** JVM heap analysis before/after flattening N players  
**Target:** ≤1KB additional memory per flattened player  
**Constraint:** ≤5KB per flattened player

---

### 5.2 Compatibility

#### NFR-4: NeoForge Compatibility
**Attribute:** Interoperability  
**Requirement:** The Hammer feature shall function correctly on NeoForge version 21.1.214 for Minecraft 1.21.1.

#### NFR-5: BMC5 Modpack Compatibility
**Attribute:** Interoperability  
**Requirement:** The Hammer feature shall function correctly when installed alongside the Better Minecraft 5 (BMC5) modpack with no crashes or visual corruption.

#### NFR-6: Rendering Mod Coexistence
**Attribute:** Robustness  
**Requirement:** The Hammer feature shall not cause crashes when Sodium or Embeddium rendering mods are installed.  
**Note:** Explicit visual compatibility with these mods is not required for MVP.

---

### 5.3 Usability

#### NFR-7: Keybind Discoverability
**Attribute:** Learnability  
**Requirement:** The Reform keybind shall appear in the standard Minecraft Controls menu under an appropriate category (e.g., "Movement" or mod-specific category).

#### NFR-8: Visual Feedback Clarity
**Attribute:** Feedback  
**Requirement:** The flattening visual effect (model scaling) shall be clearly visible to both the affected player and all observers within render distance.

---

### 5.4 Maintainability

#### NFR-9: Configuration Centralization
**Attribute:** Modifiability  
**Requirement:** All Hammer-specific configurable parameters shall reside within a `[hammer]` subsection of the existing `toonflattening.toml` configuration file.

#### NFR-10: Code Reuse
**Attribute:** Modularity  
**Requirement:** The Hammer implementation shall reuse the existing `FlattenedCapability` infrastructure with no duplication of flattening state logic.

---

## 6. Configuration Schema

The following parameters shall be configurable within `toonflattening.toml`. Hammer-specific settings reside under the `[hammer]` section and reference shared values where applicable.

```toml
# ===========================================
# Cartoon Flattening Mod Configuration
# ===========================================

[flattening]
# Y-axis scale when flattened (0.0 - 1.0)
y_scale = 0.05

# Initial X/Z scale multiplier when first flattened
initial_spread = 2.0  # TBD: Confirm default with existing anvil config

# X/Z scale increment per subsequent hit
spread_increment = 0.5  # TBD: Confirm default with existing anvil config

# Maximum X/Z scale multiplier
max_spread = 5.0  # TBD: Confirm default with existing anvil config

# Reform animation duration in milliseconds
reform_duration_ms = 250  # TBD: Confirm default with existing anvil config

[hammer]
# Attack speed (hits per second equivalent; lower = slower)
attack_speed = 0.8

# Whether hammer hits deal HP damage (should remain false)
deals_damage = false

[particles]
# Number of poof particles on initial flatten
flatten_particle_count = 25
```

**Note:** Exact default values for shared parameters (`initial_spread`, `spread_increment`, `max_spread`, `reform_duration_ms`) should be aligned with the existing anvil flattening configuration. Values shown are placeholders pending confirmation.

---

## 7. Traceability Matrix

| Requirement | Use Case(s) | Business Requirement |
|-------------|-------------|----------------------|
| FR-1, FR-2, FR-3, FR-4, FR-5, FR-6, FR-7 | UC-3 | BR-2.1 |
| FR-8, FR-9, FR-10, FR-11, FR-12 | UC-1 | BR-2.1 |
| FR-13, FR-14, FR-15, FR-16 | UC-1 | BR-2.1 |
| FR-17, FR-18, FR-19, FR-20 | UC-1 | BR-2.1 |
| FR-21, FR-46, FR-47, FR-48 | UC-1 | BR-2.1 |
| FR-22, FR-23 | UC-1 | BR-2.2, NFR-8 |
| FR-24, FR-25, FR-26, FR-27, FR-28, FR-29, FR-30 | UC-1, UC-2 | BR-2.1 |
| FR-31, FR-32, FR-33, FR-34, FR-35, FR-36, FR-37, FR-38, FR-39, FR-40 | UC-2 | BR-2.1 |
| FR-41, FR-42 | UC-4 | BR-2.2 |
| FR-43, FR-44 | UC-5 | BR-2.1 |
| FR-45 | UC-4 | BR-2.2 |
| NFR-1, NFR-2 | UC-1 | BR-2.2, BR-2.3 |
| NFR-4, NFR-5, NFR-6 | All | BR-2.4 |
| NFR-9, NFR-10 | All | BR-2.1 |

---

## 8. Open Items

| ID | Description | Status | Owner |
|----|-------------|--------|-------|
| OPEN-1 | Confirm default values for `initial_spread`, `spread_increment`, `max_spread`, `reform_duration_ms` from existing anvil config | Pending | Developer |
| OPEN-2 | Determine appropriate Creative Tab placement for Hammer item | Pending | Developer |

---

## 9. Approval

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Stakeholder | | | |
| Developer | | | |

---

**Document History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0-DRAFT | 2025-01-01 | Business Analyst (Claude) | Initial draft |
