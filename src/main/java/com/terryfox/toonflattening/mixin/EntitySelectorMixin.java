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

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

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
