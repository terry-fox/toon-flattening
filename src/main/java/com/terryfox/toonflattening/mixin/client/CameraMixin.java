package com.terryfox.toonflattening.mixin.client;

import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.event.CollisionType;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to adjust camera Y position when player is flattened.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract net.minecraft.world.phys.Vec3 getPosition();

    // Track state for restoration animation
    @Unique
    private static final java.util.Map<Integer, CollisionType> toonflattening$storedCollisionTypes = new java.util.concurrent.ConcurrentHashMap<>();
    @Unique
    private static final java.util.Map<Integer, Double> toonflattening$storedCeilingY = new java.util.concurrent.ConcurrentHashMap<>();
    @Unique
    private static final java.util.Map<Integer, Direction> toonflattening$storedWallDirection = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject(method = "setup", at = @At("TAIL"))
    private void adjustCameraForFlattening(BlockGetter level, Entity entity, boolean detached,
                                           boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (detached || !(entity instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment attachment = FlattenedStateHelper.getState(player);
        boolean isFlattened = FlattenedStateHelper.isFlattened(player);
        boolean isRestoring = attachment.isRestoring();

        if (!isFlattened && !isRestoring) {
            // Clean up stored state
            toonflattening$storedCollisionTypes.remove(player.getId());
            toonflattening$storedCeilingY.remove(player.getId());
            toonflattening$storedWallDirection.remove(player.getId());
            return;
        }

        // Store state when flattening for use during restoration
        CollisionType collisionType;
        double ceilingY;
        Direction wallDirection;
        if (isFlattened) {
            collisionType = attachment.collisionType();
            ceilingY = attachment.ceilingBlockY();
            wallDirection = attachment.wallDirection();
            toonflattening$storedCollisionTypes.put(player.getId(), collisionType);
            toonflattening$storedCeilingY.put(player.getId(), ceilingY);
            if (wallDirection != null) {
                toonflattening$storedWallDirection.put(player.getId(), wallDirection);
            }
        } else {
            // Use stored values during restoration
            collisionType = toonflattening$storedCollisionTypes.getOrDefault(player.getId(), CollisionType.FLOOR);
            ceilingY = toonflattening$storedCeilingY.getOrDefault(player.getId(), 0.0);
            wallDirection = toonflattening$storedWallDirection.get(player.getId());
        }

        float animProgress = toonflattening$getAnimationProgress(player, attachment, isRestoring);
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animProgress, 3.0);

        double depthScale = ToonFlatteningConfig.CONFIG.depthScale.get();

        // Calculate offset in direction of surface normal
        Vec3 offset = toonflattening$calculateOffset(player, collisionType, wallDirection, depthScale, easedProgress, ceilingY);

        if (offset.lengthSqr() > 0) {
            Vec3 pos = getPosition();
            setPosition(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
        }
    }

    @Unique
    private Vec3 toonflattening$calculateOffset(Player player, CollisionType collisionType,
                                                 Direction wallDirection, double depthScale,
                                                 float progress, double ceilingY) {
        // Normal eye height is 1.62, player height is 1.8
        double normalEyeHeight = 1.62;
        double flattenedHeight = 1.8 * depthScale; // ~0.09 when depthScale=0.05
        double cameraOffset = 0.15; // Offset away from surface to prevent clipping

        return switch (collisionType) {
            case FLOOR, ANVIL -> {
                // Move camera down, offset upward (surface normal is +Y)
                double targetEyeHeight = flattenedHeight + cameraOffset;
                double yOffset = (targetEyeHeight - normalEyeHeight) * progress;
                yield new Vec3(0, yOffset, 0);
            }
            case CEILING -> {
                if (ceilingY > 0) {
                    // Move camera up, offset downward from ceiling (surface normal is -Y)
                    double targetEyeY = ceilingY - flattenedHeight - cameraOffset;
                    double currentEyeY = player.getY() + normalEyeHeight;
                    double yOffset = (targetEyeY - currentEyeY) * progress;
                    yield new Vec3(0, yOffset, 0);
                }
                yield Vec3.ZERO;
            }
            case WALL -> {
                if (wallDirection == null) {
                    yield Vec3.ZERO;
                }
                // Move camera down to flattened height
                double targetEyeHeight = flattenedHeight + cameraOffset;
                double yOffset = (targetEyeHeight - normalEyeHeight) * progress;

                // Offset away from wall in direction of surface normal
                // Wall normal points away from wall (opposite of wall direction)
                double horizontalOffset = cameraOffset * progress;
                double xOffset = 0;
                double zOffset = 0;

                switch (wallDirection) {
                    case NORTH -> zOffset = horizontalOffset;  // Normal points +Z (south)
                    case SOUTH -> zOffset = -horizontalOffset; // Normal points -Z (north)
                    case EAST -> xOffset = -horizontalOffset;  // Normal points -X (west)
                    case WEST -> xOffset = horizontalOffset;   // Normal points +X (east)
                }

                yield new Vec3(xOffset, yOffset, zOffset);
            }
            default -> Vec3.ZERO;
        };
    }

    @Unique
    private float toonflattening$getAnimationProgress(Player player, FlattenedStateAttachment attachment,
                                                       boolean isRestoring) {
        if (isRestoring) {
            long currentTime = player.level().getGameTime();
            long elapsed = currentTime - attachment.restorationStartTime();
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
            return 1.0f - Math.min(1.0f, (float) elapsed / reformationTicks);
        }

        long flattenTime = attachment.flattenTime();
        long currentTime = player.level().getGameTime();
        long elapsed = currentTime - flattenTime;
        float animDurationTicks = 10f; // 500ms = 10 ticks

        return Math.min(1.0f, elapsed / animDurationTicks);
    }
}
