package com.terryfox.toonflattening.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player velocities and state from previous ticks for collision detection.
 *
 * WHY NEEDED:
 * - Minecraft modifies velocity DURING collision processing
 * - By collision check time, impact velocity is lost
 * - Need pre-collision velocity to detect impact and calculate animation duration
 *
 * WHY DOUBLE-BUFFER:
 * - Prevents race conditions between recording and reading
 * - Clean separation: record current, check previous, commit for next
 *
 * WHY CONCURRENT HASHMAP:
 * - Thread-safe for multiplayer servers
 * - Multiple simultaneous players without lock contention
 *
 * ADDITIONAL STATE:
 * - wasOnGround: Detect air->ground transitions
 * - previousY: Detect upward motion before ceiling impact
 */
public class VelocityTracker {
    // Velocity from the previous tick (used for collision detection)
    private static final Map<UUID, Vec3> previousVelocities = new ConcurrentHashMap<>();
    // Velocity recorded during current tick (will become previous next tick)
    private static final Map<UUID, Vec3> currentVelocities = new ConcurrentHashMap<>();
    // Track previous tick's onGround state for transition detection
    private static final Map<UUID, Boolean> wasOnGround = new ConcurrentHashMap<>();
    // Track previous tick's Y position for ceiling detection
    private static final Map<UUID, Double> previousY = new ConcurrentHashMap<>();

    /**
     * Records velocity at start of tick (Pre event).
     * This captures velocity before collision processing.
     */
    public static void recordPreTickVelocity(Player player) {
        currentVelocities.put(player.getUUID(), player.getDeltaMovement());
    }

    /**
     * Commits current tick velocity to become previous tick velocity.
     * Call at end of tick (Post event) after collision detection.
     */
    public static void commitVelocity(Player player) {
        Vec3 current = currentVelocities.get(player.getUUID());
        if (current != null) {
            previousVelocities.put(player.getUUID(), current);
        }
    }

    /**
     * Retrieves the velocity from the previous tick.
     */
    public static Vec3 getPreviousVelocity(Player player) {
        return previousVelocities.getOrDefault(player.getUUID(), Vec3.ZERO);
    }

    /**
     * Retrieves velocity recorded at start of current tick (Pre event).
     */
    public static Vec3 getCurrentTickVelocity(Player player) {
        return currentVelocities.getOrDefault(player.getUUID(), Vec3.ZERO);
    }

    public static void recordState(Player player) {
        wasOnGround.put(player.getUUID(), player.onGround());
        previousY.put(player.getUUID(), player.getY());
    }

    public static boolean wasOnGround(Player player) {
        return wasOnGround.getOrDefault(player.getUUID(), true);
    }

    public static double getPreviousY(Player player) {
        return previousY.getOrDefault(player.getUUID(), player.getY());
    }

    public static void clearPlayer(UUID uuid) {
        previousVelocities.remove(uuid);
        currentVelocities.remove(uuid);
        wasOnGround.remove(uuid);
        previousY.remove(uuid);
    }
}
