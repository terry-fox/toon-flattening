package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class FlatteningHandler {
    public static void flattenPlayer(Player player, double damage, FlattenCause cause) {
        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        if (currentState.isFlattened()) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        long flattenTime = player.level().getGameTime();
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            new FlattenedStateAttachment(true, flattenTime)
        );

        // Sync to clients
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.syncFlattenState(serverPlayer, true, flattenTime);
        }

        double heightScale = ToonFlatteningConfig.CONFIG.heightScale.get();
        PehkuiIntegration.setPlayerScale(player, (float) heightScale, 1.0f);

        player.hurt(player.damageSources().generic(), (float) damage);

        player.level().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            ToonFlattening.FLATTEN_SOUND.get(),
            SoundSource.PLAYERS,
            1.0f,
            1.0f
        );

        ToonFlattening.LOGGER.info("Player {} flattened", player.getName().getString());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        var directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof FallingBlockEntity fallingBlock) {
            var blockState = fallingBlock.getBlockState();

            if (blockState.is(Blocks.ANVIL) ||
                blockState.is(Blocks.CHIPPED_ANVIL) ||
                blockState.is(Blocks.DAMAGED_ANVIL)) {

                double flattenDamage = ToonFlatteningConfig.CONFIG.flattenDamage.get();
                flattenPlayer(player, flattenDamage, FlattenCause.ANVIL);
                event.setAmount((float) flattenDamage);
            }
        }
    }
}
