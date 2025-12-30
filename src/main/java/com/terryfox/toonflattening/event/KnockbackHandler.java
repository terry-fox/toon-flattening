package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

public class KnockbackHandler {
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        FlattenedStateAttachment state = player.getData(ToonFlattening.FLATTENED_STATE.get());

        if (state != null && state.isFlattened()) {
            event.setCanceled(true);
        }
    }
}
