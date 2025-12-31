package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityPushMixin {
    /**
     * Prevents flattened living entities (players) from being marked as pushable.
     * LivingEntity overrides isPushable() from Entity, so we need a separate mixin.
     */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        FlattenedStateAttachment.ifFlattened(entity, () -> cir.setReturnValue(false));
    }

    /**
     * Prevents flattened living entities from initiating pushes on other entities.
     * Flattened players should be passive and not push anyone.
     */
    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    private void onDoPush(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        // Flattened entities shouldn't initiate pushes
        FlattenedStateAttachment.ifFlattened(self, () -> ci.cancel());
    }
}
