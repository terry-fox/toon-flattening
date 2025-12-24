package com.terryfox.toonflattening.integration;

import net.minecraft.world.entity.player.Player;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;

public class PehkuiIntegration {
    private static void setScaleWithDelay(Player player, ScaleType type, float scale, int tickDelay) {
        var data = type.getScaleData(player);
        data.setScaleTickDelay(tickDelay);
        data.setTargetScale(scale);
    }

    public static void setPlayerScale(Player player, float heightScale, float widthScale) {
        ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(heightScale);
        ScaleTypes.WIDTH.getScaleData(player).setTargetScale(widthScale);
    }

    public static void resetPlayerScale(Player player) {
        setPlayerScale(player, 1.0f, 1.0f);
    }

    public static void setPlayerScaleWithDelay(Player player, float heightScale, float widthScale, int tickDelay) {
        setScaleWithDelay(player, ScaleTypes.HEIGHT, heightScale, tickDelay);
        setScaleWithDelay(player, ScaleTypes.WIDTH, widthScale, tickDelay);
    }

    public static void resetPlayerScaleWithDelay(Player player, int tickDelay) {
        setPlayerScaleWithDelay(player, 1.0f, 1.0f, tickDelay);
    }

    public static void setWallScale(Player player, float widthScale, int tickDelay) {
        setScaleWithDelay(player, ScaleTypes.HEIGHT, 1.0f, tickDelay);
        setScaleWithDelay(player, ScaleTypes.WIDTH, widthScale, tickDelay);
    }

    public static float getHeightScale(Player player) {
        return ScaleTypes.HEIGHT.getScaleData(player).getScale();
    }
}
