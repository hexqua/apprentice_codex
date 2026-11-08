package jp.aquafactory.apprenticecodex.gametest.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.PoiTypeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.crafting.ComparatorScenarios.placeAndAssertBlockEntity;
import static jp.aquafactory.apprenticecodex.gametest.spell.searchbeacon.SearchBeaconScenarios.assertSearchBeaconTarget;

final class RegistrationScenarios {
    private RegistrationScenarios() {}

    static final String REQUIRED_OPTIONAL_MODS_PROPERTY = "apprenticecodex.requiredOptionalMods";

    static final String VANILLA_NAMESPACE = "minecraft";

    static void requiredOptionalModsAreLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var requiredModsProperty = System.getProperty(REQUIRED_OPTIONAL_MODS_PROPERTY, "").trim();
            if (requiredModsProperty.isEmpty()) {
                return;
            }

            var missingModIds = Arrays.stream(requiredModsProperty.split(","))
                    .map(String::trim)
                    .filter(modId -> !modId.isEmpty())
                    .filter(modId -> !ModList.get().isLoaded(modId))
                    .toList();
            helper.assertTrue(
                    missingModIds.isEmpty(),
                    "Required optional MODs are missing for this GameTest run: " + missingModIds
            );
        });
    }

    static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {

            var apprenticeDeskPoi = PoiTypes.forState(BlockRegistry.APPRENTICE_DESK.get().defaultBlockState()).orElse(null);
            helper.assertTrue(apprenticeDeskPoi != null, "Apprentice Desk POI state mapping is missing");
            helper.assertTrue(apprenticeDeskPoi != null && apprenticeDeskPoi.is(PoiTypeRegistry.APPRENTICE_DESK_KEY),
                    "Apprentice Desk resolved to unexpected POI: " + apprenticeDeskPoi);
            helper.assertTrue(apprenticeDeskPoi != null && apprenticeDeskPoi.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),
                    "Apprentice Desk POI is missing minecraft:acquirable_job_site");
            helper.assertTrue(BuiltInRegistries.VILLAGER_PROFESSION.get(VillagerProfessionRegistry.ERRAND_MAGE_KEY.location()) == VillagerProfessionRegistry.ERRAND_MAGE.get(),
                    "Errand Mage profession is missing from BuiltInRegistries");

            for (var spellEntry : SpellRegistry.SPELLS.getEntries()) {
                var spell = spellEntry.get();
                var spellId = spell.getSpellResource();
                helper.assertTrue(spellId != null, "Spell id is null: " + spellEntry.getId());
                helper.assertTrue(ApprenticeCodex.MODID.equals(spellId.getNamespace()), "Spell namespace mismatch: " + spellId);
                helper.assertTrue(spell.getSchoolType() != null, "Spell school is null: " + spellId);
                helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.REGISTRY.get(spellId) == spell,
                        "Spell registry lookup failed: " + spellId);
            }

            var assignedDefinitions = SchoolAffinityRegistry.getDefinitions().stream()
                    .filter(definition -> SchoolAffinityRegistry.getAssignedSchool(definition.slotIndex()).isPresent())
                    .toList();
            helper.assertFalse(assignedDefinitions.isEmpty(), "No School Affinity assignments were resolved");
            helper.assertFalse(SchoolAffinityRegistry.getBrewingDefinitionsByCatalyst().isEmpty(), "No School Affinity catalysts were resolved");
            assertSearchBeaconTarget(helper, Items.BLAZE_ROD, "irons_spellbooks:pyromancer_tower");
            assertSearchBeaconTarget(helper, Items.EMERALD, "irons_spellbooks:evoker_fort");
            assertSearchBeaconTarget(helper, Items.POISONOUS_POTATO, "irons_spellbooks:mangrove_hut");
            assertSearchBeaconTarget(helper, Items.SCULK_SENSOR, "minecraft:ancient_city");

            var divinePearl = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "divine_pearl"))
                    .orElse(null);
            helper.assertTrue(divinePearl != null, "irons_spellbooks:divine_pearl is not registered");
            var villageDefinition = divinePearl != null
                    ? SearchBeaconTargetManager.getDefinition(new ItemStack(divinePearl))
                    : null;
            helper.assertTrue(villageDefinition != null, "SearchBeacon target missing for irons_spellbooks:divine_pearl");
            helper.assertTrue(
                    villageDefinition != null
                            && villageDefinition.targets().contains(new SearchBeaconTargetList.TargetReference(true, ResourceLocation.withDefaultNamespace("village"))),
                    "SearchBeacon divine pearl target should point to #minecraft:village"
            );

            for (var definition : assignedDefinitions) {
                helper.assertTrue(BuiltInRegistries.MOB_EFFECT.get(definition.effectId()) == definition.effect(),
                        "Missing School Affinity effect: " + definition.effectId());
                helper.assertTrue(BuiltInRegistries.POTION.get(definition.basePotionId()) == definition.basePotion(),
                        "Missing School Affinity potion: " + definition.basePotionId());
                helper.assertTrue(BuiltInRegistries.POTION.get(definition.longPotionId()) == definition.longPotion(),
                        "Missing School Affinity potion: " + definition.longPotionId());
                helper.assertTrue(BuiltInRegistries.POTION.get(definition.strongPotionId()) == definition.strongPotion(),
                        "Missing School Affinity potion: " + definition.strongPotionId());
            }
        });
    }

    static void ownSpellsUniqueInfoAcceptsNullCaster(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var failures = new ArrayList<String>();
            for (var spellEntry : SpellRegistry.SPELLS.getEntries()) {
                var spell = spellEntry.get();
                var spellId = String.valueOf(spell.getSpellResource());
                for (var spellLevel = spell.getMinLevel(); spellLevel <= spell.getMaxLevel(); spellLevel++) {
                    try {
                        var uniqueInfo = spell.getUniqueInfo(spellLevel, null);
                        if (uniqueInfo == null) {
                            failures.add(spellId + " level " + spellLevel + " returned null");
                        } else if (uniqueInfo.stream().anyMatch(Objects::isNull)) {
                            failures.add(spellId + " level " + spellLevel + " returned a null component");
                        }
                    } catch (RuntimeException exception) {
                        failures.add(spellId + " level " + spellLevel + " threw "
                                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
                    }
                }
            }

            helper.assertTrue(failures.isEmpty(),
                    "Apprentice spell getUniqueInfo must accept null caster: " + String.join("; ", failures));
        });
    }

    static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 0), BlockRegistry.MAGE_LIGHT_TORCH.get(), BlockEntityRegistry.MAGE_LIGHT_TORCH.get());
            placeAndAssertBlockEntity(helper, new BlockPos(0, 2, 0), BlockRegistry.WIZARDLAMP_LANTERN.get(), BlockEntityRegistry.WIZARDLAMP_LANTERN.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 0), BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 0), BlockRegistry.ARCANUM_IN_A_JAR.get(), BlockEntityRegistry.ARCANUM_IN_A_JAR.get());
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 1), BlockRegistry.ESSENCE_SMOKER.get(), BlockEntityRegistry.ESSENCE_SMOKER.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 1), BlockRegistry.ATELIER_STATION.get(), BlockEntityRegistry.ATELIER_STATION.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 1), BlockRegistry.SPELL_DISPENSER.get(), BlockEntityRegistry.SPELL_DISPENSER.get());
            placeAndAssertBlockEntity(helper, new BlockPos(3, 1, 1), BlockRegistry.CREATIVE_SPELL_DISPENSER.get(), BlockEntityRegistry.SPELL_DISPENSER.get());
            var level = helper.getLevel();
            for (var entityEntry : EntityRegistry.ENTITIES.getEntries()) {
                var entity = entityEntry.get().create(level);
                helper.assertTrue(entity != null, "Entity instantiation failed: " + entityEntry.getId());
            }
        });
    }

}
