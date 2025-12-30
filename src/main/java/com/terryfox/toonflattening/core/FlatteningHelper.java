package com.terryfox.toonflattening.core;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.entity.player.Player;

public class FlatteningHelper {
    public static boolean isFlattened(Player player) {
        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());
        return state != null && state.isFlattened();
    }
}
