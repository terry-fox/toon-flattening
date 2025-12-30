# Hammer Feature Implementation TODO

## Overview
Add hammer weapon that flattens players on melee hit, integrating with existing `FlatteningStateController`.

---

## Phase 1: Walking Skeleton

### [ ] 1.1: Item Registration Infrastructure
- [ ] Create `ModItems.java` in `com.terryfox.toonflattening.registry`
- [ ] Add `DeferredRegister<Item>` pattern matching `SOUND_EVENTS` in `ToonFlattening.java`
- [ ] Register to mod event bus in `ToonFlattening` constructor

**Files:**
- NEW: `src/main/java/com/terryfox/toonflattening/registry/ModItems.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/ToonFlattening.java`

---

### [ ] 1.2: Hammer Item Class
- [ ] Create `HammerItem.java` extending `Item`
- [ ] Register as `toonflattening:hammer` in `ModItems`
- [ ] Add to vanilla Combat creative tab via `BuildCreativeModeTabContentsEvent`

**Files:**
- NEW: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/registry/ModItems.java`

---

### [ ] 1.3: Attack Detection Event Handler
- [ ] Create `HammerAttackHandler.java` event handler
- [ ] Subscribe to `LivingIncomingDamageEvent`
- [ ] Check: attacker holding hammer, target is player, PvP rules pass
- [ ] Check `player.getAttackStrengthScale() >= 1.0` (full-strength requirement)
- [ ] Register handler in `ToonFlattening` constructor

**Files:**
- NEW: `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/ToonFlattening.java`

---

### [ ] 1.4: Trigger Flattening
- [ ] Call `event.setCanceled(true)` in `HammerAttackHandler` (no damage)
- [ ] Call `FlatteningStateController.flattenWithHammer()` on valid hit
- [ ] Add `flattenWithHammer(ServerPlayer target)` overload in `FlatteningStateController`

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`

---

## Phase 2: Item Properties (FR-3 to FR-6)

### [ ] 2.1: Durability & Damage
- [ ] Override `getMaxDamage()` return 0 or use `Item.Properties.durability(-1)`
- [ ] Use `Item.Properties.attributes()` with `AttackDamageAttribute` = 0

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`

---

### [ ] 2.2: Attack Speed
- [ ] Set attack speed attribute via `Item.Properties.attributes()`
- [ ] Attack speed 0.8 = -3.2 from base (base is 4.0)

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`

---

### [ ] 2.3: Prevent Enchanting (FR-6)
- [ ] Override `isEnchantable()` return false
- [ ] Override `isBookEnchantable()` return false
- [ ] Override `supportsEnchantment()` return false (blocks `/enchant` command)
- [ ] Subscribe to `AnvilRepairEvent` to cancel hammer + enchanted book

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/item/HammerItem.java`
- NEW/MODIFY: Add anvil event handler if needed

---

## Phase 3: Flattening Mechanics (FR-13 to FR-21)

### [ ] 3.1: Source Tracking (FR-21, FR-48)
- [ ] Add `String flatteningSource` field to `FlattenedStateAttachment`
- [ ] Update CODEC serialization
- [ ] Set source in flatten method

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/attachment/FlattenedStateAttachment.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`

---

### [ ] 3.2: Initial vs Subsequent Hit Logic
- [ ] Check `FlatteningHelper.isFlattened(target)` in `HammerAttackHandler`
- [ ] If not flattened: initial flatten + sound + particles
- [ ] If flattened: increment spread only, no sound/particles
- [ ] Ensure spread respects max config

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/event/HammerAttackHandler.java`
- MODIFY: `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java`

---

### [ ] 3.3: Unified Spread Tracking (FR-46, FR-47)
- [ ] Verify existing `spreadLevel` works for both sources
- [ ] Ensure `incrementSpread()` is source-agnostic
- [ ] Both hammer/anvil handlers use same spread logic

**Files:**
- MODIFY: `src/main/java/com/terryfox/toonflattening/core/FlatteningStateController.java` (if needed)

---

## Phase 4: Crafting & Configuration

### [ ] 4.1: Crafting Recipe (FR-2)
- [ ] Create JSON recipe at `data/toonflattening/recipe/hammer.json`
- [ ] Pattern: ISI / _S_ / _S_ (I=iron_ingot, S=stick)
- [ ] Add recipe advancement (optional)

**Files:**
- NEW: `src/main/resources/data/toonflattening/recipe/hammer.json`
- NEW: `src/main/resources/data/toonflattening/advancement/recipes/hammer.json` (optional)

---

### [ ] 4.2: Configuration (NFR-9)
- [ ] Add `hammer.spreadIncrement` (double) to `ToonFlatteningConfig.java`
- [ ] Add `hammer.enabled` (boolean, default true)
- [ ] Wire config values to handler

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
