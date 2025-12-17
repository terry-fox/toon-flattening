package com.terryfox.toonflattening.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquashAnimationRenderer {

    public static void playSquashEffect(int playerId) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null) {
            return;
        }

        Entity entity = level.getEntity(playerId);
        if (entity == null) {
            return;
        }

        double x = entity.getX();
        double y = entity.getY() + 1.0;
        double z = entity.getZ();

        // Spawn 25 POOF particles in circular burst pattern
        for (int i = 0; i < 25; i++) {
            double angle = (2 * Math.PI * i) / 25;
            double vx = Math.cos(angle) * 0.3;
            double vy = 0.1;
            double vz = Math.sin(angle) * 0.3;

            level.addParticle(ParticleTypes.POOF, x, y, z, vx, vy, vz);
        }
    }
}
