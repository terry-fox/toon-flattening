# Hammer Feature Test List

## Phase 1: Walking Skeleton

### 1.1: Item Registration Infrastructure
- [ ] **Manual:** Launch game, verify no crashes
- [ ] **Manual:** `/registries` shows `toonflattening` items registry

**AC:**
- Mod loads without errors
- Items registry exists

---

### 1.2: Hammer Item Class
- [ ] **Manual:** `/give @s toonflattening:hammer` works
- [ ] **Manual:** Item appears in creative inventory (Combat tab)

**AC:**
- Command gives hammer item
- Item visible in creative

---

### 1.3: Attack Detection Event Handler
- [ ] **Manual:** Enable PvP, hit player with hammer, verify console log
- [ ] **Manual:** Hit mob with hammer, verify no log
- [ ] **Manual:** Hit block with hammer, verify no log

**AC:**
- Console log on player hit only
- No log for mobs/blocks

---

### 1.4: Trigger Flattening
- [ ] **Manual:** Two players, attacker hits target with hammer, verify visual flattening
- [ ] **Manual:** Verify target Y-scale reduces to ~5%
- [ ] **Manual:** Verify target X/Z scale increases

**AC:**
- Hitting player causes visible flattening
- Target player scales down on Y-axis

---

## Phase 2: Item Properties

### 2.1: Durability & Damage
- [ ] **Manual:** Use hammer repeatedly (20+ attacks), verify no durability loss
- [ ] **Manual:** `/attribute @s minecraft:generic.attack_damage` shows 0 bonus
- [ ] **Manual:** Verify no durability bar appears on item

**AC:**
- No durability bar
- 0 attack damage

---

### 2.2: Attack Speed
- [ ] **Manual:** Compare hammer swing speed to iron axe (should match)
- [ ] **Manual:** Verify cooldown indicator visible on attack
- [ ] **Manual:** Measure time between full-strength attacks (~1.25s)

**AC:**
- Attack cooldown matches iron axe
- Cooldown indicator visible

---

### 2.3: Prevent Enchanting
- [ ] **Manual:** Place hammer in enchanting table, verify no enchants available
- [ ] **Manual:** Anvil with hammer + enchanted book, verify rejection
- [ ] **Manual:** Hold hammer, `/enchant @s minecraft:sharpness 5`, verify failure
- [ ] **Manual:** Grindstone with enchanted hammer (if enchanted via other means), verify behavior

**AC:**
- Enchanting table: no enchants
- Anvil: rejects enchanted book
- `/enchant` command fails

---

## Phase 3: Flattening Mechanics

### 3.1: Source Tracking
- [ ] **Unit:** Serialize `FlattenedStateAttachment` with source field, deserialize, verify intact
- [ ] **Manual:** Flatten with hammer, `/data get entity @s`, verify source = "Hammer"
- [ ] **Manual:** Flatten with hammer, logout, login, verify source persists

**AC:**
- Attachment stores "Hammer" or "Anvil"
- Source persists across logout/login

---

### 3.2: Initial vs Subsequent Hit Logic
- [ ] **Manual:** First hammer hit on unflatted player, verify sound plays
- [ ] **Manual:** First hammer hit, verify 25 poof particles appear
- [ ] **Manual:** Second hammer hit on already-flattened player, verify no sound
- [ ] **Manual:** Second hammer hit, verify no particles
- [ ] **Manual:** Second hit, verify spread increases
- [ ] **Manual:** Hit until max spread, verify spread stops increasing

**AC:**
- First hit: sound + 25 poof particles
- Subsequent hits: spread only, no sound/particles
- Spread capped at max

---

### 3.3: Unified Spread Tracking
- [ ] **Manual:** Flatten player with anvil (spread = X)
- [ ] **Manual:** Hit same player with hammer, verify spread = X + hammer increment
- [ ] **Manual:** Flatten player with hammer, hit with anvil, verify cumulative spread
- [ ] **Manual:** Verify visual scale matches combined spread value

**AC:**
- Anvil flatten + hammer hit = cumulative spread
- Hammer flatten + anvil hit = cumulative spread

---

## Phase 4: Crafting & Configuration

