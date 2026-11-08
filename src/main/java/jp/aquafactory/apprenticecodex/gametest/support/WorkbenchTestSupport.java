package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WorkbenchTestSupport {
    private WorkbenchTestSupport() {}

    public static ItemStack createInitializedPresetStack(Item item) {
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }
        return stack;
    }

    public static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithSingleInput(Player player, ItemStack stack) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        return menu;
    }

    public static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithInputs(Player player, ItemStack first, ItemStack second) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        return menu;
    }

    public static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithInputs(Player player, ItemStack first, ItemStack second,
                                                                                     ItemStack third) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        menu.getSlot(2).set(third);
        return menu;
    }

    public static SpellCalibrationBenchMenu createSpellCalibrationBenchMenuWithTarget(Player player, ItemStack stack) {
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
        return menu;
    }

    public static SpellCalibrationBenchMenu createSpellCalibrationBenchMenu(GameTestHelper helper, Player player,
                                                                             BlockPos pos) {
        helper.setBlock(pos, BlockRegistry.SPELL_CALIBRATION_BENCH.get());
        return new SpellCalibrationBenchMenu(
                0,
                player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos))
        );
    }
}
