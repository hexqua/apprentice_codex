package jp.aquafactory.apprenticecodex.gametest.loot;

import io.redspace.ironsspellbooks.loot.SpellFilter;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.loot.LootTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.world.errandmage.ErrandMageScenarios.createChestLootParams;

final class LootScenarios {
    private LootScenarios() {}

    static void genericLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAnyItem(
                helper,
                ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
                createChestLootParams(helper),
                2048,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
        ));
    }

    static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAllItems(
                helper,
                ResourceLocation.withDefaultNamespace("chests/spawn_bonus_chest"),
                createChestLootParams(helper),
                1,
                List.of(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get())
        ));
    }

    static void nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockedSpells = getNonLootableApprenticeSpells();
            var defaultSpellFilter = new SpellFilter();
            var applicableSpells = defaultSpellFilter.getApplicableSpells();

            for (var blockedSpell : blockedSpells) {
                var spellId = blockedSpell.getSpellResource();
                helper.assertTrue(!blockedSpell.allowLooting(),
                        "Non-lootable apprentice spell unexpectedly allows loot generation: " + spellId);
                helper.assertFalse(applicableSpells.contains(blockedSpell),
                        "Default loot spell filter still contains blocked apprentice spell: " + spellId);
            }
        });
    }

    static void genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableNeverGeneratesBlockedSpells(
                helper,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/generic_magic_treasure"),
                createChestLootParams(helper),
                512,
                getNonLootableApprenticeSpells()
        ));
    }
}
