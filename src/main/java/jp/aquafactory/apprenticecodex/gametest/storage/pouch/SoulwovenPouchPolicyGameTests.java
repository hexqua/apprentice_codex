package jp.aquafactory.apprenticecodex.gametest.storage.pouch;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.LinkedHashSet;
import java.util.List;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SOULWOVEN_POUCH_EFFICIENT;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SoulwovenPouchPolicyGameTests {
    private SoulwovenPouchPolicyGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void soulwovenPouchEfficientItemsFollowMalumStoragePolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var expectedItems = new LinkedHashSet<Item>();
            for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
                var item = itemEntry.get();
                if (item instanceof ArmorItem || item instanceof AbstractOffhandMagicItem) {
                    expectedItems.add(item);
                }
            }
            expectedItems.addAll(List.of(
                    ItemRegistry.CHARGECAST_CATALYSTBOOK.get(),
                    ItemRegistry.ELEMENTAL_BOW.get(),
                    ItemRegistry.LUMINOUS_DEVICE.get(),
                    ItemRegistry.STORAGE_STABILIZER.get(),
                    ItemRegistry.REFLECTCAST_SHIELD.get(),
                    ItemRegistry.PARRYCAST_BUCKLER.get(),
                    ItemRegistry.BULWARK_GREATSHIELD.get(),
                    ItemRegistry.MANA_SHIELD_CHARM.get(),
                    ItemRegistry.ASHEN_CIRCLET.get(),
                    ItemRegistry.ENCHANTED_CIRCLET.get(),
                    ItemRegistry.ARCANE_CINDER.get(),
                    ItemRegistry.SPELL_EXTRACT_SHARD.get(),
                    ItemRegistry.WISDOM_SHARD.get(),
                    ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(),
                    ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get(),
                    ItemRegistry.SPELLCASTER_QUIVER.get()
            ));
            for (var item : expectedItems) {
                helper.assertTrue(new ItemStack(item).is(MALUM_SOULWOVEN_POUCH_EFFICIENT),
                        "Soulwoven Pouch should store " + item + " efficiently");
            }

            for (var excludedItem : List.of(
                    ItemRegistry.ENDER_GRIMOIRE.get(),
                    ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                    ItemRegistry.EXPLORERS_CODEX.get(),
                    ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get(),
                    ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get(),
                    ItemRegistry.MANA_THRUSTER.get(),
                    ItemRegistry.JUMPCAST_CHARM.get()
            )) {
                helper.assertFalse(new ItemStack(excludedItem).is(MALUM_SOULWOVEN_POUCH_EFFICIENT),
                        "Soulwoven Pouch should not store " + excludedItem + " efficiently");
            }
        });
    }


}
