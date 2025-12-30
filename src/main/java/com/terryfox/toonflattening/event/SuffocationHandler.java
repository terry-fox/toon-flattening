package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class SuffocationHandler {
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        FlattenedStateAttachment.ifFlattened(event.getEntity(), () -> {
            if (event.getSource().is(DamageTypes.IN_WALL) || event.getSource().is(DamageTypes.CRAMMING)) {
                event.setCanceled(true);
            }
        });
    }
}
