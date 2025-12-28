# Reformation Module Specification

## Responsibilities

- **Clearance Validation**: Check vertical space (75% of frozen pose height) before allowing reformation
- **Anvil-Blocking Detection**: Detect placed anvil blocks within player AABB, block reformation if present
- **Fallback Timer Management**: Track timeout for bypassing anvil-blocking (default: 5 minutes)
- **Keybinding Handling**: Process SPACE key input, validate reformation conditions
- **Recovery Animation**: Coordinate with core for scale interpolation during Recovering phase

## Data Ownership

### Exclusive Data
- Fallback timer ticks (stored in core FlattenState, logic owned by reformation)

### Read-Only Dependencies
- Current FlattenPhase (from core)
- Frozen pose data (from core FlattenState)
- Player bounding box (Minecraft entity)
- World block state (Minecraft world)
- Configuration (anvil_blocking_enabled, fallback_timeout_seconds, reformation_ticks)

## Communication

### Incoming (Afferent Coupling)
- **infrastructure** → `ReformationHandler.onKeyPress(ServerPlayer)` (keybind event)
- **infrastructure** → `ReformationHandler.tick(ServerPlayer)` (fallback timer tick)
- **api** → `ReformationHandler.canReform(Player)` (query)

### Outgoing (Efferent Coupling)
- **core** → `FlattenStateManager.beginReformation(ServerPlayer)`
- **core** → `FlattenStateManager.getPhase(Player)` (query)
- **core** → `FlattenStateManager.getState(Player)` (query)

## Key Classes

### ReformationHandler
```java
public class ReformationHandler {
    // Called by infrastructure when reform keybind pressed
    public void onKeyPress(ServerPlayer player) {
        if (!canReform(player)) {
            return; // Silently ignore
        }
        FlattenStateManager.beginReformation(player);
    }

    // Check if reformation is allowed
    public boolean canReform(Player player) {
        FlattenPhase phase = FlattenStateManager.getPhase(player);
        if (phase != FlattenPhase.FULLY_FLATTENED) {
            return false;
        }

        FlattenState state = FlattenStateManager.getState(player);
        boolean fallbackExpired = state.fallbackTicksRemaining() <= 0;

        if (!fallbackExpired && Config.anvil_blocking_enabled) {
            if (ClearanceCalculator.hasAnvilAbove(player)) {
                return false; // Blocked by anvil
            }
        }

        if (!ClearanceCalculator.hasSufficientClearance(player, state.frozenPose())) {
            return false; // Blocked by insufficient space
        }

        return true;
    }

    // Called per-tick by infrastructure for fallback timer
    public void tick(ServerPlayer player) {
        FlattenState state = FlattenStateManager.getState(player);
        if (state.phase() == FlattenPhase.FULLY_FLATTENED && state.fallbackTicksRemaining() > 0) {
            // Core module decrements timer in tick()
        }
    }
}
```

### ClearanceCalculator
```java
public class ClearanceCalculator {
    // Check for anvil blocks within player AABB
    public static boolean hasAnvilAbove(Player player) {
        AABB playerBox = player.getBoundingBox();
        return BlockPos.betweenClosedStream(playerBox)
            .anyMatch(pos -> player.level().getBlockState(pos).is(BlockTags.ANVIL));
    }

    // Check if vertical space meets 75% of frozen pose height
    public static boolean hasSufficientClearance(Player player, Pose frozenPose) {
        float requiredHeight = getPoseHeight(frozenPose) * 0.75f;
        float floorY = player.getY(); // Bottom of AABB
        float ceilingY = findCeiling(player, floorY);

        return (ceilingY - floorY) >= requiredHeight;
    }

    // Get hitbox height for pose
    private static float getPoseHeight(Pose pose) {
        return switch (pose) {
            case STANDING -> 1.8f;
            case SNEAKING -> 1.5f;
            case SWIMMING, FALL_FLYING -> 0.6f;
            case SLEEPING -> 0.2f;
            default -> 1.8f;
        };
    }

    // Raycast upward to find ceiling (treat anvils as ceiling)
    private static float findCeiling(Player player, float floorY) {
        Level level = player.level();
        BlockPos.MutableBlockPos pos = player.blockPosition().mutable();

        for (int y = 0; y < 10; y++) { // Max 10 blocks up
            pos.setY((int) floorY + y);
            BlockState state = level.getBlockState(pos);
            if (state.isCollisionShapeFullBlock(level, pos) || state.is(BlockTags.ANVIL)) {
                return pos.getY();
            }
        }

        return floorY + 10; // No ceiling found, assume 10 blocks clearance
    }
}
```

