package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningStateController;
import com.terryfox.toonflattening.item.HammerItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
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
        float attackStrength = attacker.getAttackStrengthScale(0.0f);
        if (attackStrength < 1.0f) {
            return;
        }

        // Detect critical hit (all 9 vanilla conditions)
        boolean isCriticalHit = attacker.fallDistance > 0
            && !attacker.onGround()
            && !attacker.onClimbable()
            && !attacker.isInWater()
            && !attacker.hasEffect(MobEffects.BLINDNESS)
            && !attacker.hasEffect(MobEffects.SLOW_FALLING)
            && !attacker.isPassenger()
            && !attacker.isFallFlying();

        // Cancel damage, trigger flatten with crit flag
        event.setCanceled(true);
        FlatteningStateController.flattenWithHammer(target, isCriticalHit);
    }
}
