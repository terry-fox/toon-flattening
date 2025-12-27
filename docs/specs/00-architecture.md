# Architecture Overview

## Package Structure

```
com.terryfox.toonflattening/
├── ToonFlattening.java              # Entry point
├── api/                             # Public API
├── core/                            # State machine, logic
├── trigger/                         # Flatten triggers
├── restriction/                     # Movement/interaction blocking
├── integration/                     # Pehkui, NeoForge
├── persistence/                     # NBT serialization
├── network/                         # Client-server sync
├── config/                          # TOML hot-reload
├── effect/                          # Particles/sounds
├── input/                           # Keybinding
└── mixin/                           # Minecraft hooks
```

## Module Responsibilities

| Module | Responsibility | Side |
|--------|----------------|------|
| `api` | Public contracts | Both |
| `core` | State machine, logic | Server |
| `trigger` | Extensible triggers | Server |
| `restriction` | Block movement/interaction | Both |
| `integration` | Pehkui/NeoForge wiring | Both |
| `persistence` | NBT serialization | Server |
| `network` | Packets | Both |
| `config` | TOML hot-reload | Both |
| `effect` | Particles/sounds | Both |
| `input` | Keybinding | Client |
| `mixin` | MC hooks | Both |

## Data Flow

### Flatten
```
Anvil collision -> AnvilCollisionTrigger -> FlattenStateManager.applyFlatten()
  -> PreFlattenEvent (cancellable)
  -> Apply damage + Pehkui scales + NBT persist
  -> FlattenSyncPacket -> clients
  -> PostFlattenEvent
  -> Particles/sound
```

### Reform
```
Keybind -> ReformRequestPacket -> FlattenStateManager.initiateReform()
  -> ReformationController.canReform() [anvil/clearance checks]
  -> PreReformEvent (cancellable)
  -> Animated scale interpolation (Pehkui setTargetScale)
  -> Release movement/pose/shadow
  -> FlattenSyncPacket -> clients
  -> PostReformEvent
```

## Design Decisions

| Decision | Choice |
|----------|--------|
| Shadow disable | `RenderLivingEvent.Pre` (event-based) |
| Anvil detection | `LivingHurtEvent` |
| Fallback timer | Persist across logout |
| Re-flatten during reform | Allow in first 1/3 of animation only |
| Pehkui animation | `setTargetScale()` + `setScaleTickDelay()` |
| Inventory block | `ContainerMenuTickEvent` |
| Pose freeze | Mixin `Player.tick()` |

## Dependencies

- NeoForge 21.1.214+
- Pehkui 3.8.3+ (hard dependency)
- Minecraft 1.21
- Java 21
