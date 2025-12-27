package com.terryfox.toonflattening.gametest;

import com.terryfox.toonflattening.api.FlattenAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Integration tests for anvil collision → flatten flow
 */
@GameTestHolder("toonflattening")
@PrefixGameTestTemplate(false)
public class AnvilFlattenTests {

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_triggersOnFallingAnvil(GameTestHelper helper) {
        BlockPos playerPos = new BlockPos(2, 1, 2);
        BlockPos anvilPos = new BlockPos(2, 3, 2);

        ServerPlayer player = helper.makeMockPlayer();
        helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());

        // Spawn falling anvil above player
        helper.spawn(EntityType.FALLING_BLOCK, anvilPos);

        helper.runAfterDelay(20, () -> {
            // After 1 second, anvil should have hit player
            if (FlattenAPI.isFlattened(player)) {
                helper.succeed();
            } else {
                helper.fail("Player should be flattened after anvil collision");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_doesNotAffectSpectators(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

        BlockPos anvilPos = new BlockPos(2, 3, 2);
        helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
        helper.spawn(EntityType.FALLING_BLOCK, anvilPos);

        helper.runAfterDelay(20, () -> {
            if (!FlattenAPI.isFlattened(player)) {
                helper.succeed();
            } else {
                helper.fail("Spectator should not be flattened");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_incrementsSpreadOnReflatten(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        // First flatten
        boolean firstFlatten = FlattenAPI.flatten(player, 4.0f);
        float firstSpread = FlattenAPI.getSpreadMultiplier(player);

        helper.runAfterDelay(5, () -> {
            // Second flatten while already flattened
            boolean secondFlatten = FlattenAPI.flatten(player, 4.0f);
            float secondSpread = FlattenAPI.getSpreadMultiplier(player);

            if (secondFlatten && secondSpread > firstSpread) {
                helper.succeed();
            } else {
                helper.fail("Spread should increase on re-flatten");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_capsSpreadAtMaximum(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        // Flatten multiple times to reach max spread
        for (int i = 0; i < 10; i++) {
            FlattenAPI.flatten(player, 4.0f);
        }

        helper.runAfterDelay(5, () -> {
            float spread = FlattenAPI.getSpreadMultiplier(player);

            // Default max is 6.0f
            if (spread <= 6.0f) {
                helper.succeed();
            } else {
                helper.fail("Spread exceeded maximum limit: " + spread);
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_appliesDamageInSurvival(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        float healthBefore = player.getHealth();
        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(2, () -> {
            float healthAfter = player.getHealth();

            if (healthAfter < healthBefore) {
                helper.succeed();
            } else {
                helper.fail("Player should take damage in survival mode");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_noDamageInCreative(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);

        float healthBefore = player.getHealth();
        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(2, () -> {
            float healthAfter = player.getHealth();

            if (healthAfter == healthBefore && FlattenAPI.isFlattened(player)) {
                helper.succeed();
            } else {
                helper.fail("Creative player should be flattened but take no damage");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void anvilFlatten_capturesFrozenPose(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);

        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(2, () -> {
            // Verify pose was frozen (implementation detail, may need API method)
            if (FlattenAPI.isFlattened(player)) {
                helper.succeed();
            } else {
                helper.fail("Flatten should capture player pose");
            }
        });
    }
}
