package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondSpellHealOverflowIsReservedNearestFirst(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.spellHealOverflowIsReservedNearestFirst(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondSpellHealRequiresEquippedWearerAndMatchingOwner(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.spellHealRequiresEquippedWearerAndMatchingOwner(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondGreaterHealFullyRestoresEveryOwnedTarget(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.greaterHealFullyRestoresEveryOwnedTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondSpellLifestealDoesNotDependOnSchool(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.spellLifestealDoesNotDependOnSchool(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondCodexManagedEntitiesExposeHealingTarget(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.codexManagedEntitiesExposeMonarchBondHealingTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondAutoRestockRefillsEmptyOwnedTurretAtRangeBoundary(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.autoRestockRefillsEmptyOwnedTurretAtRangeBoundary(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondAutoRestockUsesNearestDeterministicPriority(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.autoRestockUsesNearestDeterministicPriority(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondAutoRestockSkipsIneligibleAndUnaffordableTurrets(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.autoRestockSkipsIneligibleAndUnaffordableTurrets(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondTransferParticleCountScalesAndCaps(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.transferParticleCountScalesAndCaps(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondFieldOverseerManaRangeExtendsWhenEquipped(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.fieldOverseerManaRangeExtendsWhenCharmIsEquipped(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void monarchBondFieldOverseerManaRangeRemainsEightBlocksWithoutCharm(GameTestHelper helper) {
        MonarchBondCharmGameTestScenarios.fieldOverseerManaRangeRemainsEightBlocksWithoutCharm(helper);
    }
}
