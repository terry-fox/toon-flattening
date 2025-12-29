package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import com.terryfox.toonflattening.integration.PehkuiIntegration;
import com.terryfox.toonflattening.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;

public class FlattenStateManager {
    public static void resetPlayer(ServerPlayer player) {
        player.setData(
            ToonFlattening.FLATTENED_STATE.get(),
            FlattenedStateAttachment.DEFAULT
        );

        PehkuiIntegration.resetPlayerScale(player);

        NetworkHandler.syncFlattenState(player, false, 0L, null);
    }
}
