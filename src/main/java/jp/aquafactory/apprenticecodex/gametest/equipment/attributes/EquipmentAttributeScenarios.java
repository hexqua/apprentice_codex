package jp.aquafactory.apprenticecodex.gametest.equipment.attributes;

import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertBaseAttackModifier;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.assertMainhandUpgradeBridge;

final class EquipmentAttributeScenarios {
    private EquipmentAttributeScenarios() {}

    static void swingMagicWeaponsUseBaseAttackModifierIds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var testedItems = 0;
            for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
                var item = itemEntry.get();
                if (!(item instanceof AbstractSwingMagicItem)) {
                    continue;
                }

                testedItems++;
                assertBaseAttackModifier(helper, itemEntry.getId(), item, Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID);
                assertBaseAttackModifier(helper, itemEntry.getId(), item, Attributes.ATTACK_SPEED, Item.BASE_ATTACK_SPEED_ID);
            }

            helper.assertTrue(testedItems > 0, "No AbstractSwingMagicItem entries were registered");
        });
    }

    static void mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertMainhandUpgradeBridge(
                    helper,
                    new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    "Spell gun upgrade bridge regression"
            );
            assertMainhandUpgradeBridge(
                    helper,
                    new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get()),
                    "Right-click weapon upgrade bridge regression"
            );
        });
    }
}
