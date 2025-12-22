## Single-Player Tests
- [ ] Basic flatten: Drop anvil on player → flattens to 1/20 height
- [ ] Damage applied: Config damage amount matches health reduction
- [ ] Immobilization: Cannot move while flattened
- [ ] Jump blocked: Cannot jump while flattened
- [ ] Reform: Press space → player returns to normal height
- [ ] Sound plays: Flatten sound on flatten event
- [ ] Animation: Particle burst visible on flatten
- [ ] Multiple anvils: Drop second anvil on flattened player → works
- [ ] Death while flattened: Player respawns still flattened
- [ ] Config changes: Modify flattenDamage in .toml → takes effect on new flatten
- [ ] Height scale config: Verify 0.05 (1/20) calculation matches visual

## Multiplayer Tests
- [ ] Initial sync: Join world while other player flattened → see flattened state
- [ ] Flatten sync: Flatten player → visible to other clients
- [ ] Reform sync: Reform → other clients see return to normal
- [ ] Animation visible: Other players see squash animation on flatten
- [ ] Sound heard: Other players hear flatten sound
- [ ] Multiple players: Both flatten independently → both work

## Edge Cases
- [ ] Creative mode: Can still be flattened
- [ ] Spectator mode: Cannot be flattened
- [ ] Rapid anvils: Drop multiple anvils quickly → all work
- [ ] Different damage configs: Change config, re-flatten → correct damage
- [ ] Config per-world: Different worlds with different configs
- [ ] Other mods: Verify compatibility (test with common mods)

## Performance
- [ ] No lag spikes on flatten
- [ ] Network packets reasonable size
- [ ] Animation smooth (no stutter)
- [ ] No memory leaks (play extended session)