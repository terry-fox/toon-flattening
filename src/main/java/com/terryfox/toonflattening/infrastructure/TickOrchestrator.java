package com.terryfox.toonflattening.infrastructure;

import com.terryfox.toonflattening.core.FlattenPhase;
import com.terryfox.toonflattening.core.FlattenState;
import com.terryfox.toonflattening.core.FlattenStateManager;
import com.terryfox.toonflattening.detection.AnvilContactDetector;
import com.terryfox.toonflattening.integration.ScalingProviderRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class TickOrchestrator {
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Orchestrate per-tick processing (runs after player tick work)
        AnvilContactDetector.getInstance().tick(serverPlayer);
        FlattenStateManager.getInstance().tick(serverPlayer);
        // ReformationHandler.tick(serverPlayer); // TODO: implement when reformation module ready

        // Apply scales via integration layer
        FlattenState state = FlattenStateManager.getInstance().getState(serverPlayer);
        if (state.phase() != FlattenPhase.NORMAL) {
            ScalingProviderRegistry.getProvider(serverPlayer)
                .setScales(serverPlayer, state.heightScale(), state.widthScale(), state.depthScale());
        } else {
            // Reset to normal scale when in NORMAL phase
            ScalingProviderRegistry.getProvider(serverPlayer)
                .setScales(serverPlayer, 1.0f, 1.0f, 1.0f);
        }
    }
}
