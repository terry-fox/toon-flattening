package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.item.HammerItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

public class HammerAnvilHandler {
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getLeft().getItem() instanceof HammerItem) {
            event.setCanceled(true);
        }
    }
}
