package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.core.FlatteningHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class PlayerPushMixin {
    /**
     * Prevents flattened players from being marked as pushable.
     * Minecarts check isPushable() before attempting to push entities.
     */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }

        if (FlatteningHelper.isFlattened(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Prevents flattened players from being pushed by entity collision.
     * Most entities bypass isPushable() and directly call push() via doPush().
     * Both injections are needed: isPushable for minecarts, push for everything else.
     */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPushByEntity(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }

        if (FlatteningHelper.isFlattened(player)) {
            ci.cancel();
        }
    }

    /**
     * Prevents other entities from colliding with flattened players.
     */
    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void onCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }

        if (FlatteningHelper.isFlattened(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Prevents flattened players from colliding with other entities.
     */
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void onCanCollideWith(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }

        if (FlatteningHelper.isFlattened(player)) {
            cir.setReturnValue(false);
        }
    }
}
