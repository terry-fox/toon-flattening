# Architecture Decision Records - Toon Flattening

## ADR-001: Modular Monolith Architecture Style

**Status**: Accepted

**Context**:
- Minecraft mods deploy as single JAR files (no runtime module loading)
- NeoForge lacks OSGi/ServiceLoader infrastructure for hot-swappable modules
- State machine logic tightly coupled to Minecraft tick lifecycle
- Performance-critical code (per-tick processing) benefits from direct method calls vs event indirection

**Decision**:
Adopt modular monolith with package-level boundaries. Modules communicate via direct method calls (sync) or NeoForge event bus (async). All modules compile into single JAR artifact.

**Consequences**:
- **Positive**: Zero overhead for cross-module calls (no reflection/proxies)
- **Positive**: Single classloader simplifies dependency management
- **Positive**: Package-private enforcement via compile-time checks
- **Negative**: Cannot independently deploy/version modules
- **Negative**: Entire mod must reload on config changes (acceptable for Minecraft context)
- **Negative**: Circular dependency risk requires strict dependency direction enforcement

---

## ADR-002: Integration Layer for External Scaling APIs

**Status**: Accepted

**Context**:
- Pehkui is hard dependency for V1, but API may change in future versions
- Future versions may support alternative scaling mods (Pehkui alternatives)
- Third-party mods may provide custom scaling implementations
- Direct Pehkui API calls in core module create tight coupling

**Decision**:
Abstract scaling operations behind `IScalingProvider` interface. Provide `PehkuiScalingProvider` adapter for Pehkui API. Register providers via `ScalingProviderRegistry` with priority-based selection.

**Consequences**:
- **Positive**: Core module decoupled from Pehkui API changes
- **Positive**: Third-party scaling providers can register adapters
- **Positive**: Unit tests can inject mock scaling provider
- **Negative**: Additional abstraction layer adds complexity (~3 classes)
- **Negative**: Provider selection at runtime adds negligible overhead (<0.01ms)

---

## ADR-003: Graceful Degradation When Pehkui Missing

**Status**: Accepted

**Context**:
- Pehkui is declared hard dependency (mod crashes if missing)
- Server administrators may accidentally remove Pehkui JAR
- Future versions may introduce optional scaling (e.g., visual-only mode)
- Current implementation throws NPE if Pehkui API unavailable

**Decision**:
Implement `NoOpScalingProvider` that logs errors and returns success (no-op). Register as fallback provider with lowest priority. Log clear error messages indicating Pehkui missing.

**Consequences**:
- **Positive**: Mod loads without crashing if Pehkui removed
- **Positive**: Clear error logs guide administrators to solution
- **Positive**: Enables future optional-scaling modes
- **Negative**: Players experience broken behavior (no visual flattening) instead of immediate crash
- **Negative**: State machine still transitions phases but scales don't apply (confusing for players)
- **Negative**: Requires configuration flag to disable flattening entirely when Pehkui missing (future work)

---

## ADR-004: Priority-Based Scaling Provider Selection

**Status**: Accepted

**Context**:
- Multiple scaling providers may register (Pehkui, custom mods, NoOp fallback)
- Need deterministic selection when multiple providers available
- Some providers more capable than others (e.g., Pehkui vs NoOp)
- ServiceLoader-style "first found" is non-deterministic

**Decision**:
`ScalingProviderRegistry` uses explicit priority integers (higher = preferred). Providers return `canHandle(ServerPlayer)` boolean. First provider with highest priority that returns true is selected.

**Consequences**:
- **Positive**: Deterministic provider selection across restarts
- **Positive**: Third-party providers can override defaults via higher priority
- **Positive**: NoOp fallback guaranteed via lowest priority (Integer.MIN_VALUE)
- **Negative**: Priority conflicts require manual resolution (log warnings)
- **Negative**: No automatic conflict resolution (first-registered wins on tie)

---

## ADR-005: Event Priority Strategy (NORMAL for Detection, LOWEST for Restrictions)

**Status**: Accepted

**Context**:
- NeoForge event bus supports priority ordering (HIGHEST → LOWEST)
- Other mods may cancel anvil damage events (e.g., god-mode mods)
- Other mods may modify player movement (e.g., flight mods, speed buffs)
- Flattening detection should respect pre-cancellations
- Flattening restrictions should not override other mods' movement

