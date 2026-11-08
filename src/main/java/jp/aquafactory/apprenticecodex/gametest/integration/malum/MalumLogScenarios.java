package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackBlockClassifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.RegistryTestSupport.requireForgeBlock;

final class MalumLogScenarios {
    private MalumLogScenarios() {}

    static void tinyLumberjackRecognizesMalumRunewoodAndSoulwoodLogs(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            assertTinyLumberjackLog(helper, "runewood_log");
            assertTinyLumberjackLog(helper, "runewood");
            assertTinyLumberjackLog(helper, "exposed_runewood_log");
            assertTinyLumberjackLog(helper, "soulwood_log");
            assertTinyLumberjackLog(helper, "soulwood");
            assertTinyLumberjackLog(helper, "exposed_soulwood_log");
            assertTinyLumberjackNonLog(helper, "runewood_planks");
        });
    }

    static void assertTinyLumberjackLog(GameTestHelper helper, String malumBlockName) {
        var id = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, malumBlockName);
        var state = requireForgeBlock(helper, id).defaultBlockState();
        helper.assertTrue(TinyLumberjackBlockClassifier.isLog(state),
                "TinyLumberjack should recognize " + id + " as a log");
    }

    static void assertTinyLumberjackNonLog(GameTestHelper helper, String malumBlockName) {
        var id = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, malumBlockName);
        var state = requireForgeBlock(helper, id).defaultBlockState();
        helper.assertFalse(TinyLumberjackBlockClassifier.isLog(state),
                "TinyLumberjack should not recognize " + id + " as a log");
    }
}
