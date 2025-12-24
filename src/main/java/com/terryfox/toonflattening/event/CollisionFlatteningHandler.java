package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.OptionalDouble;

/**
 * Handles collision-based flattening for floor, ceiling, and wall impacts.
 *
 * Uses double-buffered velocity tracking to detect sudden velocity changes from collisions.
 * We need velocity BEFORE collision processing because Minecraft modifies it during that phase.
 *
 * WHY DESIGN DECISIONS:
 * - Use onGround() for floor detection: Set immediately on landing, not delayed by tick
 * - Double-buffering velocity: Prevents race conditions between read and write
 * - Pre/Post event pair: Captures velocity before processing, checks state after
 * - Server-side only: Prevents client/server race conditions, single source of truth
 * - Floor > Ceiling > Wall priority: Early returns prevent multiple collision triggers
 */
public class CollisionFlatteningHandler {

    /**
     * Records velocity BEFORE collision processing.
     * This captures the impact velocity before Minecraft modifies it.
     */
    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            VelocityTracker.recordPreTickVelocity(player);
        }
    }

    /**
     * Detects collisions by comparing velocity before/after collision processing.
     * Runs AFTER collision processing to check results.
     */
    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        Vec3 prevVelocity = VelocityTracker.getPreviousVelocity(player);
        Vec3 currentVelocity = player.getDeltaMovement();
        boolean wasInAir = !VelocityTracker.wasOnGround(player);
        double prevY = VelocityTracker.getPreviousY(player);

        // ========== FLOOR COLLISION ==========
        // Use onGround() because it's set immediately on landing
        // Detection: Player is on ground AND had high downward velocity in previous tick
        double floorThreshold = ToonFlatteningConfig.CONFIG.floorVelocityThreshold.get();
        if (player.onGround() && prevVelocity.y < -floorThreshold) {
            if (ToonFlatteningConfig.CONFIG.enableFloorFlatten.get()) {
                double floorDamage = ToonFlatteningConfig.CONFIG.floorDamage.get();
                double velocity = Math.abs(prevVelocity.y);
                FlatteningHandler.flattenPlayer(player, floorDamage, FlattenCause.COLLISION, velocity, CollisionType.FLOOR, null);
            }
            commitAndRecordState(player);
            return; // Priority: Floor detection prevents ceiling/wall checks
        }

        // ========== CEILING COLLISION ==========
        // Detection: Was moving up fast, velocity now near zero (hit ceiling), was in air, not climbing
        //
        // Position player BEFORE flattenPlayer() so storeFlattenedPosition() locks correct location
        //
        // POSITIONING: Player's scaled hitbox should touch ceiling
        // - Height scale 0.05: hitbox shrinks from 1.8 to 0.09 blocks
        // - Scaling from feet (origin): need to translate up to compensate
        // - Calculate ceiling block Y, then position feet so top of scaled hitbox reaches it
        double ceilingThreshold = ToonFlatteningConfig.CONFIG.ceilingVelocityThreshold.get();
        boolean hitCeiling = prevVelocity.y > ceilingThreshold && currentVelocity.y <= 0.01 && wasInAir;
        if (hitCeiling && !player.onClimbable()) {
            if (ToonFlatteningConfig.CONFIG.enableCeilingFlatten.get()) {
                ToonFlattening.LOGGER.info("SERVER: Ceiling collision detected for {}, velocity={}",
                    player.getName().getString(), prevVelocity.y);

                // Position player BEFORE flattenPlayer() to ensure correct position is locked
                double originalHeight = 1.8;
                double heightScale = ToonFlatteningConfig.CONFIG.heightScale.get();
                double scaledHeight = originalHeight * heightScale;
                double headY = player.getY() + originalHeight;

                // Find actual ceiling surface (handles slabs, stairs)
                OptionalDouble ceilingSurface = findCeilingSurfaceY(player, headY);
                if (ceilingSurface.isEmpty()) {
                    // No ceiling found - skip flattening
                    ToonFlattening.LOGGER.info("SERVER: No ceiling block found for {}, skipping flatten", player.getName().getString());
                    commitAndRecordState(player);
                    return;
                }

                double ceilingSurfaceY = ceilingSurface.getAsDouble();
                double ceilingY = ceilingSurfaceY - scaledHeight;
                player.setPos(player.getX(), ceilingY, player.getZ());
                ToonFlattening.LOGGER.info("SERVER: Ceiling position set to Y={} (ceiling surface at Y={})", ceilingY, ceilingSurfaceY);

                double ceilingDamage = ToonFlatteningConfig.CONFIG.ceilingDamage.get();
                double velocity = Math.abs(prevVelocity.y);
                FlatteningHandler.flattenPlayer(player, ceilingDamage, FlattenCause.COLLISION, velocity, CollisionType.CEILING, null, ceilingSurfaceY);
            }
            commitAndRecordState(player);
            return; // Priority: Ceiling prevents wall check
        }

        // ========== WALL COLLISION ==========
        // Detection: Was moving horizontally fast, now reduced to < 30% (hit wall)
        //
        // WALL DIRECTION: Compare dominant velocity component (X vs Z)
        // - X dominant: EAST (positive) or WEST (negative)
        // - Z dominant: SOUTH (positive) or NORTH (negative)
        //
        // POSITIONING: Offset toward wall surface for visual stick effect
        double wallThreshold = ToonFlatteningConfig.CONFIG.wallVelocityThreshold.get();
        double prevHorizSpeed = Math.sqrt(prevVelocity.x * prevVelocity.x + prevVelocity.z * prevVelocity.z);
        double currHorizSpeed = Math.sqrt(currentVelocity.x * currentVelocity.x + currentVelocity.z * currentVelocity.z);
        boolean hitWall = prevHorizSpeed > wallThreshold && currHorizSpeed < prevHorizSpeed * 0.3;

        if (hitWall) {
            if (ToonFlatteningConfig.CONFIG.enableWallFlatten.get()) {
                // Detect wall direction from dominant velocity component
                Direction wallDirection;
                if (Math.abs(prevVelocity.x) > Math.abs(prevVelocity.z)) {
                    wallDirection = prevVelocity.x > 0 ? Direction.EAST : Direction.WEST;
                } else {
                    wallDirection = prevVelocity.z > 0 ? Direction.SOUTH : Direction.NORTH;
                }

                ToonFlattening.LOGGER.info("SERVER: Wall collision detected for {}, direction={}, speed={}",
                    player.getName().getString(), wallDirection, prevHorizSpeed);

                double wallDamage = ToonFlatteningConfig.CONFIG.wallDamage.get();
                FlatteningHandler.flattenPlayer(player, wallDamage, FlattenCause.COLLISION, prevHorizSpeed, CollisionType.WALL, wallDirection);

                // Position player against wall surface
                double offsetAmount = 0.4; // Move toward wall to appear stuck
                double newX = player.getX();
                double newZ = player.getZ();
                switch (wallDirection) {
                    case EAST -> newX = Math.floor(player.getX()) + 1 - offsetAmount;
                    case WEST -> newX = Math.ceil(player.getX()) - 1 + offsetAmount;
                    case SOUTH -> newZ = Math.floor(player.getZ()) + 1 - offsetAmount;
                    case NORTH -> newZ = Math.ceil(player.getZ()) - 1 + offsetAmount;
                    default -> {}
                }
                player.setPos(newX, player.getY(), newZ);
            }
        }

        commitAndRecordState(player);
    }

    private static void commitAndRecordState(Player player) {
        VelocityTracker.commitVelocity(player);
        VelocityTracker.recordState(player);
    }

    /**
     * Finds the ceiling surface Y position above the player's head.
     * Returns the actual collision surface, not just block coordinate.
     * Handles partial blocks like slabs by checking collision shape bounds.
     */
    private static OptionalDouble findCeilingSurfaceY(Player player, double headY) {
        Level level = player.level();
        int playerBlockX = Mth.floor(player.getX());
        int playerBlockZ = Mth.floor(player.getZ());

        // Start from floor(headY) to avoid skipping blocks on integer boundaries
        // (ceil(10.5)=11 would skip a slab at Y=10)
        // Check 3 blocks to cover full range
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            int blockY = Mth.floor(headY) + yOffset;
            BlockPos pos = new BlockPos(playerBlockX, blockY, playerBlockZ);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);

            if (!shape.isEmpty()) {
                // Get actual bottom of collision shape (handles slabs, stairs)
                double shapeMinY = shape.min(net.minecraft.core.Direction.Axis.Y);
                double ceilingSurfaceY = blockY + shapeMinY;

                // Only return if ceiling is at or above head (with tolerance for collision margin)
                if (ceilingSurfaceY >= headY - 0.5) {
                    return OptionalDouble.of(ceilingSurfaceY);
                }
            }
        }
        return OptionalDouble.empty();
    }
}
