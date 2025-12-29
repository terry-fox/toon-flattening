package com.terryfox.toonflattening.reformation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Utility class for spatial validation checks during reformation.
 * <p>
 * Per SRS FR-REFORM.5: Validates clearance before allowing recovery.
 * Performance: ≤0.02ms per call (limited to 10 block checks).
 */
public final class ClearanceCalculator {
    private static final int MAX_CEILING_RAYCAST = 10;
    private static final float CLEARANCE_THRESHOLD = 0.75f;

    private ClearanceCalculator() {
    }

    /**
     * Check for anvil blocks within player AABB.
     * <p>
     * Per SRS FR-REFORM.6: Anvil-blocking detection.
     *
     * @param player Target player
     * @return True if any anvil block found within AABB
     */
    public static boolean hasAnvilAbove(Player player) {
        AABB playerBox = player.getBoundingBox();
        return BlockPos.betweenClosedStream(playerBox)
                .anyMatch(pos -> player.level().getBlockState(pos).is(BlockTags.ANVIL));
    }

    /**
     * Check if vertical space meets 75% of frozen pose height.
     * <p>
     * Per SRS FR-REFORM.5: Clearance validation.
     *
     * @param player Target player
     * @param frozenPose Player's frozen pose from FullyFlattened state
     * @return True if sufficient clearance available
     */
    public static boolean hasSufficientClearance(Player player, Pose frozenPose) {
        float requiredHeight = getPoseHeight(frozenPose) * CLEARANCE_THRESHOLD;
        float floorY = (float) player.getY(); // Bottom of AABB
        float ceilingY = findCeiling(player, floorY);

        return (ceilingY - floorY) >= requiredHeight;
    }

    /**
     * Get hitbox height for pose.
     * <p>
     * Per SRS FR-REFORM.5.1: Pose height lookup table.
     *
     * @param pose Player pose
     * @return Hitbox height in blocks
     */
    private static float getPoseHeight(Pose pose) {
        return switch (pose) {
            case STANDING -> 1.8f;
            case CROUCHING -> 1.5f;
            case SWIMMING, FALL_FLYING -> 0.6f;
            case SLEEPING -> 0.2f;
            default -> 1.8f;
        };
    }

    /**
     * Raycast upward to find ceiling (treat anvils as ceiling).
     * <p>
     * Per SRS FR-REFORM.5.2: Upward raycast with 10 block limit.
     *
     * @param player Target player
     * @param floorY Starting Y coordinate (player floor)
     * @return Ceiling Y coordinate, or floorY+10 if no ceiling found
     */
    private static float findCeiling(Player player, float floorY) {
        Level level = player.level();
        BlockPos.MutableBlockPos pos = player.blockPosition().mutable();

        for (int y = 0; y < MAX_CEILING_RAYCAST; y++) {
            pos.setY((int) floorY + y);
            BlockState state = level.getBlockState(pos);
            if (state.isCollisionShapeFullBlock(level, pos) || state.is(BlockTags.ANVIL)) {
                return pos.getY();
            }
        }

        return floorY + MAX_CEILING_RAYCAST; // No ceiling found, assume 10 blocks clearance
    }
}
