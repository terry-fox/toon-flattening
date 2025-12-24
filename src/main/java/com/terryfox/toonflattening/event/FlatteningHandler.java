package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;

public class FlatteningHandler {
    /**
     * Calculates animation duration based on impact velocity.
     *
     * DESIGN: Animation speed proportional to collision velocity
     * - Higher velocity = faster flatten = fewer ticks
     * - Matches physics: faster impact = faster compression
     *
     * FORMULA: ticks = COMPRESSION / velocity
     * - COMPRESSION = 1.71 blocks (1.8 → 0.09 with scale 0.05)
     * - Bounds prevent visual glitches (MIN_TICKS) and stalls (MAX_TICKS)
     */
    private static int calculateFlatteningAnimationTicks(double velocityBlocksPerTick) {
        final double PLAYER_HEIGHT = 1.8;
        final double HEIGHT_SCALE = ToonFlatteningConfig.CONFIG.heightScale.get();
        final double COMPRESSION = PLAYER_HEIGHT - (PLAYER_HEIGHT * HEIGHT_SCALE); // 1.71 blocks
        final double VELOCITY_THRESHOLD = 0.01;
        final int MIN_TICKS = 1;
        final int MAX_TICKS = 100;
        final int DEFAULT_TICKS = 10;

        if (velocityBlocksPerTick < VELOCITY_THRESHOLD) {
            return DEFAULT_TICKS;
        }

        // Animation rate exactly matches collision velocity
        // Higher velocity = faster compression = fewer ticks
        double calculatedTicks = COMPRESSION / velocityBlocksPerTick;

        return Math.max(MIN_TICKS, Math.min(MAX_TICKS, (int) Math.round(calculatedTicks)));
    }

    public static void flattenPlayer(Player player, double damage, FlattenCause cause, double anvilVelocity, CollisionType collisionType, @Nullable Direction wallDirection) {
        flattenPlayer(player, damage, cause, anvilVelocity, collisionType, wallDirection, -1.0);
    }

    public static void flattenPlayer(Player player, double damage, FlattenCause cause, double anvilVelocity, CollisionType collisionType, @Nullable Direction wallDirection, double ceilingBlockY) {
        ToonFlattening.LOGGER.info("SERVER: flattenPlayer called for {}: cause={}, collisionType={}, wallDirection={}, ceilingBlockY={}",
            player.getName().getString(), cause, collisionType, wallDirection, ceilingBlockY);

        if (FlattenedStateHelper.isFlattened(player)) {
            ToonFlattening.LOGGER.info("SERVER: Player {} already flattened, skipping", player.getName().getString());
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        long flattenTime = player.level().getGameTime();
        float frozenYaw = player.getYRot();
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            new FlattenedStateAttachment(true, flattenTime, collisionType, wallDirection, false, 0L, ceilingBlockY, frozenYaw)
        );

        ToonFlattening.LOGGER.info("SERVER: Attachment set for {}: collisionType={}, wallDirection={}",
            player.getName().getString(), collisionType, wallDirection);

        int animationTicks = calculateFlatteningAnimationTicks(anvilVelocity);

        // Apply different Pehkui scaling based on collision type
        if (collisionType == CollisionType.WALL) {
            ToonFlattening.LOGGER.info("SERVER: Applying wall scale for {}", player.getName().getString());
            double wallHitboxScale = ToonFlatteningConfig.CONFIG.wallHitboxScale.get();
            PehkuiIntegration.setWallScale(player, (float) wallHitboxScale, animationTicks);
        } else {
            // ANVIL, FLOOR, CEILING use standard height/width scaling
            ToonFlattening.LOGGER.info("SERVER: Applying standard height/width scale for {}", player.getName().getString());
            double heightScale = ToonFlatteningConfig.CONFIG.heightScale.get();
            double widthScale = ToonFlatteningConfig.CONFIG.widthScale.get();
            PehkuiIntegration.setPlayerScaleWithDelay(player, (float) heightScale, (float) widthScale, animationTicks);

            // TODO: CEILING collision type may need position offset (player sticks to ceiling)
        }

        // Sync to clients
        if (player instanceof ServerPlayer serverPlayer) {
            ToonFlattening.LOGGER.info("SERVER: Syncing to clients for {}", player.getName().getString());
            NetworkHandler.syncFlattenState(serverPlayer, new FlattenedStateAttachment(true, flattenTime, collisionType, wallDirection, false, 0L, ceilingBlockY, frozenYaw));

            // Send particles immediately (animation happens via Pehkui scale interpolation)
            NetworkHandler.sendSquashAnimation(serverPlayer);
        }

        // Store position to lock player in place
        PlayerMovementHandler.storeFlattenedPosition(player);

        if (!player.getAbilities().invulnerable) {
            player.hurt(player.damageSources().generic(), (float) damage);
        }

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

            if (blockState.is(BlockTags.ANVIL)) {

                double velocity = Math.abs(fallingBlock.getDeltaMovement().y);
                double flattenDamage = ToonFlatteningConfig.CONFIG.flattenDamage.get();
                flattenPlayer(player, flattenDamage, FlattenCause.ANVIL, velocity, CollisionType.ANVIL, null);
                event.setAmount((float) flattenDamage);
            }
        }
    }
}
