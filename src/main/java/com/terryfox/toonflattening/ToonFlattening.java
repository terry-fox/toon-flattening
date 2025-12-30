package com.terryfox.toonflattening;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.event.AnvilBreakHandler;
import com.terryfox.toonflattening.event.AnvilStackHandler;
import com.terryfox.toonflattening.event.FlatteningHandler;
import com.terryfox.toonflattening.event.KnockbackHandler;
import com.terryfox.toonflattening.event.LoginHandler;
import com.terryfox.toonflattening.event.PlayerMovementHandler;
import com.terryfox.toonflattening.event.RespawnHandler;
import com.terryfox.toonflattening.core.FlatteningStateController;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(ToonFlattening.MODID)
public class ToonFlattening {
    public static final String MODID = "toonflattening";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);

    public static final Supplier<AttachmentType<FlattenedStateAttachment>> FLATTENED_STATE =
        ATTACHMENT_TYPES.register("flattened_state", () ->
            AttachmentType.builder(() -> FlattenedStateAttachment.DEFAULT)
                .serialize(FlattenedStateAttachment.CODEC)
                .copyOnDeath()
                .build()
        );

    public static final DeferredHolder<SoundEvent, SoundEvent> FLATTEN_SOUND =
        SOUND_EVENTS.register("flatten", () ->
            SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(MODID, "flatten")
            )
        );

    public ToonFlattening(IEventBus modEventBus, ModContainer modContainer) {
        ATTACHMENT_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, ToonFlatteningConfig.CONFIG_SPEC);

        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FlatteningHandler::onLivingHurt);
        NeoForge.EVENT_BUS.addListener(PlayerMovementHandler::onEntityTick);
        NeoForge.EVENT_BUS.addListener(RespawnHandler::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(LoginHandler::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(AnvilStackHandler::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(KnockbackHandler::onLivingKnockBack);
        NeoForge.EVENT_BUS.addListener(AnvilBreakHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(AnvilBreakHandler::onLeftClickBlock);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("ToonFlattening initialized for Minecraft 1.21.1");
        LOGGER.info("Pehkui integration ready");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("ToonFlattening server starting");
        LOGGER.info("Flatten damage: {}", ToonFlatteningConfig.CONFIG.flattenDamage.get());
        LOGGER.info("Height scale: {}", ToonFlatteningConfig.CONFIG.heightScale.get());

        // Reset all players to ensure clean state on server start
        event.getServer().getPlayerList().getPlayers().forEach(FlatteningStateController::resetPlayer);
    }

    @EventBusSubscriber(modid = ToonFlattening.MODID, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("ToonFlattening client setup complete");
        }
    }
}
