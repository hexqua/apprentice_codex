package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserGameTests {
    private SpellDispenserGameTests() {}

    public static final String TEMPLATE = "gametest/basic_floor";

    public static final String SERVER_DISABLED_CONFIG_BATCH = "spell_dispenser_server_disabled_config";

    public static final String SERVER_ALLOWLIST_CONFIG_BATCH = "spell_dispenser_server_allowlist_config";

    public static final String BLOCK_SERVER_DISABLED_CONFIG_BATCH = "spell_dispenser_block_server_disabled_config";

    public static final String BLOCK_COOLDOWN_MULTIPLIER_CONFIG_BATCH = "spell_dispenser_block_cooldown_multiplier_config";

    public static final String CREATE_COOLDOWN_MULTIPLIER_CONFIG_BATCH = "spell_dispenser_create_cooldown_multiplier_config";

    public static final String CREATIVE_CONFIG_BATCH = "creative_spell_dispenser_config";

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        CreateDispenserTestSupport.creativeSpellDispenserIsTaggedForContraptionControls(helper);
    }
}
