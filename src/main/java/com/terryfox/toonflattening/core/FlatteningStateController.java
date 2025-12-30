package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.attachment.FrozenPoseData;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FlatteningStateController {
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

    public static void flatten(ServerPlayer player, double damage, double anvilVelocity) {
        if (!canFlatten(player)) {
            return;
        }

        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        long flattenTime;
        FrozenPoseData pose;
        int spreadLevel;
        boolean sendSquashAnimation;
        int animationTicks = calculateFlatteningAnimationTicks(anvilVelocity);

        if (currentState.isFlattened()) {
            // Already flattened - accumulate spread
            spreadLevel = currentState.spreadLevel() + 1;
            flattenTime = currentState.flattenTime();
            pose = currentState.frozenPose();
            sendSquashAnimation = false;
        } else {
            // First flatten
            spreadLevel = 1;
            flattenTime = player.level().getGameTime();
            pose = FrozenPoseData.capture(player);
            sendSquashAnimation = true;
        }

        FlattenedStateAttachment newState = new FlattenedStateAttachment(true, flattenTime, pose, spreadLevel);

        player.setData(ToonFlattening.FLATTENED_STATE.get(), newState);
        player.setDeltaMovement(Vec3.ZERO);
        PehkuiIntegration.setPlayerScaleWithDelay(player, ScaleDimensions.fromConfig(spreadLevel), animationTicks);

        syncToClient(player);
        if (sendSquashAnimation) {
            NetworkHandler.sendSquashAnimation(player);
        }

        applyDamageAndSound(player, damage);

        ToonFlattening.LOGGER.info("Player {} flattened (spread level: {})", player.getName().getString(), spreadLevel);
    }

    private static boolean canFlatten(ServerPlayer player) {
        return !player.isSpectator();
    }

    private static void applyDamageAndSound(ServerPlayer player, double damage) {
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
    }

    public static boolean tryReform(ServerPlayer player) {
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (!state.isFlattened()) {
            return false;
        }

        // Check if anvil pinning is enabled and player is pinned
        if (ToonFlatteningConfig.CONFIG.anvilPinningEnabled.get() &&
            AnvilPinningHelper.isPlayerPinnedByAnvil(player)) {

            int timeoutSeconds = ToonFlatteningConfig.CONFIG.anvilPinningTimeoutSeconds.get();

            // If timeout is 0, infinite pinning - deny reform
            if (timeoutSeconds == 0) {
                return false;
            }

            // Calculate elapsed time since flattening
            long currentGameTime = player.level().getGameTime();
            long elapsedSeconds = (currentGameTime - state.flattenTime()) / 20;

            // If timeout hasn't elapsed, deny reform
            if (elapsedSeconds < timeoutSeconds) {
                return false;
            }
        }

        resetPlayer(player);
        ToonFlattening.LOGGER.info("Player {} reformed", player.getName().getString());
        return true;
    }

    public static void resetPlayer(ServerPlayer player) {
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            FlattenedStateAttachment.DEFAULT
        );

        PehkuiIntegration.resetPlayerScale(player);
        syncToClient(player);
    }

    public static void silentSpread(ServerPlayer player) {
        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        // Validate player is already flattened
        if (!currentState.isFlattened()) {
            return;
        }

        // Calculate new spread level
        int newSpreadLevel = currentState.spreadLevel() + 1;

        // Check if max spread reached via config
        ScaleDimensions proposedDimensions = ScaleDimensions.fromConfig(newSpreadLevel);
        double maxSpreadWidth = ToonFlatteningConfig.CONFIG.maxSpreadWidth.get();

        if (proposedDimensions.width() >= maxSpreadWidth) {
            ToonFlattening.LOGGER.info("Player {} already at max spread (width: {})",
                player.getName().getString(), proposedDimensions.width());
            return;
        }

        // Update attachment with new spread level (preserve flattenTime and frozenPose)
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            new FlattenedStateAttachment(
                true,
                currentState.flattenTime(),
                currentState.frozenPose(),
                newSpreadLevel
            )
        );

        // Apply new scale with delay
        PehkuiIntegration.setPlayerScaleWithDelay(
            player,
            ScaleDimensions.fromConfig(newSpreadLevel),
            5 // ~5 tick delay
        );

        // Sync to client
        syncToClient(player);

        ToonFlattening.LOGGER.info("Player {} silently spread (new spread level: {}, width: {})",
            player.getName().getString(), newSpreadLevel, proposedDimensions.width());
    }

    public static void flattenWithHammer(ServerPlayer player) {
        if (!canFlatten(player)) {
            return;
        }

        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        long flattenTime;
        FrozenPoseData pose;
        int spreadLevel;
        boolean isInitialFlatten;

        if (currentState.isFlattened()) {
            // Already flattened - check max spread
            int proposedSpread = currentState.spreadLevel() + 1;
            ScaleDimensions proposedDimensions = ScaleDimensions.fromConfig(proposedSpread);
            double maxSpreadWidth = ToonFlatteningConfig.CONFIG.maxSpreadWidth.get();

            if (proposedDimensions.width() >= maxSpreadWidth) {
                return; // At max spread, do nothing
            }

            spreadLevel = proposedSpread;
            flattenTime = currentState.flattenTime();
            pose = currentState.frozenPose();
            isInitialFlatten = false;
        } else {
            // First flatten
            spreadLevel = 1;
            flattenTime = player.level().getGameTime();
            pose = FrozenPoseData.capture(player);
            isInitialFlatten = true;
        }

        FlattenedStateAttachment newState = new FlattenedStateAttachment(true, flattenTime, pose, spreadLevel);
        player.setData(ToonFlattening.FLATTENED_STATE.get(), newState);
        player.setDeltaMovement(Vec3.ZERO);

        PehkuiIntegration.setPlayerScaleWithDelay(player, ScaleDimensions.fromConfig(spreadLevel), 5);

        syncToClient(player);
        if (isInitialFlatten) {
            NetworkHandler.sendSquashAnimation(player);
            playFlattenSound(player);
        }

        ToonFlattening.LOGGER.info("Player {} hammer-flattened (spread level: {})",
            player.getName().getString(), spreadLevel);
    }

    private static void playFlattenSound(ServerPlayer player) {
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
    }

    public static void syncToClient(ServerPlayer player) {
        NetworkHandler.syncFlattenState(player);
    }
}
