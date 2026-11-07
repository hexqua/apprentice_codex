package jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.OffhandUsePriorityHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTranslatableKey;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RightClickMagicWeaponGameTests {
    private RightClickMagicWeaponGameTests() {}
    @GameTest(template = "gametest/basic_floor")
    public static void representativeWeaponTooltipsKeepSharedAndUniqueHints(GameTestHelper helper) {
        // 共通実装の全武器走査は避け、基底実装と固有追記の経路を一つずつ確認する。
        for (var item : List.of(ItemRegistry.SMASHCAST_SCEPTER.get(), ItemRegistry.CRYSTAL_BLADED_STAFF.get())) {
            var stack = new ItemStack(item);
            var snapshot = stack.copy();
            var lines = new ArrayList<Component>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), lines, TooltipFlag.Default.NORMAL);
            helper.assertTrue(lines.size() >= 2, "Representative weapon must expose shared use hints");
            assertTranslatableKey(helper, lines.getFirst(), "item.apprenticecodex.right_click_magic_weapon.desc",
                    "Weapon must explain offhand priority");
            assertTranslatableKey(helper, lines.get(1), "item.apprenticecodex.right_click_magic_weapon.item_type",
                    "Weapon must expose its interaction type");
            if (item == ItemRegistry.CRYSTAL_BLADED_STAFF.get()) {
                helper.assertTrue(lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                                && contents.getKey().equals("item.apprenticecodex.crystal_bladed_staff.desc")),
                        "Crystal Bladed Staff must retain its unique mana orb hint");
            }
            helper.assertTrue(ItemStack.isSameItemSameComponents(stack, snapshot), "Reading hints must not mutate the weapon");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
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

        var mainBefore = mainhandStack.copy();
        var offBefore = player.getOffhandItem().copy();
        var result = mainhandStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == InteractionResult.PASS,
                "Right click magic weapon should pass to supported offhand use item " + offhandStack
                        + " but got " + result.getResult());
        helper.assertTrue(magicData.getMana() == 100.0F, "Passing to offhand must not consume mana");
        helper.assertTrue(ItemStack.isSameItemSameComponents(mainBefore, player.getMainHandItem())
                        && ItemStack.isSameItemSameComponents(offBefore, player.getOffhandItem()),
                "Passing to offhand must preserve both items");
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
