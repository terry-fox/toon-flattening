package com.terryfox.toonflattening.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ToonFlatteningConfig {
    public static final ToonFlatteningConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.DoubleValue flattenDamage;
    public final ModConfigSpec.DoubleValue heightScale;
    public final ModConfigSpec.IntValue reformationTicks;
    public final ModConfigSpec.BooleanValue anvilPinningEnabled;
    public final ModConfigSpec.IntValue anvilPinningTimeoutSeconds;
    public final ModConfigSpec.DoubleValue spreadMultiplier;
    public final ModConfigSpec.DoubleValue maxSpreadWidth;

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

        reformationTicks = builder
            .comment("Animation duration for reformation in ticks (20 = 1 second)")
            .defineInRange("reformationTicks", 5, 1, 100);

        anvilPinningEnabled = builder
            .comment("Whether anvils can pin flattened players, preventing them from reforming")
            .define("anvilPinningEnabled", true);

        anvilPinningTimeoutSeconds = builder
            .comment("Time in seconds before pinned players can reform (0 = infinite, requires manual anvil removal)")
            .defineInRange("anvilPinningTimeoutSeconds", 300, 0, 3600);

        spreadMultiplier = builder
            .comment("Width added per anvil hit (width = 1.0 + spreadMultiplier × hitCount, default 0.8)")
            .translation("config.toonflattening.spread_multiplier")
            .defineInRange("spreadMultiplier", 0.8, 0.0, 2.0);

        maxSpreadWidth = builder
            .comment("Maximum width multiplier from spread accumulation (default 6.0)")
            .translation("config.toonflattening.max_spread_width")
            .defineInRange("maxSpreadWidth", 6.0, 2.0, 20.0);

        builder.pop();
    }

    static {
        Pair<ToonFlatteningConfig, ModConfigSpec> pair =
            new ModConfigSpec.Builder().configure(ToonFlatteningConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
