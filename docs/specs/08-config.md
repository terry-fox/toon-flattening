# Config Module Specification

**Package:** `com.terryfox.toonflattening.config`
**Side:** Both

## Purpose

TOML configuration with hot-reload support.

## Files

```
config/
├── ToonFlattenConfig.java
└── ConfigCache.java
```

## ToonFlattenConfig

NeoForge ModConfigSpec definitions.

```java
public class ToonFlattenConfig {
    public static final ModConfigSpec SPEC;

    // Anvil
    public static final ModConfigSpec.DoubleValue DAMAGE_AMOUNT;

    // Scale
    public static final ModConfigSpec.DoubleValue HEIGHT_SCALE;
    public static final ModConfigSpec.DoubleValue WIDTH_SCALE;

    // Spread
    public static final ModConfigSpec.DoubleValue SPREAD_INCREMENT;
    public static final ModConfigSpec.DoubleValue MAX_SPREAD_LIMIT;

    // Reformation
    public static final ModConfigSpec.IntValue REFORMATION_TICKS;
    public static final ModConfigSpec.BooleanValue ANVIL_BLOCKING_ENABLED;
    public static final ModConfigSpec.IntValue FALLBACK_TIMEOUT_SECONDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Anvil collision settings").push("anvil");
        DAMAGE_AMOUNT = builder
            .comment("Damage in hearts (2.0 points per heart)")
            .defineInRange("damage_amount", 4.0, 0.0, 20.0);
        builder.pop();

        builder.comment("Scale transformation").push("scale");
        HEIGHT_SCALE = builder
            .comment("Height multiplier (1.0 = normal)")
            .defineInRange("height_scale", 0.05, 0.01, 1.0);
        WIDTH_SCALE = builder
            .comment("Width multiplier (1.0 = normal)")
            .defineInRange("width_scale", 1.8, 1.0, 6.0);
        builder.pop();

        builder.comment("Re-flatten spread stacking").push("spread");
        SPREAD_INCREMENT = builder
            .comment("Width added per re-flatten")
            .defineInRange("spread_increment", 0.8, 0.1, 2.0);
        MAX_SPREAD_LIMIT = builder
            .comment("Maximum width multiplier")
            .defineInRange("max_spread_limit", 6.0, 1.0, 6.0);
        builder.pop();

        builder.comment("Reformation settings").push("reform");
        REFORMATION_TICKS = builder
            .comment("Animation duration in ticks (20 = 1 second)")
            .defineInRange("reformation_ticks", 5, 1, 100);
        ANVIL_BLOCKING_ENABLED = builder
            .comment("Block reformation while anvil above player")
            .define("anvil_blocking_enabled", true);
        FALLBACK_TIMEOUT_SECONDS = builder
            .comment("Seconds until anvil-blocking bypassed (0 = never)")
            .defineInRange("fallback_timeout_seconds", 300, 0, 3600);
        builder.pop();

        SPEC = builder.build();
    }
}
```

## ConfigCache

Hot-reloadable cached primitives for fast access.

```java
public class ConfigCache {
    private static ConfigCache INSTANCE;

    // Cached values
    private volatile float damageAmount;
    private volatile float heightScale;
    private volatile float widthScale;
    private volatile float spreadIncrement;
    private volatile float maxSpreadLimit;
    private volatile int reformationTicks;
    private volatile boolean anvilBlockingEnabled;
    private volatile int fallbackTimeoutTicks;

    public static ConfigCache get() {
        return INSTANCE;
    }

    public void reload() {
        this.damageAmount = ToonFlattenConfig.DAMAGE_AMOUNT.get().floatValue();
        this.heightScale = ToonFlattenConfig.HEIGHT_SCALE.get().floatValue();
        this.widthScale = ToonFlattenConfig.WIDTH_SCALE.get().floatValue();
        this.spreadIncrement = ToonFlattenConfig.SPREAD_INCREMENT.get().floatValue();
        this.maxSpreadLimit = ToonFlattenConfig.MAX_SPREAD_LIMIT.get().floatValue();
        this.reformationTicks = ToonFlattenConfig.REFORMATION_TICKS.get();
        this.anvilBlockingEnabled = ToonFlattenConfig.ANVIL_BLOCKING_ENABLED.get();
        this.fallbackTimeoutTicks = ToonFlattenConfig.FALLBACK_TIMEOUT_SECONDS.get() * 20;

        LOGGER.info("Toon Flattening config reloaded");
    }

    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ToonFlattenConfig.SPEC) {
            reload();
        }
    }

    // Getters
    public float damageAmount() { return damageAmount; }
    public float heightScale() { return heightScale; }
    public float widthScale() { return widthScale; }
    public float spreadIncrement() { return spreadIncrement; }
    public float maxSpreadLimit() { return maxSpreadLimit; }
    public int reformationTicks() { return reformationTicks; }
    public boolean anvilBlockingEnabled() { return anvilBlockingEnabled; }
    public int fallbackTimeoutTicks() { return fallbackTimeoutTicks; }
}
```

## Registration

```java
// In ToonFlattening constructor
ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
modContainer.registerConfig(ModConfig.Type.SERVER, ToonFlattenConfig.SPEC);
```

## Config File Location

```
config/toonflattening-server.toml
```

## Requirements Traced

- **CFG-ANVIL.1**: damage_amount (0-20, default 4.0)
- **CFG-SCALE.1**: height_scale (0.01-1.0, default 0.05)
- **CFG-SCALE.2**: width_scale (1.0-6.0, default 1.8)
- **CFG-SPREAD.1**: spread_increment (0.1-2.0, default 0.8)
- **CFG-SPREAD.2**: max_spread_limit (1.0-6.0, default 6.0)
- **CFG-REFORM.1**: reformation_ticks (1-100, default 5)
- **CFG-REFORM.2**: anvil_blocking_enabled (default true)
- **CFG-REFORM.3**: fallback_timeout_seconds (0-3600, default 300)
- **NFR-CFG.1**: Hot-reload within 5 seconds
- **NFR-CFG.2**: Validate and log warnings
- **NFR-CFG.3**: Apply defaults if missing/corrupted
