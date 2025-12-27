# Persistence Module Specification

**Package:** `com.terryfox.toonflattening.persistence`
**Side:** Server

## Purpose

NBT serialization and NeoForge attachment system for state persistence.

## Files

```
persistence/
├── FlattenDataAttachment.java
└── FlattenDataSerializer.java
```

## FlattenDataAttachment

NeoForge AttachmentType registration.

```java
public class FlattenDataAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ToonFlattening.MODID);

    public static final Supplier<AttachmentType<FlattenState>> FLATTEN_STATE =
        ATTACHMENTS.register("flatten_state", () ->
            AttachmentType.builder(FlattenState::empty)
                .serialize(FlattenDataSerializer.CODEC)
                .copyOnDeath(false)  // FR-STATE.3: reset on death
                .build()
        );

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
```

## FlattenDataSerializer

Codec-based NBT serialization.

```java
public class FlattenDataSerializer {
    public static final Codec<FrozenPose> FROZEN_POSE_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Pose.CODEC.fieldOf("pose").forGetter(FrozenPose::pose),
            Codec.FLOAT.fieldOf("yRot").forGetter(FrozenPose::yRot),
            Codec.FLOAT.fieldOf("xRot").forGetter(FrozenPose::xRot),
            Codec.FLOAT.fieldOf("bodyYRot").forGetter(FrozenPose::bodyYRot)
        ).apply(instance, FrozenPose::new)
    );

    public static final Codec<FlattenState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("isFlattened").forGetter(FlattenState::isFlattened),
            Codec.FLOAT.fieldOf("spreadMultiplier").forGetter(FlattenState::spreadMultiplier),
            Codec.INT.fieldOf("fallbackTicksRemaining").forGetter(FlattenState::fallbackTicksRemaining),
            FROZEN_POSE_CODEC.optionalFieldOf("frozenPose").forGetter(s -> Optional.ofNullable(s.frozenPose())),
            Codec.INT.fieldOf("reformTicksRemaining").forGetter(FlattenState::reformTicksRemaining),
            Codec.LONG.fieldOf("flattenedAtTick").forGetter(FlattenState::flattenedAtTick)
        ).apply(instance, (flat, spread, fallback, pose, reform, tick) ->
            new FlattenState(flat, spread, fallback, pose.orElse(null), reform, tick))
    );
}
```

## NBT Schema

```nbt
toonflattening:flatten_state: {
    isFlattened: boolean
    spreadMultiplier: float
    fallbackTicksRemaining: int
    flattenedAtTick: long
    reformTicksRemaining: int
    frozenPose: {           # optional
        pose: string        # STANDING, CROUCHING, SWIMMING, SLEEPING
        yRot: float
        xRot: float
        bodyYRot: float
    }
}
```

## Persistence Lifecycle

| Event | Action |
|-------|--------|
| Player logout | Automatic via attachment system |
| Player login | Restore from attachment, sync to client, re-apply Pehkui |
| Player death | Clear (copyOnDeath=false) |
| Player respawn | Fresh FlattenState.empty() |

## State Access

```java
public static FlattenState getState(Player player) {
    return player.getData(FlattenDataAttachment.FLATTEN_STATE);
}

public static void setState(ServerPlayer player, FlattenState state) {
    player.setData(FlattenDataAttachment.FLATTEN_STATE, state);
}
```

## Requirements Traced

- **FR-STATE.1**: Persist on logout
- **FR-STATE.2**: Restore on login (scale, spread, pose)
- **FR-STATE.3**: Reset on respawn
- **FR-STATE.5**: Store via NBT
- **NFR-REL.1**: 100% reliability across restart
