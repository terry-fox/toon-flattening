# Hammer Feature Implementation TODO

## Overview
Add hammer weapon that flattens players on melee hit, integrating with existing `FlatteningStateController`.

---

## Phase 1: Walking Skeleton

### [x] 1.1: Item Registration Infrastructure
- [x] Create `ModItems.java` in `com.terryfox.toonflattening.registry`
- [x] Add `DeferredRegister<Item>` pattern matching `SOUND_EVENTS` in `ToonFlattening.java`
- [x] Register to mod event bus in `ToonFlattening` constructor

**Files:**
- NEW: `src/main/java/com/terryfox/toonflattening/registry/ModItems.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/ToonFlattening.java`

---

### [x] 1.2: Hammer Item Class
- [x] Create `HammerItem.java` extending `Item`
- [x] Register as `toonflattening:hammer` in `ModItems`
- [x] Add to vanilla Combat creative tab via `BuildCreativeModeTabContentsEvent`

**Files:**
- NEW: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/registry/ModItems.java`

---

### [x] 1.3: Attack Detection Event Handler
- [x] Create `HammerAttackHandler.java` event handler
- [x] Subscribe to `LivingIncomingDamageEvent`
- [x] Check: attacker holding hammer, target is player, PvP rules pass
- [x] Check `player.getAttackStrengthScale() >= 1.0` (full-strength requirement)
- [x] Register handler in `ToonFlattening` constructor

**Files:**
- NEW: `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/ToonFlattening.java`

---

### [x] 1.4: Trigger Flattening
- [x] Call `event.setCanceled(true)` in `HammerAttackHandler` (no damage)
- [x] Call `FlatteningStateController.flattenWithHammer()` on valid hit
- [x] Add `flattenWithHammer(ServerPlayer target)` overload in `FlatteningStateController`

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`

---

## Phase 2: Item Properties (FR-3 to FR-6)

### [x] 2.1: Durability & Damage
- [x] Override `getMaxDamage()` return 0 or use `Item.Properties.durability(-1)`
- [x] Use `Item.Properties.attributes()` with `AttackDamageAttribute` = 0

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`

---

### [x] 2.2: Attack Speed
- [x] Set attack speed attribute via `Item.Properties.attributes()`
- [x] Attack speed 0.8 = -3.2 from base (base is 4.0)

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`

---

### [x] 2.3: Prevent Enchanting (FR-6)
- [x] Override `isEnchantable()` return false
- [x] Override `isBookEnchantable()` return false
- [x] Override `supportsEnchantment()` return false (blocks `/enchant` command)
- [x] Subscribe to `AnvilRepairEvent` to cancel hammer + enchanted book

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`
- NEW/MODIFY: Add anvil event handler if needed

---

## Phase 3: Flattening Mechanics (FR-13 to FR-21)

### [x] 3.1: Source Tracking (FR-21, FR-48)
- [x] Add `String flatteningSource` field to `FlattenedStateAttachment`
- [x] Update CODEC serialization
- [x] Set source in flatten method

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/attachment/FlattenedStateAttachment.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/network/SyncFlattenStatePayload.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`

---

### [x] 3.2: Initial vs Subsequent Hit Logic
- [x] Check `FlatteningHelper.isFlattened(target)` in `HammerAttackHandler`
- [x] If not flattened: initial flatten + sound + particles
- [x] If flattened: increment spread only, no sound/particles
- [x] Ensure spread respects max config

**Files:**
- Already implemented in `flattenWithHammer()` (Phase 1.4)

---

### [x] 3.3: Unified Spread Tracking (FR-46, FR-47)
- [x] Verify existing `spreadLevel` works for both sources
- [x] Ensure `incrementSpread()` is source-agnostic
- [x] Both hammer/anvil handlers use same spread logic

**Files:**
- Already implemented - single `spreadLevel` field used by both sources

---

## Phase 4: Crafting & Configuration

### [x] 4.1: Crafting Recipe (FR-2)
- [x] Create JSON recipe at `data/toonflattening/recipe/hammer.json`
- [x] Pattern: ISI / _S_ / _S_ (I=iron_ingot, S=stick)
- [x] Add recipe advancement (optional)

**Files:**
- NEW: `src/main/resources/data/toonflattening/recipe/hammer.json`
- NEW: `src/main/resources/data/toonflattening/advancement/recipes/hammer.json` (optional)

---

### [x] 4.2: Configuration (NFR-9)
- [x] Add `hammer.spreadIncrement` (double) to `ToonFlatteningConfig.java`
- [x] Add `hammer.enabled` (boolean, default true)
- [x] Wire config values to handler

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/config/ToonFlatteningConfig.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`

---

## Phase 5: Assets & Polish

### [ ] 5.1: Item Model & Texture
- [ ] Create item model JSON
- [ ] Create texture PNG (16x16 placeholder)
- [ ] Add lang entry "item.toonflattening.hammer"

**Files:**
- NEW: `src/main/resources/assets/toonflattening/models/item/hammer.json`
- NEW: `src/main/resources/assets/toonflattening/textures/item/hammer.png`
- MODIFY: `src/main/resources/assets/toonflattening/lang/en_us.json`

---

## Phase 6: Testing & Validation

### [ ] 6.1: Unit Tests
- [ ] `FlattenedStateAttachmentTest`: Source field serialization
- [ ] `HammerItemTest`: Item properties (if testable)

**Files:**
- NEW/MODIFY: `src/test/java/.../FlattenedStateAttachmentTest.java`

---

### [ ] 6.2: Integration Tests
- [ ] Hammer attack triggers flatten
- [ ] Spread stacking across sources
- [ ] Config values respected
- [ ] State persists across logout

---

## Risks & Spikes

| Risk | Mitigation |
|------|------------|
| PvP detection | Spike: `LivingIncomingDamageEvent` + `ServerPlayer.canHarmPlayer()` |
| Attack cooldown enforcement | Investigate `Player.getAttackStrengthScale()` |
| Enchant anvil event | Spike: Find correct NeoForge event |

---

## Decisions

- **Attack cooldown:** Full-strength only (`getAttackStrengthScale() >= 1.0`)
- **Creative tab:** Vanilla Combat tab
- **Damage handling:** Cancel damage event (no hurt anim)
- **Texture:** Placeholder OK
- **Enchant blocking:** Include `/enchant` command via `supportsEnchantment()`
