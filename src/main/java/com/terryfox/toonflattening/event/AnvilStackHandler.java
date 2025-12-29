package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.List;

public class AnvilStackHandler {

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof FallingBlockEntity fallingBlock)) {
            return;
        }

        if (!fallingBlock.getBlockState().is(BlockTags.ANVIL)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos landingPos = fallingBlock.blockPosition();

        // Check if anvil block exists at landing position (confirms it landed successfully)
        if (!level.getBlockState(landingPos).is(BlockTags.ANVIL)) {
            return;
        }

        // Check if landed ON another anvil (stacking condition)
        BlockPos belowPos = landingPos.below();
        if (!level.getBlockState(belowPos).is(BlockTags.ANVIL)) {
            return;
        }

        // Find bottom of anvil stack
        BlockPos bottomPos = findBottomOfAnvilStack(level, belowPos);

        // Find flattened player under stack
        ServerPlayer flattenedPlayer = findFlattenedPlayerUnder(level, bottomPos);

        if (flattenedPlayer != null) {
            FlatteningStateController.silentSpread(flattenedPlayer);
        }
    }

    private static BlockPos findBottomOfAnvilStack(Level level, BlockPos startPos) {
        BlockPos current = startPos;

        while (level.getBlockState(current.below()).is(BlockTags.ANVIL)) {
            current = current.below();
        }

        return current;
    }

    private static ServerPlayer findFlattenedPlayerUnder(Level level, BlockPos anvilPos) {
        float raycastLength = 20;

        // Search AABB along raycast path
        AABB searchBox = new AABB(
            anvilPos.getX() - 1, anvilPos.getY() - raycastLength, anvilPos.getZ() - 1,
            anvilPos.getX() + 1, anvilPos.getY() + 1, anvilPos.getZ() + 1
        );

        List<ServerPlayer> players = level.getEntitiesOfClass(
            ServerPlayer.class,
            searchBox,
            player -> {
                FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
                return state.isFlattened();
            }
        );

        return players.isEmpty() ? null : players.get(0);
    }
}
