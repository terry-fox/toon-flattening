package com.terryfox.toonflattening.config;

import com.terryfox.toonflattening.event.FlattenCause;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;

public class ToonFlatteningConfig {
    public static final ToonFlatteningConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    @Deprecated(forRemoval = true)
    public final ModConfigSpec.DoubleValue flattenDamage;
    @Deprecated(forRemoval = true)
    public final ModConfigSpec.DoubleValue heightScale;
    @Deprecated(forRemoval = true)
    public final ModConfigSpec.DoubleValue widthScale;
    public final ModConfigSpec.IntValue reformationTicks;

    private final Map<FlattenCause, TriggerConfigSpec> triggerConfigs;

    private ToonFlatteningConfig(ModConfigSpec.Builder builder) {
        builder.comment("Toon Flattening Server Configuration")
               .push("flattening");

        // Legacy config - kept for backwards compatibility
        flattenDamage = builder
            .comment("Amount of damage dealt when a player is flattened by an anvil")
            .translation("config.toonflattening.flatten_damage")
            .defineInRange("flattenDamage", 4.0, 0.0, 20.0);

        heightScale = builder
            .comment("Height multiplier when flattened (default 0.05 = 1/20th height)")
            .translation("config.toonflattening.height_scale")
            .defineInRange("heightScale", 0.05, 0.01, 1.0);

        widthScale = builder
            .comment("Width multiplier when flattened (default 1.8 = 1.8x width)")
            .translation("config.toonflattening.width_scale")
            .defineInRange("widthScale", 1.8, 1.0, 6.0);

        reformationTicks = builder
            .comment("Animation duration for reformation in ticks (20 = 1 second)")
            .defineInRange("reformationTicks", 5, 1, 100);

        builder.pop();

        // Per-trigger configuration
        builder.comment("Per-trigger settings (hot-reloadable)")
               .push("triggers");

        triggerConfigs = new EnumMap<>(FlattenCause.class);
        for (FlattenCause cause : FlattenCause.values()) {
            TriggerDefaults defaults = TriggerDefaults.forCause(cause);
            triggerConfigs.put(cause, new TriggerConfigSpec(builder, cause.name(), defaults));
        }

        builder.pop();
    }

    public TriggerConfigSpec getTriggerConfig(FlattenCause cause) {
        return triggerConfigs.get(cause);
    }

    static {
        Pair<ToonFlatteningConfig, ModConfigSpec> pair =
            new ModConfigSpec.Builder().configure(ToonFlatteningConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
