package jp.aquafactory.apprenticecodex.gametest.equipment.absorptionamulet;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipNecklaceCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.tickEquippedAbsorptionAmplifyAmulet;
import static jp.aquafactory.apprenticecodex.gametest.support.EquipmentStateTestSupport.getAbsorptionAmplifyAmuletState;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

final class AbsorptionAmuletScenarios {
    private AbsorptionAmuletScenarios() {}

    static void absorptionAmplifyAmuletZeroRecoveryDelayRepairsResumeToCurrentTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "absorption_zero_delay_repair_test");

            try (var ignored = ApprenticeCodexServerConfig.useAbsorptionAmplifyAmuletConfigOverrideForGameTest(8.0D, 0)) {
                equipNecklaceCurio(player, new ItemStack(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get()));
                player.setAbsorptionAmount(0.0F);

                Capabilities.withSpellData(player, data -> data.edit(
                        CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE,
                        state -> {
                            state.initialized = true;
                            state.recoveryResumeGameTime = level.getGameTime()
                                    + ApprenticeCodexServerConfig.savedAbsoluteTickClampMaxTicks()
                                    + 1200L;
                            state.nextRecoveryGameTime = level.getGameTime();
                            state.nextProcGameTime = level.getGameTime();
                            state.lastKnownAbsorption = 0.0F;
                        }
                ));

                tickEquippedAbsorptionAmplifyAmulet(player);

                var state = getAbsorptionAmplifyAmuletState(player);
                helper.assertTrue(state.recoveryResumeGameTime == level.getGameTime(),
                        "AbsorptionAmplifyAmulet zero recovery delay should repair resume time to the current tick");
                helper.assertTrue(player.getAbsorptionAmount() == 1.0F,
                        "AbsorptionAmplifyAmulet should recover immediately after zero-delay repair");
            }
        });
    }
}
