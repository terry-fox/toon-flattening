# Restriction Module Specification

**Package:** `com.terryfox.toonflattening.restriction`
**Side:** Both

## Purpose

Block player movement and interactions while flattened.

## Files

```
restriction/
├── MovementBlocker.java
├── InteractionBlocker.java
├── PoseFreezer.java
└── ShadowDisabler.java
```

## MovementBlocker

Prevents all movement while flattened.

```java
public class MovementBlocker {
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!FlattenAPI.isFlattened(player)) return;

        // Zero velocity
        player.setDeltaMovement(Vec3.ZERO);

        // Prevent jumping
        player.setOnGround(true);
    }
}
```

## InteractionBlocker

Blocks item use and inventory access.

```java
public class InteractionBlocker {
    @SubscribeEvent
    public void onItemUse(PlayerInteractEvent event) {
        if (!FlattenAPI.isFlattened(event.getEntity())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!FlattenAPI.isFlattened(event.getEntity())) return;
        // Close container
        event.getEntity().closeContainer();
    }
}
```

### Allowed Actions

- Chat messages (FR-MOVE.3)
- Command execution (FR-MOVE.4)

## PoseFreezer

Freezes player pose at moment of impact.

**Implementation:** Mixin into `Player.tick()`

```java
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void freezePose(CallbackInfo ci) {
        Player self = (Player)(Object)this;
        FlattenState state = FlattenAPI.getState(self);

        if (state.isFlattened() && state.frozenPose() != null) {
            FrozenPose frozen = state.frozenPose();
            self.setPose(frozen.pose());
            self.setYRot(frozen.yRot());
            self.setXRot(frozen.xRot());
            self.yBodyRot = frozen.bodyYRot();
        }
    }
}
```

## ShadowDisabler

Disables player shadow rendering while flattened.

**Implementation:** Event-based

```java
public class ShadowDisabler {
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!FlattenAPI.isFlattened(player)) return;

        // Disable shadow
        event.getPartialTick(); // Access render state
        // Set shadow radius to 0 via entity render dispatcher
    }
}
```

Alternative approach if event insufficient:
```java
// Mixin into EntityRenderDispatcher or LivingEntityRenderer
// Override getShadowRadius() to return 0 for flattened players
```

## Requirements Traced

- **FR-MOVE.1**: Zero velocity each tick
- **FR-MOVE.2**: Cancel item use
- **FR-MOVE.3**: Allow chat
- **FR-MOVE.4**: Allow commands
- **FR-MOVE.5**: Freeze pose at impact
- **FR-MOVE.6**: Prevent inventory changes
- **FR-MOVE.7**: Disable shadow
