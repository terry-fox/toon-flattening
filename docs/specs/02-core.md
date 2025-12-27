# Core Module Specification

**Package:** `com.terryfox.toonflattening.core`
**Side:** Server (authoritative)

## Purpose

Central state machine and business logic for flattening mechanics.

## Files

```
core/
├── FlattenStateManager.java     # Central state machine
├── FlattenState.java            # Immutable state record
├── FrozenPose.java              # Pose data at impact
├── ReformationController.java   # Reform checks/animation
└── SpreadCalculator.java        # Spread stacking logic
```

## FlattenState

Immutable record representing player's flatten state.

```java
public record FlattenState(
    boolean isFlattened,
    float spreadMultiplier,
    int fallbackTicksRemaining,    // -1 if disabled
    @Nullable FrozenPose frozenPose,
    int reformTicksRemaining,      // 0 if not reforming
    long flattenedAtTick
) {
    public static FlattenState empty() {
        return new FlattenState(false, 0f, -1, null, 0, 0);
    }

    public boolean isReforming() {
        return reformTicksRemaining > 0;
    }
}
```

## FrozenPose

```java
public record FrozenPose(
    Pose pose,           // STANDING, CROUCHING, SWIMMING, SLEEPING
    float yRot,
    float xRot,
    float bodyYRot
) {
    public float getHitboxHeight() {
        return switch (pose) {
            case STANDING -> 1.8f;
            case CROUCHING -> 1.5f;
            case SWIMMING, FALL_FLYING -> 0.6f;
            case SLEEPING -> 0.2f;
            default -> 1.8f;
        };
    }
}
```

## FlattenStateManager

Central orchestrator for all state transitions.

```java
public class FlattenStateManager {
    // State access
    public FlattenState getState(Player player);

    // State mutations (server only)
    public boolean applyFlatten(ServerPlayer player, float damage, @Nullable Entity source);
    public boolean initiateReform(ServerPlayer player);

    // Tick processing
    public void tickPlayer(ServerPlayer player);

    // Lifecycle
    public void onPlayerDeath(ServerPlayer player);
    public void onPlayerRespawn(ServerPlayer player);
    public void onPlayerLogin(ServerPlayer player);
    public void onPlayerLogout(ServerPlayer player);
}
```

### applyFlatten Flow

1. Check not spectator
2. Fire `PreFlattenEvent` - abort if cancelled
3. Apply damage (survival/adventure only)
4. Capture frozen pose
5. Calculate initial fallback ticks
6. Apply Pehkui scales via adapter
7. Persist state
8. Sync to clients
9. Fire `PostFlattenEvent`
10. Trigger effects

### initiateReform Flow

1. Check `isFlattened`
2. Check `ReformationController.canReform()`
3. Fire `PreReformEvent` - abort if cancelled
4. Start Pehkui transition animation
5. Set `reformTicksRemaining`
6. (Tick loop handles completion)

## ReformationController

Handles reform blocking conditions and animation.

```java
public class ReformationController {
    public boolean canReform(ServerPlayer player);
    public boolean isAnvilBlocking(Player player);
    public boolean hasSufficientClearance(Player player);
    public boolean isFallbackExpired(Player player);
    public void tickReformation(ServerPlayer player);
}
```

### canReform Logic

```
canReform = hasSufficientClearance AND (NOT isAnvilBlocking OR isFallbackExpired)
```

### Anvil Blocking (FR-REFORM.8)

```java
public boolean isAnvilBlocking(Player player) {
    if (!config.anvilBlockingEnabled) return false;
    AABB box = player.getBoundingBox();
    return level.getBlockStates(box)
        .anyMatch(state -> state.is(BlockTags.ANVIL));
}
```

### Clearance Check (FR-REFORM.10)

```java
public boolean hasSufficientClearance(Player player) {
    FrozenPose pose = getState(player).frozenPose();
    float requiredHeight = pose.getHitboxHeight() * 0.75f;
    float clearance = calculateClearance(player); // floor to ceiling/anvil
    return clearance >= requiredHeight;
}
```

## SpreadCalculator

```java
public class SpreadCalculator {
    public float calculateNewSpread(float current, float increment, float max) {
        return Math.min(current + increment, max);
    }
}
```

## Re-flatten During Reform

```java
// In FlattenStateManager.applyFlatten()
if (state.isReforming()) {
    int totalTicks = config.reformationTicks;
    int elapsed = totalTicks - state.reformTicksRemaining();
    if (elapsed < totalTicks / 3) {
        // Allow re-flatten: abort reform, apply new flatten
    } else {
        // Ignore anvil collision
        return false;
    }
}
```

## Requirements Traced

- **FR-STATE.1-5**: State persistence and sync
- **FR-REFL.1-6**: Re-flatten stacking
- **FR-REFORM.3-7**: Reformation mechanics
- **FR-REFORM.8**: Anvil blocking
- **FR-REFORM.9**: Fallback timer
- **FR-REFORM.10**: Clearance check
