package com.terryfox.toonflattening.integration;

import com.terryfox.toonflattening.core.ScaleDimensions;
import net.minecraft.world.entity.player.Player;
import virtuoel.pehkui.api.ScaleTypes;

public class PehkuiIntegration {
    public static void setPlayerScale(Player player, ScaleDimensions scale) {
        ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(scale.height());
        ScaleTypes.WIDTH.getScaleData(player).setTargetScale(scale.width());
    }

    public static void resetPlayerScale(Player player) {
        setPlayerScale(player, ScaleDimensions.NORMAL);
    }

    public static void setPlayerScaleWithDelay(Player player, ScaleDimensions scale, int tickDelay) {
        ScaleTypes.HEIGHT.getScaleData(player).setScaleTickDelay(tickDelay);
        ScaleTypes.HEIGHT.getScaleData(player).setTargetScale(scale.height());

        ScaleTypes.WIDTH.getScaleData(player).setScaleTickDelay(tickDelay);
        ScaleTypes.WIDTH.getScaleData(player).setTargetScale(scale.width());
    }

    public static void resetPlayerScaleWithDelay(Player player, int tickDelay) {
        setPlayerScaleWithDelay(player, ScaleDimensions.NORMAL, tickDelay);
    }
}
