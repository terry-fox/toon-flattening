package com.terryfox.toonflattening.infrastructure;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration specification for Toon Flattening mod.
 * <p>
 * Per SRS Section 7: Defines 10 config values across 4 sections.
 * Values are injected into FlattenStateManager during mod initialization.
 */
public final class ConfigSpec {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Flattening section
    public static final ModConfigSpec.BooleanValue progressive_enabled;
    public static final ModConfigSpec.DoubleValue damage_amount;
    public static final ModConfigSpec.DoubleValue height_scale;

    // Stacking section
    public static final ModConfigSpec.DoubleValue spread_increment;
    public static final ModConfigSpec.DoubleValue max_spread_limit;
    public static final ModConfigSpec.DoubleValue stack_damage_per_anvil;

    // Reformation section
    public static final ModConfigSpec.IntValue reformation_ticks;
    public static final ModConfigSpec.BooleanValue anvil_blocking_enabled;
    public static final ModConfigSpec.IntValue fallback_timeout_seconds;

    // Reflatten section
    public static final ModConfigSpec.IntValue reflatten_cooldown_ticks;

    public static final ModConfigSpec SPEC;

    static {
        // Flattening section
        BUILDER.comment("Flattening behavior settings")
                .push("flattening");

        progressive_enabled = BUILDER
                .comment("Enable progressive flattening animation (true) or instant flattening (false)")
                .define("progressive_enabled", true);

        damage_amount = BUILDER
                .comment("Base damage in hearts when fully flattened (0.0-20.0)")
                .defineInRange("damage_amount", 4.0, 0.0, 20.0);

        height_scale = BUILDER
                .comment("Minimum height scale when fully flattened (0.01-1.0)")
                .defineInRange("height_scale", 0.05, 0.01, 1.0);

        BUILDER.pop();

        // Stacking section
        BUILDER.comment("Anvil stacking settings")
                .push("stacking");

        spread_increment = BUILDER
                .comment("Horizontal spread added per anvil in stack (0.1-2.0)")
                .defineInRange("spread_increment", 0.8, 0.1, 2.0);

        max_spread_limit = BUILDER
                .comment("Maximum horizontal spread multiplier (1.0-6.0)")
                .defineInRange("max_spread_limit", 6.0, 1.0, 6.0);

        stack_damage_per_anvil = BUILDER
                .comment("Additional damage in hearts per anvil in stack (0.0-10.0)")
                .defineInRange("stack_damage_per_anvil", 1.0, 0.0, 10.0);

        BUILDER.pop();

        // Reformation section
        BUILDER.comment("Reformation behavior settings")
                .push("reformation");

        reformation_ticks = BUILDER
                .comment("Duration of recovery animation in ticks (1-100)")
                .defineInRange("reformation_ticks", 5, 1, 100);

        anvil_blocking_enabled = BUILDER
                .comment("Prevent anvil from landing while player is flattened")
                .define("anvil_blocking_enabled", true);

        fallback_timeout_seconds = BUILDER
                .comment("Seconds before anvil blocking times out and reformation begins (0-3600, 0=disabled)")
                .defineInRange("fallback_timeout_seconds", 300, 0, 3600);

        BUILDER.pop();

        // Reflatten section
        BUILDER.comment("Re-flattening cooldown settings")
                .push("reflatten");

        reflatten_cooldown_ticks = BUILDER
                .comment("Cooldown in ticks before next re-flatten spread calculation (1-100)")
                .defineInRange("reflatten_cooldown_ticks", 20, 1, 100);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ConfigSpec() {
    }
}
