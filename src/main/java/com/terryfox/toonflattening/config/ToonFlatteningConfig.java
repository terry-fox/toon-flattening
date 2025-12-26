package com.terryfox.toonflattening.config;

import com.terryfox.toonflattening.event.FlattenCause;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;

public class ToonFlatteningConfig {
    public static final ToonFlatteningConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.IntValue reformationTicks;

    private final Map<FlattenCause, TriggerConfigSpec> triggerConfigs;

    private ToonFlatteningConfig(ModConfigSpec.Builder builder) {
        builder.comment("Toon Flattening Server Configuration")
               .push("flattening");

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