### FallbackTimer
```java
public class FallbackTimer {
    // Start timer when entering FullyFlattened phase
    public static int initializeTimer() {
        int seconds = Config.fallback_timeout_seconds;
        if (seconds == 0) {
            return -1; // Disabled
        }
        return seconds * 20; // Convert to ticks
    }

    // Reset timer on re-flatten
    public static int resetTimer() {
        return initializeTimer();
    }

    // Decrement timer each tick (called by core)
    public static int tick(int currentTicks) {
        if (currentTicks <= 0) {
            return 0; // Already expired or disabled
        }
        return currentTicks - 1;
    }
}
```

## Reformation Flow

```
Player presses SPACE keybind
        |
        v
ReformationHandler.onKeyPress()
        |
        +---> Check phase == FULLY_FLATTENED? --> NO --> Return (silent)
        |                                    |
        |                                   YES
        v                                    |
Check fallback timer expired?               |
        |                                    |
   NO --+--> anvil_blocking_enabled?        |
        |            |                       |
        |           YES                      |
        v            |                       |
   hasAnvilAbove? ---+--> YES --> Return     |
        |                        (blocked)   |
       NO                                    |
        |                                    |
        v                                    |
   hasSufficientClearance?                  |
        |                                    |
   NO --+--> Return (blocked)                |
        |                                    |
       YES                                   |
        v                                    |
   FlattenStateManager.beginReformation() <-+
        |
        v
   Transition to Recovering phase
        |
        v
   Linear interpolation over reformation_ticks
        |
        v
   Transition to Normal phase
```

## Testing Strategy

### Unit Tests
- **Clearance Calculation**:
  - `testSufficientClearanceStanding()` - 75% of 1.8 blocks clearance passes
  - `testInsufficientClearance()` - 50% of pose height fails
  - `testClearanceForSneakingPose()` - 75% of 1.5 blocks required
  - `testClearanceForSwimmingPose()` - 75% of 0.6 blocks required

- **Anvil Detection**:
  - `testAnvilAboveDetected()` - Anvil in AABB returns true
  - `testNoAnvilAbove()` - Empty space returns false
  - `testAnvilBesidePlayerIgnored()` - Anvil outside AABB returns false

- **Fallback Timer**:
  - `testFallbackTimerInitialized()` - Config 300s → 6000 ticks
  - `testFallbackTimerDisabled()` - Config 0 → -1 ticks
  - `testFallbackTimerDecrement()` - Tick reduces by 1
  - `testFallbackTimerExpiry()` - 0 ticks bypasses anvil-blocking

- **Reform Validation**:
  - `testReformBlockedWhenProgressiveFlattening()` - canReform() returns false
  - `testReformBlockedByAnvil()` - canReform() returns false
  - `testReformBlockedByClearance()` - canReform() returns false
  - `testReformAllowedAfterFallback()` - canReform() returns true (timer expired)

### Integration Tests
- **Cross-Module**:
  - `testKeybindTriggersReformation()` - SPACE press → core.beginReformation()
  - `testReformationStartsAnimation()` - Recovering phase interpolates scales
  - `testReformBlockedUntilAnvilRemoved()` - Anvil present → keybind no-op

- **End-to-End**:
  - `testFullReformCycleWithAnvil()` - FullyFlattened → remove anvil → SPACE → Recovering → Normal
  - `testFallbackBypassesAnvil()` - Wait 5 minutes → SPACE → Recovering (anvil ignored)

## Coupling Metrics

- **Afferent Coupling (Ca)**: 2 (infrastructure, api)
- **Efferent Coupling (Ce)**: 1 (core)
- **Instability (I)**: 0.33 (stable)

## Performance Constraints

- **Clearance Check**: ≤0.02ms (raycast max 10 blocks)
- **Anvil Detection**: ≤0.01ms (AABB betweenClosed limited to player size)
- **Fallback Timer Tick**: ≤0.001ms (simple integer decrement)
