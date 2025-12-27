# Effect Module Specification

**Package:** `com.terryfox.toonflattening.effect`
**Side:** Both

## Purpose

Visual and audio feedback for flatten/reform events.

## Files

```
effect/
└── FlattenEffects.java
```

## FlattenEffects

Handles particle spawning and sound playback.

```java
public class FlattenEffects {

    @SubscribeEvent
    public void onPostFlatten(PostFlattenEvent event) {
        ServerPlayer player = event.getPlayer();
        spawnFlattenParticles(player);
        playFlattenSound(player);
    }

    private void spawnFlattenParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();

        // Spawn 25 POOF particles
        for (int i = 0; i < 25; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

            level.sendParticles(
                ParticleTypes.POOF,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                1,      // count
                0, 0, 0, // velocity
                0       // speed
            );
        }
    }

    private void playFlattenSound(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();

        // Custom sound or vanilla fallback
        level.playSound(
            null,                           // no excluded player
            pos.x, pos.y, pos.z,
            ModSounds.FLATTEN.get(),        // or SoundEvents.ANVIL_LAND
            SoundSource.PLAYERS,
            1.0f,                           // volume
            1.0f                            // pitch
        );
    }
}
```

## Sound Registration

```java
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ToonFlattening.MODID);

    public static final Supplier<SoundEvent> FLATTEN = SOUNDS.register("flatten",
        () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "flatten")
        )
    );

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
```

## Sound Asset

```
src/main/resources/assets/toonflattening/sounds/flatten.ogg
```

### sounds.json

```json
{
    "flatten": {
        "category": "players",
        "sounds": [
            "toonflattening:flatten"
        ]
    }
}
```

## Requirements Traced

- **UI-2**: Visual feedback during flattening
- **UI-2.1**: 25 POOF particles
- **UI-3**: Audio feedback during flattening
- **UI-3.2**: Sound category PLAYERS
- **FR-VFX.1**: Spawn 25 POOF particles
- **FR-VFX.1.2**: Use ParticleTypes.POOF
- **FR-VFX.2**: Play sound at player location
- **FR-VFX.2.2**: Sound category PLAYERS

## TBD Items

- **TBD-1**: Sound source specification (currently using custom)
- **TBD-2**: Particle count configurability
- **TBD-4**: Particle spawn pattern
- **TBD-5**: Reformation feedback
