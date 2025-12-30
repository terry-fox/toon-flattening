package com.terryfox.toonflattening.mixin.client;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.attachment.FrozenPoseData;
import com.terryfox.toonflattening.mixin.accessor.WalkAnimationStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freezes player pose when flattened for third-person rendering.
 * Camera remains free for local player in first-person.
 */
@Mixin(value = PlayerRenderer.class, priority = 1500)
public class PlayerRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci,
                          @Share("xRot") LocalFloatRef originalXRot, @Share("xRotO") LocalFloatRef originalXRotO) {
        if (!FlattenedStateAttachment.isFlattened(player)) {
            return;
        }

        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state.frozenPose() == null) {
            return;
        }

        FrozenPoseData pose = state.frozenPose();
        boolean isLocalPlayer = player == Minecraft.getInstance().player;

        // Override body rotation (both current and old for smooth interpolation)
        player.yBodyRot = pose.rotation().yBodyRot();
        player.yBodyRotO = pose.rotation().yBodyRot();

        // Freeze head rotation for all players
        player.yHeadRot = pose.rotation().yHeadRot();
        player.yHeadRotO = pose.rotation().yHeadRot();

        // Freeze pitch (xRot) for head visual
        // Store original for camera, will be restored in POST
        if (isLocalPlayer) {
            originalXRot.set(player.getXRot());
            originalXRotO.set(player.xRotO);
        }
        player.setXRot(pose.rotation().xRot());
        player.xRotO = pose.rotation().xRot();

        // Override walk animation
        ((WalkAnimationStateAccessor) player.walkAnimation).setPosition(pose.animation().walkAnimPos());
        player.walkAnimation.setSpeed(pose.animation().walkAnimSpeed());

        // Override attack animation (both current and old)
        player.attackAnim = pose.animation().attackAnim();
        player.oAttackAnim = pose.animation().attackAnim();

        // Override swing state
        player.swingTime = pose.animation().swingTime();
        player.swinging = pose.animation().swinging();

        // Override crouch state
        player.setShiftKeyDown(pose.animation().crouching());
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void onRenderPost(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci,
                              @Share("xRot") LocalFloatRef originalXRot, @Share("xRotO") LocalFloatRef originalXRotO) {
        if (!FlattenedStateAttachment.isFlattened(player)) {
            return;
        }

        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (state.frozenPose() == null) {
            return;
        }

        // Restore original xRot for camera after render
        if (player == Minecraft.getInstance().player) {
            player.setXRot(originalXRot.get());
            player.xRotO = originalXRotO.get();
        }
    }
}
