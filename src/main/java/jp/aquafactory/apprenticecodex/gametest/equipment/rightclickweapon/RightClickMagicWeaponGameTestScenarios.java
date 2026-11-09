package jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

final class RightClickMagicWeaponGameTestScenarios {
    private RightClickMagicWeaponGameTestScenarios() {}

    static ItemStack createIronAutoloaderCrossbowStack(GameTestHelper helper) {
        var autoloaderCrossbow = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "autoloader_crossbow")
        );
        helper.assertTrue(autoloaderCrossbow != null,
                "Missing irons_spellbooks:autoloader_crossbow for offhand priority test");
        return new ItemStack(autoloaderCrossbow);
    }
}
