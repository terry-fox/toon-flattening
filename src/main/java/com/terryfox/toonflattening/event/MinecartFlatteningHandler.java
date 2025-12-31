package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.core.FlatteningStateController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class MinecartFlatteningHandler {
    /**
     * Attempt to flatten a player hit by a minecart.
     * Phase 1: No velocity/Y-level/immunity checks - just flatten.
     */
    public static boolean tryFlatten(AbstractMinecart cart, ServerPlayer victim) {
        FlatteningStateController.flattenWithMinecart(victim);
        return true;
    }
}
