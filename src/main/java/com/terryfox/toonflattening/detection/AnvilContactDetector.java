package com.terryfox.toonflattening.detection;

import com.terryfox.toonflattening.core.FlattenPhase;
import com.terryfox.toonflattening.core.FlattenState;
import com.terryfox.toonflattening.core.FlattenStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Main detection logic for anvil-player collision per tick.
 * <p>
 * Per SRS FR-DETECT: Detects falling anvil entities and placed anvil blocks,
 * calculates stack height, determines floor position, and routes to appropriate
 * state machine transitions.
 * <p>
 * Stateless singleton - all state queries delegate to FlattenStateManager.
 */
public final class AnvilContactDetector {
    private static final AnvilContactDetector INSTANCE = new AnvilContactDetector();

    // Detection constants
    private static final double HORIZONTAL_INFLATE = 1.0;
    private static final double VERTICAL_INFLATE = 1.5;
    private static final int MAX_STACK_HEIGHT = 5;
    private static final int MAX_FLOOR_RAYCAST = 10;
    private static final double PLACED_ANVIL_OFFSET = 0.1;

    private AnvilContactDetector() {
    }

    public static AnvilContactDetector getInstance() {
        return INSTANCE;
    }

    /**
     * Main tick entry point.
     * <p>
     * Per SRS FR-DETECT.1: Called once per tick per ServerPlayer.
     * Priority order: custom triggers → falling anvils → placed anvils.
     *
     * @param player Target player
     */
    public void tick(ServerPlayer player) {
        FlattenStateManager manager = FlattenStateManager.getInstance();
        FlattenState state = manager.getState(player);

        // Check custom triggers first (highest priority)
        IFlattenTrigger customTrigger = TriggerRegistry.getInstance().getActiveTrigger(player);
        if (customTrigger != null) {
            handleCustomTrigger(player, customTrigger);
            return;
        }

        // Standard anvil detection
        AnvilContact contact = detectAnvilContact(player);

        if (contact != null) {
            handleAnvilContact(player, contact, state);
        } else {
            handleNoContact(player, state);
        }
    }

    /**
     * Detect anvil contact (falling entity or placed block).
     * <p>
     * Per SRS FR-DETECT.2: Falling anvils take precedence over placed anvils.
     *
     * @param player Target player
     * @return Contact info, or null if no anvil detected
     */
    @Nullable
    private AnvilContact detectAnvilContact(ServerPlayer player) {
        // Check falling anvils first
        FallingBlockEntity fallingAnvil = detectFallingAnvil(player);
        if (fallingAnvil != null) {
            double anvilY = fallingAnvil.getBoundingBox().minY;
            double floorY = calculateFloorY(player);
            int anvilCount = 1; // Falling anvils don't stack
            double velocityY = fallingAnvil.getDeltaMovement().y;
            return new AnvilContact(anvilY, floorY, anvilCount, ContactType.FALLING_ENTITY, velocityY);
        }

        // Check placed anvils
        BlockPos placedAnvil = detectPlacedAnvil(player);
        if (placedAnvil != null) {
            double anvilY = placedAnvil.getY();
            double floorY = calculateFloorY(player);
            int anvilCount = countAnvilStack(player.serverLevel(), placedAnvil);
            // Placed anvils have no velocity
            return new AnvilContact(anvilY, floorY, anvilCount, ContactType.PLACED_BLOCK, 0.0);
        }

        return null;
    }

