## TODO for v0.6

* [x] Anvil spread case 1:
  1. Detect anvil–player collision
  1. Apply damage to player
  1. Flatten player
  1. Trigger flattening particle effects
  1. Play first-hit SFX
  1. Break anvil
  1. Re-drop broken anvil above flattened player
  1. Apply damage equal to first hit
  1. Play second-hit SFX
  1. Suppress particle effects on second hit

* [x] Anvil spread case 2:
  1. Detect anvil–player collision
  1. Apply damage to player
  1. Flatten player
  1. Trigger flattening particle effects
  1. Play hit SFX
  1. Second anvil dropped on top of first anvil
  1. No damage, SFX, particles on second hit
  1. Apply spread
  1. Repeat for further anvils, further applying spread until max spread hit

* [x] Prevent the player from being pushed by mobs, other players, minecarts, boats (other vehicles or rideable objects) from colliding with the flattened player

* [x] Prevent mobs, piston heads, other players, minecarts, boats (other vehicles or rideable objects) from colliding with the flattened player

* [x] Prevent the player from breaking the anvil that is squashing them

* [x] Prevent accidental suffocation in walls
