package jp.aquafactory.apprenticecodex.gametest.loot;

import io.redspace.ironsspellbooks.loot.SpellFilter;
import java.util.ArrayList;
import java.util.List;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.loot.LootTestSupport.assertLootTableGeneratesAnyItem;
import static jp.aquafactory.apprenticecodex.gametest.loot.LootTestSupport.assertLootTableNeverGeneratesBlockedSpells;
import static jp.aquafactory.apprenticecodex.gametest.loot.LootTestSupport.getNonLootableApprenticeSpells;
import static jp.aquafactory.apprenticecodex.gametest.world.errandmage.ErrandMageScenarios.createChestLootParams;

final class LootScenarios {
    private LootScenarios() {}

    static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lootTable = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SPAWN_BONUS_CHEST);
            var generatedLoot = new ArrayList<ItemStack>();
            var lootParams = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(new BlockPos(0, 1, 0)))
                    .create(LootContextParamSets.CHEST);
            lootTable.getRandomItems(lootParams, generatedLoot::add);

            helper.assertTrue(generatedLoot.stream().anyMatch(stack -> stack.is(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get())),
                    "Spawn bonus chest loot no longer contains Isekai Travel Guidebook: " + generatedLoot);
        });
    }

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

    static void ironsStructureLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAnyItem(
                helper,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/generic_magic_treasure"),
                createChestLootParams(helper),
                512,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
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
