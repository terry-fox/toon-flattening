package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningHelper;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class SuffocationHandler {
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (FlatteningHelper.isFlattened(player)) {
            if (event.getSource().is(DamageTypes.IN_WALL) || event.getSource().is(DamageTypes.CRAMMING)) {
                event.setCanceled(true);
            }
        }
    }
}
