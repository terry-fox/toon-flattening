package com.terryfox.toonflattening.detection;

import com.terryfox.toonflattening.core.FlattenPhase;
import com.terryfox.toonflattening.core.FlattenStateManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Event handler to cancel vanilla anvil damage during progressive flattening.
 * <p>
 * Per SRS FR-PROG.6: Mod applies custom damage when transitioning to FullyFlattened.
 * Vanilla falling anvil damage must be cancelled to prevent double-damage.
 * <p>
 * Registered to NeoForge.EVENT_BUS (FORGE event bus, not MOD event bus).
 */
public class AnvilDamageCanceller {

    /**
     * Cancel vanilla falling anvil damage if player is in PROGRESSIVE_FLATTENING phase.
     * <p>
     * Per SRS FR-PROG.6.1: Custom damage is applied at FullyFlattened transition.
     * Vanilla damage must be cancelled to avoid stacking with custom damage.
     *
     * @param event Living damage event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingDamage(LivingIncomingDamageEvent event) {
        // Only process server-side ServerPlayer entities
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Check if damage source is falling anvil
        if (!event.getSource().is(DamageTypes.FALLING_ANVIL)) {
            return;
        }

        // Check if player is in progressive flattening phase
        FlattenPhase phase = FlattenStateManager.getInstance().getPhase(player);
        if (phase == FlattenPhase.PROGRESSIVE_FLATTENING) {
            // Cancel vanilla damage - custom damage will be applied at FullyFlattened transition
            event.setCanceled(true);
        }
    }
}
