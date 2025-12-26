package com.terryfox.toonflattening.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.config.TriggerConfigSpec;
import com.terryfox.toonflattening.event.FlattenCause;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ToonFlattening.MODID, value = Dist.CLIENT)
public class DepthScaleRenderer {

    private static final Map<UUID, AnimationState> animations = new HashMap<>();

    private static class AnimationState {
        float currentDepthScale = 1.0f;
        float targetDepthScale = 1.0f;
        long lastUpdateTime = 0;

        AnimationState(float initial) {
            this.currentDepthScale = initial;
            this.targetDepthScale = initial;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());

        if (state == null) {
            return;
        }

        UUID playerId = player.getUUID();
        AnimationState animState = animations.computeIfAbsent(playerId, id -> new AnimationState(1.0f));

        float targetDepth;
        if (state.isFlattened()) {
            TriggerConfigSpec config = ToonFlatteningConfig.CONFIG.getTriggerConfig(FlattenCause.ANVIL);
            targetDepth = (float) config.getHeightScale();
        } else {
            targetDepth = 1.0f;
        }

        if (animState.targetDepthScale != targetDepth) {
            animState.targetDepthScale = targetDepth;
        }

        updateAnimation(animState);

        if (animState.currentDepthScale != 1.0f) {
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.scale(1.0f, animState.currentDepthScale, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());

        if (state == null) {
            return;
        }

        UUID playerId = player.getUUID();
        AnimationState animState = animations.get(playerId);

        if (animState != null && animState.currentDepthScale != 1.0f) {
            PoseStack poseStack = event.getPoseStack();
            poseStack.popPose();
        }
    }

    private static void updateAnimation(AnimationState state) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - state.lastUpdateTime) / 1000.0f;
        state.lastUpdateTime = currentTime;

        int reformTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
        float animSpeed = 1.0f / (reformTicks / 20.0f);

        if (Math.abs(state.currentDepthScale - state.targetDepthScale) < 0.001f) {
            state.currentDepthScale = state.targetDepthScale;
        } else {
            float delta = (state.targetDepthScale - state.currentDepthScale) * animSpeed * deltaTime * 10.0f;
            state.currentDepthScale += delta;

            if ((delta > 0 && state.currentDepthScale > state.targetDepthScale) ||
                (delta < 0 && state.currentDepthScale < state.targetDepthScale)) {
                state.currentDepthScale = state.targetDepthScale;
            }
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        animations.clear();
    }
}
