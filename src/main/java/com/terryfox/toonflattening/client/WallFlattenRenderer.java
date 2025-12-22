package com.terryfox.toonflattening.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.event.CollisionType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side renderer for wall flattening visual effects.
 *
 * WHY COUNTERSCALING:
 * - Pehkui's ScaleTypes.WIDTH scales BOTH X and Z uniformly to 0.2
 * - Need directional flattening (thin in one axis, normal in other)
 * - Use PoseStack.scale() to counteract unwanted axis
 * - Example: NORTH wall should be thin in Z only
 *   - Pehkui: X=0.2, Z=0.2
 *   - PoseStack: X=5.0, Z=0.25
 *   - Result: X=1.0 (normal), Z=0.05 (thin)
 *
 * WHY ENTITY TRACKING:
 * - Must pair pushPose() in Pre with popPose() in Post
 * - ConcurrentHashMap prevents unmatched push/pop crashes
 * - Tracks which entities had transformations applied
 */
@OnlyIn(Dist.CLIENT)
public class WallFlattenRenderer {

    private static final Set<Integer> pushedEntities = ConcurrentHashMap.newKeySet();

    // Counteract Pehkui's uniform WIDTH scaling (0.2 on both axes)
    // Multiply to achieve target scales: 0.05 for thin axis, 1.0 for normal axis
    private static final float THIN_SCALE = 0.25f;   // 0.05 / 0.2
    private static final float NORMAL_SCALE = 5.0f;  // 1.0 / 0.2

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment attachment = player.getData(ToonFlattening.FLATTENED_STATE.get());

        if (!attachment.isFlattened()) {
            return;
        }

        CollisionType collisionType = attachment.collisionType();
        Direction wallDirection = attachment.wallDirection();

        PoseStack poseStack = event.getPoseStack();

        if (collisionType == CollisionType.WALL && wallDirection != null) {
            poseStack.pushPose();
            pushedEntities.add(player.getId());

            float offsetAmount = 0.45f;
            switch (wallDirection) {
                case NORTH -> {
                    poseStack.translate(0, 0, -offsetAmount);
                    poseStack.scale(NORMAL_SCALE, 1.0f, THIN_SCALE);
                }
                case SOUTH -> {
                    poseStack.translate(0, 0, offsetAmount);
                    poseStack.scale(NORMAL_SCALE, 1.0f, THIN_SCALE);
                }
                case EAST -> {
                    poseStack.translate(offsetAmount, 0, 0);
                    poseStack.scale(THIN_SCALE, 1.0f, NORMAL_SCALE);
                }
                case WEST -> {
                    poseStack.translate(-offsetAmount, 0, 0);
                    poseStack.scale(THIN_SCALE, 1.0f, NORMAL_SCALE);
                }
                default -> {}
            }
        }
        // CEILING: No render offset needed - server positions player so scaled head is at ceiling
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (pushedEntities.remove(player.getId())) {
            event.getPoseStack().popPose();
        }
    }
}
