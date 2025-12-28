# Core Module Specification

## Responsibilities

- **State Machine**: Manage four-phase lifecycle (Normal → ProgressiveFlattening → FullyFlattened → Recovering → Normal)
- **Phase Transitions**: Execute state changes with validation, trigger events, update timestamps
- **Scale Calculation**: Compute height/width/depth scales based on anvil-to-floor distance
- **State Queries**: Provide thread-safe access to current phase and scale values
- **Spread Management**: Track and update horizontal spread multiplier for re-flatten stacking
- **Effects Coordination**: Trigger visual/audio effects only on initial flatten (NOT on re-flatten)

## Data Ownership

### Exclusive Data
- `FlattenState` per player:
  - Current phase (Normal, ProgressiveFlattening, FullyFlattened, Recovering)
  - Current scales (height, width, depth as floats)
  - Spread multiplier (accumulated horizontal expansion)
  - Original hitbox height (captured at ProgressiveFlattening start)
  - Frozen pose data (captured at FullyFlattened transition)
  - Recovery ticks remaining (for animation interpolation)
  - Fallback ticks remaining (for anvil-blocking timeout)
  - Anvil reference (entity UUID or block position)

### Read-Only Dependencies
- Configuration values (height_scale, spread_increment, max_spread_limit, reformation_ticks)
- Player bounding box (from Minecraft entity)
- Floor Y position (from detection module)
- Anvil bottom Y position (from detection module)

## Communication

### Incoming (Afferent Coupling)
- **detection** → `FlattenStateManager.beginCompression(ServerPlayer, float anvilY, float floorY, int anvilCount)`
- **detection** → `FlattenStateManager.updateCompression(ServerPlayer, float anvilY, float floorY, int anvilCount)`
- **detection** → `FlattenStateManager.lostContact(ServerPlayer)`
- **detection** → `FlattenStateManager.applyReflatten(ServerPlayer, int anvilCount, boolean isReplacement)`
- **reformation** → `FlattenStateManager.beginReformation(ServerPlayer)`
- **api** → `FlattenStateManager.getState(Player)` (query)
- **api** → `FlattenStateManager.getPhase(Player)` (query)
- **infrastructure** → `FlattenStateManager.tick(ServerPlayer)` (called per-tick)

### Outgoing (Efferent Coupling)
- **integration** → `IScalingProvider.setScales(ServerPlayer, height, width, depth)` (overwrites every tick)
- **api** → Post `PreFlattenEvent`, `PostFlattenEvent`, `PreReformEvent`, `PostReformEvent`
- **infrastructure** → `EffectHandler.playFlattenEffects(ServerPlayer)` (only on initial flatten transition to FullyFlattened)

## Key Classes

### FlattenPhase (Enum)
```java
public enum FlattenPhase {
    NORMAL,                 // No anvil contact, scale = 1.0
    PROGRESSIVE_FLATTENING, // Anvil compressing, height > 0.05
    FULLY_FLATTENED,        // Height = 0.05, restrictions active
    RECOVERING              // Animating back to normal
}
```

### FlattenState (Record)
```java
public record FlattenState(
    FlattenPhase phase,
    float heightScale,
    float widthScale,
    float depthScale,
    float spreadMultiplier,
    float originalHitboxHeight,
    Pose frozenPose,
    int recoveryTicksRemaining,
    int fallbackTicksRemaining,
    int reflattenCooldownTicks,
    int trackedAnvilCount,
    boolean hasContactingAnvil,
    @Nullable UUID anvilEntityUUID,
    @Nullable BlockPos anvilBlockPos
) {
    public static FlattenState normal() { /* 1.0, 1.0, 1.0, count=0, hasContact=false */ }
    public FlattenState withPhase(FlattenPhase newPhase) { /* copy with new phase */ }
}
```

