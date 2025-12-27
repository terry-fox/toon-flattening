package com.terryfox.toonflattening.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("toonflattening")
@PrefixGameTestTemplate(false)
public class ToonFlattenGameTests {

    @GameTest(template = "empty_3x3x3")
    public static void exampleTest(GameTestHelper helper) {
        helper.succeed();
    }
}