**Decision**:
- Detection event handlers: `@SubscribeEvent(priority = EventPriority.NORMAL)`
- Restriction event handlers: `@SubscribeEvent(priority = EventPriority.LOWEST)`

**Consequences**:
- **Positive**: God-mode mods can prevent flattening via event cancellation
- **Positive**: Flight mods retain control over player movement during ProgressiveFlattening
- **Positive**: Restrictions apply last, only if no other mod handled event
- **Negative**: Other mods at LOWEST priority may conflict (rare)
- **Negative**: Movement restrictions in FullyFlattened phase can be bypassed by HIGH priority movement mods (acceptable trade-off for compatibility)

---

## ADR-006: Single-Threaded Assumption for API

**Status**: Accepted

**Context**:
- Minecraft server tick executes on single thread (main server thread)
- NeoForge event bus dispatches events synchronously on caller thread
- Client-side rendering executes on render thread (separate from logical client thread)
- State mutations during tick must be thread-safe if background tasks access state

**Decision**:
Assume all state mutations occur on server/logical client thread. No locks in `FlattenStateManager`. Document API methods as "must be called on server thread". Expose state queries as thread-safe (immutable snapshots).

**Consequences**:
- **Positive**: Zero synchronization overhead in hot paths (per-tick processing)
- **Positive**: No deadlock risk from lock ordering
- **Positive**: Simpler state manager implementation
- **Negative**: Third-party mods calling API from background threads will corrupt state (document as unsupported)
- **Negative**: Client-side rendering must copy state snapshots (minor GC pressure)
- **Negative**: Future async features (e.g., database persistence) require explicit synchronization

---

## ADR-007: Re-Flatten Damage Threshold

**Status**: Superseded by ADR-010

**Context**:
- Re-flatten events can occur from anvils continuously resting on FullyFlattened players
- Applying damage for every anvil contact (even stationary anvils) creates unfair gameplay
- Need to distinguish between "anvil was lifted and dropped again" vs "anvil is just sitting there"
- Anvil gap = vertical distance anvil moved since last re-flatten

**Decision**:
Only apply damage on re-flatten if anvil gap exceeds `reflatten_damage_threshold` (default: 0.09 blocks). Continuous resting anvils increase spread but do not damage. Track `lastAnvilY` in FlattenState to calculate gap. Configurable threshold (0.01-1.0 blocks).

**Consequences**:
- **Positive**: Prevents unfair damage from stationary anvils
- **Positive**: Maintains cartoon physics (spread increases regardless of damage)
- **Positive**: Server administrators can adjust threshold for custom gameplay
- **Negative**: Adds complexity to re-flatten logic (gap calculation)
- **Negative**: Edge case: player could stack spread infinitely with stationary anvil (mitigated by max_spread_limit)

**Superseded By**: ADR-010 replaces gap-based damage with anvil-count-based spread and replacement vs stacking detection.

---

## ADR-008: Pehkui Scale Override Strategy

**Status**: Accepted

**Context**:
- Pehkui allows multiple mods to modify entity scales simultaneously
- Other mods may apply temporary scale changes (growth potions, size abilities)
- Toon Flattening must maintain consistent flattened appearance
- Pehkui uses "last write wins" for scale conflicts

**Decision**:
Toon Flattening overwrites Pehkui scales every tick during all flattening phases (ProgressiveFlattening, FullyFlattened, Recovering). Integration module calls `IScalingProvider.setScales()` every tick. Document conflict behavior in compatibility guide.

**Consequences**:
- **Positive**: Guarantees consistent flattened appearance (no conflicts)
- **Positive**: Simple implementation (no conflict resolution logic)
- **Positive**: Respects Pehkui-modified bounding boxes for anvil detection (uses vanilla Entity.getBoundingBox())
- **Negative**: Overrides other mods' scale changes during flattening (potential incompatibility)
- **Negative**: Other mods cannot temporarily resize flattened players (acceptable trade-off)
- **Negative**: Documented breaking change for mods expecting Pehkui scale persistence

---

## ADR-009: Creative Mode Restriction

**Status**: Accepted

**Context**:
- Creative mode players can fly, which conflicts with FullyFlattened "grounded" physics
- Switching to creative mode could allow players to escape flattened state
- Flattened state should persist across gamemode switches for consistency
- Creative mode should not grant immunity to cartoon physics

