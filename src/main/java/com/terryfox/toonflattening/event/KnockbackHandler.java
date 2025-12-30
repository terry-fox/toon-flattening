package com.terryfox.toonflattening.event;

import com.terryfox.toonflattening.attachment.FlattenedStateAttachment;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

public class KnockbackHandler {
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        FlattenedStateAttachment.ifFlattened(event.getEntity(), () -> event.setCanceled(true));
    }
}
