package com.terryfox.toonflattening.client;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.network.RequestReformPayload;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ToonFlattening.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    static {
        NeoForge.EVENT_BUS.addListener(FlattenRenderer::onRenderLivingPre);
        NeoForge.EVENT_BUS.addListener(FlattenRenderer::onRenderLivingPost);
    }
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (!FlattenedStateHelper.isFlattened(player)) {
            return;
        }

        // Block all movement input
        Input input = event.getInput();
        input.leftImpulse = 0.0f;
        input.forwardImpulse = 0.0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        while (KeyBindings.reformKey.consumeClick()) {
            if (FlattenedStateHelper.isFlattened(player)) {
                PacketDistributor.sendToServer(new RequestReformPayload());
            }
        }
    }
}
