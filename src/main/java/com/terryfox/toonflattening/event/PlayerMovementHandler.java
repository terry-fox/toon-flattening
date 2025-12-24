package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import com.terryfox.toonflattening.util.RotationState;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles position and rotation locking for flattened players.
 *
 * REQUIREMENT: Complete immobility when flattened
 * - No gravity (wall/ceiling players would fall)
 * - No movement (input blocked)
 * - No rotation (prevent visual glitches)
 * - Position locked at flatten point
 *
 * WHY DUAL Pre/Post APPROACH:
 * 1. Pre-tick (HIGHEST priority): Zero velocity BEFORE tick processing
 *    - Prevents gravity from being applied
 *    - Prevents movement input from taking effect
 * 2. Post-tick: Enforce position with teleportTo()
 *    - Authoritative server method, overrides client prediction
 *    - Locks rotation to prevent model spinning
 *
 * Use both because:
 * - Pre alone: Movement code still might change position
 * - Post alone: Gravity applies before we can stop it
 * - Together: Prevents movement AND corrects any drift
 */
public class PlayerMovementHandler {

    // Store the position where player was flattened to keep them locked there
    private static final Map<UUID, Vec3> flattenedPositions = new ConcurrentHashMap<>();

    public static void storeFlattenedPosition(Player player) {
        flattenedPositions.put(player.getUUID(), player.position());
    }

    public static void clearFlattenedPosition(Player player) {
        flattenedPositions.remove(player.getUUID());
    }

    public static Vec3 getFlattenedPosition(Player player) {
        return flattenedPositions.get(player.getUUID());
    }

    /**
     * Pre-tick: Stop movement BEFORE physics/input processing.
     * CRITICAL: Must run at HIGHEST priority to prevent gravity from applying.
     */
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        if (!FlattenedStateHelper.isFlattened(player)) {
            return;
        }

        // Zero velocity BEFORE tick processing
        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);
        player.hurtMarked = false; // Prevent knockback from pushing player
    }

    /**
     * Post-tick: Enforce position AFTER any movement processing.
     * Uses teleportTo() for authoritative position correction.
     */
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        if (!FlattenedStateHelper.isFlattened(player)) {
            flattenedPositions.remove(player.getUUID());
            return;
        }

        // Enforce position lock using teleportTo (authoritative server method)
        Vec3 lockedPos = flattenedPositions.get(player.getUUID());
        if (lockedPos != null && player instanceof ServerPlayer serverPlayer) {
            // teleportTo is authoritative, unlike setPos which client can override
            serverPlayer.teleportTo(lockedPos.x, lockedPos.y, lockedPos.z);
        }

        // Reinforce movement prevention (redundant but safe)
        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);
        player.fallDistance = 0;
        player.setSprinting(false);
        player.setSwimming(false);

        // ROTATION LOCKING: Prevent model from spinning while flattened
        // Setting current rotation to previous rotation freezes it
        RotationState.freeze(player);

        // Prevent other movement states
        if (player.isPassenger()) {
            player.stopRiding();
        }

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }
}
