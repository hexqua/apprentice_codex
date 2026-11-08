package jp.aquafactory.apprenticecodex.gametest.runtime;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    public static final String TEMPLATE = "gametest/basic_floor";

    public static final String PROCESSING_RECIPE_DENYLIST_CONFIG_BATCH = "processing_recipe_denylist_config";

    @GameTest(template = TEMPLATE)
    public static void requiredOptionalModsAreLoaded(GameTestHelper helper) {
        RegistrationScenarios.requiredOptionalModsAreLoaded(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        RegistrationScenarios.registriesAndDynamicContentAreRegistered(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        RegistrationScenarios.serverBlocksAndEntitiesCanBeInstantiated(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
        CreativeTabScenarios.creativeTabSpellsStayGroupedBySchool(helper);
    }
}
