package jp.aquafactory.apprenticecodex.gametest.equipment.flask;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.gametest.crafting.support.WorkbenchMenuScope;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellcasterWorkbenchMenuWithSingleInput;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FlaskWorkbenchGameTests {
    private FlaskWorkbenchGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskWorkbenchResultTogglesEffectParticles(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "flask_workbench");
                var spellcastersFlaskMenu = menus.track(createSpellcasterWorkbenchMenuWithSingleInput(
                        player,
                        new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get())
                ), player);
                var spellcastersFlaskResult = spellcastersFlaskMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
                helper.assertTrue(spellcastersFlaskResult.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                        "Spellcaster's Flask should keep the Workbench particle toggle result");
                helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(spellcastersFlaskResult),
                        "Spellcaster's Flask Workbench result should toggle particles off from the default state");
            }
        });
    }
}
