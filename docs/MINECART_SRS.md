# Software Requirements Specification
## Minecart Flattening Feature

## 1. Introduction

### 1.1 Purpose
This document defines the functional and nonfunctional requirements for the Minecart Flattening feature, an extension to the ToonFlattening mod that enables minecarts to flatten players upon collision.

### 1.2 Scope
This feature adds environmental/trap-based flattening to complement the existing Hammer-based player-versus-player flattening mechanic. It targets multiplayer roleplay servers seeking "classic cartoon railroad" interactions.

### 1.3 Definitions

| Term | Definition |
|------|------------|
| Flattened State | The existing player state in which the player model is scaled down on the Y-axis and outward on X/Z axes, producing a "pancake" appearance. |
| Spread Multiplier | A server-configurable parameter controlling how much flatter a player becomes upon repeated flattening. |
| Victim | The player entity subject to flattening. |
| Cart | Any vanilla minecart entity capable of triggering the flattening effect. |
| Driver | A player entity riding a minecart (passive role; no special controls). |

---

## 2. Business Requirements

### BR-1: Environmental Flattening Expansion
**Statement:** The mod shall support trap-based and environmental roleplay scenarios by enabling non-weapon flattening sources.

**Success Metric:** A player can construct a powered rail loop, stand on the track, be run over by an unpiloted minecart, and transition to the Flattened State while the minecart continues unimpeded.

### BR-2: Behavioral Consistency
**Statement:** Minecart flattening shall produce outcomes visually and mechanically consistent with existing Hammer and Anvil flattening systems.

---

## 3. User Requirements

### 3.1 Use Case: UC-1 — Player Flattened by Moving Minecart

**Primary Actor:** Victim (Player)  
**Secondary Actor:** Cart (Minecart entity)  
**Optional Actor:** Driver (Player riding cart)

**Preconditions:**
- Victim is in Survival or Adventure mode.
- Victim is not riding another entity.
- Cart velocity meets or exceeds the minimum threshold.
- PvP is enabled for the victim's location/context.

**Main Success Scenario:**
1. Cart travels along rail at or above minimum velocity.
2. Cart's bounding box intersects Victim's bounding box.
3. System validates Victim's Y-level is approximately equal to rail Y-level (grounded check).
4. System applies Flattened State to Victim (or increases spread if already flattened).
5. System plays `toonflattening:flatten` sound at Victim's location (first-time flattening only).
6. System temporarily disables Victim's collision for Cart, allowing pass-through.
7. Cart continues along rail with no momentum loss.
8. Victim recovers via existing recovery mechanic.

**Alternative Flows:**

| ID | Condition | Outcome |
|----|-----------|---------|
| UC-1-A | Cart velocity below threshold | No flattening; vanilla collision behavior applies. |
| UC-1-B | Victim is airborne (Y-level above rail) | No flattening; cart passes beneath. |
| UC-1-C | Victim lands on cart from above | Flattening triggers upon Y-level intersection. |
| UC-1-D | Victim in Creative or Spectator mode | No flattening; vanilla collision behavior applies. |
| UC-1-E | Victim riding another entity | No flattening; standard entity collision applies. |
| UC-1-F | PvP disabled for victim/region | No flattening; vanilla collision behavior applies. |
| UC-1-G | Victim already in Flattened State | Spread increases per global multiplier; no sound or particles. |

**Postconditions:**
- Victim is in Flattened State (or spread increased).
- Cart position and velocity are unchanged by collision.

---

### 3.2 User Story: US-1

**As a** roleplay server player,  
**I want** to be flattened when a minecart runs me over,  
**So that** I can participate in cartoon railroad trap scenarios without requiring another player to wield a weapon.

---

## 4. Functional Requirements

### 4.1 Collision Detection

**FR-1:** The system shall detect collision between a minecart entity and a player entity using bounding box intersection.

**FR-2:** The system shall validate that the player's Y-coordinate is within ±0.5 blocks of the rail block's Y-coordinate before triggering flattening.

**FR-3:** The system shall ignore player facing direction when evaluating collision eligibility.

### 4.2 Velocity Threshold

**FR-4:** The system shall enforce a minimum velocity threshold equal to 50% of the maximum speed achievable on a powered rail (approximately 4 blocks/second).

**FR-5:** The system shall apply the same velocity threshold regardless of whether the minecart is occupied or empty.

### 4.3 Flattening Behavior

**FR-6:** The system shall apply the standard Flattened State (vertical pancake: Y-scale reduced, X/Z-scale increased) upon valid collision.

**FR-7:** The system shall play the `toonflattening:flatten` sound event at the victim's location, audible to all players within 16 blocks, when flattening a player who is not already in the Flattened State.

**FR-8:** The system shall not play any sound effect when flattening a player who is already in the Flattened State.

**FR-9:** The system shall not generate any particle effects upon flattening.

**FR-10:** The system shall increase the victim's spread value by the global spread multiplier when the victim is already in the Flattened State.

### 4.4 Cart Behavior

**FR-11:** The system shall temporarily suppress the victim's collision box for the colliding minecart, allowing the cart to pass through without momentum loss.

