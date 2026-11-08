package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.assertSpellData;

final class SpellContainerScenarios {
    private SpellContainerScenarios() {}

    static void initialSpellContainerHelperSkipsUnavailablePresetSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.GRIMOIRE_MANIFEST.get());
            InitialSpellContainerHelper.setInitialContainer(
                    stack,
                    1,
                    true,
                    false,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.none(),
                    1
            );
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Initial helper should still create an empty spell container");
            helper.assertTrue(spellContainer.getActiveSpellCount() == 0,
                    "Initial helper should skip unavailable preset spells");

            var enabledStack = new ItemStack(ItemRegistry.GRIMOIRE_MANIFEST.get());
            var spell = SpellRegistry.MANIFESTATION_GRIMOIRE.get();
            InitialSpellContainerHelper.setInitialContainer(enabledStack, 1, true, false, spell, 1);
            var enabledContainer = ISpellContainer.get(enabledStack);
            helper.assertTrue(enabledContainer != null, "Initial helper should create enabled preset spell container");
            assertSpellData(helper, enabledContainer, 0, spell, 1, true,
                    "Initial helper should keep enabled preset spells locked");
        });
    }
}
