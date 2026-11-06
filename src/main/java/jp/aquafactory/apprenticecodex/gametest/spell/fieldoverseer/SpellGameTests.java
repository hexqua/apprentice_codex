package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_isolated", timeoutTicks = 80)
    public static void fieldOverseerFallsWhenSupportRemoved(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_isolated")
    public static void fieldOverseerCastDataRoundTripsPlacementAndIdentity(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerCastDataRoundTripsPlacementAndIdentity(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_isolated")
    public static void fieldOverseerIgnoresOwnerDamage(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerIgnoresOwnerDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerUsesDurationBoundPersistencePolicy(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerUsesDurationBoundPersistencePolicy(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerRecastRemovesPlacedStaff(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerRecastRemovesPlacedStaff(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerTimeoutRemovesPlacedStaff(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerTimeoutRemovesPlacedStaff(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerCancelledWhileUnloadedDoesNotReturn(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerCancelledWhileUnloadedDoesNotReturn(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerDestructionEndsMatchingRecast(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerDestructionEndsMatchingRecast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_isolated", timeoutTicks = 100)
    public static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        FieldOverseerScenarios.fieldOverseerPrioritizesHealthAndTransfersMana(helper);
    }
}
