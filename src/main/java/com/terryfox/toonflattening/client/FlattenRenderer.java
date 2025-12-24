package com.terryfox.toonflattening.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.event.CollisionType;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import com.terryfox.toonflattening.util.RotationState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side renderer for flattening visual effects using direct matrix transforms.
 * Replaces Pehkui-based scaling with manual PoseStack transformations.
 */
@OnlyIn(Dist.CLIENT)
public class FlattenRenderer {

    private static final Set<Integer> pushedEntities = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, RotationState> rotationStates = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> flattenStartTimes = new ConcurrentHashMap<>();
    private static final Map<Integer, CollisionType> flattenTypes = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment attachment = FlattenedStateHelper.getState(player);
        boolean isFlattened = FlattenedStateHelper.isFlattened(player);
        boolean isRestoring = attachment.isRestoring();

        if (!isFlattened && !isRestoring) {
            flattenStartTimes.remove(player.getId());
            flattenTypes.remove(player.getId());
            return;
        }

        // Track flatten start time for animation
        if (isFlattened && !flattenStartTimes.containsKey(player.getId())) {
            flattenStartTimes.put(player.getId(), System.currentTimeMillis());
            flattenTypes.put(player.getId(), attachment.collisionType());
        }

        CollisionType collisionType = attachment.collisionType();
        Direction wallDirection = attachment.wallDirection();
        float frozenYaw = attachment.frozenYaw();

        // Store and override rotation
        RotationState stored = RotationState.capture(player);
        rotationStates.put(player.getId(), stored);
        player.yBodyRot = frozenYaw;
        player.yBodyRotO = frozenYaw;
        player.yHeadRot = frozenYaw;
        player.yHeadRotO = frozenYaw;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        pushedEntities.add(player.getId());

        // Calculate animation progress (0.0 to 1.0)
        float animProgress = 1.0f;
        if (isRestoring) {
            long currentTime = player.level().getGameTime();
            long elapsed = currentTime - attachment.restorationStartTime();
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
            animProgress = 1.0f - Math.min(1.0f, (float)elapsed / reformationTicks);
        } else if (flattenStartTimes.containsKey(player.getId())) {
            long elapsed = System.currentTimeMillis() - flattenStartTimes.get(player.getId());
            float animDuration = 500f; // 500ms flatten animation
            animProgress = Math.min(1.0f, elapsed / animDuration);
        }

        // Apply easing to animation progress (ease-out cubic)
        float easedProgress = 1.0f - (float)Math.pow(1.0f - animProgress, 3.0);

        // Apply transformations based on collision type
        applyFlattenTransform(poseStack, player, collisionType, wallDirection, attachment, easedProgress);
    }

    private static void applyFlattenTransform(PoseStack poseStack, Player player, CollisionType collisionType,
                                             Direction wallDirection, FlattenedStateAttachment attachment, float progress) {
        double depthScale = ToonFlatteningConfig.CONFIG.depthScale.get();
        double widthScale = ToonFlatteningConfig.CONFIG.widthScale.get();

        // Interpolate from normal (1.0) to flattened state
        float heightScale = 1.0f - ((1.0f - (float)depthScale) * progress);
        float xyScale = 1.0f + (((float)widthScale - 1.0f) * progress);

        if (collisionType == CollisionType.WALL && wallDirection != null) {
            double wallSurfacePos = attachment.wallSurfacePos();
            if (wallSurfacePos > 0) {
                double halfModelWidth = 0.6 * depthScale / 2 * progress;
                float renderOffset = 0;

                switch (wallDirection) {
                    case EAST -> renderOffset = (float)((wallSurfacePos - player.getX()) - halfModelWidth);
                    case WEST -> renderOffset = (float)((wallSurfacePos - player.getX()) + halfModelWidth);
                    case SOUTH -> renderOffset = (float)((wallSurfacePos - player.getZ()) - halfModelWidth);
                    case NORTH -> renderOffset = (float)((wallSurfacePos - player.getZ()) + halfModelWidth);
                }

                renderOffset *= progress;

                if (wallDirection == Direction.NORTH || wallDirection == Direction.SOUTH) {
                    poseStack.translate(0, 0, renderOffset);
                } else {
                    poseStack.translate(renderOffset, 0, 0);
                }
            }

            // Wall flattening: thin in wall direction, normal perpendicular
            float thinScale = 1.0f - ((1.0f - (float)depthScale) * progress);
            switch (wallDirection) {
                case NORTH, SOUTH -> poseStack.scale(1.0f, 1.0f, thinScale);
                case EAST, WEST -> poseStack.scale(thinScale, 1.0f, 1.0f);
            }
        } else if (collisionType == CollisionType.CEILING) {
            double ceilingBlockY = attachment.ceilingBlockY();
            if (ceilingBlockY > 0) {
                float scaledHeight = 1.8f * heightScale;
                double currentY = player.getY();
                double targetY = ceilingBlockY - scaledHeight;
                float yOffset = (float)(targetY - currentY) * progress;
                poseStack.translate(0, yOffset, 0);
            }

            // Ceiling flatten: squash Y from top, expand X/Z
            poseStack.scale(xyScale, heightScale, xyScale);
        } else {
            // Floor/Anvil: squash Y, expand X/Z
            poseStack.scale(xyScale, heightScale, xyScale);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Restore rotation
        RotationState state = rotationStates.remove(player.getId());
        if (state != null) {
            state.restore(player);
        }

        if (pushedEntities.remove(player.getId())) {
            event.getPoseStack().popPose();
        }
    }
}