**Decision**:
Disable creative flying (force `abilities.flying = false`) while in FullyFlattened phase. Persist flattened state on gamemode switch (FullyFlattened → Creative → still FullyFlattened). Re-enable creative flying upon transition to Normal phase.

**Consequences**:
- **Positive**: Maintains cartoon physics consistency in creative mode
- **Positive**: Prevents gamemode-switch exploit to escape flattening
- **Positive**: Flattened state behavior identical across all gamemodes
- **Negative**: Creative players lose flight capability while flattened (may frustrate builders)
- **Negative**: Unconventional restriction (most mods respect creative mode invulnerability)
- **Negative**: May conflict with mods granting permanent flight in creative mode

---

## ADR-010: Anvil-Count-Based Re-Flatten Mechanics

**Status**: Accepted

**Context**:
- ADR-007's gap-based damage threshold doesn't match desired cartoon physics
- Players expect spread to scale with number of anvils in stack, not timing of contacts
- Two distinct scenarios: (1) stacking anvils on top, (2) replacing anvil before reformation
- Initial flatten with multi-anvil stack should apply spread immediately, not incrementally
- Damage should distinguish between adding to existing stack (no damage) vs replacing anvil (damage)

**Decision**:
Re-flatten mechanics based on anvil count and contact state:

**Spread Calculation**:
- Spread added = `anvil_count × spread_increment` (where anvil_count = height of vertically stacked anvils)
- Initial flatten with N-anvil stack: spread = N × spread_increment
- Spread accumulates across flatten/re-flatten cycles

**Damage Calculation** (scenario-based):
- **Stacking**: Anvil contact present when new anvil lands → NO damage (spread only)
- **Replacement**: No anvil contact when new anvil lands → `base_damage + (anvil_count - 1) × stack_damage_per_anvil`
- **Initial**: First flatten with N anvils → `base_damage + (N - 1) × stack_damage_per_anvil`

**Replacement Detection**:
- Track `hasContactingAnvil` boolean in detection module
- If anvil contact lost then re-gained before Normal phase → replacement scenario

**Consequences**:
- **Positive**: Intuitive spread scaling (more anvils = more spread)
- **Positive**: Prevents damage spam from stacking (cartoon physics)
- **Positive**: Replacement damage maintains challenge
- **Positive**: Simpler configuration (no gap threshold tuning)
- **Negative**: Anvil stack counting adds per-tick overhead (~5 block checks)
- **Negative**: Replacement detection requires tracking contact state (extra boolean in state)
- **Negative**: Breaking change from gap-based damage (incompatible with v1 behavior)

---

## ADR-011: Recovering Phase Re-Flatten Handling

**Status**: Accepted

**Context**:
- Original design: re-flatten only applies during FullyFlattened phase
- Players expect anvil dropped during recovery animation to re-flatten them
- Recovering phase has partial compression (scales between flattened and normal)
- Re-flattening during Recovering should preserve accumulated spread

**Decision**:
Allow re-flatten during Recovering phase with progressive compression restart:

**Phase Transition**:
- Recovering + anvil contact → ProgressiveFlattening (from current scale)
- Progressive compression continues until height = 0.05 → FullyFlattened
- This is treated as "replacement" scenario (no contacting anvil was present)

**Spread Preservation**:
- Existing spread multiplier carries forward through Recovering → ProgressiveFlattening → FullyFlattened
- New anvil spread added when reaching FullyFlattened: `spread += anvil_count × spread_increment`

**Damage Application**:
- Damage applied when transitioning to FullyFlattened (not during ProgressiveFlattening)
- Uses replacement formula: `base_damage + (anvil_count - 1) × stack_damage_per_anvil`

**Consequences**:
- **Positive**: Consistent anvil behavior across all non-Normal phases
- **Positive**: No "safe window" during recovery animation
- **Positive**: Spread accumulation feels natural (doesn't reset mid-recovery)
- **Negative**: Adds complexity to phase transition logic (Recovering → ProgressiveFlattening not in original design)
- **Negative**: Recovery animation can be interrupted (may frustrate players expecting guaranteed escape)
- **Negative**: State machine has additional edge (increases test surface)