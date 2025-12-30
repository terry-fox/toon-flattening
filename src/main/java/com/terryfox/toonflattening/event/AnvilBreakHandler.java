package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class AnvilBreakHandler {
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Check if block is anvil
        if (!isAnvilBlock(event.getState())) return;

        // Get player
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer)) return;

        // Allow creative mode players to break anvils
        if (player.isCreative()) return;

        // Check if anvil is pinning the breaking player
        BlockPos anvilPos = event.getPos();
        BlockPos playerPos = player.blockPosition();
        boolean isPinningPlayer = anvilPos.equals(playerPos) || anvilPos.equals(playerPos.above());

        // Check if player is flattened
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened() && isPinningPlayer) {
            event.setCanceled(true);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Must run on client to prevent animation/sound
        if (!event.getLevel().isClientSide()) return;

        // Only care about START action
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;

        // Check if block is anvil
        BlockState blockState = event.getLevel().getBlockState(event.getPos());
        if (!blockState.is(BlockTags.ANVIL)) return;

        Player player = event.getEntity();

        // Allow creative mode
        if (player.isCreative()) return;

        // Check if anvil is pinning player
        BlockPos anvilPos = event.getPos();
        BlockPos playerPos = player.blockPosition();
        boolean isPinningPlayer = anvilPos.equals(playerPos) || anvilPos.equals(playerPos.above());

        // Check if player is flattened
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened() && isPinningPlayer) {
            event.setUseItem(TriState.FALSE);
            event.setCanceled(true);
        }
    }

    private static boolean isAnvilBlock(BlockState state) {
        return state.is(BlockTags.ANVIL);
    }
}
