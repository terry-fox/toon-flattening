# API Module Specification

## Responsibilities

- **Public Facade**: Expose `ToonFlatteningAPI` singleton for third-party mod integration
- **Event System**: Post cancellable Pre/Post events for flatten/reform lifecycle
- **Extension Points**: Provide `IFlattenTrigger` interface for custom triggers
- **State Queries**: Thread-safe read-only access to FlattenPhase and scales
- **API Stability**: Maintain backward compatibility across minor versions

## Data Ownership

### Exclusive Data
- Event instances (created per-dispatch, transient)

### Read-Only Dependencies
- FlattenState (from core)
- Configuration (from infrastructure)

## Communication

### Incoming (Afferent Coupling)
- **Third-Party Mods** → `ToonFlatteningAPI.getInstance().*` (public methods)

### Outgoing (Efferent Coupling)
- **core** → `FlattenStateManager.getState(Player)` (query)
- **core** → `FlattenStateManager.getPhase(Player)` (query)
- **core** → `FlattenStateManager.beginCompression(ServerPlayer, anvilY, floorY)` (command)
- **core** → `FlattenStateManager.beginReformation(ServerPlayer)` (command)
- **reformation** → `ReformationHandler.canReform(Player)` (query)
- **detection** → `TriggerRegistry.registerTrigger(IFlattenTrigger, int priority)` (command)
- **NeoForge Event Bus** → Post events to FORGE bus

## Key Classes

### ToonFlatteningAPI (Public Facade)
```java
public final class ToonFlatteningAPI {
    private static final ToonFlatteningAPI INSTANCE = new ToonFlatteningAPI();

    public static ToonFlatteningAPI getInstance() {
        return INSTANCE;
    }

    // --- State Queries (Thread-Safe) ---

    /**
     * Returns current flattening phase.
     * Thread-safe: Can be called from any thread.
     */
    public FlattenPhase getPhase(Player player) {
        return FlattenStateManager.getPhase(player);
    }

    /**
     * Returns true if player is in FullyFlattened phase.
     * Thread-safe: Can be called from any thread.
     */
    public boolean isFlattened(Player player) {
        return getPhase(player) == FlattenPhase.FULLY_FLATTENED;
    }

    /**
     * Returns current scale values (height, width, depth).
     * Thread-safe: Returns immutable snapshot.
     */
    public Scales getCurrentScales(Player player) {
        FlattenState state = FlattenStateManager.getState(player);
        return new Scales(state.heightScale(), state.widthScale(), state.depthScale());
    }

    /**
     * Returns accumulated spread multiplier.
     * Returns 0.0 if not in FullyFlattened phase.
     * Thread-safe: Can be called from any thread.
     */
    public float getSpreadMultiplier(Player player) {
        FlattenState state = FlattenStateManager.getState(player);
        return state.spreadMultiplier();
    }

    /**
     * Returns true if reformation is blocked (anvil above or insufficient clearance).
     * Thread-safe: Can be called from any thread.
     */
    public boolean isReformBlocked(Player player) {
        return !ReformationHandler.canReform(player);
    }

    /**
     * Returns remaining ticks until fallback timeout.
     * Returns -1 if fallback disabled, 0 if expired.
     * Thread-safe: Can be called from any thread.
     */
    public int getRemainingFallbackTicks(Player player) {
        FlattenState state = FlattenStateManager.getState(player);
        return state.fallbackTicksRemaining();
    }

    // --- Commands (Server Thread Only) ---

    /**
     * Initiates flattening for player.
     *
     * When progressive_enabled=true, transitions to ProgressiveFlattening phase.
     * When progressive_enabled=false, instantly transitions to FullyFlattened (legacy mode).
     *
     * @param player Server-side player instance
     * @param damage Damage to apply when reaching FullyFlattened (in hearts)
     * @param anvilCount Number of anvils in stack (affects spread calculation)
     * @return true if flatten initiated, false if player already flattening
     * @throws IllegalStateException if called from non-server thread
     */
    public boolean flatten(ServerPlayer player, float damage, int anvilCount) {
        validateServerThread();
        // Delegate to core module (anvilCount passed for spread calculation)
        return FlattenStateManager.beginCompression(player, damage, anvilCount);
    }

    /**
     * Initiates flattening for player with single anvil (anvilCount=1).
     * Convenience method for backwards compatibility.
     */
    public boolean flatten(ServerPlayer player, float damage) {
        return flatten(player, damage, 1);
    }

    /**
     * Initiates reformation for player.
     *
     * Only succeeds if:
     * - Player is in FullyFlattened phase
     * - No anvil above player (or fallback timeout expired)
     * - Sufficient vertical clearance (75% of frozen pose height)
     *
     * @param player Server-side player instance
     * @return true if reformation started, false if blocked or not flattened
     * @throws IllegalStateException if called from non-server thread
     */
    public boolean reform(ServerPlayer player) {
        validateServerThread();
        if (!ReformationHandler.canReform(player)) {
            return false;
        }
        FlattenStateManager.beginReformation(player);
        return true;
    }

    /**
     * Sets spread multiplier for player.
     * Only applies when player is in FullyFlattened phase.
     * Value clamped to configured max_spread_limit.
     *
     * @param player Server-side player instance
     * @param spread Horizontal spread multiplier (added to width/depth)
     * @throws IllegalStateException if called from non-server thread
     */
    public void setSpreadMultiplier(ServerPlayer player, float spread) {
        validateServerThread();
        FlattenStateManager.setSpreadMultiplier(player, spread);
    }

    // --- Extension Points ---

    /**
     * Registers custom flatten trigger.
     * Higher priority triggers checked first.
     *
     * @param trigger Trigger implementation
     * @param priority Priority (higher = checked first, default anvil = 0)
     */
    public void registerFlattenTrigger(IFlattenTrigger trigger, int priority) {
        TriggerRegistry.registerTrigger(trigger, priority);
    }

    /**
     * Registers custom scaling provider.
     * Higher priority providers selected first.
     *
     * @param provider Scaling provider implementation
     * @param priority Priority (higher = preferred, Pehkui = 100, NoOp = Integer.MIN_VALUE)
     */
    public void registerScalingProvider(IScalingProvider provider, int priority) {
        ScalingProviderRegistry.registerProvider(provider, priority);
    }

    private void validateServerThread() {
        if (!Thread.currentThread().getName().contains("Server")) {
            throw new IllegalStateException("API command must be called on server thread");
        }
    }
}
```

