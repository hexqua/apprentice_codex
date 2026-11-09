package jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon.RightClickMagicWeaponGameTestScenarios.createIronAutoloaderCrossbowStack;
import static jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon.RightClickMagicWeaponGameTests.assertRightClickMagicWeaponPrioritizesOffhandUse;

final class RightClickMagicWeaponScenarios {
    private RightClickMagicWeaponScenarios() {}

    static void rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    new ItemStack(Items.SHIELD),
                    "right_click_magic_weapon_offhand_shield_test"
            );
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "right_click_magic_weapon_offhand_elemental_bow_test"
            );
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    createIronAutoloaderCrossbowStack(helper),
                    "right_click_magic_weapon_offhand_autoloader_crossbow_test"
            );
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "right_click_magic_weapon_offhand_spellgun_test"
            );
        });
    }
}
