# Minecart Flattening Feature - Implementation Changelog

## Summary
Changes since commit 9e97e4dca9f0502ea1c1ef0f6948aa74b53eb3a6

## Feature Overview
Minecarts can now flatten players on collision at sufficient velocity. Flattened players become collision-passive, allowing the cart to pass through without momentum loss.

---

## Phase 1: SRS & Planning (5f29c21)

**Created:** `docs/MINECART_SRS.md` - Formal requirements specification

**Key Requirements:**
- Minecarts flatten players on collision at ≥0.1 blocks/tick relative velocity
- Cart passes through flattened players without momentum loss
- Y-level validation: player must be grounded (±0.5 blocks of rail level)
- Immunity conditions: Creative/Spectator mode, riding entity, PvP disabled
- Re-flattening increases spread per global multiplier

---

## Phase 2: Core Implementation (3366029)

**New Files:**
- `AbstractMinecartMixin.java` - Minecart tick hooks
- `MinecartFlatteningHandler.java` - Flattening validation logic

**Modified:**
- `FlatteningStateController.java` - Added `flattenWithMinecart()` method
- `toonflattening.mixins.json` - Registered `AbstractMinecartMixin`

### Implementation Decisions

#### HEAD/TAIL Velocity Pattern
```java
@Inject(method = "tick", at = @At("HEAD"))
// Save velocity before collision processing

@Inject(method = "tick", at = @At("TAIL"))
// Detect collision, restore velocity if flattened player hit
```

**Reason:** Cart loses momentum during vanilla collision processing. Must capture original velocity at tick start, then restore at tick end for pass-through behavior.

#### Server-Side Only
All checks skip `isClientSide`.

**Reason:** Flattening state is server-authoritative. Client receives sync packets.

---

## Phase 3: Relative Velocity Check (351de82)

**Modified:** `MinecartFlatteningHandler.java`, `AbstractMinecartMixin.java`

### Added Validation Checks

#### 1. Relative Velocity Threshold (0.1 blocks/tick)
```java
double relativeSpeed = cartSpeed - playerSpeedAlongCart;
if (relativeSpeed < VELOCITY_THRESHOLD) return false;
```

**Reason:** Use relative velocity instead of absolute. Player running same direction as cart shouldn't be flattened. Player running toward cart should be flattened more easily.

#### 2. Direction Check (dot product > 0.5)
```java
double dot = cartVelocity.normalize().dot(toPlayer.normalize());
if (dot < DIRECTION_THRESHOLD) return false;
```

**Reason:** Player must be in front of cart's movement (~60° cone). Prevents flattening when cart backs up or sideswipes.

---

## Phase 4: Y-Level & Immunity Checks (91340bc)

**Modified:** `MinecartFlatteningHandler.java`

### Added Validations

1. **Y-tolerance ±0.5 blocks** - Player must be at rail level (grounded)
2. **Game mode check** - Skip Creative/Spectator
3. **Passenger check** - Skip if riding any entity
4. **PvP check** - Skip if server disables PvP
5. **Cooldown (20 ticks)** - Prevent rapid re-flattening spam

**Reason:** Match SRS requirements FR-16 through FR-19. Respect game mode/server settings for player protection.

---

## Phase 5-9: Cart Pass-Through & Collision Fixes (4090170)

### Problem 1: Cart Stopped on First Flatten

**Symptom:** Cart stopped when flattening player for first time. Velocity restored only when hitting already-flattened players.

**Root Cause:** `tryFlatten()` success didn't set `hitFlattenedPlayer` flag.

```java
// BEFORE (line 54-57)
if (alreadyFlattened) {
    hitFlattenedPlayer = true;
}
// Missing: check tryFlatten() return value
```

**Fix (AbstractMinecartMixin.java:60):**
```java
// Try to flatten - restore velocity on success OR if already flattened
if (MinecartFlatteningHandler.tryFlatten(self, player, toonflattening$savedVelocity)) {
    hitFlattenedPlayer = true;
}
```

---

### Problem 2: Cart Still Stopped After Fix

**Symptom:** Same behavior persisted. Cart stops, plays rail sound indefinitely.

**Root Cause:** `LivingEntity.isPushable()` overrides `Entity.isPushable()` without calling super.

```java
// LivingEntity.java:3071
@Override
public boolean isPushable() {
    return this.isAlive() && !this.isSpectator() && !this.onClimbable();
}
```

