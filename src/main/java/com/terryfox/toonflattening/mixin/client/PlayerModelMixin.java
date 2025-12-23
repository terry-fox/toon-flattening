package com.terryfox.toonflattening.mixin.client;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freezes player animations when flattened.
 *
 * WHY TARGET HumanoidModel:
 * - Body parts (head, body, arms, legs) are in HumanoidModel, not PlayerModel
 * - PlayerModel extends HumanoidModel and adds layer parts
 * - Cast to PlayerModel when syncing layer parts
 */
@Mixin(HumanoidModel.class)
public abstract class PlayerModelMixin {

    @Shadow
    public ModelPart head;
    @Shadow
    public ModelPart body;
    @Shadow
    public ModelPart rightArm;
    @Shadow
    public ModelPart leftArm;
    @Shadow
    public ModelPart rightLeg;
    @Shadow
    public ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void freezeAnimationWhenFlattened(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment attachment = player.getData(ToonFlattening.FLATTENED_STATE.get());
        if (!attachment.isFlattened()) {
            return;
        }

        // Reset all rotations to default standing pose
        head.xRot = 0.0f;
        head.yRot = 0.0f;
        head.zRot = 0.0f;

        body.xRot = 0.0f;
        body.yRot = 0.0f;
        body.zRot = 0.0f;

        rightArm.xRot = 0.0f;
        rightArm.yRot = 0.0f;
        rightArm.zRot = 0.0f;

        leftArm.xRot = 0.0f;
        leftArm.yRot = 0.0f;
        leftArm.zRot = 0.0f;

        rightLeg.xRot = 0.0f;
        rightLeg.yRot = 0.0f;
        rightLeg.zRot = 0.0f;

        leftLeg.xRot = 0.0f;
        leftLeg.yRot = 0.0f;
        leftLeg.zRot = 0.0f;

        // Sync layer parts if this is a PlayerModel
        if ((Object) this instanceof PlayerModel<?> playerModel) {
            playerModel.jacket.copyFrom(body);
            playerModel.rightSleeve.copyFrom(rightArm);
            playerModel.leftSleeve.copyFrom(leftArm);
            playerModel.rightPants.copyFrom(rightLeg);
            playerModel.leftPants.copyFrom(leftLeg);
        }
    }
}
