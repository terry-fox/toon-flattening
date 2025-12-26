package com.terryfox.toonflattening.trigger;

import com.terryfox.toonflattening.ToonFlattening;
import com.terryfox.toonflattening.api.FlattenContext;
import com.terryfox.toonflattening.api.FlattenDirection;
import com.terryfox.toonflattening.event.FlattenCause;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Anvil flatten trigger - migrated from FlatteningHandler.
 */
public class AnvilFlattenTrigger implements FlattenTrigger {

    private static final ResourceLocation TRIGGER_ID = ResourceLocation.fromNamespaceAndPath(ToonFlattening.MODID, "anvil");

    @Override
    public FlattenCause getCause() {
        return FlattenCause.ANVIL;
    }

    @Override
    public ResourceLocation getTriggerId() {
        return TRIGGER_ID;
    }

    @Override
    public FlattenContext shouldTrigger(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return null;
        }

        var directEntity = event.getSource().getDirectEntity();
        if (!(directEntity instanceof FallingBlockEntity fallingBlock)) {
            return null;
        }

        var blockState = fallingBlock.getBlockState();
        if (!blockState.is(BlockTags.ANVIL)) {
            return null;
        }

        double velocity = Math.abs(fallingBlock.getDeltaMovement().y);
        return new FlattenContext(TRIGGER_ID, velocity, FlattenDirection.DOWN, fallingBlock);
    }
}
