# Restriction Module Specification

## Responsibilities

- **Movement Blocking**: Set player velocity to zero while in FullyFlattened phase only
- **Interaction Blocking**: Cancel item use events (right-click, left-click) while FullyFlattened
- **Pose Freezing**: Capture and restore player pose (standing, sneaking, swimming, sleeping) at FullyFlattened transition
- **Shadow Disabling**: Hide player shadow rendering while FullyFlattened
- **Phase-Aware Restrictions**: Allow movement/interaction during ProgressiveFlattening and Recovering phases

## Data Ownership

### Exclusive Data
None (reads FlattenPhase from core module)

### Read-Only Dependencies
- Current FlattenPhase (from core)
- Frozen pose data (from core FlattenState)
- Player entity (Minecraft)

## Communication

### Incoming (Afferent Coupling)
- **infrastructure** → Event bus dispatches movement/interaction events

### Outgoing (Efferent Coupling)
- **core** → `FlattenStateManager.getPhase(Player)` (query)
- **core** → `FlattenStateManager.getState(Player)` (query for frozen pose)

## Key Classes

### MovementRestriction (Event Handler)
```java
public class MovementRestriction {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            FlattenPhase phase = FlattenStateManager.getPhase(player);
            if (phase == FlattenPhase.FULLY_FLATTENED) {
                player.setDeltaMovement(Vec3.ZERO);
                player.setOnGround(true); // Prevent fall damage accumulation

                // Disable creative flying, force to ground
                if (player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityPush(LivingEntityPushEvent event) {
        if (event.getEntity() instanceof Player player) {
            FlattenPhase phase = FlattenStateManager.getPhase(player);
            if (phase == FlattenPhase.FULLY_FLATTENED) {
                event.setCanceled(true); // Block external entity pushing
            }
        }
    }
}
```

### InteractionRestriction (Event Handler)
```java
public class InteractionRestriction {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Blocks eating, drinking, shield blocking, attacking
        Player player = event.getEntity();
        FlattenPhase phase = FlattenStateManager.getPhase(player);
        if (phase == FlattenPhase.FULLY_FLATTENED) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Same logic as above
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Same logic as above
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        // Persist flattened state across gamemode changes
        // State restrictions continue to apply in creative mode
    }
}
```

### PoseController
```java
public class PoseController {
    // Called by core when transitioning to FullyFlattened
    public static Pose capturePose(Player player) {
        return player.getPose();
    }

    // Called by core during FullyFlattened tick
    public static void applyFrozenPose(Player player, Pose frozenPose) {
        player.setPose(frozenPose);
    }

    // Called by core when transitioning to Normal
    public static void releasePose(Player player) {
        // Allow Minecraft to recalculate pose naturally
    }
}
```

### ShadowRenderer (Client-Side Mixin)
```java
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private void onRenderShadow(CallbackInfo ci) {
        Player player = getPlayer();
        FlattenPhase phase = FlattenStateManager.getPhase(player);
        if (phase == FlattenPhase.FULLY_FLATTENED) {
            ci.cancel(); // Skip shadow rendering
        }
    }
}
```

## Event Priority Rationale

**EventPriority.LOWEST** for restrictions:
- Allows flight mods to control movement during ProgressiveFlattening (executed first)
- Allows speed buff mods to modify velocity before restriction applies
- Restrictions apply as final override only for FullyFlattened phase
- Trade-off: HIGH priority movement mods can bypass restrictions (acceptable for compatibility)

## Phase-Specific Behavior

| Phase | Movement | Interaction | Pose | Shadow | Creative Flight |
|-------|----------|-------------|------|--------|-----------------|
| Normal | Allowed | Allowed | Dynamic | Rendered | Allowed |
| ProgressiveFlattening | **Allowed** | **Allowed** | Dynamic | Rendered | Allowed |
| FullyFlattened | **Blocked** | **Blocked** | **Frozen** | **Hidden** | **Disabled** |
| Recovering | **Allowed** | **Allowed** | Dynamic | Rendered | Allowed |

## Coupling Metrics

- **Afferent Coupling (Ca)**: 1 (infrastructure via event bus)
- **Efferent Coupling (Ce)**: 1 (core)
- **Instability (I)**: 0.5 (balanced)

## Performance Constraints

- **Per-Tick Movement Check**: ≤0.01ms (simple phase enum check)
- **Event Cancellation**: ≤0.005ms (no complex logic, just cancel)
