package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.event.MinecartFlatteningHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public class AbstractMinecartCollisionMixin {

    /**
     * Prevents fast-approaching minecarts from being blocked by player's bounding box during movement.
     * Also triggers flattening when cart is close enough.
     * This runs during Entity.move() → collide() → getEntityCollisions(), BEFORE position updates.
     * Cart's current position matches visual position, fixing early flatten and pass-through issues.
     */
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void onCanCollideWith(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;

        if (entity instanceof ServerPlayer player && !cart.level().isClientSide()) {
            Vec3 velocity = cart.getDeltaMovement();
            double cartSpeed = velocity.horizontalDistance();

            // Distance from cart's CURRENT position (before move updates)
            // This matches the visual position, preventing early flatten
            double dx = player.getX() - cart.getX();
            double dz = player.getZ() - cart.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            // Collision bypass: cart approaching fast (includes dirDot check)
            boolean fastApproaching = MinecartFlatteningHandler.meetsVelocityThreshold(cart, player, velocity);

            // Flatten trigger: cart close and fast (NO dirDot - catches "just passed through")
            boolean closeAndFast = distance <= MinecartFlatteningHandler.TRIGGER_RADIUS
                                   && cartSpeed >= 0.1;

            // Trigger flatten when close and moving fast (regardless of direction)
            if (closeAndFast) {
                MinecartFlatteningHandler.tryFlatten(cart, player, velocity);
            }

            // Make player non-solid if:
            // - Fast approaching (to let cart through), OR
            // - Close and fast (to let cart exit after passing through)
            if (fastApproaching || closeAndFast) {
                cir.setReturnValue(false);
            }
        }
    }
}
