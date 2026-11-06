package jp.aquafactory.apprenticecodex.gametest.equipment.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void nonDamageableAnvilMergeRejectsStoredRightContents(GameTestHelper helper) {
        AnvilStorageScenarios.nonDamageableAnvilMergeRejectsStoredRightContents(helper);
    }
}
