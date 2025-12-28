# System Overview - Toon Flattening

## Context

Toon Flattening is a NeoForge 1.21 mod implementing cartoon-style player flattening mechanics. When an anvil contacts a player from above, the player is progressively compressed based on anvil-to-floor distance. Once fully compressed (5% height), movement/interaction is blocked until reformation via keybinding.

### External Dependencies
- **Pehkui 3.8.3+**: Entity scaling API (hard dependency)
- **NeoForge 21.1.214+**: Event bus, networking, attachments
- **Minecraft 1.21**: Entity tick system, collision detection

### Context Diagram

```
┌─────────────────────────────────────────────────┐
│           Third-Party Mods                      │
│  (consume events, trigger API, extend triggers) │
└───────────────────┬─────────────────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │   API Module         │
         │  (Events, Facade)    │
         └──────────┬───────────┘
                    │
         ┌──────────▼───────────┐
         │   Core Module        │
         │ (State Machine,      │
         │  Scale Calculator)   │
         └──┬────────────────┬──┘
            │                │
    ┌───────▼─────┐    ┌────▼─────────┐
    │ Detection   │    │ Restriction  │
    │ Module      │    │ Module       │
    └───────┬─────┘    └────┬─────────┘
            │                │
    ┌───────▼─────┐    ┌────▼─────────┐
    │ Reformation │    │ Integration  │
    │ Module      │    │ Module       │
    └───────┬─────┘    └────┬─────────┘
            │                │
            └────────┬───────┘
                     │
         ┌───────────▼──────────────┐
         │  Infrastructure Module   │
         │ (Persistence, Network,   │
         │  Config, Effects)        │
         └───────────┬──────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
┌────────┐    ┌───────────┐    ┌─────────┐
│ Pehkui │    │ NeoForge  │    │ Minecraft│
│  API   │    │ Event Bus │    │  Engine  │
└────────┘    └───────────┘    └─────────┘
```

## Architecture Characteristics (Ranked)

### 1. Performance (Critical)
**Rationale**: Tick processing executes per-player every server tick (50ms). Must maintain ≤0.1ms mean execution time to avoid TPS degradation with 100+ flattened players.

**Implications**:
- Single-threaded event handlers (no locks)
- Lazy state queries (no per-tick world scans)
- Minimal object allocation in hot paths
- Defer mod-specific adapters for V1 (reduce branching)

### 2. Interoperability (High)
**Rationale**: Minecraft modpacks combine dozens of mods. Must respect event cancellations, avoid movement conflicts, and provide integration hooks for custom triggers/effects.

**Implications**:
- EventPriority.NORMAL for detection (allow pre-cancellation)
- EventPriority.LOWEST for restrictions (allow other mods' movement)
- Expose cancellable events for third-party control
- NoOpScalingProvider for graceful degradation when Pehkui missing

### 3. Extensibility (Medium)
**Rationale**: Third-party mods may add custom flatten triggers (e.g., falling boulders), custom damage sources, or alternative scaling providers.

**Implications**:
- IFlattenTrigger interface with TriggerRegistry
- IScalingProvider abstraction with priority-based selection
- Public API facade (flatten(), reform(), getPhase())
- Pre/Post events for flatten/reform lifecycle

### 4. Maintainability (Medium)
**Rationale**: Modular separation reduces coupling for testing and future feature additions (e.g., sound effects, custom particles).

**Implications**:
- Module boundaries enforce data ownership
- Integration layer isolates external API changes
- Infrastructure module centralizes cross-cutting concerns

## Module Overview

| Module | Responsibilities | Key Artifacts |
|--------|------------------|---------------|
| **core** | State machine (4 phases), scale calculation, phase transitions | FlattenPhase, FlattenState, FlattenStateManager, ScaleCalculator |
| **detection** | Anvil contact detection (falling/placed), trigger system | AnvilContactDetector, IFlattenTrigger, TriggerRegistry |
| **restriction** | Movement/interaction blocking (FullyFlattened only), pose freezing | MovementRestriction, InteractionRestriction, PoseController |
| **reformation** | Recovery logic, clearance checks, fallback timer | ReformationHandler, ClearanceCalculator, FallbackTimer |
| **integration** | Pehkui abstraction, scaling provider registry, graceful degradation | IScalingProvider, PehkuiScalingProvider, NoOpScalingProvider, ScalingProviderRegistry |
| **api** | Public facade, cancellable events, extension points | ToonFlatteningAPI, Pre/PostFlattenEvent, Pre/PostReformEvent |
| **infrastructure** | Persistence (NBT attachments), networking, config, effects | PlayerDataAttachment, NetworkPackets, ConfigSpec, EffectHandler |

## Deployment Architecture

### Single-Player
```
Minecraft Client Process
├── Core (Client + Logical Server)
├── Detection (Server-side only)
├── Restriction (Client + Server)
└── Effects (Client-side rendering)
```

### Dedicated Server
```
Minecraft Server Process              Minecraft Client Process
├── Core (Logical Server)     ←───→   ├── Core (Client-side state sync)
├── Detection (Server-side)            ├── Restriction (Client prediction)
└── Persistence                        └── Effects (Rendering)
```

## Architectural Style

**Modular Monolith with Integration Layer**

- Modules deployed as single JAR artifact
- Compile-time package boundaries (no OSGi/ServiceLoader)
- Integration layer abstracts external APIs (Pehkui)
- Single-threaded assumption (no locks in state manager)
