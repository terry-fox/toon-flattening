package com.terryfox.toonflattening;

import com.terryfox.toonflattening.core.FlattenStateManager;
import com.terryfox.toonflattening.detection.AnvilDamageCanceller;
import com.terryfox.toonflattening.infrastructure.ConfigSpec;
import com.terryfox.toonflattening.infrastructure.NetworkPackets;
import com.terryfox.toonflattening.infrastructure.PlayerDataAttachment;
import com.terryfox.toonflattening.infrastructure.SoundRegistry;
import com.terryfox.toonflattening.infrastructure.TickOrchestrator;
import com.terryfox.toonflattening.integration.ScalingIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ToonFlattening.MODID)
public class ToonFlattening {
    public static final String MODID = "toonflattening";
    private static final Logger LOGGER = LoggerFactory.getLogger(ToonFlattening.class);

    public ToonFlattening(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("Initializing Toon Flattening mod");

        // Register config
        container.registerConfig(ModConfig.Type.COMMON, ConfigSpec.SPEC);

        // Register DeferredRegisters
        PlayerDataAttachment.ATTACHMENT_TYPES.register(modEventBus);
        SoundRegistry.SOUNDS.register(modEventBus);

        // Initialize integration layer
        ScalingIntegration.initialize();

        // Register FORGE event handlers
        NeoForge.EVENT_BUS.register(new AnvilDamageCanceller());
        NeoForge.EVENT_BUS.register(new TickOrchestrator());

        // Config injection listeners
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);

        // Network packet registration
        modEventBus.addListener(this::onRegisterPayloads);
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        injectConfigIntoStateManager();
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        injectConfigIntoStateManager();
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        NetworkPackets.register(registrar);
    }

    /**
     * Inject config values into FlattenStateManager.
     * <p>
     * Converts hearts to damage points (*2.0) and seconds to ticks (*20).
     */
    private void injectConfigIntoStateManager() {
        FlattenStateManager manager = FlattenStateManager.getInstance();
        manager.setMinHeightScale(ConfigSpec.height_scale.get().floatValue());
        manager.setSpreadIncrement(ConfigSpec.spread_increment.get().floatValue());
        manager.setMaxSpreadLimit(ConfigSpec.max_spread_limit.get().floatValue());
        manager.setBaseDamage((float) (ConfigSpec.damage_amount.get() * 2.0)); // hearts → damage points
        manager.setStackDamagePerAnvil((float) (ConfigSpec.stack_damage_per_anvil.get() * 2.0));
        manager.setReformationTicks(ConfigSpec.reformation_ticks.get());
        manager.setFallbackTimeoutTicks(ConfigSpec.fallback_timeout_seconds.get() * 20); // seconds → ticks
        manager.setReflattenCooldownTicks(ConfigSpec.reflatten_cooldown_ticks.get());
    }
}
