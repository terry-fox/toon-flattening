package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.core.FlatteningHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreakAction(
            BlockPos pos,
            ServerboundPlayerActionPacket.Action action,
            Direction direction,
            int worldHeight,
            int sequence,
            CallbackInfo ci
    ) {
        // Cancel all block-break-related actions
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK &&
            action != ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK &&
            action != ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) return;

        if (player.isCreative()) return;

        BlockState blockState = player.level().getBlockState(pos);
        if (!blockState.is(BlockTags.ANVIL)) return;

        BlockPos playerPos = player.blockPosition();
        boolean isPinning = pos.equals(playerPos) || pos.equals(playerPos.above());
        if (!isPinning) return;

        if (FlatteningHelper.isFlattened(player)) {
            ci.cancel();
        }
    }
}
