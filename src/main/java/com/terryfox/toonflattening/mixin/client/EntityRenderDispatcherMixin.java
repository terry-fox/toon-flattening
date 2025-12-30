package com.terryfox.toonflattening.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void disableShadowWhenFlattened(
            PoseStack poseStack, MultiBufferSource buffer, Entity entity,
            float weight, float partialTicks, LevelReader level, float radius,
            CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            if (FlattenedStateAttachment.isFlattened(player)) {
                ci.cancel();
            }
        }
    }
}