**FR-12:** The system shall restore normal collision behavior for the victim after the cart has cleared the victim's position.

**FR-13:** The system shall not provide any feedback (visual, audio, or haptic) to a player riding the minecart upon flattening a victim.

### 4.5 Multi-Victim Handling

**FR-14:** The system shall evaluate and apply flattening independently for each player intersected by a minecart's path during a single traversal.

**FR-15:** The system shall not impose an artificial limit on the number of players flattened by a single minecart in one traversal.

### 4.6 Immunity Conditions

**FR-16:** The system shall not apply flattening to players in Creative mode. Vanilla collision behavior shall apply.

**FR-17:** The system shall not apply flattening to players in Spectator mode. Vanilla collision behavior shall apply.

**FR-18:** The system shall not apply flattening to players currently riding any entity (minecart, horse, pig, boat, etc.). Standard entity collision shall apply.

**FR-19:** The system shall not apply flattening when PvP is disabled for the victim's region or player context. Vanilla collision behavior shall apply.

### 4.7 Player State Handling

**FR-20:** The system shall apply flattening to players in sneaking/crouching state.

**FR-21:** The system shall apply flattening to players in swimming or crawling state.

### 4.8 Minecart Variant Support

**FR-22:** The system shall trigger flattening from all vanilla minecart variants:
- Minecart (rideable)
- Minecart with Chest
- Minecart with Hopper
- Minecart with Furnace
- Minecart with TNT
- Minecart with Command Block

**FR-23:** The system shall not suppress vanilla TNT Minecart behavior. If collision triggers TNT ignition per vanilla logic, the flattening shall occur immediately upon impact, prior to or concurrent with explosion.

### 4.9 Rail Independence

**FR-24:** The system shall evaluate flattening eligibility based solely on minecart velocity and player collision, independent of the rail block type beneath the minecart.

### 4.10 Recovery

**FR-25:** The system shall use the existing Flattened State recovery mechanic for players flattened by minecarts, with no modifications.

---

## 5. Nonfunctional Requirements

### 5.1 Performance

**NFR-1: Collision Processing Capacity**

| Attribute | Value |
|-----------|-------|
| Scale | Standard multiplayer environment (10–20 concurrent players) |
| Target | The system shall process at least 5 simultaneous minecart-player collision events within a single server tick. |
| Constraint | Processing 5 simultaneous events shall not introduce a tick lag spike exceeding 50 milliseconds. |
| Context | Industrial-scale minecart systems (100+ carts) are outside the performance guarantee scope. |

### 5.2 Compatibility

**NFR-2:** The system shall integrate with server PvP state APIs to respect region/player protection settings.

**NFR-3:** The system shall function on NeoForge 21.1.214 targeting Minecraft 1.21.1.

### 5.3 Maintainability

**NFR-4:** The system shall reuse the existing global spread multiplier configuration parameter rather than introducing a feature-specific parameter.

**NFR-5:** The system shall reuse the existing `toonflattening:flatten` sound asset rather than introducing a feature-specific sound.

---

## 6. Configuration Parameters

| Parameter | Scope | Notes |
|-----------|-------|-------|
| *(Global Spread Multiplier)* | Existing | Reused for re-flattening spread calculation. No new parameter introduced. |
| *(Velocity Threshold)* | Hard-coded | Fixed at ~50% max powered rail speed. Not configurable. |
| *(Feature Toggle)* | None | Feature is always enabled when mod is installed. |

---

## 7. Acceptance Test Scenarios

| ID | Scenario | Preconditions | Action | Expected Outcome | Status |
|----|----------|---------------|--------|------------------|--------|
| AT-1 | Standard flattening | Player in Survival on powered rail; empty cart at full speed | Cart collides with player | Player enters Flattened State; sound plays; cart continues unimpeded | Pending |
| AT-2 | Below velocity threshold | Player on rail; cart at <50% max speed | Cart contacts player | No flattening; vanilla push/stop behavior | Pending |
| AT-3 | Airborne evasion | Player on rail; cart approaching | Player jumps before collision | No flattening; cart passes beneath | Pending |
| AT-4 | Landing on cart | Cart moving at speed | Player falls onto cart path, lands at rail Y-level | Flattening triggers upon intersection | Pending |
| AT-5 | Re-flattening (spread) | Player already flattened on rail; cart approaching | Cart collides with flattened player | Spread increases; no sound; no particles | Pending |
| AT-6 | Creative immunity | Player in Creative on rail; cart at full speed | Cart collides with player | No flattening; vanilla collision | Pending |
| AT-7 | Mounted immunity | Player riding horse on rail; cart at full speed | Cart approaches mounted player | No flattening; standard entity collision | Pending |
| AT-8 | Multi-victim | Five players lined up on straight rail; cart at full speed | Cart traverses all five | All five flattened sequentially; cart continues | Pending |
| AT-9 | PvP disabled | Player on rail in PvP-disabled region; cart at full speed | Cart collides with player | No flattening; vanilla collision | Pending |
| AT-10 | TNT cart interaction | Player on rail; TNT cart at full speed (primed on impact) | Cart collides with player | Player flattened; TNT explodes per vanilla logic | Pending |