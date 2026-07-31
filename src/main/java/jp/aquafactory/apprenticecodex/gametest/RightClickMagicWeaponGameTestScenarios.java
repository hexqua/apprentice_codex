package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.OffhandUsePriorityHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;

final class RightClickMagicWeaponGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private RightClickMagicWeaponGameTestScenarios() {
    }

    static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Right Click Magic Weapon",
                item -> item instanceof AbstractRightClickMagicWeaponItem,
                stack -> expectedRightClickMagicWeaponEnchantments(helper.getLevel().registryAccess(), stack)
        ));
    }

    static void rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var rightClickMagicWeapons = BuiltInRegistries.ITEM.stream()
                    .filter(item -> item instanceof AbstractRightClickMagicWeaponItem)
                    .toList();
            helper.assertTrue(!rightClickMagicWeapons.isEmpty(),
                    "Right Click Magic Weapon tooltip test found no target items");

            for (var item : rightClickMagicWeapons) {
                var stack = new ItemStack(item);
                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(!tooltipLines.isEmpty(),
                        item + " should expose right click magic weapon tooltip");
                helper.assertTrue(tooltipLines.size() > 1,
                        item + " should expose right click magic weapon item type tooltip");
                // Iron Swingcast Staffは専用能力ヒントを最上段に置き、その直後から共通ヒントを維持する。
                var offhandTooltipStart = item == ItemRegistry.IRON_SWINGCAST_STAFF.get() ? 1 : 0;
                assertTranslatableKey(
                        helper,
                        tooltipLines.get(offhandTooltipStart),
                        "item.apprenticecodex.right_click_magic_weapon.desc",
                        item + " should show offhand priority tooltip at its expected start"
                );
                assertTranslatableKey(
                        helper,
                        tooltipLines.get(offhandTooltipStart + 1),
                        "item.apprenticecodex.right_click_magic_weapon.item_type",
                        item + " should show offhand priority item type tooltip next"
                );
            }

            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    2,
                    "item.apprenticecodex.crystal_bladed_staff.swing_miss.desc",
                    "Crystal Bladed Staff should show miss-swing tooltip after offhand priority tooltips"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    3,
                    "item.apprenticecodex.crystal_bladed_staff.desc",
                    "Crystal Bladed Staff should keep its mana orb tooltip after swingcast tooltip"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    2,
                    "item.apprenticecodex.swingcast.common.desc",
                    "Swingcast Staff should show swingcast tooltip after offhand priority tooltips"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get()),
                    2,
                    "item.apprenticecodex.swingcast.common.desc",
                    "Revolvercast Staff should show swingcast tooltip after offhand priority tooltips"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Spell Gun shift hint should stand out"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Swingcast Staff shift hint should stand out"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Reflectcast Shield shift hint should stand out"
            );
        });
    }

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

    private static void assertRightClickMagicWeaponPrioritizesOffhandUse(
            GameTestHelper helper,
            ItemStack offhandStack,
            String profileName
    ) {
        helper.assertTrue(OffhandUsePriorityHelper.isPriorityOffhandUseItem(offhandStack),
                "Expected a supported priority offhand use item but got " + offhandStack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        var mainhandStack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, mainhandStack);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack.copy());
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Right click magic weapon offhand priority test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = mainhandStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Right click magic weapon should pass to supported offhand use item " + offhandStack
                        + " but got " + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Right click magic weapon should not cast before supported offhand use item " + offhandStack);
    }

    private static ItemStack createIronAutoloaderCrossbowStack(GameTestHelper helper) {
        var autoloaderCrossbow = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "autoloader_crossbow")
        );
        helper.assertTrue(autoloaderCrossbow != Items.AIR,
                "Missing irons_spellbooks:autoloader_crossbow for offhand priority test");
        return new ItemStack(autoloaderCrossbow);
    }
}
