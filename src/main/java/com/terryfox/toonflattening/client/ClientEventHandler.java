package com.terryfox.toonflattening.client;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.core.FlatteningHelper;
import com.terryfox.toonflattening.network.RequestReformPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ToonFlattening.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (!FlatteningHelper.isFlattened(player)) {
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
            FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
            if (state.isFlattened()) {
                PacketDistributor.sendToServer(new RequestReformPayload());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (!FlatteningHelper.isFlattened(player)) {
            return;
        }

        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK) {
            event.setCanceled(true);
        }
    }
}
