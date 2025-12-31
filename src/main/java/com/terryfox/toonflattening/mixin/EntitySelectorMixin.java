package com.terryfox.toonflattening.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.terryfox.toonflattening.event.MinecartFlatteningHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/**
 * Modifies EntitySelector.pushableBy() to exclude players from minecart push lists.
 * This is part of the push system (separate from collision system).
 * 
 * When a minecart calls getEntities(EntitySelector.pushableBy(this)), this mixin
 * wraps the predicate to return false for players when the cart is approaching fast,
 * preventing the cart from pushing the player and instead allowing it to pass through.
 */
@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    /**
     * Wraps the pushableBy predicate to exclude players from fast-approaching carts.
     * 
     * Execution flow: AbstractMinecart.tick() → getEntities(pushableBy) → this predicate
     * 
     * When cart is approaching fast, returns false to exclude player from push list,
     * allowing cart to pass through. The actual flattening is triggered separately
     * in AbstractMinecartCollisionMixin.canCollideWith().
     */
    @ModifyReturnValue(method = "pushableBy", at = @At("RETURN"))
    private static Predicate<Entity> wrapPushablePredicate(
            Predicate<Entity> original, Entity pusher) {

        if (!(pusher instanceof AbstractMinecart cart)) {
            return original;
        }

        return original.and(target -> {
            if (!(target instanceof ServerPlayer player)) {
                return true;
            }
            if (cart.level().isClientSide()) {
                return true;
            }

            Vec3 velocity = cart.getDeltaMovement();

            // Bypass collision for fast approaching carts
            // Flattening is triggered in AbstractMinecartCollisionMixin.canCollideWith()
            if (MinecartFlatteningHandler.meetsVelocityThreshold(cart, player, velocity)) {
                return false;
            }

            return true; // Normal collision
        });
    }
}
