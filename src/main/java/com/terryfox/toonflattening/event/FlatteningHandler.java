package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.api.FlattenContext;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.config.TriggerConfigSpec;
import com.terryfox.toonflattening.core.FlatteningService;
import com.terryfox.toonflattening.registry.FlattenTriggerRegistry;
import com.terryfox.toonflattening.trigger.FlattenTrigger;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Event handler that checks for flattening triggers and delegates to FlatteningService.
 */
public class FlatteningHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        for (FlattenTrigger trigger : FlattenTriggerRegistry.getAll()) {
            TriggerConfigSpec config = ToonFlatteningConfig.CONFIG.getTriggerConfig(trigger.getCause());

            if (!config.isEnabled()) {
                continue;
            }

            FlattenContext context = trigger.shouldTrigger(event);
            if (context != null) {
                boolean wasFlattened = FlatteningService.tryFlattenPlayer(player, context, config);
                if (wasFlattened) {
                    event.setAmount((float) config.getDamage());
                }
                return;
            }
        }
    }
}