### FlattenStateManager (Singleton)
```java
public class FlattenStateManager {
    // Called by detection module when anvil first contacts player
    public void beginCompression(ServerPlayer player, float anvilY, float floorY);

    // Called by detection module each tick while anvil present
    public void updateCompression(ServerPlayer player, float anvilY, float floorY);

    // Called by detection module when anvil removed
    public void lostContact(ServerPlayer player);

    // Called by detection module when anvil re-flattens FullyFlattened player
    public void applyReflatten(ServerPlayer player);

    // Called by reformation module when reform triggered
    public void beginReformation(ServerPlayer player);

    // Called by infrastructure tick handler
    public void tick(ServerPlayer player);

    // Query methods (thread-safe via immutable return)
    public FlattenState getState(Player player);
    public FlattenPhase getPhase(Player player);
}
```

### ScaleCalculator (Stateless Utility)
```java
public class ScaleCalculator {
    // Calculate height scale from anvil-floor distance
    public static float calculateHeightScale(float anvilY, float floorY, float originalHeight, float minScale);

    // Calculate width/depth from height compression
    public static float calculateWidthScale(float heightScale);

    // Calculate recovery interpolation (linear lerp)
    public static Scales interpolateRecovery(Scales frozen, int ticksElapsed, int totalTicks);

    // Apply spread multiplier to width/depth
    public static Scales applySpread(Scales base, float spreadMultiplier);

    // NEW: Calculate spread from anvil count
    public static float calculateSpread(int anvilCount, float spreadIncrement);

    // NEW: Calculate damage from anvil count (replacement/initial scenario)
    public static float calculateStackDamage(int anvilCount, float baseDamage, float stackDamagePerAnvil);
}
```

## Testing Strategy

### Unit Tests
- **Scale Calculation**:
  - `testHeightScaleClampedToMinimum()` - Anvil at floor Y returns minScale (0.05)
  - `testHeightScaleLinear()` - Anvil halfway returns 0.5 scale
  - `testWidthScaleInverseHeight()` - Height 0.05 → width 1.475 (1.0 + 0.95/2)
  - `testSpreadMultiplierApplied()` - Spread 0.8 adds to width/depth

- **Phase Transitions**:
  - `testNormalToProgressiveFlattening()` - beginCompression() transitions phase
  - `testProgressiveFlatteningToFullyFlattened()` - updateCompression() at height 0.05 transitions
  - `testProgressiveFlatteningToRecovering()` - lostContact() before 0.05 transitions
  - `testFullyFlattenedToRecovering()` - beginReformation() transitions
  - `testRecoveringToNormal()` - tick() completes animation
  - `testRecoveringToProgressiveFlattening()` - anvil contact during Recovering restarts compression (NEW)

- **Event Cancellation**:
  - `testPreFlattenEventCancelled()` - Transition to FullyFlattened aborts
  - `testPreReformEventCancelled()` - Transition to Recovering aborts

### Integration Tests
- **Multi-Phase Lifecycle**:
  - `testFullFlattenCycle()` - Normal → Progressive → Fully → Recovering → Normal
  - `testEarlyRecovery()` - Normal → Progressive → Recovering → Normal
  - `testReflattenStacking()` - FullyFlattened + anvil contact (stack scenario) increases spread, no damage
  - `testReflattenReplacement()` - FullyFlattened + anvil removed + new anvil (replacement) increases spread + damage (NEW)
  - `testRecoveringReFlattened()` - Recovering + anvil contact → Progressive → Fully with spread preservation (NEW)
  - `testAnvilStackSpread()` - Initial flatten with 3-anvil stack applies 3× spread immediately (NEW)

- **Cross-Module**:
  - `testDetectionTriggersStateTransition()` - Anvil contact → ProgressiveFlattening
  - `testReformationTriggersStateTransition()` - Keybind → Recovering
  - `testScalingProviderReceivesUpdates()` - State changes invoke IScalingProvider

## Coupling Metrics

- **Afferent Coupling (Ca)**: 4 (detection, reformation, api, infrastructure)
- **Efferent Coupling (Ce)**: 2 (integration, api)
- **Instability (I)**: Ce / (Ca + Ce) = 0.33 (stable, many dependents)

## Performance Constraints

- **Per-Tick Processing**: ≤0.05ms for updateCompression() + tick()
- **State Queries**: ≤0.01ms for getState() (immutable copy)
- **Transition Events**: ≤0.02ms for event posting (synchronous listeners)