### Event Classes

#### PreFlattenEvent (Cancellable)
```java
@Cancelable
public class PreFlattenEvent extends Event {
    private final ServerPlayer player;
    private final float damage;
    private final Object source; // FallingBlockEntity or BlockPos

    public PreFlattenEvent(ServerPlayer player, float damage, Object source) {
        this.player = player;
        this.damage = damage;
        this.source = source;
    }

    public ServerPlayer getPlayer() { return player; }
    public float getDamage() { return damage; }
    public Object getSource() { return source; }
}
```

#### PostFlattenEvent
```java
public class PostFlattenEvent extends Event {
    private final ServerPlayer player;
    private final float appliedDamage;
    private final float spreadMultiplier;
    private final int anvilCount; // NEW: Number of anvils in stack

    public PostFlattenEvent(ServerPlayer player, float appliedDamage, float spreadMultiplier, int anvilCount) {
        this.player = player;
        this.appliedDamage = appliedDamage;
        this.spreadMultiplier = spreadMultiplier;
        this.anvilCount = anvilCount;
    }

    public ServerPlayer getPlayer() { return player; }
    public float getAppliedDamage() { return appliedDamage; }
    public float getSpreadMultiplier() { return spreadMultiplier; }
    public int getAnvilCount() { return anvilCount; } // NEW
}
```

#### PreReformEvent (Cancellable)
```java
@Cancelable
public class PreReformEvent extends Event {
    private final ServerPlayer player;

    public PreReformEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() { return player; }
}
```

#### PostReformEvent
```java
public class PostReformEvent extends Event {
    private final ServerPlayer player;

    public PostReformEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() { return player; }
}
```

### IFlattenTrigger (Extension Interface)
```java
public interface IFlattenTrigger {
    /**
     * Return true if this trigger should flatten the player this tick.
     * Called per-tick for each player.
     *
     * @param player Server-side player instance
     * @return true to trigger flatten
     */
    boolean shouldTrigger(ServerPlayer player);

    /**
     * Return damage amount (in hearts) to apply when reaching FullyFlattened.
     * Default: configured damage_amount.
     */
    default float getDamage() {
        return Config.damage_amount;
    }

    /**
     * Return anvil position for scale calculation.
     * Default: null (uses floor Y for anvil Y calculation).
     */
    default @Nullable Vec3 getAnvilPosition(ServerPlayer player) {
        return null;
    }

    /**
     * Return anvil count for spread calculation (NEW).
     * Default: 1 anvil.
     */
    default int getAnvilCount(ServerPlayer player) {
        return 1;
    }
}
```

