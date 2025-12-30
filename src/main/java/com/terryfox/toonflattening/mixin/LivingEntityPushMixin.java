package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityPushMixin {
    /**
     * Prevents flattened players from pushing other entities.
     */
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void onPushEntities(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened()) {
            ci.cancel();
        }
    }
}
