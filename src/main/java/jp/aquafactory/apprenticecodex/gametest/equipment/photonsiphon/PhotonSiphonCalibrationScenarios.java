package jp.aquafactory.apprenticecodex.gametest.equipment.photonsiphon;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.applyLegacyLockedReplacement;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.assertSpellData;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createInitializedPresetStack;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenuWithTarget;

final class PhotonSiphonCalibrationScenarios {
    private PhotonSiphonCalibrationScenarios() {}

    static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = ItemRegistry.PHOTON_SIPHON.get();
            var stack = createInitializedPresetStack(item);
            var spellContainer = ISpellContainer.get(stack);

            helper.assertFalse(item instanceof UniqueItem,
                    "Photon Siphon should not block external imbue as a UniqueItem");
            helper.assertTrue(spellContainer != null, "Photon Siphon default spell container is null");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon should still start with locked Mana Charge");
        });
    }

    static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "photon_siphon_calibration_repair_test");
            var item = (PhotonSiphon) ItemRegistry.PHOTON_SIPHON.get();
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            var legacyReplacementStack = createInitializedPresetStack(item);
            applyLegacyLockedReplacement(helper, legacyReplacementStack, replacementSpell, 1);
            createSpellCalibrationBenchMenuWithTarget(player, legacyReplacementStack);
            var repairedReplacementContainer = ISpellContainer.get(legacyReplacementStack);
            helper.assertTrue(repairedReplacementContainer != null,
                    "Photon Siphon repaired replacement spell container is null");
            assertSpellData(helper, repairedReplacementContainer, 0, replacementSpell, 1, false,
                    "Photon Siphon Calibration Bench repair should unlock legacy non-default replacement spells");

            var defaultStack = createInitializedPresetStack(item);
            createSpellCalibrationBenchMenuWithTarget(player, defaultStack);
            var defaultContainer = ISpellContainer.get(defaultStack);
            helper.assertTrue(defaultContainer != null, "Photon Siphon default spell container is null after Calibration Bench check");
            assertSpellData(helper, defaultContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon Calibration Bench repair should not unlock the default Mana Charge");
        });
    }
}
