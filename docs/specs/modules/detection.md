# Detection Module Specification

## Responsibilities

- **Anvil Contact Detection**: Per-tick scanning for falling anvil entities and placed anvil blocks above players
- **Anvil Stack Counting**: Count vertically stacked anvils for spread calculation
- **Replacement vs Stacking Detection**: Distinguish between adding to existing stack vs replacing anvil
- **Trigger System**: Extensible interface for third-party flatten triggers (e.g., falling boulders)
- **Collision Validation**: Verify anvil bottom is above player head, bounding box intersection
- **Floor Detection**: Calculate floor Y position via downward raycast
- **Vanilla Damage Cancellation**: Cancel anvil damage events during progressive flattening

## Data Ownership

### Exclusive Data
- Trigger registry (IFlattenTrigger instances with priority)
- Transient anvil detection cache (per-tick, not persisted)

### Read-Only Dependencies
- Player bounding box (Minecraft entity)
- World block state (Minecraft world)
- Entity positions (FallingBlockEntity)
- Configuration (progressive_enabled, anvil_blocking_enabled)

## Communication

### Incoming (Afferent Coupling)
- **infrastructure** → `AnvilContactDetector.tick(ServerPlayer)` (called per-tick)
- **api** → `TriggerRegistry.registerTrigger(IFlattenTrigger, int priority)`

### Outgoing (Efferent Coupling)
- **core** → `FlattenStateManager.beginCompression(player, anvilY, floorY, anvilCount)`
- **core** → `FlattenStateManager.updateCompression(player, anvilY, floorY, anvilCount)`
- **core** → `FlattenStateManager.lostContact(player)`
- **core** → `FlattenStateManager.applyReflatten(player, anvilCount, isReplacement)`
- **NeoForge Event Bus** → Cancel vanilla anvil damage events

## Key Classes

### AnvilContactDetector
```java
public class AnvilContactDetector {
    // Main tick handler, called by infrastructure
    public void tick(ServerPlayer player);

    // Detect falling anvil entities above player
    private @Nullable FallingBlockEntity detectFallingAnvil(ServerPlayer player);

    // Detect placed anvil blocks above player
    private @Nullable BlockPos detectPlacedAnvil(ServerPlayer player);

    // Count vertically stacked anvils above player
    private int countAnvilStack(ServerPlayer player, BlockPos bottomAnvil);

    // Check if player currently has contacting anvil (for replacement detection)
    private boolean hasContactingAnvil(ServerPlayer player);

    // Calculate floor Y via downward raycast
    private float calculateFloorY(ServerPlayer player);

    // Handle anvil contact based on current phase
    private void handleAnvilContact(ServerPlayer player, float anvilY, float floorY, int anvilCount);

    // Handle loss of anvil contact
    private void handleNoContact(ServerPlayer player);
}
```

### IFlattenTrigger (Extension Point)
```java
public interface IFlattenTrigger {
    // Return true if this trigger should flatten the player this tick
    boolean shouldTrigger(ServerPlayer player);

    // Return damage amount (default: config value)
    default float getDamage() { return Config.damage_amount; }

    // Return anvil position for scale calculation (null = use floor Y)
    default @Nullable Vec3 getAnvilPosition(ServerPlayer player) { return null; }
}
```

### TriggerRegistry
```java
public class TriggerRegistry {
    // Register custom trigger with priority (higher = checked first)
    public static void registerTrigger(IFlattenTrigger trigger, int priority);

    // Query all triggers for player (returns first match)
    public static @Nullable IFlattenTrigger getActiveTrigger(ServerPlayer player);
}
```

### AnvilDamageCanceller (Event Handler)
```java
public class AnvilDamageCanceller {
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingDamage(LivingIncomingDamageEvent event) {
        // Cancel if player is in ProgressiveFlattening phase
        // Damage applied only at FullyFlattened transition
        // Use event.getSource(), event.getAmount(), event.setCanceled(true)
    }
}
```

## Detection Algorithm

### Per-Tick Flow
```
1. Query current FlattenPhase and hasContactingAnvil from core
2. Use Entity.getBoundingBox() (automatically respects Pehkui modifications)
3. Check custom triggers via TriggerRegistry
4. If no custom trigger:
   a. Scan for FallingBlockEntity with ANVIL tag
   b. Prioritize falling anvils over placed blocks
   c. If no falling anvil, scan blocks above player AABB
5. If anvil detected:
   a. Count anvil stack height via upward iteration from bottom anvil
   b. Calculate floor Y via raycast (max 10 blocks down)
   c. Determine if replacement: !previousHasContactingAnvil
6. Dispatch to core based on phase:
   - Normal → beginCompression(player, anvilY, floorY, anvilCount)
   - ProgressiveFlattening → updateCompression(player, anvilY, floorY, anvilCount)
   - FullyFlattened + anvil present → applyReflatten(player, anvilCount, isReplacement)
   - Recovering + anvil present → beginCompression(player, anvilY, floorY, anvilCount) [NEW]
   - Any phase + no anvil → lostContact()
```

### Falling Anvil Detection
```
AABB searchBox = player.getBoundingBox().inflate(1.0, 5.0, 1.0);
List<FallingBlockEntity> entities = level.getEntitiesOfClass(FallingBlockEntity.class, searchBox);
return entities.stream()
    .filter(e -> e.getBlockState().is(BlockTags.ANVIL))
    .filter(e -> e.getY() >= player.getEyeY())
    .min(Comparator.comparingDouble(Entity::getY))
    .orElse(null);
```

### Placed Anvil Detection
```
AABB playerBox = player.getBoundingBox();
BlockPos.betweenClosedStream(playerBox.move(0, 0.1, 0)) // Check slightly above
    .filter(pos -> level.getBlockState(pos).is(BlockTags.ANVIL))
    .findFirst()
    .orElse(null);
```

### Anvil Stack Counting
```
// Count vertically stacked anvils starting from bottom anvil
int countAnvilStack(ServerPlayer player, BlockPos bottomAnvil) {
    Level level = player.level();
    int count = 1; // Start with bottom anvil
    BlockPos.MutableBlockPos pos = bottomAnvil.mutable();

    // Iterate upward (max 5 blocks to prevent performance issues)
    for (int i = 1; i <= 5; i++) {
        pos.setY(bottomAnvil.getY() + i);
        if (level.getBlockState(pos).is(BlockTags.ANVIL)) {
            count++;
        } else {
            break; // No longer stacked
        }
    }

    return count;
}
```

### Replacement Detection
```
// Check if anvil contact was present on previous tick
boolean isReplacement = !state.hasContactingAnvil();

// Core updates hasContactingAnvil flag:
// - Set to true when anvil detected
// - Set to false when lostContact() called
```

## Event Priority Rationale

**EventPriority.NORMAL** for anvil damage cancellation:
- Allows god-mode mods (HIGHEST priority) to prevent flattening
- Allows damage-modification mods (HIGH priority) to scale damage
- Executes before default Minecraft damage logic (LOW priority)

## Coupling Metrics

- **Afferent Coupling (Ca)**: 2 (infrastructure, api)
- **Efferent Coupling (Ce)**: 1 (core)
- **Instability (I)**: 0.33 (stable)

## Performance Constraints

- **Per-Tick Detection**: ≤0.04ms for entity scan + block scan + stack count + floor raycast
- **Entity Scan**: Limit search box to 5 blocks vertical radius (prevent large AABB queries)
- **Block Scan**: Limit to player AABB + 1 block inflation (prevent large betweenClosed calls)
- **Stack Counting**: Max 5 blocks upward iteration (≤0.005ms overhead)
