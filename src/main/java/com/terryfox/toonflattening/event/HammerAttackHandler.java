package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningStateController;
import com.terryfox.toonflattening.item.HammerItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class HammerAttackHandler {
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // Check attacker is ServerPlayer
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        // Check attacker holding hammer
        if (!(attacker.getMainHandItem().getItem() instanceof HammerItem)) {
            return;
        }

        // Check target is ServerPlayer
        if (!(event.getEntity() instanceof ServerPlayer target)) {
            return;
        }

        // Check PvP allowed
        if (!attacker.canHarmPlayer(target)) {
            return;
        }

        // Check full attack strength
        if (attacker.getAttackStrengthScale(0.0f) < 1.0f) {
            return;
        }

        // Cancel damage, trigger flatten
        event.setCanceled(true);
        FlatteningStateController.flattenWithHammer(target);
    }
}
