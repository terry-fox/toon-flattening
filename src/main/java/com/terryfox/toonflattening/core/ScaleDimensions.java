package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.config.ToonFlatteningConfig;

public record ScaleDimensions(float height, float width) {
    public static final ScaleDimensions NORMAL = new ScaleDimensions(1.0f, 1.0f);

    public static ScaleDimensions fromConfig() {
        return new ScaleDimensions(
            ToonFlatteningConfig.CONFIG.heightScale.get().floatValue(),
            ToonFlatteningConfig.CONFIG.widthScale.get().floatValue()
        );
    }
}
