package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player)(Object)this;
        FlattenedStateAttachment state = self.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void onCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player)(Object)this;
        FlattenedStateAttachment state = self.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        Player self = (Player)(Object)this;
        FlattenedStateAttachment state = self.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened()) {
            ci.cancel();
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void onPushEntities(CallbackInfo ci) {
        Player self = (Player)(Object)this;
        FlattenedStateAttachment state = self.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state != null && state.isFlattened()) {
            ci.cancel();
        }
    }
}
