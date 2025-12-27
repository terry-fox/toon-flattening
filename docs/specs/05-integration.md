# Integration Module Specification

**Package:** `com.terryfox.toonflattening.integration`
**Side:** Both

## Purpose

External integrations with Pehkui and NeoForge systems.

## Files

```
integration/
├── pehkui/
│   ├── PehkuiScaleAdapter.java
│   └── ScaleTransition.java
└── neoforge/
    ├── EventBusSetup.java
    └── LifecycleHandler.java
```

---

## Pehkui Integration

### PehkuiScaleAdapter

Wraps Pehkui API calls for scale manipulation.

```java
public class PehkuiScaleAdapter {
    private static final Version MIN_PEHKUI_VERSION = new Version("3.8.3");

    public static boolean isCompatible() {
        return ModList.get().getModContainerById("pehkui")
            .map(c -> c.getModInfo().getVersion().compareTo(MIN_PEHKUI_VERSION) >= 0)
            .orElse(false);
    }

    public void applyFlattenedScale(ServerPlayer player, float height, float width) {
        setScale(player, ScaleTypes.HEIGHT, height);
        setScale(player, ScaleTypes.WIDTH, width);
        setScale(player, ScaleTypes.MODEL_HEIGHT, height);
        setScale(player, ScaleTypes.MODEL_WIDTH, width);
    }

    public void applySpreadScale(ServerPlayer player, float width) {
        setScale(player, ScaleTypes.WIDTH, width);
        setScale(player, ScaleTypes.MODEL_WIDTH, width);
    }

    public void startReformTransition(ServerPlayer player, int durationTicks) {
        setTargetScale(player, ScaleTypes.HEIGHT, 1.0f, durationTicks);
        setTargetScale(player, ScaleTypes.WIDTH, 1.0f, durationTicks);
        setTargetScale(player, ScaleTypes.MODEL_HEIGHT, 1.0f, durationTicks);
        setTargetScale(player, ScaleTypes.MODEL_WIDTH, 1.0f, durationTicks);
    }

    public void resetToNormal(ServerPlayer player) {
        resetScale(player, ScaleTypes.HEIGHT);
        resetScale(player, ScaleTypes.WIDTH);
        resetScale(player, ScaleTypes.MODEL_HEIGHT);
        resetScale(player, ScaleTypes.MODEL_WIDTH);
    }

    private void setScale(ServerPlayer player, ScaleType type, float scale) {
        ScaleData data = type.getScaleData(player);
        data.setScale(scale);
    }

    private void setTargetScale(ServerPlayer player, ScaleType type, float target, int delay) {
        ScaleData data = type.getScaleData(player);
        data.setTargetScale(target);
        data.setScaleTickDelay(delay);
    }

    private void resetScale(ServerPlayer player, ScaleType type) {
        ScaleData data = type.getScaleData(player);
        data.resetScale();
    }
}
```

### Failure Handling

```java
public void applyFlattenedScale(ServerPlayer player, float height, float width) {
    try {
        // Pehkui calls...
    } catch (Exception e) {
        LOGGER.error("Pehkui scale application failed for {}", player.getName(), e);
        // State remains internally consistent
        // Player experiences no visual change but restrictions apply
    }
}
```

---

## NeoForge Integration

### EventBusSetup

Registers all event handlers to appropriate buses.

```java
public class EventBusSetup {
    public static void register(IEventBus modBus) {
        // MOD bus events
        modBus.addListener(LifecycleHandler::onCommonSetup);
        modBus.addListener(ModNetworking::registerPayloads);

        // GAME bus events
        NeoForge.EVENT_BUS.register(AnvilCollisionTrigger.class);
        NeoForge.EVENT_BUS.register(MovementBlocker.class);
        NeoForge.EVENT_BUS.register(InteractionBlocker.class);
        NeoForge.EVENT_BUS.register(ShadowDisabler.class);
        NeoForge.EVENT_BUS.register(FlattenEffects.class);
        NeoForge.EVENT_BUS.register(ConfigCache.class);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerClient(IEventBus modBus) {
        modBus.addListener(KeybindRegistry::register);
        NeoForge.EVENT_BUS.register(ReformInputHandler.class);
    }
}
```

### LifecycleHandler

Handles mod lifecycle events.

```java
public class LifecycleHandler {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Validate Pehkui
            if (!PehkuiScaleAdapter.isCompatible()) {
                throw new RuntimeException("Pehkui 3.8.3+ required");
            }

            // Register built-in trigger
            TriggerRegistry.INSTANCE.register(new AnvilCollisionTrigger());
        });
    }
}
```

## Requirements Traced

- **SI-1**: Pehkui integration
- **SI-2**: NeoForge event bus
- **CON-3**: Pehkui 3.8.3+ compatibility
- **DEP-1**: Pehkui dependency
- **DEP-2**: NeoForge APIs
