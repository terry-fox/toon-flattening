## Single-Player Tests
- [ ] Anvil flatten: Drop anvil on player → flattens (height/width scale per config)
- [ ] Damage applied: Config damage amount matches health reduction
- [ ] Immobilization: Cannot move while flattened
- [ ] Jump blocked: Cannot jump while flattened
- [ ] Reform: Press jump key → player returns to normal size
- [ ] Sound plays: Flatten sound audible on flatten event
- [ ] Squash animation: Player squishes at anvil fall velocity rate
- [ ] Pose frozen: Player model freezes in current pose when flattened
- [ ] Horizontal spread: Player width increases based on widthScale config
- [ ] Movement cancelled: Sprinting, swimming, riding, elytra all stopped on flatten
- [ ] Config changes: Modify trigger configs in .toml → takes effect on new flatten
- [ ] Reform animation: Reformation takes reformationTicks duration

## Multiplayer Tests
- [ ] Initial sync: Join world while other player flattened → see flattened state
- [ ] Flatten sync: Flatten player → visible to other clients
- [ ] Reform sync: Reform → other clients see return to normal
- [ ] Animation visible: Other players see squash animation on flatten
- [ ] Sound heard: Other players hear flatten sound in range
- [ ] Pose sync: Frozen pose visible to other clients
- [ ] Multiple players: Both flatten independently → both work

## Edge Cases
- [ ] Creative mode: Can be flattened, no damage dealt
- [ ] Spectator mode: Cannot be flattened
- [ ] Already flattened: Drop second anvil on flattened player → no effect (first-wins)
- [ ] Trigger disabled: Set trigger enabled=false → no flattening
- [ ] Height scale range: Test min 0.01 to max 1.0 values
- [ ] Width scale range: Test min 1.0 to max 6.0 values
- [ ] Damage range: Test 0.0 to 20.0 damage values
- [ ] Velocity-based timing: Fast anvil = short animation, slow anvil = longer

## Performance
- [ ] No lag spikes on flatten
- [ ] Network packets reasonable size (sync payload, squash animation)
- [ ] Animation smooth (no stutter during squash/reform)
- [ ] No memory leaks (play extended session)
- [ ] Pehkui scale operations efficient
