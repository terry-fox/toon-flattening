package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.api.FlattenContext;
import com.terryfox.toonflattening.api.FlattenDirection;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.config.TriggerConfigSpec;
import com.terryfox.toonflattening.event.FlattenCause;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Core service for applying flattening effects to players.
 * Implements first-wins logic: once flattened, player cannot be flattened again until reformed.
 */
public class FlatteningService {

    private static final double PLAYER_HEIGHT = 1.8;
    private static final double VELOCITY_THRESHOLD = 0.01;
    private static final int MIN_TICKS = 1;
    private static final int MAX_TICKS = 20;
    private static final int DEFAULT_TICKS = 10;

    /**
     * Attempts to flatten a player. Uses first-wins logic: if already flattened, no-op.
     *
     * @param player The player to flatten
     * @param context The flatten context (trigger ID, velocity, direction, source)
     * @param config The trigger configuration
     * @param dealDamage Whether to deal damage when flattening
     * @return true if player was flattened, false if already flattened or spectator
     */
    public static boolean tryFlattenPlayer(Player player, FlattenContext context, TriggerConfigSpec config, boolean dealDamage) {
        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        // First-wins logic: already flattened
        if (currentState.isFlattened()) {
            return false;
        }

        // Spectators cannot be flattened
        if (player.isSpectator()) {
            return false;
        }

        applyFlatteningEffect(player, context, config, dealDamage);
        return true;
    }

    /**
     * Applies the flattening effect to a player without checking preconditions.
     * Updates state, applies scale, syncs to clients, deals damage (if enabled), plays sound.
     */
    private static void applyFlatteningEffect(Player player, FlattenContext context, TriggerConfigSpec config, boolean dealDamage) {
        long flattenTime = player.level().getGameTime();
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            new FlattenedStateAttachment(true, flattenTime, context.triggerId(), context.direction())
        );

        int animationTicks = calculateFlatteningAnimationTicks(context.impactVelocity());

        double heightScale = config.getHeightScale();
        double widthScale = config.getWidthScale();
        PehkuiIntegration.setPlayerScaleWithDelay(player, (float) heightScale, (float) widthScale, animationTicks);

        // Sync to clients
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.syncFlattenState(serverPlayer, true, flattenTime, context.triggerId(), context.direction());
            NetworkHandler.sendSquashAnimation(serverPlayer);
        }

        if (dealDamage) {
            player.hurt(player.damageSources().generic(), (float) config.getDamage());
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

        ToonFlattening.LOGGER.info("Player {} flattened by trigger {}", player.getName().getString(), context.triggerId());
    }

    /**
     * Calculates animation duration based on velocity of flattening object.
     * Faster velocity = shorter animation (compression happens faster).
     */
    private static int calculateFlatteningAnimationTicks(double velocity) {
        if (velocity < VELOCITY_THRESHOLD) {
            return DEFAULT_TICKS;
        }

        double calculatedTicks = PLAYER_HEIGHT / velocity;

        return Math.max(MIN_TICKS, Math.min(MAX_TICKS, (int) Math.round(calculatedTicks)));
    }

    /**
     * Flattens a player hit by an anvil falling block entity.
     * Called from FallingBlockEntityMixin for creative mode players.
     *
     * @param player The player to flatten
     * @param anvil The falling anvil entity
     */
    public static void flattenPlayerFromAnvil(Player player, FallingBlockEntity anvil) {
        ResourceLocation triggerId = ResourceLocation.fromNamespaceAndPath(
            ToonFlattening.MODID,
            "anvil"
        );

        double velocity = Math.abs(anvil.getDeltaMovement().y);

        FlattenContext context = new FlattenContext(
            triggerId,
            velocity,
            FlattenDirection.DOWN,
            anvil
        );

        TriggerConfigSpec config = ToonFlatteningConfig.CONFIG.getTriggerConfig(FlattenCause.ANVIL);

        tryFlattenPlayer(player, context, config, !player.getAbilities().invulnerable);
    }
}
