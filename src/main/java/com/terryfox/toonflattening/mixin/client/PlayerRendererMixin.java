package com.terryfox.toonflattening.mixin.client;

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

    // Store original xRot to restore after render (for camera)
    private static float originalXRot;
    private static float originalXRotO;
    private static boolean needsRestore = false;

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (!state.isFlattened() || state.frozenPose() == null) {
            return;
        }

        FrozenPoseData pose = state.frozenPose();
        boolean isLocalPlayer = player == Minecraft.getInstance().player;

        // Override body rotation (both current and old for smooth interpolation)
        player.yBodyRot = pose.yBodyRot();
        player.yBodyRotO = pose.yBodyRot();

        // Freeze head rotation for all players
        player.yHeadRot = pose.yHeadRot();
        player.yHeadRotO = pose.yHeadRot();

        // Freeze pitch (xRot) for head visual
        // Store original for camera, will be restored in POST
        if (isLocalPlayer) {
            originalXRot = player.getXRot();
            originalXRotO = player.xRotO;
            needsRestore = true;
        }
        player.setXRot(pose.xRot());
        player.xRotO = pose.xRot();

        // Override walk animation
        ((WalkAnimationStateAccessor) player.walkAnimation).setPosition(pose.walkAnimPos());
        player.walkAnimation.setSpeed(pose.walkAnimSpeed());

        // Override attack animation (both current and old)
        player.attackAnim = pose.attackAnim();
        player.oAttackAnim = pose.attackAnim();

        // Override swing state
        player.swingTime = pose.swingTime();
        player.swinging = pose.swinging();

        // Override crouch state
        player.setShiftKeyDown(pose.crouching());
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void onRenderPost(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        // Restore original xRot for camera after render
        if (needsRestore && player == Minecraft.getInstance().player) {
            player.setXRot(originalXRot);
            player.xRotO = originalXRotO;
            needsRestore = false;
        }
    }
}
