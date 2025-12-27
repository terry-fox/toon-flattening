package com.terryfox.toonflattening.gametest;

import com.terryfox.toonflattening.api.FlattenAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Integration tests for reformation mechanics
 */
@GameTestHolder("toonflattening")
@PrefixGameTestTemplate(false)
public class ReformationTests {

    @GameTest(template = "empty_5x5x5")
    public static void reform_succeeds_withClearance(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        // Flatten player
        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            // Attempt reform with sufficient clearance
            boolean reformed = FlattenAPI.reform(player);

            if (reformed) {
                helper.runAfterDelay(10, () -> {
                    // After reform animation completes
                    if (!FlattenAPI.isFlattened(player)) {
                        helper.succeed();
                    } else {
                        helper.fail("Player should be reformed after animation");
                    }
                });
            } else {
                helper.fail("Reform should succeed with clearance");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reform_blockedByAnvil(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        BlockPos playerPos = new BlockPos(2, 1, 2);
        BlockPos anvilPos = playerPos.above();

        // Flatten player
        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            // Place anvil above player
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());

            // Attempt reform
            boolean reformed = FlattenAPI.reform(player);

            if (!reformed && FlattenAPI.isReformBlocked(player)) {
                helper.succeed();
            } else {
                helper.fail("Reform should be blocked by anvil");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reform_fallbackTimer_bypassesAnvilBlock(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        BlockPos playerPos = new BlockPos(2, 1, 2);
        BlockPos anvilPos = playerPos.above();

        // Flatten player
        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            // Place anvil above player
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());

            // Wait for fallback timeout (default 300 seconds = 6000 ticks)
            // For testing, simulate timeout by waiting and checking
            helper.runAfterDelay(6100, () -> {
                boolean canReform = !FlattenAPI.isReformBlocked(player);

                if (canReform) {
                    helper.succeed();
                } else {
                    helper.fail("Fallback timer should bypass anvil blocking");
                }
            });
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reform_requiresSufficientClearance(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();
        BlockPos playerPos = new BlockPos(2, 1, 2);

        // Flatten player while crouching (1.5 blocks tall)
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            // Create low ceiling (75% of 1.5 = 1.125 blocks required)
            helper.setBlock(playerPos.above(), Blocks.STONE.defaultBlockState());

            boolean reformed = FlattenAPI.reform(player);

            if (!reformed) {
                helper.succeed();
            } else {
                helper.fail("Reform should fail with insufficient clearance");
            }
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reform_resetsSpread(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        // Flatten multiple times to increase spread
        FlattenAPI.flatten(player, 4.0f);
        FlattenAPI.flatten(player, 4.0f);

        float spreadBeforeReform = FlattenAPI.getSpreadMultiplier(player);

        helper.runAfterDelay(5, () -> {
            FlattenAPI.reform(player);

            helper.runAfterDelay(10, () -> {
                float spreadAfterReform = FlattenAPI.getSpreadMultiplier(player);

                // After reform, spread should reset to 0
                if (spreadAfterReform == 0f && spreadBeforeReform > 0f) {
                    helper.succeed();
                } else {
                    helper.fail("Spread should reset after reformation");
                }
            });
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reform_cannotReformWhenNotFlattened(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        // Attempt reform without being flattened
        boolean reformed = FlattenAPI.reform(player);

        if (!reformed) {
            helper.succeed();
        } else {
            helper.fail("Cannot reform when not flattened");
        }
    }

    @GameTest(template = "empty_5x5x5")
    public static void reform_animationDuration(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            FlattenAPI.reform(player);

            // Default reformation ticks = 5
            helper.runAfterDelay(4, () -> {
                // Should still be reforming
                if (FlattenAPI.isFlattened(player)) {
                    helper.runAfterDelay(2, () -> {
                        // After animation completes
                        if (!FlattenAPI.isFlattened(player)) {
                            helper.succeed();
                        } else {
                            helper.fail("Player should be fully reformed");
                        }
                    });
                } else {
                    helper.fail("Player should still be reforming at tick 4");
                }
            });
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reflatten_duringReform_allowedInFirstThird(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            FlattenAPI.reform(player);

            // Immediately re-flatten during first tick (within first third of 5 ticks)
            helper.runAfterDelay(1, () -> {
                boolean reflattened = FlattenAPI.flatten(player, 4.0f);

                if (reflattened) {
                    helper.succeed();
                } else {
                    helper.fail("Re-flatten should be allowed in first third of reform");
                }
            });
        });
    }

    @GameTest(template = "empty_5x5x5")
    public static void reflatten_duringReform_blockedAfterFirstThird(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockPlayer();

        FlattenAPI.flatten(player, 4.0f);

        helper.runAfterDelay(5, () -> {
            FlattenAPI.reform(player);

            // Attempt re-flatten after first third (after tick 1 of 5)
            helper.runAfterDelay(3, () -> {
                boolean reflattened = FlattenAPI.flatten(player, 4.0f);

                if (!reflattened) {
                    helper.succeed();
                } else {
                    helper.fail("Re-flatten should be blocked after first third of reform");
                }
            });
        });
    }
}
