package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningHelper;

import net.minecraft.world.entity.player.Player;
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

        if (!FlatteningHelper.isFlattened(player)) {
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
