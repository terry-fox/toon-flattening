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
    private static int calculateFlatteningAnimationTicks(double anvilVelocityBlocksPerTick) {
        final double PLAYER_HEIGHT = 1.8;
        final double HEIGHT_SCALE = ToonFlatteningConfig.CONFIG.heightScale.get();
        final double COMPRESSION = PLAYER_HEIGHT - (PLAYER_HEIGHT * HEIGHT_SCALE);
        final double VELOCITY_THRESHOLD = 0.01;
        final int MIN_TICKS = 1;
        final int MAX_TICKS = 20;
        final int DEFAULT_TICKS = 10;

        if (anvilVelocityBlocksPerTick < VELOCITY_THRESHOLD) {
            return DEFAULT_TICKS;
        }

        double calculatedTicks = COMPRESSION / anvilVelocityBlocksPerTick;

        return Math.max(MIN_TICKS, Math.min(MAX_TICKS, (int) Math.round(calculatedTicks)));
    }

    public static void flattenPlayer(Player player, double damage, FlattenCause cause, double anvilVelocity) {
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

        int animationTicks = calculateFlatteningAnimationTicks(anvilVelocity);

        double heightScale = ToonFlatteningConfig.CONFIG.heightScale.get();
        PehkuiIntegration.setPlayerScaleWithDelay(player, (float) heightScale, 1.0f, animationTicks);

        // Sync to clients
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.syncFlattenState(serverPlayer, true, flattenTime);

            // Send particles immediately (animation happens via Pehkui scale interpolation)
            NetworkHandler.sendSquashAnimation(serverPlayer);
        }

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

                double velocity = Math.abs(fallingBlock.getDeltaMovement().y);
                double flattenDamage = ToonFlatteningConfig.CONFIG.flattenDamage.get();
                flattenPlayer(player, flattenDamage, FlattenCause.ANVIL, velocity);
                event.setAmount((float) flattenDamage);
            }
        }
    }
}
