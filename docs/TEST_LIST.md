# Minecart Flattening - Test List

## Acceptance Tests (from SRS)

| ID | Scenario | Preconditions | Action | Expected Outcome | Status |
|----|----------|---------------|--------|------------------|--------|
| AT-1 | Standard flattening | Player in Survival on powered rail; empty cart at full speed | Cart collides with player | Player enters Flattened State; sound plays; cart continues unimpeded | Pending |
| AT-2 | Below velocity threshold | Player on rail; cart at <50% max speed (~0.2 blocks/tick) | Cart contacts player | No flattening; vanilla push/stop behavior | Pending |
| AT-3 | Airborne evasion | Player on rail; cart approaching | Player jumps before collision | No flattening; cart passes beneath | Pending |
| AT-4 | Landing on cart | Cart moving at speed | Player falls onto cart path, lands at rail Y-level | Flattening triggers upon intersection | Pending |
| AT-5 | Re-flattening (spread) | Player already flattened on rail; cart approaching | Cart collides with flattened player | Spread increases; no sound; no particles | Pending |
| AT-6 | Creative immunity | Player in Creative on rail; cart at full speed | Cart collides with player | No flattening; vanilla collision | Pending |
| AT-7 | Mounted immunity | Player riding horse on rail; cart at full speed | Cart approaches mounted player | No flattening; standard entity collision | Pending |
| AT-8 | Multi-victim | Five players lined up on straight rail; cart at full speed | Cart traverses all five | All five flattened sequentially; cart continues | Pending |
| AT-9 | PvP disabled | Player on rail in PvP-disabled region; cart at full speed (any type) | Cart collides with player | No flattening; vanilla collision | Pending |
| AT-10 | TNT cart interaction | Player on rail; TNT cart at full speed (primed on impact) | Cart collides with player | Player flattened; TNT explodes per vanilla logic | Pending |

---

## Manual Test Scenarios (Development)

### Phase 1 Testing

#### MT-1: Detection Verification
**Setup:** Powered rail loop, empty cart at full speed
**Test:** Stand on track, observe console logs
**Expected:** Handler detects collision, logs output

#### MT-2: Velocity Threshold Boundary
**Setup:** Unpowered rail, cart coasting to slow speed
**Test:** Stand on track as cart decelerates
**Expected:** Flattening stops when speed < 0.2 blocks/tick

#### MT-3: Y-Level Grounded Check
**Setup:** Elevated rail, player standing below
**Test:** Cart passes overhead
**Expected:** No flattening (Y-level difference > 0.5)

#### MT-4: Sound Behavior - First Flatten
**Setup:** Survival mode, cart at full speed
**Test:** Stand on track, get hit
**Expected:** `toonflattening:flatten` sound plays, squash animation triggers

#### MT-5: Sound Behavior - Re-flatten
**Setup:** Already flattened player on track, cart approaching
**Test:** Cart hits again
**Expected:** Spread increases, NO sound, NO animation

#### MT-6: Cart Continues Through
**Setup:** Flattened player on track, cart at speed
**Test:** Cart collides
**Expected:** Cart passes through without momentum loss (existing mixin)

---

### Phase 2 Testing

#### MT-7: Spectator Immunity
**Setup:** Player in Spectator mode on track
**Test:** Cart collision
**Expected:** No flattening, normal spectator behavior

#### MT-8: Creative Immunity
**Setup:** Player in Creative mode on track
**Test:** Cart collision
**Expected:** No flattening, vanilla collision

#### MT-9: Mounted Player Immunity
**Setup:** Player riding horse on track
**Test:** Cart collision
**Expected:** No flattening, standard entity collision

#### MT-10: World PvP Disabled - Empty Cart
**Setup:** Server with `pvp=false` in server.properties; empty cart
**Test:** Cart collides with player
**Expected:** No flattening (respects world PvP setting)

#### MT-11: World PvP Enabled - Driver with Player PvP Denied
**Setup:** Server with `pvp=true`; driver in cart; victim has protection preventing driver damage
**Test:** Driven cart collides with protected player
**Expected:** No flattening (`driver.canHarmPlayer(victim)` returns false)

---

### Phase 3 Testing

#### MT-12: Multi-Victim Sequential
**Setup:** 5 players lined up on straight rail, cart at full speed
**Test:** Cart traverses entire line
**Expected:** All 5 flattened in sequence, cart continues