### 4.1: Crafting Recipe
- [ ] **Manual:** Open crafting table, place 2 iron ingots (top-left, top-right) + 3 sticks (middle column)
- [ ] **Manual:** Verify hammer appears in output slot
- [ ] **Manual:** Craft hammer, verify consumed ingredients
- [ ] **Manual:** Check recipe book after obtaining iron/sticks, verify recipe unlocks

**AC:**
- Crafting table shows hammer with correct ingredients
- Recipe book unlocks

---

### 4.2: Configuration
- [ ] **Manual:** Launch game, verify `config/toonflattening.toml` contains `[hammer]` section
- [ ] **Manual:** Verify default `spreadIncrement` value present
- [ ] **Manual:** Verify default `enabled = true` present
- [ ] **Manual:** Set `enabled = false`, reload, hit player with hammer, verify no flatten
- [ ] **Manual:** Set custom `spreadIncrement`, reload, verify spread matches config

**AC:**
- Config file shows `[hammer]` section
- Changing config affects behavior

---

## Phase 5: Assets & Polish

### 5.1: Item Model & Texture
- [ ] **Manual:** Give hammer via command, view in inventory, verify custom texture visible
- [ ] **Manual:** Hover over hammer, verify tooltip shows "Hammer"
- [ ] **Manual:** Hold hammer in hand, verify model renders correctly (if 3D)
- [ ] **Manual:** Drop hammer as entity, verify texture appears on ground

**AC:**
- Item displays custom texture
- Tooltip shows "Hammer" name

---

## Phase 6: Integration Tests

### 6.1: Full Flow Tests
- [ ] **Integration:** Craft hammer in survival, attack player, verify flatten
- [ ] **Integration:** Flattened player presses reform key, verify unflattens
- [ ] **Integration:** Hammer attack + reform + second attack, verify re-flattening works
- [ ] **Integration:** Flattened player takes fall damage, verify still flattened
- [ ] **Integration:** Flattened player dies, verify unflattened on respawn

---

### 6.2: Multiplayer Sync Tests
- [ ] **Manual (2 players):** P1 hits P2 with hammer, both see flatten within 100ms
- [ ] **Manual (3 players):** P1 hits P2, P3 (observer) sees flatten
- [ ] **Manual:** Flattened player logout, other players verify state cleared
- [ ] **Manual:** Flattened player login, other players verify state restored

**AC (NFR-1):**
- Visual sync ≤100ms (target), ≤150ms (max)

---

### 6.3: Performance Tests
- [ ] **Manual:** 20 players simultaneously flattened, verify TPS ≥19.0
- [ ] **Manual:** Measure heap before/after flattening, verify ≤5KB per player

**AC (NFR-2, NFR-3):**
- 20 concurrent flattened players: TPS ≥19.0
- Memory ≤5KB per flattened player

---

### 6.4: Compatibility Tests
- [ ] **Manual:** Test in BMC5 modpack, verify no crashes
- [ ] **Manual:** Test with Sodium/Embeddium installed, verify no crashes
- [ ] **Manual:** Test dimension change while flattened, verify state persists

**AC (NFR-4, NFR-5, NFR-6):**
- Works on NeoForge 21.1.214 + Minecraft 1.21.1
- Compatible with BMC5
- No crashes with Sodium/Embeddium

---

## Edge Cases

- [ ] **Manual:** Hammer hit on player with invulnerability frames, verify no double-flatten
- [ ] **Manual:** Hammer hit on creative mode player, verify behavior
- [ ] **Manual:** Hammer hit on spectator mode player, verify ignored
- [ ] **Manual:** Weak attack (cooldown < 100%), verify no flatten
- [ ] **Manual:** Attack during lag spike, verify eventual consistency
- [ ] **Manual:** Reform during lag spike, verify no visual glitches
- [ ] **Manual:** Rapid-fire attacks (spam click), verify only full-strength hits flatten
- [ ] **Manual:** PvP disabled server, verify no flattening occurs

---

## Regression Tests

- [ ] **Manual:** Existing anvil flattening still works
- [ ] **Manual:** Anvil + hammer spread stacking verified
- [ ] **Manual:** Reform keybind works for both sources
- [ ] **Manual:** Existing config values unaffected
