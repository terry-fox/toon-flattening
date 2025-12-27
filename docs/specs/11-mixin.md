# Mixin Module Specification

**Package:** `com.terryfox.toonflattening.mixin`
**Side:** Both

## Purpose

Minecraft class hooks where events are insufficient.

## Files

```
mixin/
└── PlayerMixin.java
```

## PlayerMixin

Handles pose freezing.

```java
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void toonflattening$freezePose(CallbackInfo ci) {
        Player self = (Player)(Object)this;

        // Get state (works both sides via attachment/sync)
        if (!FlattenAPI.isFlattened(self)) return;

        FlattenState state = FlattenDataAttachment.getState(self);
        FrozenPose frozen = state.frozenPose();
        if (frozen == null) return;

        // Force pose
        self.setPose(frozen.pose());
        self.setYRot(frozen.yRot());
        self.setXRot(frozen.xRot());
        self.yBodyRot = frozen.bodyYRot();
        self.yHeadRot = frozen.yRot();
    }
}
```

## Mixin Configuration

### toonflattening.mixins.json

```json
{
    "required": true,
    "package": "com.terryfox.toonflattening.mixin",
    "compatibilityLevel": "JAVA_21",
    "minVersion": "0.8",
    "mixins": [
        "PlayerMixin"
    ],
    "client": [],
    "server": [],
    "injectors": {
        "defaultRequire": 1
    }
}
```

## Build Configuration

### build.gradle additions

```groovy
mixin {
    add(sourceSets.main, "toonflattening.refmap.json")
    config("toonflattening.mixins.json")
}
```

### neoforge.mods.toml additions

```toml
[[mixins]]
config = "toonflattening.mixins.json"
```

## Avoided Mixins

These were considered but event-based solutions are preferred:

| Concern | Solution | Why Not Mixin |
|---------|----------|---------------|
| Shadow disable | `RenderLivingEvent.Pre` | Event works |
| Movement block | `PlayerTickEvent` | Event works |
| Inventory block | `ContainerMenuTickEvent` | Event works |
| Anvil detection | `LivingHurtEvent` | Event works |

## Requirements Traced

- **FR-MOVE.5**: Freeze pose at impact
- **FR-MOVE.5.1**: Persist until reformation
- **FR-REFORM.7**: Unfreeze on reform (handled by not applying freeze)
- **CON-2**: Mixin-based approach (not base class modification)