#### MT-13: TNT Cart Timing
**Setup:** TNT cart at full speed, player on track
**Test:** Collision triggers TNT
**Expected:** Player flattened, THEN/concurrent TNT explosion (check logs for timing)

#### MT-14: Cooldown Prevents Spam
**Setup:** Player on track, cart oscillating back/forth rapidly
**Test:** Cart passes over player multiple times within 1 second
**Expected:** Only one flatten event per ~20 tick window

#### MT-15: Sneaking Player
**Setup:** Player sneaking/crouching on track (FR-20)
**Test:** Cart collision
**Expected:** Flattening triggers (pose irrelevant)

#### MT-16: Swimming Player on Waterlogged Rail
**Setup:** Player swimming/crawling on waterlogged rail (FR-21)
**Test:** Cart collision
**Expected:** Flattening triggers

---

## Minecart Variant Tests (FR-22)

| Variant | Status |
|---------|--------|
| Empty Minecart (rideable) | Pending |
| Minecart with Chest | Pending |
| Minecart with Hopper | Pending |
| Minecart with Furnace | Pending |
| Minecart with TNT | Pending (see AT-10) |
| Minecart with Command Block | Pending |

**Test:** Each variant at full speed colliding with player should trigger flattening.

---

## Performance Tests (NFR-1)

#### PT-1: Simultaneous Collisions
**Setup:** 5 carts, 5 players, all collisions within same tick
**Test:** Measure tick lag spike
**Expected:** <50ms lag spike

#### PT-2: Industrial-Scale Disclaimer
**Setup:** 100+ cart system (e.g., item sorter)
**Test:** Performance observation
**Expected:** May degrade; outside performance guarantee scope per NFR-1

---

## Regression Tests

#### RT-1: Existing Hammer Flattening
**Test:** Hammer attack on player
**Expected:** Still works, no impact from minecart feature

#### RT-2: Existing Anvil Flattening
**Test:** Anvil drop on player
**Expected:** Still works, no impact from minecart feature

#### RT-3: Existing Recovery Mechanic
**Test:** Reformed from minecart flattening
**Expected:** Uses existing recovery mechanic (FR-25)

#### RT-4: Anvil Pinning (if source="MINECART")
**Test:** Minecart-flattened player under anvil
**Expected:** Pinning applies? OR pinning only for source="ANVIL"? (Clarify)

---

## Edge Case Tests

#### EC-1: Cart Removed Mid-Pass
**Setup:** Cart despawns/unloaded during pass-through
**Test:** Entity removed while passing through player
**Expected:** Graceful handling, no crash

#### EC-2: Player Dimension Change
**Setup:** Player flattened, immediately teleports to Nether
**Test:** State persistence
**Expected:** Flattened state preserved (attachment has `copyOnDeath()`)

#### EC-3: Cart Direction Irrelevant
**Setup:** Cart moving sideways/backwards relative to player
**Test:** Collision from any direction
**Expected:** Flattening triggers (velocity is scalar check, not directional)

#### EC-4: Player Logs Out While Flattened
**Setup:** Minecart-flattened player, logout, login
**Test:** State on reconnect
**Expected:** Still flattened (attachment persists)

---

## Mod Compatibility Tests

#### MC-1: Pehkui Integration
**Test:** Minecart flattening applies scale via Pehkui
**Expected:** Visual scale change matches anvil/hammer flattening

#### MC-2: Protection Plugin (e.g., WorldGuard-like)
**Test:** Cart in protected region with PvP disabled
**Expected:** Depends on plugin's API - may require additional integration

---

## Unit Tests (if test framework exists)

### Velocity Threshold
- `horizontalSpeed = 0.19` → no flatten
- `horizontalSpeed = 0.2` → flatten
- `horizontalSpeed = 0.21` → flatten

### Y-Level Boundary
- `|playerY - cartY| = 0.49` → flatten
- `|playerY - cartY| = 0.5` → flatten (inclusive)
- `|playerY - cartY| = 0.51` → no flatten

### Eligibility Predicate
- Survival, not mounted, PvP enabled → eligible
- Creative → not eligible
- Spectator → not eligible
- Mounted → not eligible
- PvP disabled → not eligible
