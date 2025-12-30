package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class PlayerMovementHandler {

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state == null || !state.isFlattened()) {
            return;
        }

        player.setOnGround(true);
        player.setSprinting(false);
        player.setSwimming(false);

        if (player.isPassenger()) {
            player.stopRiding();
        }

        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }
}
