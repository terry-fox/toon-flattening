package com.terryfox.toonflattening.integration;

import net.minecraft.world.entity.player.Player;
import virtuoel.pehkui.api.ScaleTypes;

public class PehkuiIntegration {
    public static void setPlayerScale(Player player, float heightScale, float widthScale) {
        ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(heightScale);
        ScaleTypes.WIDTH.getScaleData(player).setTargetScale(widthScale);
    }

    public static void resetPlayerScale(Player player) {
        setPlayerScale(player, 1.0f, 1.0f);
    }

    public static void setPlayerScaleWithDelay(Player player, float heightScale, float widthScale, int tickDelay) {
        ScaleTypes.HEIGHT.getScaleData(player).setScaleTickDelay(tickDelay);
        ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(heightScale);

        ScaleTypes.WIDTH.getScaleData(player).setScaleTickDelay(tickDelay);
        ScaleTypes.WIDTH.getScaleData(player).setTargetScale(widthScale);
    }

    public static void resetPlayerScaleWithDelay(Player player, int tickDelay) {
        setPlayerScaleWithDelay(player, 1.0f, 1.0f, tickDelay);
    }

    public static void setWallScale(Player player, float widthScale, int tickDelay) {
        ScaleTypes.HEIGHT.getScaleData(player).setScaleTickDelay(tickDelay);
        ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(1.0f);

        ScaleTypes.WIDTH.getScaleData(player).setScaleTickDelay(tickDelay);
        ScaleTypes.WIDTH.getScaleData(player).setTargetScale(widthScale);
    }
}
