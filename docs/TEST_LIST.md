## v0.6.0

### Re-drop Spread

Basic Spread Accumulation

- First anvil: flatten + animation + particles + sound
- Subsequent anvils: spread increase + sound only
- Reform: returns to normal

Spread Reset

- Verify spread resets after reform, not cumulative across sessions

Max Width Cap

- Verify 6.0x cap

Damage Consistency

- Same damage each hit

Persistence

- Spread level survives disconnect/reconnect

Config Validation

- Test custom spreadMultiplier and maxSpreadWidth values

Edge: Reform While Pinned

- Spread resets properly with anvil pinning enabled

### Stacking Spread

Basic Stacking Behavior

- Flatten player with first anvil (normal: damage + sound + particles)
- Stack second anvil on top of first anvil
- Verify player spreads wider (NO damage, NO sound, NO particles)
- Stack third anvil, verify further spread
- Verify spread stops at maxSpreadWidth (6.0x default)

Stacking Requirements

- Anvils must be contiguous (no gaps)
- Place anvil with gap above first anvil - verify NO spread
- Fill gap, then stack - verify spread triggers

Raycast Detection (20 blocks)

- Flatten player at ground level
- Build anvil stack 5 blocks above player - verify spread works
- Build anvil stack 15 blocks above player - verify spread works
- Build anvil stack 21+ blocks above player - verify NO spread (beyond raycast)

Non-Stacking Scenarios

- Drop anvil directly on flattened player (not on another anvil) - verify normal behavior (damage + sound)
- Place anvil block manually on stack - verify NO spread (only falling anvils)

Multiple Players

- Flatten two players side-by-side
- Stack anvil above player A only - verify only player A spreads
- Verify player B unchanged

Persistence

- Stack anvils to spread level 3
- Disconnect/reconnect - verify spread level persists
- Add another anvil to stack - verify continues from level 3

Max Spread Interaction

- Hit player with anvils until maxSpreadWidth reached
- Stack more anvils - verify NO further spread
- Check logs for "already at max spread" message

Pinning Timeout

- Flatten player with anvil
- Stack additional anvils
- Verify pinning timeout NOT reset (continues from first flatten time)