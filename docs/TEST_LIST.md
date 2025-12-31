# Test List: Minecart Flattening Feature

## Phase 1: Walking Skeleton

### 1.1: Mixin Collision Detection
- [x] **Manual:** Ride minecart into player, observe log output showing collision detected

### 1.2: flattenWithMinecart() Method
- [x] **Manual:** Verify method compiles and can be called from handler

### 1.3: MinecartFlatteningHandler
- [x] **Manual:** Place powered rail, send empty cart into standing player
- [x] **Expected:** Player enters Flattened State (no velocity/y-level checks yet)

---

## Phase 2: Velocity Threshold

### 2.1: Velocity Check
- [x] **Manual:** Push cart gently (< 4 blocks/sec)
- [x] **Expected:** No flattening occurs
- [x] **Manual:** Powered rail full speed (≥ 4 blocks/sec)
- [x] **Expected:** Flattening occurs

---

## Phase 3: Y-Level Validation

### 3.1: Grounded Check
- [ ] **Manual:** Jump over approaching cart
- [ ] **Expected:** No flattening (player airborne)
- [ ] **Manual:** Land on track as cart passes
- [ ] **Expected:** Flattening triggers upon Y-level intersection

---

## Phase 4: Immunity Conditions

### 4.1: Game Mode Check
- [ ] **Manual:** Switch to Creative mode, stand on rail with approaching cart
- [ ] **Expected:** No flattening, vanilla collision

### 4.2: Riding Entity Check
- [ ] **Manual:** Ride horse on rail, cart passes through
- [ ] **Expected:** No flattening, standard entity collision

### 4.3: PvP Check
- [ ] **Manual:** Set `pvp=false` in server.properties, cart collides with player
- [ ] **Expected:** No flattening, vanilla collision
- [ ] **Manual:** Set `pvp=true`, cart collides with player
- [ ] **Expected:** Flattening occurs

---

## Phase 5: Cart Pass-Through

### 5.1: Collision Suppression
- [ ] **Manual:** Observe cart speed before/after flattening player
- [ ] **Expected:** Cart continues at same velocity (no momentum loss)
- [ ] **Manual:** Send second cart through flattened player
- [ ] **Expected:** Second cart also passes through without collision

---

## Phase 6: Sound and Spread Logic

### 6.1: Sound/Spread Behavior
- [ ] **Manual:** First cart hits player
- [ ] **Expected:** Sound plays, player flattened
- [ ] **Manual:** Second cart hits same player
- [ ] **Expected:** No sound, player gets wider (spread increases)

---

## Phase 7: Edge Cases and Polish

### 7.1: Multi-Victim Handling
- [ ] **Manual:** Line up 5 players in a row on straight rail, send cart at full speed
- [ ] **Expected:** All 5 players flattened sequentially, cart continues unimpeded

### 7.2: All Minecart Variants
- [ ] **Manual:** Test with Minecart (rideable)
- [ ] **Expected:** Flattens player
- [ ] **Manual:** Test with Minecart with Chest
- [ ] **Expected:** Flattens player
- [ ] **Manual:** Test with Minecart with Hopper
- [ ] **Expected:** Flattens player
- [ ] **Manual:** Test with Minecart with Furnace
- [ ] **Expected:** Flattens player
- [ ] **Manual:** Test with Minecart with TNT (primed on impact)
- [ ] **Expected:** Player flattened, TNT explodes per vanilla logic

### 7.3: Player States
- [ ] **Manual:** Sneak on rail, cart approaches
- [ ] **Expected:** Player flattened
- [ ] **Manual:** Swim/crawl in water on rail, cart approaches
- [ ] **Expected:** Player flattened
- [ ] **Manual:** Crawl under 1-block gap near rail, cart approaches
- [ ] **Expected:** Player flattened

---

## SRS Acceptance Tests

### AT-1: Standard Flattening
- [ ] Player in Survival on powered rail; empty cart at full speed
- [ ] Cart collides with player
- [ ] **Expected:** Player enters Flattened State; sound plays; cart continues unimpeded

### AT-2: Below Velocity Threshold
- [ ] Player on rail; cart at <50% max speed
- [ ] Cart contacts player
- [ ] **Expected:** No flattening; vanilla push/stop behavior

### AT-3: Airborne Evasion
- [ ] Player on rail; cart approaching
- [ ] Player jumps before collision
- [ ] **Expected:** No flattening; cart passes beneath

### AT-4: Landing on Cart
- [ ] Cart moving at speed
- [ ] Player falls onto cart path, lands at rail Y-level
- [ ] **Expected:** Flattening triggers upon intersection

### AT-5: Re-flattening (Spread)
- [ ] Player already flattened on rail; cart approaching
- [ ] Cart collides with flattened player
- [ ] **Expected:** Spread increases; no sound; no particles

### AT-6: Creative Immunity
- [ ] Player in Creative on rail; cart at full speed
- [ ] Cart collides with player
- [ ] **Expected:** No flattening; vanilla collision

### AT-7: Mounted Immunity
- [ ] Player riding horse on rail; cart at full speed
- [ ] Cart approaches mounted player
- [ ] **Expected:** No flattening; standard entity collision

### AT-8: Multi-Victim
- [ ] Five players lined up on straight rail; cart at full speed
- [ ] Cart traverses all five
- [ ] **Expected:** All five flattened sequentially; cart continues

### AT-9: PvP Disabled
- [ ] Player on rail in PvP-disabled region; cart at full speed
- [ ] Cart collides with player
- [ ] **Expected:** No flattening; vanilla collision

### AT-10: TNT Cart Interaction
- [ ] Player on rail; TNT cart at full speed (primed on impact)
- [ ] Cart collides with player
- [ ] **Expected:** Player flattened; TNT explodes per vanilla logic
