package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.config.ToonFlatteningConfig;

public record ScaleDimensions(float height, float width) {
    public static final ScaleDimensions NORMAL = new ScaleDimensions(1.0f, 1.0f);

    public static ScaleDimensions fromConfig(double accumulatedSpread) {
        float height = ToonFlatteningConfig.CONFIG.heightScale.get().floatValue();
        double maxSpreadWidth = ToonFlatteningConfig.CONFIG.maxSpreadWidth.get();

        double calculatedWidth = 1.0 + accumulatedSpread;
        float width = (float) Math.min(calculatedWidth, maxSpreadWidth);

        return new ScaleDimensions(height, width);
    }
}
