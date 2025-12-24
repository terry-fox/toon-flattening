package com.terryfox.toonflattening.util;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.entity.player.Player;

public final class FlattenedStateHelper {
    private FlattenedStateHelper() {}

    public static FlattenedStateAttachment getState(Player player) {
        return player.getData(ToonFlattening.FLATTENED_STATE.get());
    }

    public static boolean isFlattened(Player player) {
        FlattenedStateAttachment state = getState(player);
        return state != null && state.isFlattened();
    }

    public static void setState(Player player, FlattenedStateAttachment state) {
        player.setData(ToonFlattening.FLATTENED_STATE.get(), state);
    }
}
