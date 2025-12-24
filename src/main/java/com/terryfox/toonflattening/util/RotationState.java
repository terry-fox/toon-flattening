package com.terryfox.toonflattening.util;

import net.minecraft.world.entity.player.Player;

public record RotationState(float yRot, float yRotO, float xRot, float xRotO, float yHeadRot, float yHeadRotO, float yBodyRot, float yBodyRotO) {
    public static RotationState capture(Player player) {
        return new RotationState(
            player.getYRot(), player.yRotO,
            player.getXRot(), player.xRotO,
            player.yHeadRot, player.yHeadRotO,
            player.yBodyRot, player.yBodyRotO
        );
    }

    public void restore(Player player) {
        player.setYRot(yRot);
        player.yRotO = yRotO;
        player.setXRot(xRot);
        player.xRotO = xRotO;
        player.yHeadRot = yHeadRot;
        player.yHeadRotO = yHeadRotO;
        player.yBodyRot = yBodyRot;
        player.yBodyRotO = yBodyRotO;
    }

    public static void freeze(Player player) {
        player.setYRot(player.yRotO);
        player.setXRot(player.xRotO);
        player.yHeadRot = player.yHeadRotO;
        player.yBodyRot = player.yBodyRotO;
    }
}
