# Integration Module Specification

## Responsibilities

- **Scaling Abstraction**: Abstract Pehkui API behind `IScalingProvider` interface
- **Provider Registry**: Manage multiple scaling providers with priority-based selection
- **Graceful Degradation**: Provide `NoOpScalingProvider` when Pehkui missing or unavailable
- **API Compatibility**: Isolate Pehkui version changes from core module
- **Third-Party Extension**: Allow custom mods to register alternative scaling providers

## Data Ownership

### Exclusive Data
- Scaling provider registry (priority-sorted list of IScalingProvider instances)
- Provider selection cache (per-player, transient)

### Read-Only Dependencies
- Pehkui ScaleType API (external)
- Player entity (Minecraft)

## Communication

### Incoming (Afferent Coupling)
- **core** → `ScalingProviderRegistry.getProvider(ServerPlayer)` (query)
- **core** → `IScalingProvider.setScales(ServerPlayer, height, width, depth)`
- **api** → `ScalingProviderRegistry.registerProvider(IScalingProvider, int priority)`

### Outgoing (Efferent Coupling)
- **Pehkui** → `ScaleType.WIDTH.getScaleData(player).setScale(value)`
- **Pehkui** → `ScaleType.HEIGHT.getScaleData(player).setScale(value)`
- **Pehkui** → `ScaleType.MODEL_WIDTH.getScaleData(player).setScale(value)` (depth)

## Key Classes

### IScalingProvider (Interface)
```java
public interface IScalingProvider {
    // Return true if this provider can handle the player
    boolean canHandle(ServerPlayer player);

    // Apply scale values to player
    void setScales(ServerPlayer player, float height, float width, float depth);

    // Get provider name for logging
    String getName();
}
```

### PehkuiScalingProvider (Adapter)
```java
public class PehkuiScalingProvider implements IScalingProvider {
    @Override
    public boolean canHandle(ServerPlayer player) {
        // Check if Pehkui is loaded and player has scale data
        return ModList.get().isLoaded("pehkui")
            && ScaleType.WIDTH.getScaleData(player) != null;
    }

    @Override
    public void setScales(ServerPlayer player, float height, float width, float depth) {
        ScaleType.HEIGHT.getScaleData(player).setScale(height);
        ScaleType.WIDTH.getScaleData(player).setScale(width);
        ScaleType.MODEL_WIDTH.getScaleData(player).setScale(depth);

        // Pehkui handles persistence and client synchronization
    }

    @Override
    public String getName() {
        return "Pehkui";
    }
}
```

### NoOpScalingProvider (Fallback)
```java
public class NoOpScalingProvider implements IScalingProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private boolean warningLogged = false;

    @Override
    public boolean canHandle(ServerPlayer player) {
        return true; // Always accepts (lowest priority fallback)
    }

    @Override
    public void setScales(ServerPlayer player, float height, float width, float depth) {
        if (!warningLogged) {
            LOGGER.error("No scaling provider available! Pehkui is required for visual flattening.");
            LOGGER.error("Player {} will experience broken flattening (state changes but no visual scaling).",
                player.getName().getString());
            warningLogged = true;
        }
        // No-op: scales not applied
    }

    @Override
    public String getName() {
        return "NoOp (Fallback)";
    }
}
```

### ScalingProviderRegistry
```java
public class ScalingProviderRegistry {
    private static final List<ProviderEntry> PROVIDERS = new ArrayList<>();
    private static final Map<UUID, IScalingProvider> CACHE = new ConcurrentHashMap<>();

    // Register provider with priority (higher = preferred)
    public static void registerProvider(IScalingProvider provider, int priority) {
        PROVIDERS.add(new ProviderEntry(provider, priority));
        PROVIDERS.sort(Comparator.comparingInt(ProviderEntry::priority).reversed());
        LOGGER.info("Registered scaling provider '{}' with priority {}",
            provider.getName(), priority);
    }

    // Get provider for player (cached)
    public static IScalingProvider getProvider(ServerPlayer player) {
        UUID uuid = player.getUUID();
        return CACHE.computeIfAbsent(uuid, k -> selectProvider(player));
    }

    // Select first provider that canHandle player
    private static IScalingProvider selectProvider(ServerPlayer player) {
        for (ProviderEntry entry : PROVIDERS) {
            if (entry.provider().canHandle(player)) {
                LOGGER.debug("Selected scaling provider '{}' for player {}",
                    entry.provider().getName(), player.getName().getString());
                return entry.provider();
            }
        }

        LOGGER.warn("No scaling provider found for player {}, using NoOp fallback",
            player.getName().getString());
        return new NoOpScalingProvider(); // Should never happen if NoOp registered
    }

    // Clear cache on player disconnect
    public static void invalidateCache(UUID playerUUID) {
        CACHE.remove(playerUUID);
    }

    private record ProviderEntry(IScalingProvider provider, int priority) {}
}
```

### ScalingIntegration (Initialization)
```java
public class ScalingIntegration {
    public static void initialize() {
        // Register providers in priority order
        ScalingProviderRegistry.registerProvider(new PehkuiScalingProvider(), 100);
        ScalingProviderRegistry.registerProvider(new NoOpScalingProvider(), Integer.MIN_VALUE);
    }
}
```

## Priority Levels

| Provider | Priority | Condition |
|----------|----------|-----------|
| PehkuiScalingProvider | 100 | Pehkui loaded, player has scale data |
| Custom Third-Party | 50-200 | Configurable by third-party mod |
| NoOpScalingProvider | Integer.MIN_VALUE | Always fallback |

## Coupling Metrics

- **Afferent Coupling (Ca)**: 2 (core, api)
- **Efferent Coupling (Ce)**: 1 (Pehkui - external)
- **Instability (I)**: 0.33 (stable, isolates external dependency)

## Performance Constraints

- **Provider Selection**: ≤0.01ms (cached after first selection)
- **Scale Application**: ≤0.02ms (Pehkui API overhead)
- **Cache Lookup**: ≤0.001ms (ConcurrentHashMap.get)

## Error Handling

### Pehkui API Failures
- **Scale data null**: canHandle() returns false, next provider selected
- **setScale() throws**: Catch exception, log error, revert to NoOpScalingProvider for player
- **Pehkui unloaded mid-session**: Cache invalidated, provider reselected

### Provider Conflicts
- **Multiple providers same priority**: First-registered wins, log warning
- **No providers registered**: Mod initialization fails fast (log error, disable mod)