### Scales (Immutable DTO)
```java
public record Scales(float height, float width, float depth) {
    public static Scales normal() {
        return new Scales(1.0f, 1.0f, 1.0f);
    }
}
```

## Event Dispatch Flow

```
Core Module: FullyFlattened transition
        |
        v
Post PreFlattenEvent to FORGE bus (cancellable)
        |
        +---> Event cancelled? --> Abort transition, return to Normal
        |
        v
Apply damage, update scales, freeze pose
        |
        v
Post PostFlattenEvent to FORGE bus
        |
        v
Third-party listeners execute
```

## API Usage Examples

### Third-Party Mod: Custom Trigger
```java
public class FallingBoulderTrigger implements IFlattenTrigger {
    @Override
    public boolean shouldTrigger(ServerPlayer player) {
        // Check for custom FallingBoulderEntity above player
        AABB searchBox = player.getBoundingBox().inflate(1.0, 5.0, 1.0);
        return player.level().getEntitiesOfClass(FallingBoulderEntity.class, searchBox)
            .stream()
            .anyMatch(boulder -> boulder.getY() >= player.getEyeY());
    }

    @Override
    public float getDamage() {
        return 6.0f; // Boulders do more damage
    }
}

// Registration in mod constructor:
ToonFlatteningAPI.getInstance().registerFlattenTrigger(new FallingBoulderTrigger(), 50);
```

### Third-Party Mod: Event Listener
```java
@SubscribeEvent
public void onPreFlatten(PreFlattenEvent event) {
    Player player = event.getPlayer();
    if (player.hasEffect(ProtectionEffects.ANVIL_IMMUNITY)) {
        event.setCanceled(true); // Prevent flattening
    }
}
```

### Third-Party Mod: API Query
```java
public boolean canPlayerMine(Player player) {
    return !ToonFlatteningAPI.getInstance().isFlattened(player);
}
```

## Testing Strategy

### Unit Tests
- **Facade Methods**:
  - `testGetPhaseReturnsCorrectPhase()` - API returns core state
  - `testIsFlattenedOnlyTrueForFullyFlattened()` - isFlattened() phase check
  - `testGetCurrentScalesReturnsSnapshot()` - Immutable Scales returned

- **Thread Safety**:
  - `testQueryMethodsThreadSafe()` - Call from background thread succeeds
  - `testCommandMethodsThrowOnWrongThread()` - flatten() on render thread throws

- **Event Cancellation**:
  - `testPreFlattenEventCancelled()` - Cancelled event aborts flatten
  - `testPreReformEventCancelled()` - Cancelled event aborts reform

### Integration Tests
- **Cross-Module**:
  - `testAPIFlattenDelegatesToCore()` - API.flatten() → core.beginCompression()
  - `testAPIReformDelegatesToCore()` - API.reform() → core.beginReformation()

- **Event Flow**:
  - `testPreFlattenEventFiredBeforeStateChange()` - Event fires before transition
  - `testPostFlattenEventFiredAfterStateChange()` - Event fires after transition

- **Third-Party Extension**:
  - `testCustomTriggerRegistered()` - API.registerFlattenTrigger() adds to registry
  - `testCustomProviderRegistered()` - API.registerScalingProvider() adds to registry

## Coupling Metrics

- **Afferent Coupling (Ca)**: 1 (third-party mods - external)
- **Efferent Coupling (Ce)**: 3 (core, reformation, detection)
- **Instability (I)**: 0.75 (unstable by design - public API facade)

## Versioning Contract

- **Major version**: Breaking API changes (method signature changes, removal)
- **Minor version**: Backward-compatible additions (new methods, events)
- **Patch version**: Bug fixes (no API changes)

Example: 1.2.3
- Add new method: 1.2.3 → 1.3.0
- Change method signature: 1.2.3 → 2.0.0
- Fix event dispatch bug: 1.2.3 → 1.2.4
