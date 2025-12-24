package com.terryfox.toonflattening.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class FallDamageCalculator {

    public static boolean hasSlowFalling(Player player) {
        return player.hasEffect(MobEffects.SLOW_FALLING);
    }

    public static double calculateFallThresholdVelocity(Player player, double targetDamage) {
        double safeFallDistance = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        double fallDamageMultiplier = player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        double gravity = player.getAttributeValue(Attributes.GRAVITY);

        double enchantReduction = getEnchantmentDamageReduction(player);

        double rawDamageNeeded = targetDamage / Math.max(0.2, 1.0 - enchantReduction);
        double requiredDistance = (rawDamageNeeded / fallDamageMultiplier) + safeFallDistance;

        return Math.sqrt(2.0 * gravity * requiredDistance);
    }

    public static double calculateWallThresholdVelocity(Player player, double targetDamage) {
        double enchantReduction = getEnchantmentDamageReduction(player);

        double rawDamageNeeded = targetDamage / Math.max(0.2, 1.0 - enchantReduction);

        return Math.sqrt(2.0 * rawDamageNeeded);
    }

    public static double calculateFallDamage(Player player, double velocity) {
        double gravity = player.getAttributeValue(Attributes.GRAVITY);
        double safeFallDistance = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        double fallDamageMultiplier = player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);

        double fallDistance = (velocity * velocity) / (2.0 * gravity);
        double rawDamage = Math.max(0.0, (fallDistance - safeFallDistance) * fallDamageMultiplier);

        double enchantReduction = getEnchantmentDamageReduction(player);
        double finalDamage = Math.ceil(rawDamage * (1.0 - enchantReduction));

        return finalDamage;
    }

    private static double getEnchantmentDamageReduction(Player player) {
        DamageSource fallDamageSource = player.damageSources().fall();

        // Cast Level to ServerLevel for enchantment calculation
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return 0.0; // Client-side or invalid level, no reduction
        }

        float enchantmentProtection = EnchantmentHelper.getDamageProtection(serverLevel, player, fallDamageSource);

        double reduction = enchantmentProtection * 0.04;
        return Math.min(0.8, reduction);
    }
}
