package com.terryfox.toonflattening.mixin;

import com.terryfox.toonflattening.event.MinecartFlatteningHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin {
    @Unique
    private Vec3 toonflattening$savedVelocity = null;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart)(Object)this;
        if (self.level().isClientSide) return;

        Vec3 velocity = self.getDeltaMovement();
        // Save velocity if cart is moving (for use in TAIL)
        toonflattening$savedVelocity = velocity.horizontalDistanceSqr() > 0.0001 ? velocity : null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart)(Object)this;
        if (self.level().isClientSide) return;

        // Only process if cart was moving at start of tick
        if (toonflattening$savedVelocity == null) return;

        // Detect players AFTER movement (cart now overlaps player)
        AABB box = self.getBoundingBox().inflate(0.2F, 0.0D, 0.2F);
        boolean flattenedSomeone = false;

        for (Entity entity : self.level().getEntities(self, box)) {
            if (entity instanceof ServerPlayer player && player.isPushable()) {
                MinecartFlatteningHandler.tryFlatten(self, player);
                flattenedSomeone = true;
            }
        }

        // Restore velocity if we flattened someone (cart passes through)
        if (flattenedSomeone) {
            self.setDeltaMovement(toonflattening$savedVelocity);
        }
    }
}
