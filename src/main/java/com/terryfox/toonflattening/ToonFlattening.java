package com.terryfox.toonflattening;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ToonFlattening.MODID)
public class ToonFlattening {
    public static final String MODID = "toonflattening";
    private static final Logger LOGGER = LoggerFactory.getLogger(ToonFlattening.class);

    public ToonFlattening(IEventBus modEventBus) {
        LOGGER.info("Initializing Toon Flattening mod");
    }
}
