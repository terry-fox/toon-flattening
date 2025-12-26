## Version 0.1.0

- [x] Players shall be flattened by an anvil when it falls on them
- [x] The amount to flatten a player by shall be configurable in the server config files
- [x] Players shall remain immobilized until reforming via the reforming action
- [x] The reform action should be mapped to the same key as the jump action by default
- [x] There shall be a custom sound effect when a player is flattened (Mario Party 4 squish by default)
- [x] The player should squish down at the same rate as the anvil that is falling on them
- [x] The custom flattening sound effect shall be audible by other players in a multiplayer setting, if the player is in range
- [x] The custom flattening animation shall be visible to other players in a multiplayer setting

## Version 0.2.0

- [x] Players shall spread horizontally after being squished
- [x] The amount of horizontal spread shall be configurable in the server config files

## Version 0.3.0
(Moved to later version)
- [x] ~~Players shall flatten when hitting a floor, wall or ceiling at a velocity that would deal 4 hearts of damage when landing on the ground after falling with no special armor or enchantements~~
- [x] ~~A player shall not be able to be pushed while flattened~~
- [x] ~~Players shall be able to walk directly through flattened players without "bumping" over them~~
- [x] ~~Players shall be immune to being damaged from mob or player attacks while flattened~~
- [x] ~~Players shall be immune to being damaged while restoring and 1 second after their restoration animation has finished~~
- [x] ~~Players in Creative mode should be able to be flattened by all flattening methods~~
- [x] ~~Player models shall not move at all while flattened~~

## Version 0.4.0
- [x] Players in creative mode shall be flattenable
- [x] Players in spectator mode shall not be flattenable
- [x] A player's model shall freeze in the middle of its current state when it is hit
- [x] No animations except for lighting and particle effects (eg from enchanted armor) shall occur while a player is flattened
- [ ] When an anvil is dropped on a player while they are flattened, the player shall continue to gradually spread up to a maximum limit
- [ ] The width of the player immediately after the first anvil impact shall be determined by the existing horizontal spread setting in the server files
- [ ] The maximum spread limit shall be configurable in the server config files
- [ ] The spreading effect begins when a falling anvil strikes a player who is already in the flattened state, or when an anvil lands on top of a stack of anvils flattening the player
- [ ] When spreading, the player shall not suffer additional immediate deformation. Instead, the player shall begin a gradual spreading process.
- [ ] The player's width when spreading shall smoothly increase over time, moving from their current size toward the maximum spread limit

## Version 0.5.0
- [ ] There shall be a custom hammer powerup to allow players to squish other players and mobs

## Version 0.6.0
- [ ] Players shall be flattened by pistons when compressed between the head and a solid block such as a wall
- [ ] The mod should an option to stick to a piston head when it retracts (thoughts: random chance for stick, 100% chance for sticky pistons?)

## Version 0.7.0
- [ ] Players shall be flattened by pistons when compressed between the head and a solid block such as a wall
- [ ] The mod should an option to stick to a piston head when it retracts (thoughts: random chance for stick, 100% chance for sticky pistons?)

## Version 0.8.0
- [ ] When a player enters a minecart or boat with a player already in it, the player shall flatten under the player entering the minecart/boat

## Version 0.9.0
- [ ] Iron Golems shall flatten players
- [ ] The mod may have an option to leave a still-flattened clone of a player when they die and respawn from flattening
- [ ] Players may take on random poses when flattened