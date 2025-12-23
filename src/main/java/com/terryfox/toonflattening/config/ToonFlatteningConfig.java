package com.terryfox.toonflattening.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ToonFlatteningConfig {
    public static final ToonFlatteningConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.DoubleValue flattenDamage;
    public final ModConfigSpec.DoubleValue heightScale;
    public final ModConfigSpec.DoubleValue widthScale;
    public final ModConfigSpec.IntValue reformationTicks;
    public final ModConfigSpec.IntValue postRestorationImmunityTicks;

    public final ModConfigSpec.DoubleValue floorVelocityThreshold;
    public final ModConfigSpec.DoubleValue ceilingVelocityThreshold;
    public final ModConfigSpec.DoubleValue wallVelocityThreshold;
    public final ModConfigSpec.DoubleValue floorDamage;
    public final ModConfigSpec.DoubleValue ceilingDamage;
    public final ModConfigSpec.DoubleValue wallDamage;
    public final ModConfigSpec.DoubleValue wallHitboxScale;
    public final ModConfigSpec.BooleanValue enableFloorFlatten;
    public final ModConfigSpec.BooleanValue enableCeilingFlatten;
    public final ModConfigSpec.BooleanValue enableWallFlatten;

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

        widthScale = builder
            .comment("Width multiplier when flattened (default 1.8 = 1.8x width)")
            .translation("config.toonflattening.width_scale")
            .defineInRange("widthScale", 1.8, 1.0, 6.0);

        reformationTicks = builder
            .comment("Animation duration for reformation in ticks (20 = 1 second)")
            .defineInRange("reformationTicks", 5, 1, 100);

        postRestorationImmunityTicks = builder
            .comment("Ticks of damage immunity after restoration completes (20 = 1 second)")
            .defineInRange("postRestorationImmunityTicks", 20, 0, 100);

        builder.pop();

        builder.comment("Collision-based flattening settings")
               .push("collision_flattening");

        floorVelocityThreshold = builder
            .comment("Minimum downward velocity to trigger floor flattening")
            .defineInRange("floorVelocityThreshold", 0.1, 0.1, 5.0);

        ceilingVelocityThreshold = builder
            .comment("Minimum upward velocity to trigger ceiling flattening")
            .defineInRange("ceilingVelocityThreshold", 0.1, 0.1, 5.0);

        wallVelocityThreshold = builder
            .comment("Minimum horizontal velocity to trigger wall flattening")
            .defineInRange("wallVelocityThreshold", 0.1, 0.1, 5.0);

        floorDamage = builder
            .comment("Damage dealt when flattened by floor collision")
            .defineInRange("floorDamage", 4.0, 0.0, 20.0);

        ceilingDamage = builder
            .comment("Damage dealt when flattened by ceiling collision")
            .defineInRange("ceilingDamage", 4.0, 0.0, 20.0);

        wallDamage = builder
            .comment("Damage dealt when flattened by wall collision")
            .defineInRange("wallDamage", 4.0, 0.0, 20.0);

        wallHitboxScale = builder
            .comment("Hitbox width/depth scale when wall-flattened")
            .defineInRange("wallHitboxScale", 0.2, 0.1, 1.0);

        enableFloorFlatten = builder
            .comment("Enable floor collision flattening")
            .define("enableFloorFlatten", true);

        enableCeilingFlatten = builder
            .comment("Enable ceiling collision flattening")
            .define("enableCeilingFlatten", true);

        enableWallFlatten = builder
            .comment("Enable wall collision flattening")
            .define("enableWallFlatten", true);

        builder.pop();
    }

    static {
        Pair<ToonFlatteningConfig, ModConfigSpec> pair =
            new ModConfigSpec.Builder().configure(ToonFlatteningConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
