package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.validation;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserValidationGameTests {
    private SpellDispenserValidationGameTests() {}

    @GameTest(template = TEMPLATE, batch = SERVER_DISABLED_CONFIG_BATCH)
    public static void spellDispenserValidatorRejectsWhenServerDisabled(GameTestHelper helper) {
        SpellDispenserValidationScenarios.spellDispenserValidatorRejectsWhenServerDisabled(helper);
    }

    @GameTest(template = TEMPLATE, batch = SERVER_ALLOWLIST_CONFIG_BATCH)
    public static void spellDispenserValidatorRequiresServerAllowlist(GameTestHelper helper) {
        SpellDispenserValidationScenarios.spellDispenserValidatorRequiresServerAllowlist(helper);
    }

    public static ItemStack createInitializedPresetStack(Item item) {
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }
        return stack;
    }
}
