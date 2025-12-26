package com.terryfox.toonflattening.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum FlattenDirection implements StringRepresentable {
    DOWN("down"),
    UP("up"),
    NORTH("north"),
    SOUTH("south"),
    EAST("east"),
    WEST("west");

    public static final Codec<FlattenDirection> CODEC = StringRepresentable.fromEnum(FlattenDirection::values);

    private final String name;

    FlattenDirection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
