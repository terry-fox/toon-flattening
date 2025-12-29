package com.terryfox.toonflattening.infrastructure.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.terryfox.toonflattening.ToonFlattening;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side keybind registration (MOD bus).
 * <p>
 * Registers SPACE key for reformation.
 */
@EventBusSubscriber(modid = ToonFlattening.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeybindHandler {
    /**
     * Reformation keybind (SPACE).
     */
    public static final KeyMapping REFORM_KEY = new KeyMapping(
            "key.toonflattening.reform",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            "key.categories.toonflattening"
    );

    private KeybindHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REFORM_KEY);
    }
}
