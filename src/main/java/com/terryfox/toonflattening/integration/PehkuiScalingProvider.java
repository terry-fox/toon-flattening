package com.terryfox.toonflattening.integration;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * Scaling provider that uses Pehkui API to modify player hitbox and visual size.
 * Applies scales to HEIGHT, WIDTH, MODEL_HEIGHT, and MODEL_WIDTH ScaleTypes.
 */
public class PehkuiScalingProvider implements IScalingProvider {

    @Override
    public boolean canHandle(ServerPlayer player) {
        // Check if Pehkui is loaded and player has valid scale data
        return ModList.get().isLoaded("pehkui")
            && ScaleTypes.WIDTH.getScaleData(player) != null;
    }

    @Override
    public void setScales(ServerPlayer player, float height, float width, float depth) {
        // HEIGHT/WIDTH affect actual hitbox dimensions
        ScaleData heightData = ScaleTypes.HEIGHT.getScaleData(player);
        heightData.setScale(height);

        ScaleData widthData = ScaleTypes.WIDTH.getScaleData(player);
        widthData.setScale(width);

        // MODEL scales affect visual rendering (syncs with hitbox for consistency)
        ScaleTypes.MODEL_HEIGHT.getScaleData(player).setScale(height);
        ScaleTypes.MODEL_WIDTH.getScaleData(player).setScale(width);

        // Note: Pehkui handles persistence and client synchronization automatically
    }

    @Override
    public String getName() {
        return "Pehkui";
    }
}
