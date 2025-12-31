package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This code relies on many confusing tricks and hacks to get decent minecart flattening working. I've
 * tried to document to my best ability all of the edge cases encountered for future reference, but
 * ultimately I barely understand this code myself and will probably forget how it works tomorrow. Please
 * don't try to understand this code. I hope I don't have to touch it again.
 */
public class MinecartFlatteningHandler {
    /**
     * Minimum relative velocity (blocks/tick) required for minecart to flatten.
     */
    private static final double VELOCITY_THRESHOLD = 0.1;

    /**
     * Distance (blocks) at which flattening triggers, center-to-center.
     */
    public static final double TRIGGER_RADIUS = 0.4;

    /**
     * Minimum dot product for player to be considered "in front" of cart.
     * 0.5 = ~60° cone in front of cart's movement direction.
     */
    private static final double DIRECTION_THRESHOLD = 0.5;

    /**
     * Maximum Y-level difference between cart and player for flattening.
     * Player must be grounded near cart's rail level.
     */
    private static final double Y_TOLERANCE = 0.5;

    /**
     * Cooldown period in ticks (1 second = 20 ticks).
     */
    private static final int COOLDOWN_TICKS = 20;

    /**
     * Maximum player speed toward cart to allow flattening.
     * If player is moving toward cart faster than this, they're initiating contact.
     * Lower than sneaking speed (0.065) to catch all movement.
     */
    private static final double PLAYER_APPROACH_THRESHOLD = 0.03;

    /**
     * Tracks last flatten time per player to enforce cooldown.
     */
    private static final Map<UUID, Long> lastFlattenTime = new HashMap<>();

    /**
     * Check if cart velocity meets threshold for flattening.
     * @return true if cart should flatten/suppress collision
     */
    public static boolean meetsVelocityThreshold(AbstractMinecart cart, ServerPlayer player, Vec3 cartVelocity) {
        double cartSpeed = cartVelocity.horizontalDistance();
        if (cartSpeed < 0.01) {
            ToonFlattening.LOGGER.info("[VELOCITY] FAIL: cartSpeed {} < 0.01", cartSpeed);
            return false;  // Stationary cart
        }

        // Check cart moving toward player
        Vec3 toPlayer = new Vec3(player.getX() - cart.getX(), 0, player.getZ() - cart.getZ());
        Vec3 cartDir = new Vec3(cartVelocity.x, 0, cartVelocity.z).normalize();
        double dirDot = cartDir.dot(toPlayer.normalize());
        if (dirDot < DIRECTION_THRESHOLD) {
            ToonFlattening.LOGGER.info("[VELOCITY] FAIL: dirDot {} < {}", dirDot, DIRECTION_THRESHOLD);
            return false;  // Not approaching
        }

        // Relative velocity: cart speed minus player's component along cart direction
        Vec3 playerVel = player.getDeltaMovement();
        double playerSpeedAlongCart = cartDir.dot(new Vec3(playerVel.x, 0, playerVel.z));
        double relativeSpeed = cartSpeed - playerSpeedAlongCart;
        if (relativeSpeed < VELOCITY_THRESHOLD) {
            ToonFlattening.LOGGER.info("[VELOCITY] FAIL: relSpeed {} < {}", relativeSpeed, VELOCITY_THRESHOLD);
            return false;
        }

        ToonFlattening.LOGGER.info("[VELOCITY] PASS: speed={} dir={} rel={}", cartSpeed, dirDot, relativeSpeed);
        return true;
    }

    /**
     * Attempt to flatten a player hit by a minecart.
     * Uses relative velocity - cart must be approaching player faster than threshold.
     * @param cartVelocity The cart's velocity at the start of the tick (before collisions)
     */
    public static boolean tryFlatten(AbstractMinecart cart, ServerPlayer victim, Vec3 cartVelocity) {
        // Check cooldown
        UUID playerUuid = victim.getUUID();
        long currentTime = victim.level().getGameTime();
        Long lastTime = lastFlattenTime.get(playerUuid);
        if (lastTime != null && (currentTime - lastTime) < COOLDOWN_TICKS) {
            return false; // Still in cooldown
        }

        // Check if player is pushing through cart (initiating contact)
        // Only block if: velocities aligned (same direction) AND player moving toward cart position
        // This prevents false blocking when cart rebounds and player has turned around
        Vec3 playerVel = victim.getDeltaMovement();
        Vec3 playerVelHoriz = new Vec3(playerVel.x, 0, playerVel.z);
        Vec3 cartVelHoriz = new Vec3(cartVelocity.x, 0, cartVelocity.z);
        double velocityAlignment = playerVelHoriz.dot(cartVelHoriz);

        if (velocityAlignment > 0) {
            // Velocities aligned (same direction) - might be pushing through
            Vec3 toCart = new Vec3(
                cart.getX() - victim.getX(),
                0,
                cart.getZ() - victim.getZ()
            );
            if (toCart.horizontalDistanceSqr() > 0.0001) {
                double playerSpeedTowardCart = toCart.normalize().dot(playerVelHoriz);
                if (playerSpeedTowardCart > PLAYER_APPROACH_THRESHOLD) {
                    return false; // Player pushing through cart
                }
            }
        }
        // If velocities opposed or player moving away from cart, allow flatten

        // Get cart's horizontal direction (normalized)
        Vec3 cartDir = new Vec3(cartVelocity.x, 0, cartVelocity.z).normalize();
        double cartSpeed = cartVelocity.horizontalDistance();

        // Project player velocity onto cart direction
        double playerSpeedAlongCart = cartDir.dot(new Vec3(playerVel.x, 0, playerVel.z));

        // Relative velocity = how fast cart approaches player
        double relativeSpeed = cartSpeed - playerSpeedAlongCart;

        // Check relative velocity threshold
        if (relativeSpeed < VELOCITY_THRESHOLD) {
            return false;
        }

        // Check player is in front of cart's movement
        Vec3 toPlayer = new Vec3(
            victim.getX() - cart.getX(),
            0,
            victim.getZ() - cart.getZ()
        );
        double distToPlayer = toPlayer.horizontalDistance();
        double dot = cartVelocity.normalize().dot(toPlayer.normalize());

        // At close range, direction vector is unreliable due to small distances
        // Only reject if cart is clearly moving AWAY from player
        // At distance, use stricter threshold to ensure cart is approaching
        double minDot = distToPlayer <= TRIGGER_RADIUS ? 0 : DIRECTION_THRESHOLD;
        if (dot < minDot) {
            return false; // Player not in front
        }

        // Player must be grounded
        double railY = cart.blockPosition().getY();
        double playerY = victim.getY();
        if (Math.abs(playerY - railY) > Y_TOLERANCE) {
            return false;
        }

        // Game mode check
        GameType gameMode = victim.gameMode.getGameModeForPlayer();
        if (gameMode == GameType.CREATIVE || gameMode == GameType.SPECTATOR) {
            return false;
        }

        // Riding entity check
        if (victim.isPassenger()) {
            return false;
        }

        // PvP check
        MinecraftServer server = victim.getServer();
        if (server != null && !server.isPvpAllowed()) {
            return false;
        }

        FlatteningStateController.flattenWithMinecart(victim);

        // Update cooldown
        lastFlattenTime.put(playerUuid, currentTime);

        return true;
    }
}
