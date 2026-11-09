package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer.FieldOverseerTestSupport.createDurationBoundFieldOverseer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondLegacyScenarios {
    private MonarchBondLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void monarchBondSpellLifestealFollowsModifiedDamage(GameTestHelper helper) {
        MonarchBondTestSupport.spellLifestealFollowsModifiedDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondSpellLifestealSkipsCanceledDamage(GameTestHelper helper) {
        MonarchBondTestSupport.spellLifestealSkipsCanceledDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondSpellLifestealPreservesHealingWithoutCharm(GameTestHelper helper) {
        MonarchBondTestSupport.spellLifestealPreservesHealingWithoutCharm(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondCodexManagedEntitiesExposeHealingTarget(GameTestHelper helper) {
        MonarchBondTestSupport.codexManagedEntitiesExposeMonarchBondHealingTarget(helper);
    }



    @GameTest(template = TEMPLATE)
    public static void monarchBondFieldOverseerManaRangeExtendsWhenEquipped(GameTestHelper helper) {
        MonarchBondTestSupport.fieldOverseerManaRangeExtendsWhenCharmIsEquipped(helper);
    }

    public static FieldOverseerStaffEntity createFieldOverseerTestEntity(
            GameTestHelper helper, FakePlayer owner, BlockPos anchorPos, float maxMana, int attackManaCost) {
        return createDurationBoundFieldOverseer(
                helper, owner, 1, anchorPos, maxMana, attackManaCost,
                ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
    }
}
