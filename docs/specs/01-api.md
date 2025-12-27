# API Module Specification

**Package:** `com.terryfox.toonflattening.api`
**Side:** Both (client + server)

## Purpose

Public contracts for third-party mod integration.

## Files

```
api/
├── FlattenAPI.java              # Static facade
├── IFlattenTrigger.java         # Extension interface
└── event/
    ├── PreFlattenEvent.java
    ├── PostFlattenEvent.java
    ├── PreReformEvent.java
    └── PostReformEvent.java
```

## FlattenAPI

Static facade exposing all public operations.

```java
public final class FlattenAPI {
    // Core operations (server-side only)
    public static boolean flatten(ServerPlayer player, float damage);
    public static boolean reform(ServerPlayer player);

    // Queries (both sides)
    public static boolean isFlattened(Player player);
    public static float getSpreadMultiplier(Player player);
    public static boolean isReformBlocked(Player player);
    public static int getRemainingFallbackTicks(Player player);

    // Mutation (server-side only)
    public static void setSpreadMultiplier(ServerPlayer player, float spread);

    // Extension registration
    public static void registerFlattenTrigger(IFlattenTrigger trigger);
}
```

### Contracts

- `flatten()`: Posts `PreFlattenEvent`, returns false if cancelled or already flattened
- `reform()`: Posts `PreReformEvent`, returns false if cancelled, not flattened, or blocked
- All methods null-safe, log warnings on invalid inputs
- Server methods throw `IllegalStateException` if called client-side

## IFlattenTrigger

Extension interface for custom flatten triggers.

```java
public interface IFlattenTrigger {
    @Nullable
    FlattenRequest shouldTriggerFlatten(ServerPlayer player);

    ResourceLocation getId();
}

public record FlattenRequest(float damage, @Nullable Entity source) {}
```

### Contracts

- Must complete within 0.01ms per player
- `getId()` returns stable, unique identifier
- Return null for no-op

## Events

### PreFlattenEvent
```java
public class PreFlattenEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final float damage;
    @Nullable private final Entity source;
    // getters...
}
```

### PostFlattenEvent
```java
public class PostFlattenEvent extends Event {
    private final ServerPlayer player;
    private final float appliedDamage;
    private final float spreadMultiplier;
    // getters...
}
```

### PreReformEvent
```java
public class PreReformEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    // getters...
}
```

### PostReformEvent
```java
public class PostReformEvent extends Event {
    private final ServerPlayer player;
    // getters...
}
```

## Requirements Traced

- **API-METHOD.1**: `flatten(ServerPlayer, float)`
- **API-METHOD.2**: `reform(ServerPlayer)`
- **API-METHOD.3**: `isFlattened(Player)`
- **API-METHOD.4**: `getSpreadMultiplier(Player)`
- **API-METHOD.5**: `setSpreadMultiplier(ServerPlayer, float)`
- **API-METHOD.6**: `isReformBlocked(Player)`
- **API-METHOD.7**: `getRemainingFallbackTicks(Player)`
- **API-EVENT.1**: PreFlattenEvent
- **API-EVENT.2**: PostFlattenEvent
- **API-EVENT.3**: PreReformEvent
- **API-EVENT.4**: PostReformEvent
- **API-EXT.1**: IFlattenTrigger
- **API-EXT.2**: registerFlattenTrigger
