# Toon Flattening Refactoring Plan

## Goal
Refactor to registry-based trigger system with per-cause config and scale transformations.

## Key Decisions
- **Scaling**: Pehkui for hitbox/gameplay (height/width), custom client renderer for visual depth
- **State**: First-wins with optional override priority per trigger
- **Reformation**: Per-cause timing and animations
- **Direction**: Store normal vector or direction enum in attachment for peel-off animation
- **Config**: Hot-reloadable trigger configs
- **Authority**: Server-authoritative triggering (no client-side velocity reliance)

## New Package Structure
```
com.terryfox.toonflattening/
├── api/                          # Public API
│   ├── FlattenTrigger.java       # Core interface
│   ├── FlattenContext.java       # Context record (velocity, direction, source)
│   ├── ScaleTransformation.java  # Height/width/depth scales
│   └── FlattenDirection.java     # Direction enum (UP, DOWN, NORTH, SOUTH, EAST, WEST)
├── registry/
│   ├── FlattenTriggerRegistry.java
│   └── BuiltinTriggers.java
├── trigger/
│   └── AnvilFlattenTrigger.java  # Migrated anvil logic
├── config/
│   ├── ToonFlatteningConfig.java # Global + per-trigger sections
│   ├── TriggerConfigSpec.java    # Per-trigger config builder (hot-reloadable)
│   └── TriggerDefaults.java      # Default values per cause
├── core/
│   ├── FlatteningService.java    # Central flatten/reform logic
│   └── FlatteningEventDispatcher.java  # Routes events to triggers
├── client/
│   └── DepthScaleRenderer.java   # Client-side visual depth scaling (NEW)
└── (existing packages unchanged)
```

## Core Abstractions

### FlattenTrigger Interface
```java
public interface FlattenTrigger {
    ResourceLocation getId();
    Optional<FlattenContext> shouldTrigger(LivingIncomingDamageEvent event, Player player);
    ScaleTransformation getScaleTransformation();
    int calculateAnimationTicks(FlattenContext context);
    int getReformationTicks();  // Per-cause reformation timing
    TriggerConfigSpec getConfigSpec();

    // Override priority: higher = can override lower priority flattened states
    // Default 0 = cannot override, only trigger on non-flattened players
    default int getOverridePriority() { return 0; }
}
```

### FlattenContext Record
```java
public record FlattenContext(
    ResourceLocation triggerId,
    double impactVelocity,
    FlattenDirection direction,  // Direction enum for peel-off animation
    @Nullable Entity sourceEntity
) {}
```

### FlattenDirection Enum
```java
public enum FlattenDirection {
    DOWN,   // Anvil, falling
    UP,     // Launched upward into ceiling
    NORTH, SOUTH, EAST, WEST  // Wall collisions
    // Codec for serialization
}
```

### ScaleTransformation Record
```java
public record ScaleTransformation(
    float heightScale,  // Pehkui HEIGHT
    float widthScale,   // Pehkui WIDTH
    float depthScale    // Client-side visual only (DepthScaleRenderer)
) {}
```

## Implementation Steps

### Phase 1: Core API
1. Create `api/FlattenTrigger.java`, `api/FlattenContext.java`, `api/ScaleTransformation.java`
2. Create `api/FlattenDirection.java` enum with codec
3. Create `config/TriggerConfigSpec.java`, `config/TriggerDefaults.java`
4. Create `registry/FlattenTriggerRegistry.java`

### Phase 2: Attachment Extension
5. Add `causeId` and `direction` fields to `FlattenedStateAttachment`
6. Update `SyncFlattenStatePayload` to include `causeId` and `direction`

### Phase 3: Service Layer
7. Create `core/FlatteningService.java` - extract from `FlatteningHandler.flattenPlayer()`
   - Implement first-wins logic with override priority check
8. Create `core/FlatteningEventDispatcher.java` - iterates triggers, checks override priority

### Phase 4: Client Depth Renderer
9. Create `client/DepthScaleRenderer.java` - visual depth scale via model transformation
   - Hook into RenderPlayerEvent or use RenderLayerParent
   - Read depth scale from synced state
   - Animate depth transitions

### Phase 5: Anvil Migration
10. Create `trigger/AnvilFlattenTrigger.java` implementing `FlattenTrigger`
11. Create `registry/BuiltinTriggers.java`
12. Refactor `ToonFlatteningConfig.java` for per-trigger configs + hot-reload

### Phase 6: Wiring
13. Update `ToonFlattening.java` to register triggers and use dispatcher
14. Update `LoginHandler`, `RespawnHandler`, `NetworkHandler` for new attachment
15. Delete `FlatteningHandler.java` and `FlattenCause.java`

## Files to Modify
- `attachment/FlattenedStateAttachment.java` - add causeId, direction
- `config/ToonFlatteningConfig.java` - per-trigger sections, hot-reload
- `network/SyncFlattenStatePayload.java` - add causeId, direction
- `network/NetworkHandler.java` - use FlatteningService, per-cause reformation
- `ToonFlattening.java` - wire registry and dispatcher
- `client/ClientEventHandler.java` - register depth renderer

## Files to Create
- `api/FlattenTrigger.java`
- `api/FlattenContext.java`
- `api/ScaleTransformation.java`
- `api/FlattenDirection.java`
- `config/TriggerConfigSpec.java`
- `config/TriggerDefaults.java`
- `registry/FlattenTriggerRegistry.java`
- `registry/BuiltinTriggers.java`
- `trigger/AnvilFlattenTrigger.java`
- `core/FlatteningService.java`
- `core/FlatteningEventDispatcher.java`
- `client/DepthScaleRenderer.java`

## Files to Delete
- `event/FlatteningHandler.java`
- `event/FlattenCause.java`
