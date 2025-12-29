package com.terryfox.toonflattening.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;

public class AnvilPinningHelper {
    /**
     * Checks if the player is pinned by an anvil block.
     *
     * @param player The player to check
     * @return true if an anvil block exists at the player's position or one block above
     */
    public static boolean isPlayerPinnedByAnvil(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        // Check block at player position
        if (player.level().getBlockState(playerPos).is(BlockTags.ANVIL)) {
            return true;
        }

        // Check block one above player position
        BlockPos abovePos = playerPos.above();
        return player.level().getBlockState(abovePos).is(BlockTags.ANVIL);
    }
}
