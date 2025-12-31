package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class MinecartFlatteningHandler {
    /**
     * Minimum relative velocity (blocks/tick) required for minecart to flatten.
     */
    private static final double VELOCITY_THRESHOLD = 0.1;

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
     * Attempt to flatten a player hit by a minecart.
     * Uses relative velocity - cart must be approaching player faster than threshold.
     * @param cartVelocity The cart's velocity at the start of the tick (before collisions)
     */
    public static boolean tryFlatten(AbstractMinecart cart, ServerPlayer victim, Vec3 cartVelocity) {
        // Get cart's horizontal direction (normalized)
        Vec3 cartDir = new Vec3(cartVelocity.x, 0, cartVelocity.z).normalize();
        double cartSpeed = cartVelocity.horizontalDistance();

        // Project player velocity onto cart direction
        Vec3 playerVel = victim.getDeltaMovement();
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
        double dot = cartVelocity.normalize().dot(toPlayer.normalize());
        if (dot < DIRECTION_THRESHOLD) {
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
        return true;
    }
}
