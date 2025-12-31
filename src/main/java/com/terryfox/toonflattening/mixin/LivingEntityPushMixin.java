package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.event.MinecartFlatteningHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
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
     * Also prevents players from pushing fast-approaching minecarts (which would stop them).
     */
    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    private void onDoPush(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // Flattened entities shouldn't initiate pushes
        FlattenedStateAttachment.ifFlattened(self, () -> ci.cancel());

        // Don't push fast-approaching minecarts (prevents player stopping the cart)
        if (entity instanceof AbstractMinecart cart && self instanceof ServerPlayer player) {
            Vec3 velocity = cart.getDeltaMovement();
            if (MinecartFlatteningHandler.meetsVelocityThreshold(cart, player, velocity)) {
                ci.cancel();
            }
        }
    }

    /**
     * Prevents flattened entities from being pushed.
     */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // Flattened entities don't participate in collisions
        FlattenedStateAttachment.ifFlattened(self, () -> ci.cancel());
    }
}
