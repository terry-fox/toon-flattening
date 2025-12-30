# Manual In-Game Test Plan

Tests for commits since dc28882 (4 commits: 90b62b1, f20f9bf, 797a14e, 74128e8)

## Prerequisites
- Get flattened by an anvil before each test group
- Have both survival and creative mode available
- Spawn various mobs for entity tests

---

## 1. No-Push When Flattened (90b62b1)

### 1.1 Entity Collision Push
- [x] While flattened, have a cow/pig walk into you - should NOT push you
- [x] While flattened, have a zombie walk into you - should NOT push you
- [x] While NOT flattened, entities should push you normally (control test)

### 1.2 Damage Knockback
- [x] While flattened, get hit by zombie - should take damage but NO knockback
- [x] While flattened, get hit by skeleton arrow - should take damage but NO knockback
- [x] While NOT flattened, attacks should knockback normally (control test)

### 1.3 External Forces Still Work
- [x] While flattened, get hit by explosion (TNT/creeper) - SHOULD move you
- [x] While flattened, get pushed by piston - SHOULD move you
- [x] While flattened, get pushed by minecart - should NOT move you

### 1.4 Initial Velocity Zero
- [x] Get flattened while moving - should stop immediately
- [x] After stopping, external forces (explosion) should still work

---

## 2. Entity Collision Prevention (f20f9bf)

### 2.1 Pass-Through Collision
- [x] While flattened, mobs should walk THROUGH you (not around)
- [x] While flattened, other players should walk through you (if multiplayer)
- [x] While NOT flattened, entities collide normally (control test)

### 2.2 Pushing Other Entities
- [x] While flattened, you shouldn't push mobs when they're on you
- [x] While NOT flattened, walking into mobs pushes them (control test)

### 2.3 Piston Still Works
- [x] Place piston next to flattened player - should push player normally

---

## 3. Anvil Break Prevention (797a14e)

### 3.1 Survival Mode - Cannot Break
- [x] While flattened (survival), try to break anvil - should NOT break
- [x] While flattened (survival), hold left click on anvil - no break animation/sound
- [x] While NOT flattened (survival), can break anvil normally (control test)

### 3.2 Creative Mode - Can Break
- [x] While flattened (creative), try to break anvil - SHOULD break instantly

### 3.3 Other Blocks
- [x] While flattened (survival), can break non-anvil blocks normally
- [x] Verify damaged anvil and chipped anvil also protected (uses BlockTags.ANVIL)

---

## 4. Suffocation & Cramming Prevention (74128e8)

### 4.1 Suffocation Damage
- [x] While flattened, push yourself into a wall - should NOT take suffocation damage
- [x] While flattened, piston pushes you into wall - should NOT take suffocation damage
- [x] While NOT flattened, suffocation works normally (control test)

### 4.2 Suffocation Overlay
- [x] While flattened inside a block, screen should NOT show red suffocation overlay
- [x] While NOT flattened inside a block, overlay shows normally (control test)

### 4.3 Cramming Damage
- [x] While flattened, spawn 24+ mobs in same space - should NOT take cramming damage
- [x] While NOT flattened, cramming damage works normally (control test)

---

## Test Commands Reference

```
/effect give @p minecraft:resistance 1000 255 true  # Temp invincibility for setup
/summon cow ~ ~ ~                                    # Spawn test mobs
/setblock ~ ~2 ~ minecraft:anvil                    # Place anvil above
/gamemode survival                                   # Switch modes
/gamemode creative
/summon tnt ~ ~ ~ {Fuse:40}                        # Explosion test
/gamerule maxEntityCramming 24                     # Verify cramming rule
```