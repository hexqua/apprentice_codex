package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeCompat;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ManaStabilizerModuleGameTests {
    private ManaStabilizerModuleGameTests() {}
    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_stabilizer")
    public static void registrationAndRecoveryMath(GameTestHelper h) {
        h.assertTrue(ItemRegistry.MANA_STABILIZER_MODULE.get().getDefaultInstance().getMaxStackSize() == 1, "Module must be registered without optional mods");
        ManaStabilizerModuleGameTestScenariosCommon.math(h);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_stabilizer")
    public static void adsAndPriority(GameTestHelper h) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { h.succeed(); return; }
        ManaStabilizerModuleGameTestScenarios.adsAndPriority(h);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_stabilizer")
    public static void paymentAndFireOrdering(GameTestHelper h) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { h.succeed(); return; }
        ManaStabilizerModuleGameTestScenarios.paymentAndFireOrdering(h);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_stabilizer")
    public static void clockworkRecoveryAndTooltip(GameTestHelper h) throws Exception {
        if (!IronsArtificeCompat.isAvailable()) { h.succeed(); return; }
        ManaStabilizerModuleGameTestScenarios.clockworkRecoveryAndTooltip(h);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_stabilizer", timeoutTicks = 40)
    public static void lostInputExpires(GameTestHelper h) {
        if (!IronsArtificeCompat.isAvailable()) { h.succeed(); return; }
        ManaStabilizerModuleGameTestScenarios.lostInputExpires(h);
    }
}
