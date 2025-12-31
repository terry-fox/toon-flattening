# TODO: Minecart Flattening Feature

## Overview
Implement minecart collision flattening per MINECART_SRS.md. Minecarts at sufficient velocity flatten players on collision.

---

## Phase 1: Walking Skeleton

- [x] **1.1: Create AbstractMinecartMixin**
  - Create `src/main/java/com/terryfox/toonflattening/mixin/AbstractMinecartMixin.java`
  - Use `@Inject` at HEAD of collision loop in `tick()` method (lines 311-335)
  - Capture when minecart detects player in bounding box
  - Add log message for collision detection

- [x] **1.2: Add flattenWithMinecart() method**
  - Add to `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`
  - No damage, sound only on first flatten
  - Use 3-tick animation (fast)
  - Source: "MINECART"

- [x] **1.3: Create MinecartFlatteningHandler**
  - Create `src/main/java/com/terryfox/toonflattening/event/MinecartFlatteningHandler.java`
  - Method: `tryFlatten(AbstractMinecart cart, ServerPlayer victim)` returns boolean
  - Call `FlatteningStateController.flattenWithMinecart(victim)`

- [x] **1.4: Register mixin**
  - Update `src/main/resources/toonflattening.mixins.json`
  - Add `AbstractMinecartMixin` to mixins array

---

## Phase 2: Velocity Threshold (FR-4, FR-5)

- [x] **2.1: Implement velocity check**
  - In `MinecartFlatteningHandler.tryFlatten()`:
  - Calculate horizontal velocity: `cart.getDeltaMovement().horizontalDistance()`
  - Threshold constant: `0.2` blocks/tick (50% of max powered rail speed)
  - Return false if below threshold

---

## Phase 3: Y-Level Validation (FR-2)

- [x] **3.1: Implement grounded check**
  - Get cart rail block Y: `cart.blockPosition().getY()`
  - Compare with `victim.getY()`
  - Tolerance: `Math.abs(playerY - railY) <= 0.5`
  - Return false if player airborne

---

## Phase 4: Immunity Conditions (FR-16 to FR-19)

- [x] **4.1: Game mode check**
  - Check `victim.gameMode.getGameModeForPlayer()`
  - Return false if CREATIVE or SPECTATOR

- [x] **4.2: Riding entity check**
  - Check `victim.isPassenger()`
  - Return false if true

- [x] **4.3: PvP check**
  - Get server: `victim.getServer()`
  - Check: `server.isPvpAllowed()`
  - Return false if PvP disabled

---

## Phase 5: Cart Pass-Through (FR-11, FR-12)

- [x] **5.1: Permanent collision suppression**
  - Fixed `AbstractMinecartMixin`: allows flattened players in loop (for re-flattening)
  - Non-pushable players skipped UNLESS already flattened
  - Cart velocity always restored on flatten/re-flatten

---

## Phase 6: Sound and Spread Logic (FR-7, FR-8, FR-10)

- [x] **6.1: Sound/spread behavior implemented**
  - Sound only on first flatten ✓
  - Spread accumulates on re-flatten ✓
  - Particles suppressed for MINECART source ✓

---

## Phase 7: Edge Cases and Polish

- [x] **7.1: Multi-victim handling (FR-14, FR-15)**
  - Mixin processes ALL entities in collision list ✓
  - No artificial limits ✓

- [x] **7.2: All minecart variants (FR-22, FR-23)**
  - Mixin targets `AbstractMinecart` (all variants inherit) ✓
  - TNT cart: flattening occurs, vanilla TNT behavior unmodified ✓

- [x] **7.3: Player states (FR-20, FR-21)**
  - Y-level check uses feet position - works for all poses ✓
  - Sneaking/swimming/crawling all handled ✓

---

## Phase 8: Cooldown System

- [x] **8.1: Minecart flatten cooldown**
  - 1-second (20 tick) cooldown after each flatten ✓
  - Prevents spread accumulation from successive ticks ✓
  - Tracked via static Map<UUID, Long> ✓

---

## Phase 9: Max Spread Pass-Through Fix

- [x] **9.1: Fix cart stopping at max spread**
  - Cart now passes through flattened players even when tryFlatten() fails ✓
  - Velocity restored based on hitFlattenedPlayer flag, not flatten success ✓
  - Works for max spread, cooldown, and any other failure case ✓

---

## Design Decisions

1. **Damage:** No damage - flattening effect only
2. **Cart pass-through:** Flattened players never collide with minecarts (permanent)
3. **Velocity threshold:** All cart variants use 0.2 blocks/tick threshold

---

## Files to Create

- `src/main/java/com/terryfox/toonflattening/mixin/AbstractMinecartMixin.java`
- `src/main/java/com/terryfox/toonflattening/event/MinecartFlatteningHandler.java`

## Files to Modify

- `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`
- `src/main/resources/toonflattening.mixins.json`
