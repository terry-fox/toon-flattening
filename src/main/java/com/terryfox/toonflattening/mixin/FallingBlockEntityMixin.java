package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.event.CollisionType;
import com.terryfox.toonflattening.event.FlattenCause;
import com.terryfox.toonflattening.event.FlatteningHandler;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {

    @Shadow
    public abstract BlockState getBlockState();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity)(Object)this;

        if (self.level().isClientSide()) {
            return;
        }

        BlockState blockState = getBlockState();
        if (!blockState.is(BlockTags.ANVIL)) {
            return;
        }

        // Check for colliding players
        AABB boundingBox = self.getBoundingBox();
        List<Player> players = self.level().getEntitiesOfClass(Player.class, boundingBox);

        for (Player player : players) {
            if (player.isSpectator()) {
                continue;
            }

            double velocity = Math.abs(self.getDeltaMovement().y);
            double flattenDamage = ToonFlatteningConfig.CONFIG.flattenDamage.get();
            FlatteningHandler.flattenPlayer(player, flattenDamage, FlattenCause.ANVIL, velocity, CollisionType.ANVIL, null);
        }
    }
}
