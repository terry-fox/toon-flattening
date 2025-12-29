package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class FlatteningHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof FallingBlockEntity fallingBlock) {
            var blockState = fallingBlock.getBlockState();

            if (blockState.is(BlockTags.ANVIL)) {
                double velocity = Math.abs(fallingBlock.getDeltaMovement().y);
                double flattenDamage = ToonFlatteningConfig.CONFIG.flattenDamage.get();
                FlatteningStateController.flatten(player, flattenDamage, velocity);
                event.setAmount((float) flattenDamage);
            }
        }
    }
}
