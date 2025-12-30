package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

public class KnockbackHandler {
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (FlatteningHelper.isFlattened(player)) {
            event.setCanceled(true);
        }
    }
}
