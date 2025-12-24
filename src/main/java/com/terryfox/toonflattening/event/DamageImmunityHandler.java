package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.config.ToonFlatteningConfig;
import com.terryfox.toonflattening.util.FlattenedStateHelper;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class DamageImmunityHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment state = FlattenedStateHelper.getState(player);
        long currentTime = player.level().getGameTime();

        // Only block specific damage types (combat-related)
        var damageSource = event.getSource();
        boolean isBlockedDamageType = damageSource.is(DamageTypes.MOB_ATTACK) ||
            damageSource.is(DamageTypes.MOB_ATTACK_NO_AGGRO) ||
            damageSource.is(DamageTypes.PLAYER_ATTACK) ||
            damageSource.is(DamageTypes.SPIT) ||
            damageSource.is(DamageTypes.STING) ||
            damageSource.is(DamageTypes.WITHER_SKULL);

        if (!isBlockedDamageType) {
            return; // Allow all other damage types
        }

        // Allow initial flatten damage (same tick as flatten)
        if (state.isFlattened() && state.flattenTime() == currentTime) {
            return; // Allow initial flatten damage
        }

        // Cancel damage if flattened (after initial tick)
        if (state.isFlattened()) {
            event.setCanceled(true);
            ToonFlattening.LOGGER.debug("Blocked damage to flattened player {}", player.getName().getString());
            return;
        }

        // Cancel damage if restoring
        if (state.isRestoring()) {
            event.setCanceled(true);
            ToonFlattening.LOGGER.debug("Blocked damage to restoring player {}", player.getName().getString());
            return;
        }

        // Check post-restoration immunity
        if (state.restorationStartTime() > 0) {
            long elapsed = currentTime - state.restorationStartTime();
            int reformationTicks = ToonFlatteningConfig.CONFIG.reformationTicks.get();
            int postRestorationImmunityTicks = ToonFlatteningConfig.CONFIG.postRestorationImmunityTicks.get();
            long totalImmunityTicks = reformationTicks + postRestorationImmunityTicks;

            if (elapsed < totalImmunityTicks) {
                event.setCanceled(true);
                ToonFlattening.LOGGER.debug("Blocked damage to player {} in post-restoration immunity (elapsed={}, total={})",
                    player.getName().getString(), elapsed, totalImmunityTicks);
            }
        }
    }
}
