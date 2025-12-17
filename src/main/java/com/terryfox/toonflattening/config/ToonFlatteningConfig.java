package com.terryfox.toonflattening.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ToonFlatteningConfig {
    public static final ToonFlatteningConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.DoubleValue flattenDamage;
    public final ModConfigSpec.DoubleValue heightScale;

    private ToonFlatteningConfig(ModConfigSpec.Builder builder) {
        builder.comment("Toon Flattening Server Configuration")
               .push("flattening");

        flattenDamage = builder
            .comment("Amount of damage dealt when a player is flattened by an anvil")
            .translation("config.toonflattening.flatten_damage")
            .defineInRange("flattenDamage", 4.0, 0.0, 20.0);

        heightScale = builder
            .comment("Height multiplier when flattened (default 0.05 = 1/20th height)")
            .translation("config.toonflattening.height_scale")
            .defineInRange("heightScale", 0.05, 0.01, 1.0);

        builder.pop();
    }

    static {
        Pair<ToonFlatteningConfig, ModConfigSpec> pair =
            new ModConfigSpec.Builder().configure(ToonFlatteningConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
