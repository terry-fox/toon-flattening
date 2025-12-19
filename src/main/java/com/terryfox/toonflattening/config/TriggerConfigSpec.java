package com.terryfox.toonflattening.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-trigger configuration builder for hot-reloadable settings.
 */
public class TriggerConfigSpec {
    private final ModConfigSpec.BooleanValue enabled;
    private final ModConfigSpec.DoubleValue damage;
    private final ModConfigSpec.DoubleValue heightScale;
    private final ModConfigSpec.DoubleValue widthScale;

    public TriggerConfigSpec(ModConfigSpec.Builder builder, String triggerName, TriggerDefaults defaults) {
        builder.comment(triggerName + " trigger configuration")
               .push(triggerName.toLowerCase());

        enabled = builder
            .comment("Enable " + triggerName + " trigger")
            .define("enabled", defaults.enabled());

        damage = builder
            .comment("Damage dealt by " + triggerName)
            .defineInRange("damage", defaults.damage(), 0.0, 20.0);

        heightScale = builder
            .comment("Height scale for " + triggerName)
            .defineInRange("heightScale", defaults.heightScale(), 0.01, 1.0);

        widthScale = builder
            .comment("Width scale for " + triggerName)
            .defineInRange("widthScale", defaults.widthScale(), 1.0, 6.0);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public double getDamage() {
        return damage.get();
    }

    public double getHeightScale() {
        return heightScale.get();
    }

    public double getWidthScale() {
        return widthScale.get();
    }
}
