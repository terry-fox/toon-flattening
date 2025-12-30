# Minecart Flattening - Implementation TODO

## Overview
Add minecart-triggered player flattening per `MINECART_SRS.md`. Reuses existing `FlatteningStateController` + collision mixin infrastructure.

**Key Architecture Note:** `PlayerPushMixin` already suppresses collision for flattened players (including minecarts). Pass-through is automatic once flattened.

---

## Phase 1: Walking Skeleton (Core Detection + Flattening)

### [ ] Step 1.1: Create MinecartFlatteningHandler

**Goal:** Detect minecart-player collisions via tick event.

**Files to Create:**
- `src/main/java/com/terryfox/toonflattening/event/MinecartFlatteningHandler.java`

**Implementation:**
- Subscribe to `ServerTickEvent.Post` (or `EntityTickEvent.Post` for `AbstractMinecart`)
- Iterate all `AbstractMinecart` instances in loaded levels
- Check velocity threshold: `horizontalSpeed >= 0.2 blocks/tick` (~4 b/s)
  ```java
  double horizontalVelocity = Math.sqrt(
      cart.getDeltaMovement().x * cart.getDeltaMovement().x +
      cart.getDeltaMovement().z * cart.getDeltaMovement().z
  );
  if (horizontalVelocity < 0.2) return;
  ```
- Check bounding box intersection with players:
  ```java
  AABB cartBox = cart.getBoundingBox();
  List<Player> nearbyPlayers = cart.level().getEntitiesOfClass(
      Player.class, cartBox.inflate(0.5), this::isEligibleVictim
  );
  ```
- Check Y-level grounded: `Math.abs(player.getY() - cart.getY()) <= 0.5`

**Acceptance Criteria:**
- AT-1: Cart at full speed + standing player → handler detects collision
- AT-2: Cart below threshold → no detection
- AT-3: Airborne player → no detection

---

### [ ] Step 1.2: Add flattenWithMinecart() to FlatteningStateController

**Goal:** Entry point for minecart flattening with correct sound behavior (no damage).

**Files to Modify:**
- `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`

**Implementation:**
- Add `flattenWithMinecart(ServerPlayer player, AbstractMinecart cart)` method
- Add `canFlattenWithMinecart(ServerPlayer player, AbstractMinecart cart)` helper:
  - Check `!player.isSpectator()` (FR-17)
  - Check `!player.isCreative()` (FR-16)
  - Check `!player.isPassenger()` (FR-18)
  - PvP check (see Step 2.1)
- Logic flow:
  ```java
  if (!canFlattenWithMinecart(player, cart)) return;

  FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
  if (state.isFlattened()) {
      silentSpread(player);  // FR-10: increase spread, no sound
  } else {
      double spreadToAdd = ToonFlatteningConfig.CONFIG.spreadMultiplier.get();
      applyFlatteningState(player, spreadToAdd, "MINECART", 5);
      playFlattenSound(player);  // FR-7: sound only on first flatten
  }
  ```
- **No damage dealt** (visual effect only per design decision)

**Acceptance Criteria:**
- First flatten: sound plays, squash animation triggers
- Already flattened: spread increases, no sound (FR-8)

---

### [ ] Step 1.3: Wire Detection to Flattening

**Goal:** Connect handler to controller and register event.

**Files to Modify:**
- `MinecartFlatteningHandler.java` - call `FlatteningStateController.flattenWithMinecart()`
- `src/main/java/com/terryfox/toonflattening/ToonFlattening.java` - register handler

**Implementation:**
- In handler, iterate detected players and call `FlatteningStateController.flattenWithMinecart(player, cart)`
- In `ToonFlattening.java`:
  ```java
  NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, MinecartFlatteningHandler::onServerTick);
  ```

**Acceptance Criteria:**
- AT-1: Standard flattening works end-to-end
- Cart continues through flattened player (existing mixin handles collision suppression)

---

## Phase 2: Immunity Conditions

### [ ] Step 2.1: Implement Eligibility Checks

**Goal:** Prevent flattening ineligible players, respect PvP settings.

**Files to Modify:**
- `FlatteningStateController.java` - extend `canFlattenWithMinecart()`

