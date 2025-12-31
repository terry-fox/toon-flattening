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

        double spreadToAdd = ToonFlatteningConfig.CONFIG.spreadMultiplier.get();
        int animationTicks = calculateFlatteningAnimationTicks(anvilVelocity);
        applyFlatteningState(player, spreadToAdd, "ANVIL", animationTicks);
        applyDamageAndSound(player, damage);
    }

    private static boolean canFlatten(ServerPlayer player) {
        return !player.isSpectator();
    }

    private static void applyFlatteningState(ServerPlayer player, double spreadToAdd, String source, int animationTicks) {
        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        long flattenTime;
        FrozenPoseData pose;
        double accumulatedSpread;
        boolean sendSquashAnimation;

        if (currentState.isFlattened()) {
            // Already flattened - accumulate spread
            accumulatedSpread = currentState.accumulatedSpread() + spreadToAdd;
            flattenTime = currentState.flattenTime();
            pose = currentState.frozenPose();
            sendSquashAnimation = false;
        } else {
            // First flatten
            accumulatedSpread = spreadToAdd;
            flattenTime = player.level().getGameTime();
            pose = FrozenPoseData.capture(player);
            sendSquashAnimation = true;
        }

        FlattenedStateAttachment newState = new FlattenedStateAttachment(true, flattenTime, pose, accumulatedSpread, source);

        player.setData(ToonFlattening.FLATTENED_STATE.get(), newState);
        player.setDeltaMovement(Vec3.ZERO);
        PehkuiIntegration.setPlayerScaleWithDelay(player, ScaleDimensions.fromConfig(accumulatedSpread), animationTicks);

        syncToClient(player);
        if (sendSquashAnimation) {
            NetworkHandler.sendSquashAnimation(player);
        }

        ToonFlattening.LOGGER.info("Player {} flattened (accumulated spread: {})", player.getName().getString(), accumulatedSpread);
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

        // Calculate new accumulated spread
        double spreadToAdd = ToonFlatteningConfig.CONFIG.spreadMultiplier.get();
        double newAccumulatedSpread = currentState.accumulatedSpread() + spreadToAdd;

        // Check if max spread reached via config
        ScaleDimensions proposedDimensions = ScaleDimensions.fromConfig(newAccumulatedSpread);
        double maxSpreadWidth = ToonFlatteningConfig.CONFIG.maxSpreadWidth.get();

        if (proposedDimensions.width() >= maxSpreadWidth) {
            ToonFlattening.LOGGER.info("Player {} already at max spread (width: {})",
                player.getName().getString(), proposedDimensions.width());
            return;
        }

        // Update attachment with new accumulated spread (preserve flattenTime, frozenPose, and source)
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            currentState.withSpread(newAccumulatedSpread)
        );

        // Apply new scale with delay
        PehkuiIntegration.setPlayerScaleWithDelay(
            player,
            ScaleDimensions.fromConfig(newAccumulatedSpread),
            5 // ~5 tick delay
        );

        // Sync to client
        syncToClient(player);

        ToonFlattening.LOGGER.info("Player {} silently spread (accumulated spread: {}, width: {})",
            player.getName().getString(), newAccumulatedSpread, proposedDimensions.width());
    }

    public static void flattenWithHammer(ServerPlayer player) {
        flattenWithHammer(player, false);
    }

    public static void flattenWithHammer(ServerPlayer player, boolean isCriticalHit) {
        if (!canFlatten(player)) {
            return;
        }

        // Calculate spread to add (normal: +spreadMultiplier, crit: +spreadMultiplier×1.5)
        double baseSpreadMultiplier = ToonFlatteningConfig.CONFIG.spreadMultiplier.get();
        double spreadToAdd = isCriticalHit ? baseSpreadMultiplier * 1.5 : baseSpreadMultiplier;

        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());

        // Check max spread limit before applying
        if (currentState.isFlattened()) {
            double proposedSpread = currentState.accumulatedSpread() + spreadToAdd;
            double proposedWidth = 1.0 + proposedSpread;
            double maxSpreadWidth = ToonFlatteningConfig.CONFIG.maxSpreadWidth.get();

            if (proposedWidth >= maxSpreadWidth) {
                return; // At max spread, do nothing
            }
        }

        applyFlatteningState(player, spreadToAdd, "HAMMER", 5);

        // Apply damage effect (sound + red flash, no animation)
        player.hurt(player.damageSources().generic(), 0.1f);
        player.setDeltaMovement(Vec3.ZERO);  // Cancel knockback
        player.hurtTime = 0;  // Cancel hurt animation

        playFlattenSound(player);

        ToonFlattening.LOGGER.info("Player {} hammer-flattened (accumulated spread: {}, critical: {})",
            player.getName().getString(), player.getData(ToonFlattening.FLATTENED_STATE.get()).accumulatedSpread(), isCriticalHit);
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

    public static void flattenWithMinecart(ServerPlayer player) {
        if (!canFlatten(player)) {
            return;
        }

        FlattenedStateAttachment currentState = player.getData(ToonFlattening.FLATTENED_STATE.get());
        boolean wasAlreadyFlattened = currentState.isFlattened();

        double spreadToAdd = ToonFlatteningConfig.CONFIG.spreadMultiplier.get();
        applyFlatteningState(player, spreadToAdd, "MINECART", 3);

        // Sound only on first flatten
        if (!wasAlreadyFlattened) {
            playFlattenSound(player);
        }

        ToonFlattening.LOGGER.info("Player {} minecart-flattened", player.getName().getString());
    }

    public static void syncToClient(ServerPlayer player) {
        NetworkHandler.syncFlattenState(player);
    }
}
