package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.core.FlatteningService;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin for FallingBlockEntity to handle creative mode anvil flattening.
 * Creative players don't trigger LivingIncomingDamageEvent, so we check AABB collision directly.
 */
@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;

        // Skip client-side
        if (self.level().isClientSide()) {
            return;
        }

        // Skip if not anvil
        if (!self.getBlockState().is(BlockTags.ANVIL)) {
            return;
        }

        // Get AABB and find intersecting players
        AABB boundingBox = self.getBoundingBox();
        List<Player> players = self.level().getEntitiesOfClass(Player.class, boundingBox);

        for (Player player : players) {
            // Skip spectators
            if (player.isSpectator()) {
                continue;
            }

            FlatteningService.flattenPlayerFromAnvil(player, self);
        }
    }
}
