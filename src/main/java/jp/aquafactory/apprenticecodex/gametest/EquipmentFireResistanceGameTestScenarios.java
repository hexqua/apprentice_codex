package jp.aquafactory.apprenticecodex.gametest;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class EquipmentFireResistanceGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final TagKey<Item> IRONS_STAFF = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "staff")
    );
    private static final TagKey<Item> CURIOS_SPELLBOOK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", "spellbook")
    );

    private EquipmentFireResistanceGameTestScenarios() {
    }

    static void fireResistantEquipmentContractsStayInSync(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertRegisteredItemsMatchingAreFireResistant(
                    helper,
                    stack -> stack.is(IRONS_STAFF),
                    "irons_spellbooks:staff tagged item"
            );
            assertRegisteredItemsMatchingAreFireResistant(
                    helper,
                    stack -> stack.is(CURIOS_SPELLBOOK),
                    "curios:spellbook tagged item"
            );
            assertRegisteredItemsMatchingAreFireResistant(
                    helper,
                    stack -> stack.getItem() instanceof AbstractSwingcastStaffItem,
                    "AbstractSwingcastStaffItem"
            );
            assertRegisteredItemsMatchingAreFireResistant(
                    helper,
                    EquipmentFireResistanceGameTestScenarios::isNonStackRegisteredStaffPathItem,
                    "non-stack registered item with staff in path"
            );

            for (var itemEntry : explicitFireResistantItems()) {
                assertFireResistant(helper, itemEntry.get(), "explicit fire-resistant equipment");
            }

            assertNotFireResistant(
                    helper,
                    ItemRegistry.SCARLET_THIRST.get(),
                    "Scarlet Thirst should stay excluded as a normal Mithril Curio"
            );
        });
    }

    private static List<Supplier<? extends Item>> explicitFireResistantItems() {
        return List.of(
                ItemRegistry.SMASHCAST_SCEPTER,
                ItemRegistry.SCROLLCASTER_GAUNTLET,
                ItemRegistry.MANA_FORCE_BLADE,
                ItemRegistry.ELEMENTAL_BOW,
                ItemRegistry.DIAMOND_SPELLCASTER_GUN,
                ItemRegistry.NETHERITE_SPELL_AMPLIFIER,
                ItemRegistry.PHOTON_SIPHON,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS
        );
    }

    private static void assertRegisteredItemsMatchingAreFireResistant(
            GameTestHelper helper,
            Predicate<ItemStack> predicate,
            String categoryName
    ) {
        var stacks = ItemRegistry.ITEMS.getEntries().stream()
                .map(Supplier::get)
                .map(ItemStack::new)
                .filter(predicate)
                .toList();

        helper.assertFalse(stacks.isEmpty(), "No items matched fire resistance category: " + categoryName);

        for (var stack : stacks) {
            assertFireResistant(helper, stack.getItem(), categoryName);
        }
    }

    private static boolean isNonStackRegisteredStaffPathItem(ItemStack stack) {
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null
                && ApprenticeCodex.MODID.equals(itemId.getNamespace())
                && itemId.getPath().contains("staff")
                && stack.getMaxStackSize() == 1;
    }

    private static void assertFireResistant(GameTestHelper helper, Item item, String categoryName) {
        helper.assertTrue(
                new ItemStack(item).has(DataComponents.FIRE_RESISTANT),
                categoryName + " should be fire resistant: " + BuiltInRegistries.ITEM.getKey(item)
        );
    }

    private static void assertNotFireResistant(GameTestHelper helper, Item item, String message) {
        helper.assertFalse(
                new ItemStack(item).has(DataComponents.FIRE_RESISTANT),
                message + ": " + BuiltInRegistries.ITEM.getKey(item)
        );
    }
}