**Implementation:**
- Basic immunity checks (already in Step 1.2):
  - `player.isSpectator()` → return false
  - `player.isCreative()` → return false
  - `player.isPassenger()` → return false
- **PvP check (ALL carts, occupied or not):**
  ```java
  // Check world-level PvP setting
  if (!player.level().getServer().isPvpAllowed()) return false;

  // If cart has driver, also check player-specific PvP
  Entity firstPassenger = cart.getFirstPassenger();
  if (firstPassenger instanceof ServerPlayer driver) {
      if (!driver.canHarmPlayer(player)) return false;
  }
  // Empty cart allowed if world PvP enabled
  return true;
  ```

**Rationale:** Empty carts can be manipulated (pushed) to grief in safe zones. PvP must be respected for all minecarts.

**Acceptance Criteria:**
- AT-6: Creative player not flattened
- AT-7: Mounted player not flattened
- AT-9: PvP-disabled region not flattened (any cart type)

---

## Phase 3: Edge Cases & Polish

### [ ] Step 3.1: Multi-Victim Handling

**Goal:** Flatten all players in cart path (FR-14, FR-15).

**Files to Modify:**
- `MinecartFlatteningHandler.java` - iteration logic

**Implementation:**
- Already handled by iterating all intersecting players in `getEntitiesOfClass()`
- Call `flattenWithMinecart()` for each eligible player
- No artificial limit on player count

**Acceptance Criteria:**
- AT-8: 5 players lined up in row → all 5 flatten sequentially

---

### [ ] Step 3.2: TNT Cart Interaction

**Goal:** Verify TNT cart behavior preserved (FR-23).

**Implementation:**
- No special code needed - flattening triggers on collision, TNT explosion is independent vanilla behavior
- Verify execution order: flatten occurs before/concurrent with explosion

**Acceptance Criteria:**
- AT-10: Player flattened, then TNT explodes per vanilla logic

---

### [ ] Step 3.3: Collision Cooldown (Prevent Re-trigger)

**Goal:** Prevent same cart from flattening same player multiple times during single pass.

**Files to Create/Modify:**
- `MinecartFlatteningHandler.java` - add cooldown tracking

**Implementation:**
- Add field: `private static final Map<Pair<UUID,UUID>, Long> flattenCooldowns = new ConcurrentHashMap<>();`
  - Key: (playerUUID, cartUUID)
  - Value: lastFlattenTick
- Before flattening, check: `if (currentTick - lastFlattenTick < 20) skip;`
- After flattening, record: `flattenCooldowns.put(pair, currentTick);`
- Periodically clean stale entries (e.g., remove entries older than 100 ticks)

**Alternative:** Use simpler approach - track which carts have already processed this tick, clear map each tick.

**Acceptance Criteria:**
- Single cart pass = single flatten event (no spam)

---

## Files Summary

### Files to CREATE:
- `src/main/java/com/terryfox/toonflattening/event/MinecartFlatteningHandler.java`

### Files to MODIFY:
- `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`
- `src/main/java/com/terryfox/toonflattening/ToonFlattening.java`

### Reference Files (read-only):
- `src/main/java/com/terryfox/toonflattening/mixin/PlayerPushMixin.java`
- `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`
- `src/main/java/com/terryfox/toonflattening/event/FlatteningHandler.java`

---

## Design Decisions (Resolved)

1. **Empty cart PvP:** Block flattening in PvP-disabled regions for ALL carts (prevent griefing loophole via cart pushing)
2. **Damage:** No damage (0) - visual effect only
3. **Config toggle:** None - always enabled per SRS
4. **Event type:** ServerTickEvent.Post or EntityTickEvent.Post for AbstractMinecart
5. **Pass-through:** Reuse existing PlayerPushMixin - automatic collision suppression for flattened players

---

## Risks & Known Issues

1. **Event priority:** May need HIGH priority to flatten before vanilla collision resolves
2. **Performance:** Iterating all minecarts per tick acceptable for 10-20 players; may need spatial optimization for industrial scales (100+ carts)
3. **Mod compatibility:** Protection mods may have different PvP APIs beyond `isPvpAllowed()` - may require additional integration hooks
