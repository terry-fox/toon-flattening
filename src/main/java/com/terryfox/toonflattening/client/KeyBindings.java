package com.terryfox.toonflattening.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.terryfox.toonflattening.ToonFlattening;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ToonFlattening.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    public static final KeyMapping reformKey = new KeyMapping(
        "key.toonflattening.reform",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_SPACE,
        "key.categories.toonflattening"
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(reformKey);
    }
}
