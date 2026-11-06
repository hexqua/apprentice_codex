package jp.aquafactory.apprenticecodex.gametest.equipment.attributes;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertBaseAttackModifier;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertModifierAmount;
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

    static void rightClickMagicWeaponsExposeBaseAttributesAsComponents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertModifierAmount(
                    helper,
                    new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get())
                            .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY),
                    Attributes.ATTACK_SPEED.value(),
                    EquipmentSlotGroup.MAINHAND,
                    -2.4D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Illuminate Stellar Staff attack speed component regression"
            );
            assertModifierAmount(
                    helper,
                    new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get())
                            .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY),
                    Attributes.ATTACK_DAMAGE.value(),
                    EquipmentSlotGroup.MAINHAND,
                    5.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Illuminate Stellar Staff attack damage component regression"
            );
            var crystalBladedStaff = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            var crystalModifiers = crystalBladedStaff.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            assertModifierAmount(
                    helper,
                    crystalModifiers,
                    Attributes.ATTACK_DAMAGE.value(),
                    EquipmentSlotGroup.MAINHAND,
                    4.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Crystal Bladed Staff attack damage component regression"
            );
            assertModifierAmount(
                    helper,
                    crystalModifiers,
                    Attributes.ATTACK_SPEED.value(),
                    EquipmentSlotGroup.MAINHAND,
                    -2.4D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Crystal Bladed Staff attack speed component regression"
            );
            assertModifierAmount(
                    helper,
                    crystalModifiers,
                    Attributes.ENTITY_INTERACTION_RANGE.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.5D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Crystal Bladed Staff entity reach component regression"
            );
            assertModifierAmount(
                    helper,
                    crystalModifiers,
                    AttributeRegistry.SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND,
                    0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Crystal Bladed Staff spell power component regression"
            );
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
