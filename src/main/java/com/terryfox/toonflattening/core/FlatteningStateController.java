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
        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        if (currentState.isFlattened()) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        long flattenTime = player.level().getGameTime();
        FrozenPoseData pose = FrozenPoseData.capture(player);
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            new FlattenedStateAttachment(true, flattenTime, pose)
        );

        int animationTicks = calculateFlatteningAnimationTicks(anvilVelocity);
        PehkuiIntegration.setPlayerScaleWithDelay(player, ScaleDimensions.fromConfig(), animationTicks);

        syncToClient(player);
        NetworkHandler.sendSquashAnimation(player);

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

    public static void syncToClient(ServerPlayer player) {
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        NetworkHandler.syncFlattenState(player, state.isFlattened(), state.flattenTime(), state.frozenPose());
    }
}
