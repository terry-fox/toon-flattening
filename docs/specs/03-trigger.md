# Trigger Module Specification

**Package:** `com.terryfox.toonflattening.trigger`
**Side:** Server

## Purpose

Extensible system for detecting flatten triggers (anvil collision, future custom triggers).

## Files

```
trigger/
├── TriggerRegistry.java
└── AnvilCollisionTrigger.java
```

## TriggerRegistry

Central registry for all flatten triggers.

```java
public class TriggerRegistry {
    private final List<IFlattenTrigger> triggers = new ArrayList<>();

    public void register(IFlattenTrigger trigger);

    public void unregister(ResourceLocation id);

    @Nullable
    public FlattenRequest checkTriggers(ServerPlayer player) {
        for (IFlattenTrigger trigger : triggers) {
            FlattenRequest request = trigger.shouldTriggerFlatten(player);
            if (request != null) return request;
        }
        return null;
    }
}
```

## AnvilCollisionTrigger

Default trigger detecting falling anvil damage.

```java
public class AnvilCollisionTrigger implements IFlattenTrigger {
    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath("toonflattening", "anvil");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    @Nullable
    public FlattenRequest shouldTriggerFlatten(ServerPlayer player) {
        // Not used for tick-based detection
        // Instead hooks LivingHurtEvent
        return null;
    }
}
```

### Event-Based Detection

```java
@SubscribeEvent
public void onLivingHurt(LivingHurtEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    if (player.isSpectator()) return;

    DamageSource source = event.getSource();
    if (!isFallingAnvilDamage(source)) return;

    // Cancel vanilla damage
    event.setCanceled(true);

    // Trigger flatten via API
    float damage = config.damageAmount * 2.0f; // hearts -> points
    FlattenAPI.flatten(player, damage);
}

private boolean isFallingAnvilDamage(DamageSource source) {
    return source.is(DamageTypes.FALLING_ANVIL);
}
```

## Requirements Traced

- **FR-ANVIL.1**: Detect falling anvil intersection
- **FR-ANVIL.2**: Exclude spectators
- **FR-ANVIL.3**: Apply to creative without damage
- **FR-ANVIL.4**: Apply to survival/adventure with damage
- **FR-ANVIL.5**: 4.0 hearts damage (configurable)
- **FR-ANVIL.6**: Consume collision event
- **API-EXT.1**: IFlattenTrigger interface
- **API-EXT.2**: Trigger registration