    /**
     * Detect falling anvil entities above player.
     * <p>
     * Per SRS FR-DETECT.2: AABB search box inflated by (1.0, 5.0, 1.0).
     *
     * @param player Target player
     * @return First falling anvil entity, or null if none
     */
    @Nullable
    private FallingBlockEntity detectFallingAnvil(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(HORIZONTAL_INFLATE, VERTICAL_INFLATE, HORIZONTAL_INFLATE);
        ServerLevel level = player.serverLevel();

        List<Entity> entities = level.getEntities(player, searchBox);
        for (Entity entity : entities) {
            if (entity instanceof FallingBlockEntity falling) {
                BlockState blockState = falling.getBlockState();
                if (blockState.is(BlockTags.ANVIL)) {
                    // Verify anvil is above player
                    if (falling.getBoundingBox().minY > player.getBoundingBox().minY) {
                        return falling;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Detect placed anvil block above player.
     * <p>
     * Per SRS FR-DETECT.3: Use BlockPos.betweenClosedStream on player AABB + move(0, 0.1, 0).
     *
     * @param player Target player
     * @return First placed anvil position, or null if none
     */
    @Nullable
    private BlockPos detectPlacedAnvil(ServerPlayer player) {
        AABB playerBox = player.getBoundingBox().move(0, PLACED_ANVIL_OFFSET, 0);
        ServerLevel level = player.serverLevel();

        return BlockPos.betweenClosedStream(playerBox)
                .filter(pos -> level.getBlockState(pos).is(BlockTags.ANVIL))
                .findFirst()
                .orElse(null);
    }

    /**
     * Count anvils stacked vertically above initial position.
     * <p>
     * Per SRS FR-DETECT.4: Max 5 blocks upward using MutableBlockPos.
     *
     * @param level Server level
     * @param initialPos Starting anvil position
     * @return Total anvil count (including initial)
     */
    private int countAnvilStack(ServerLevel level, BlockPos initialPos) {
        BlockPos.MutableBlockPos mutable = initialPos.mutable();
        int count = 1; // Count initial anvil

        for (int i = 1; i <= MAX_STACK_HEIGHT; i++) {
            mutable.move(0, 1, 0);
            if (!level.getBlockState(mutable).is(BlockTags.ANVIL)) {
                break;
            }
            count++;
        }

        return count;
    }

    /**
     * Calculate floor Y coordinate below player.
     * <p>
     * Per SRS FR-DETECT.5: Raycast downward max 10 blocks, check isSolid().
     *
     * @param player Target player
     * @return Floor Y coordinate
     */
    private double calculateFloorY(ServerPlayer player) {
        Vec3 playerPos = player.position();
        Vec3 rayStart = new Vec3(playerPos.x, playerPos.y - 0.1, playerPos.z);
        Vec3 rayEnd = new Vec3(playerPos.x, playerPos.y - MAX_FLOOR_RAYCAST, playerPos.z);

        ClipContext context = new ClipContext(
                rayStart,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );

        BlockHitResult result = player.level().clip(context);
        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getBlockPos().getY() + 1.0; // Top of block
        }

        // Fallback: use player's current Y
        return playerPos.y;
    }

    /**
     * Route anvil contact to appropriate state machine method based on phase.
     * <p>
     * Per SRS FR-DETECT.6: Switch on FlattenPhase.
     *
     * @param player Target player
     * @param contact Anvil contact info
     * @param state Current player state
     */
    private void handleAnvilContact(ServerPlayer player, AnvilContact contact, FlattenState state) {
        FlattenStateManager manager = FlattenStateManager.getInstance();

        switch (state.phase()) {
            case NORMAL:
                // Normal → ProgressiveFlattening
                manager.beginCompression(player, contact.anvilY, contact.floorY, contact.anvilCount, contact.velocityY);
                break;

            case PROGRESSIVE_FLATTENING:
                // Update compression
                manager.updateCompression(player, contact.anvilY, contact.floorY, contact.anvilCount, contact.velocityY);
                break;

            case FULLY_FLATTENED:
            case RECOVERING:
                // Check for replacement scenario (new anvil after prior lost contact)
                boolean isReplacement = !state.hasContactingAnvil();
                manager.applyReflatten(player, contact.anvilCount, isReplacement);
                break;
        }
    }

    /**
     * Handle no anvil contact detected.
     * <p>
     * Per SRS FR-DETECT.6: Call lostContact() if not NORMAL phase.
     *
     * @param player Target player
     * @param state Current player state
     */
    private void handleNoContact(ServerPlayer player, FlattenState state) {
        if (state.phase() != FlattenPhase.NORMAL) {
            FlattenStateManager.getInstance().lostContact(player);
        }
    }

    /**
     * Handle custom trigger activation.
     * <p>
     * Per SRS FR-DETECT.7: Custom triggers bypass standard detection.
     * Null position = instant flatten without progressive animation.
     *
     * @param player Target player
     * @param trigger Active custom trigger
     */
    private void handleCustomTrigger(ServerPlayer player, IFlattenTrigger trigger) {
        FlattenStateManager manager = FlattenStateManager.getInstance();
        BlockPos anvilPos = trigger.getAnvilPosition();

        if (anvilPos == null) {
            // Instant flatten (no progressive animation)
            // TODO: Implement instant flatten logic when required
            // For now, treat as standard anvil with position = player position
            double playerY = player.getBoundingBox().minY;
            double floorY = calculateFloorY(player);
            manager.beginCompression(player, playerY, floorY, trigger.getAnvilCount(), 0.0);
        } else {
            // Standard progressive flatten with custom position
            double floorY = calculateFloorY(player);
            manager.beginCompression(player, anvilPos.getY(), floorY, trigger.getAnvilCount(), 0.0);
        }
    }

    /**
     * Internal record for anvil contact information.
     */
    private record AnvilContact(double anvilY, double floorY, int anvilCount, ContactType type, double velocityY) {
    }

    /**
     * Contact type for debugging/logging.
     */
    private enum ContactType {
        FALLING_ENTITY,
        PLACED_BLOCK
    }
}