PlayerPushMixin targeted `Entity.class`, so mixin never executed for players.

**Fix:** Created `LivingEntityPushMixin.java`

**New File:** `LivingEntityPushMixin.java` targeting `LivingEntity.class`
```java
@Mixin(LivingEntity.class)
@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
    Entity entity = (Entity) (Object) this;
    FlattenedStateAttachment.ifFlattened(entity, () -> cir.setReturnValue(false));
}
```

**Reason:** LivingEntity overrides Entity.isPushable() without super call. Need separate mixin to intercept at correct level.

---

### Problem 3: Player-to-Player Collision

**Symptom 1:** Walking player slows down when walking over flattened player
**Symptom 2:** Flattened player pushed away when stood on

**Root Cause Analysis:**

#### Movement Collision Path
```
Entity.move() → collide() → level.getEntityCollisions(this, aabb)
  → EntityGetter.getEntityCollisions() uses predicate:
    EntitySelector.NO_SPECTATORS.and(p_entity::canCollideWith)
  → Calls WalkingPlayer.canCollideWith(FlattenedPlayer)
```

#### Push System Path
```
FlattenedPlayer.pushEntities() (still runs - is LivingEntity)
  → Gets WalkingPlayer via pushableBy filter
  → FlattenedPlayer.doPush(WalkingPlayer)
  → WalkingPlayer.push(FlattenedPlayer)
```

**Issue:** Mixins only checked if `self` was flattened, not if `entity` parameter was flattened.

**Fixes:**

#### Fix 3a: canCollideWith - Check Both Entities (PlayerPushMixin.java:52)
```java
@Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
private void onCanCollideWith(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    Entity self = (Entity) (Object) this;
    FlattenedStateAttachment.ifFlattened(self, () -> cir.setReturnValue(false));
    // Also prevent collision with flattened entities
    FlattenedStateAttachment.ifFlattened(entity, () -> cir.setReturnValue(false));
}
```

**Reason:** Movement collision checks `canCollideWith(target)`. Must check if target is flattened, not just self.

#### Fix 3b: push(Entity) - Bidirectional Check (PlayerPushMixin.java:35)
```java
@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
private void onPushByEntity(Entity entity, CallbackInfo ci) {
    Entity self = (Entity) (Object) this;
    // Cancel if self is flattened (don't get pushed)
    FlattenedStateAttachment.ifFlattened(self, () -> ci.cancel());
    // Cancel if pusher is flattened (flattened entities can't push)
    FlattenedStateAttachment.ifFlattened(entity, () -> ci.cancel());
}
```

**Reason:** `push()` is bidirectional. Must prevent push when EITHER entity is flattened.

#### Fix 3c: doPush - Prevent Initiation (LivingEntityPushMixin.java:28)
```java
@Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
private void onDoPush(Entity entity, CallbackInfo ci) {
    Entity self = (Entity) (Object) this;
    // Flattened entities shouldn't initiate pushes
    FlattenedStateAttachment.ifFlattened(self, () -> ci.cancel());
}
```

**Reason:** `LivingEntity.pushEntities()` calls `doPush()` for each nearby entity. Flattened players must not initiate pushes on others.

---

## Final State: Collision-Passive Flattened Players

Flattened players are now completely passive:
- ✅ Don't get pushed by entities
- ✅ Can't push other entities
- ✅ Don't affect entity movement (walkable like flat sticker)
- ✅ Minecarts pass through without momentum loss

---

## Files Modified

| File | Lines Changed | Purpose |
|------|---------------|---------|
| `AbstractMinecartMixin.java` | +71 | HEAD/TAIL velocity save/restore, collision detection |
| `MinecartFlatteningHandler.java` | +114 | tryFlatten() validation: velocity, direction, Y-level, immunity |
| `LivingEntityPushMixin.java` | +13 | isPushable() + doPush() mixins for flattened state |
| `PlayerPushMixin.java` | +5 | Bidirectional checks in push() and canCollideWith() |
| `FlatteningStateController.java` | +21 | flattenWithMinecart() method |
| `toonflattening.mixins.json` | +1 | Register AbstractMinecartMixin |

---

## Documentation Created

- `docs/MINECART_SRS.md` - Requirements specification
- `docs/TODO.md` - Implementation task tracking
- `docs/TEST_LIST.md` - Test case tracking
