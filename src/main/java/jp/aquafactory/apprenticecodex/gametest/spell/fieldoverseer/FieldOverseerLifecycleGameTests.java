package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.FIELD_OVERSEER_LIFECYCLE_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FieldOverseerLifecycleGameTests {
    private FieldOverseerLifecycleGameTests() {}

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerUsesDurationBoundPersistencePolicy(GameTestHelper helper) {
        FieldOverseerLifecycleScenarios.fieldOverseerUsesDurationBoundPersistencePolicy(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerRecastRemovesPlacedStaff(GameTestHelper helper) {
        FieldOverseerLifecycleScenarios.fieldOverseerRecastRemovesPlacedStaff(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerTimeoutRemovesPlacedStaff(GameTestHelper helper) {
        FieldOverseerLifecycleScenarios.fieldOverseerTimeoutRemovesPlacedStaff(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerCancelledWhileUnloadedDoesNotReturn(GameTestHelper helper) {
        FieldOverseerLifecycleScenarios.fieldOverseerCancelledWhileUnloadedDoesNotReturn(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerDestructionEndsMatchingRecast(GameTestHelper helper) {
        FieldOverseerLifecycleScenarios.fieldOverseerDestructionEndsMatchingRecast(helper);
    }
}
