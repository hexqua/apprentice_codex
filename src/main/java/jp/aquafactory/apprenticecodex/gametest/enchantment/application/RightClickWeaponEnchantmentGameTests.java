package jp.aquafactory.apprenticecodex.gametest.enchantment.application;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertCategoryEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.expectedRightClickMagicWeaponEnchantments;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RightClickWeaponEnchantmentGameTests {
    private RightClickWeaponEnchantmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Right Click Magic Weapon",
                item -> item instanceof AbstractRightClickMagicWeaponItem,
                stack -> expectedRightClickMagicWeaponEnchantments(helper.getLevel().registryAccess(), stack)
        ));
    }
}
