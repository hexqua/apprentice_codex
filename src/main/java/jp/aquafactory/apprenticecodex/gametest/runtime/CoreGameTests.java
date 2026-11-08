package jp.aquafactory.apprenticecodex.gametest.runtime;

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
    public static void requiredOptionalModsAreLoaded(GameTestHelper helper) {
        RegistrationScenarios.requiredOptionalModsAreLoaded(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        RegistrationScenarios.registriesAndDynamicContentAreRegistered(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        RegistrationScenarios.serverBlocksAndEntitiesCanBeInstantiated(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
        CreativeTabScenarios.creativeTabSpellsStayGroupedBySchool(helper);
    }
}
