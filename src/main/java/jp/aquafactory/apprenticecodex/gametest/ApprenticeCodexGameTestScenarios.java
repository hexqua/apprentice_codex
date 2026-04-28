package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellListManager;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverPickupEvent;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.mixin.SinglePoolElementAccessor;
import jp.aquafactory.apprenticecodex.mixin.StructureTemplatePoolAccessor;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.harvestmoon.HarvestMoon;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSearchService;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PoiTypeRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeStats;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.worldgen.ErrandMageVillageAddition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ApprenticeCodexGameTestScenarios {
    private static final double SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE = 1.5D;

    private static final String CREATE_GAMETEST_HOOKS_CLASS =
            "jp.aquafactory.apprenticecodex.gametest.create.CreateGameTestHooks";
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final String FARMERS_DELIGHT_MOD_ID = "farmersdelight";
    private static final String LODESTONE_MOD_ID = "lodestone";
    private static final String MALUM_MOD_ID = "malum";
    private static final ResourceLocation FARMERS_DELIGHT_TOMATO_BLOCK =
            ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MOD_ID, "tomatoes");
    private static final ResourceLocation FARMERS_DELIGHT_TOMATO_ITEM =
            ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MOD_ID, "tomato");
    private static final ResourceLocation LODESTONE_MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath(LODESTONE_MOD_ID, "magic_proficiency");
    private static final TagKey<DamageType> COMMON_IS_MAGIC = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "is_magic")
    );
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );
    private static final TagKey<Item> MALUM_MAGIC_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "magic_capable_weapon")
    );
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("create", "contraption_controlled")
    );
    private static final TagKey<Item> CURIOS_BACK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BACK)
    );
    private static final TagKey<Item> CURIOS_CHARM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.CHARM)
    );
    private static final ResourceLocation FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "focus_staffbow_overcharge"
    );
    private static final ResourceLocation CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "casting_movespeed_dynamic_test_external");
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");
    private static final ResourceLocation MALUM_HAUNTED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "haunted");
    private static final ResourceLocation MALUM_ANIMATED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "animated");

    private ApprenticeCodexGameTestScenarios() {
    }
    static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertBuiltinRegistryEntries(helper, "item", BuiltInRegistries.ITEM, ItemRegistry.ITEMS.getEntries());
            assertBuiltinRegistryEntries(helper, "block", BuiltInRegistries.BLOCK, BlockRegistry.BLOCKS.getEntries());
            assertBuiltinRegistryEntries(helper, "block entity", BuiltInRegistries.BLOCK_ENTITY_TYPE, BlockEntityRegistry.BLOCK_ENTITY_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "entity", BuiltInRegistries.ENTITY_TYPE, EntityRegistry.ENTITIES.getEntries());
            assertBuiltinRegistryEntries(helper, "mob effect", BuiltInRegistries.MOB_EFFECT, EffectRegistry.EFFECTS.getEntries());
            assertBuiltinRegistryEntries(helper, "attribute", BuiltInRegistries.ATTRIBUTE, ApprenticeAttributeRegistry.ATTRIBUTES.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe serializer", BuiltInRegistries.RECIPE_SERIALIZER, RecipeRegistry.RECIPE_SERIALIZERS.getEntries());
            assertBuiltinRegistryEntries(helper, "point of interest type", BuiltInRegistries.POINT_OF_INTEREST_TYPE, PoiTypeRegistry.POI_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "villager profession", BuiltInRegistries.VILLAGER_PROFESSION, VillagerProfessionRegistry.VILLAGER_PROFESSIONS.getEntries());
            assertBuiltinRegistryEntries(helper, "potion", BuiltInRegistries.POTION, PotionRegistry.POTIONS.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe type", BuiltInRegistries.RECIPE_TYPE, RecipeRegistry.RECIPE_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "creative tab", BuiltInRegistries.CREATIVE_MODE_TAB, CreativeTabRegistry.TABS.getEntries());
            assertEnchantmentsRegistered(helper);

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
    static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var unknownMarker = new SearchBeaconState.StructureMarker(
                    helper.getLevel().dimension().location(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city"),
                    1L
            );
            var searchedMarker = new SearchBeaconState.StructureMarker(
                    helper.getLevel().dimension().location(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "village_plains"),
                    2L
            );
            var unknownResult = new SearchBeaconSearchService.SearchResult(List.of(
                    new SearchBeaconSearchService.LocatedStructure(
                            unknownMarker,
                            BlockPos.ZERO,
                            SearchBeaconState.StructureKnowledge.UNKNOWN,
                            4.0
                    )
            ));
            var knownOnlyResult = new SearchBeaconSearchService.SearchResult(List.of(
                    new SearchBeaconSearchService.LocatedStructure(
                            searchedMarker,
                            new BlockPos(8, 0, 0),
                            SearchBeaconState.StructureKnowledge.SEARCHED,
                            64.0
                    )
            ));

            helper.assertFalse(
                    SearchBeaconSearchService.shouldRefundOfferedItems(unknownResult),
                    "SearchBeacon should not refund items when it found an unknown structure"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(knownOnlyResult),
                    "SearchBeacon should refund items when it only found known structures"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(new SearchBeaconSearchService.SearchResult(List.of())),
                    "SearchBeacon should refund items when it found nothing"
            );
            helper.assertTrue(
                    SearchBeaconSearchService.shouldRefundOfferedItems(null),
                    "SearchBeacon should refund items before any result is available"
            );
        });
    }
    static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
        var deskPos = new BlockPos(1, 1, 0);
        var absoluteDeskPos = helper.absolutePos(deskPos);
        helper.setBlock(deskPos, BlockRegistry.APPRENTICE_DESK.get());
        helper.getLevel().setDayTime(2000L);

        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        villager.setVillagerData(new VillagerData(villager.getVillagerData().getType(), VillagerProfession.NONE, 1));
        villager.refreshBrain(helper.getLevel());
        villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(helper.getLevel().dimension(), absoluteDeskPos));

        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isAlive(), "Villager died before claiming the Apprentice Desk");
            helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfessionRegistry.ERRAND_MAGE.get(),
                    "Villager did not become Errand Mage: " + villager.getVillagerData().getProfession());
            var jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
            helper.assertTrue(jobSite != null, "Villager did not store a job site");
            helper.assertTrue(jobSite != null && absoluteDeskPos.equals(jobSite.pos()),
                    "Villager claimed unexpected job site: " + jobSite);
        });
    }
    static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var templatePoolRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);

            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/plains/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("mossify_10_percent"),
                    ErrandMageVillageAddition.HOUSE_WEIGHT
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/desert/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    ErrandMageVillageAddition.HOUSE_WEIGHT
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/savanna/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    ErrandMageVillageAddition.HOUSE_WEIGHT
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/snowy/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("empty"),
                    ErrandMageVillageAddition.HOUSE_WEIGHT
            );
            assertVillageHousePoolContains(
                    helper,
                    templatePoolRegistry,
                    ResourceLocation.withDefaultNamespace("village/taiga/houses"),
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("mossify_10_percent"),
                    ErrandMageVillageAddition.HOUSE_WEIGHT
            );
        });
    }
    static void errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var structureTemplateManager = helper.getLevel().getStructureManager();
            assertVillageHouseTemplateLoadsWithJigsaws(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/plains/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/plains/villagers")
            );
            assertVillageHouseTemplateLoadsWithJigsaws(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/desert/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/desert/villagers")
            );
            assertVillageHouseTemplateLoadsWithJigsaws(
                    helper,
                    structureTemplateManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "village/savanna/errand_mage_house"),
                    ResourceLocation.withDefaultNamespace("village/savanna/villagers")
            );
        });
    }
    static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var damagedCrown = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get());
            damagedCrown.setDamageValue(2);
            var taggedCrownCost = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get());
            CustomData.update(DataComponents.CUSTOM_DATA, taggedCrownCost, tag -> tag.putString("apprenticecodex_test", "cost"));
            var taggedCrownItemCost = new ItemCost(
                    taggedCrownCost.getItemHolder(),
                    taggedCrownCost.getCount(),
                    DataComponentPredicate.allOf(taggedCrownCost.getComponents())
            );
            var crownOffer = new net.minecraft.world.item.trading.MerchantOffer(
                    taggedCrownItemCost,
                    new ItemStack(Items.EMERALD),
                    16,
                    2,
                    0.05F
            );
            helper.assertTrue(crownOffer.satisfiedBy(damagedCrown, ItemStack.EMPTY),
                    "Damaged crown should satisfy the errand mage buy offer even when the saved cost stack has tags");

            var taggedScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            CustomData.update(DataComponents.CUSTOM_DATA, taggedScroll, tag -> tag.putString("apprenticecodex_test", "tagged"));
            var taggedScrollCost = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            CustomData.update(DataComponents.CUSTOM_DATA, taggedScrollCost, tag -> tag.putString("apprenticecodex_test", "cost"));
            var taggedScrollItemCost = new ItemCost(
                    taggedScrollCost.getItemHolder(),
                    taggedScrollCost.getCount(),
                    DataComponentPredicate.allOf(taggedScrollCost.getComponents())
            );
            var scrollOffer = new net.minecraft.world.item.trading.MerchantOffer(
                    taggedScrollItemCost,
                    java.util.Optional.of(new ItemCost(Items.EMERALD, 16)),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                    3,
                    5,
                    0.05F
            );
            helper.assertTrue(scrollOffer.satisfiedBy(taggedScroll, new ItemStack(Items.EMERALD, 16)),
                    "Tagged scroll should satisfy the errand mage ink trade even when the saved cost stack has tags");
        });
    }
    static void customRecipeDataIsLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_transfer"),
                    RecipeRegistry.SPELLCASTERS_FLASK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"),
                    RecipeRegistry.SPELLCASTERS_FLASK_EXTRACT_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellstained_runic_tablet"),
                    RecipeRegistry.SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"),
                    RecipeRegistry.ALCHEMISTS_FLASK_SMITHING_SERIALIZER.get(), net.minecraft.world.item.crafting.RecipeType.SMITHING);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"),
                    RecipeRegistry.ALCHEMISTS_FLASK_TIPPED_ARROW_SERIALIZER.get(), net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_cane_lodestone_bind"),
                    RecipeRegistry.EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"),
                    RecipeRegistry.EXPLORERS_CODEX_GUIDEBOOK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "isekai_travel_guidebook"),
                    net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE, net.minecraft.world.item.crafting.RecipeType.CRAFTING);

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker/infuse_coal_to_arcane_cinder"),
                    RecipeRegistry.ESSENCE_SMOKER_SERIALIZER.get(), RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "grind_runner/bone_meal_from_bone"),
                    RecipeRegistry.GRIND_RUNNER_SERIALIZER.get(), RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/basic_spellcaster_round"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemist_cauldron/brew_isekai_travel_guidebook_to_common_ink"),
                    io.redspace.ironsspellbooks.registries.RecipeRegistry.ALCHEMIST_CAULDRON_BREW_SERIALIZER.get(),
                    io.redspace.ironsspellbooks.registries.RecipeRegistry.ALCHEMIST_CAULDRON_BREW_TYPE.get());

            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                assertRecipePresent(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_crucible/repair/apprentice_mage_robe"));
                assertRecipePresent(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_crucible/repair/enchantress_robe"));
                assertRecipePresent(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_crucible/repair/reflectcast_shield"));
            }

            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).isEmpty(),
                    "No Essence Smoker recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).isEmpty(),
                    "No Grind Runner recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).isEmpty(),
                    "No Spellcaster Workbench recipes were loaded");
        });
    }
    static void spellcastersFlaskRejectsSplashAndLingeringPotions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionContentsHelper.createPotionStack(
                    Items.SPLASH_POTION,
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var lingeringPotion = PotionContentsHelper.createPotionStack(
                    Items.LINGERING_POTION,
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var emptyFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Spellcaster's Flask rejected a regular potion");
            helper.assertFalse(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Spellcaster's Flask accepted a splash potion");
            helper.assertFalse(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Spellcaster's Flask accepted a lingering potion");
            helper.assertTrue(SpellcastersFlask.copyWithAddedDoses(emptyFlask, splashPotion, 1).isEmpty(),
                    "Spellcaster's Flask stored a splash potion through copyWithAddedDoses");
            helper.assertTrue(SpellcastersFlask.copyWithAddedDoses(emptyFlask, lingeringPotion, 1).isEmpty(),
                    "Spellcaster's Flask stored a lingering potion through copyWithAddedDoses");
        });
    }
    static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    PotionContentsHelper.createPotionStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION.value()),
                    1,
                    0
            );
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_drink_test"));

            var result = flask.getItem().finishUsingItem(flask, helper.getLevel(), player);

            helper.assertTrue(result.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Drinking the last dose should keep the flask item");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(result) == 0,
                    "Drinking the last dose did not clear the stored dose count: " + SpellcastersFlask.getStoredDoseCount(result));
            helper.assertTrue(SpellcastersFlask.getStoredItem(result).isEmpty(),
                    "Drinking the last dose left StoredItem behind");
            helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
                    "Drinking the flask did not apply the stored potion effect");
        });
    }
    static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionContentsHelper.createPotionStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());
            var originalEffect = PotionContentsHelper.getMobEffects(storedPotion).get(0);
            var flask = createFilledSpellcastersFlask(helper.getLevel().registryAccess(), storedPotion, 1, 2);
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_glow_tradeoff_test"));

            flask.getItem().finishUsingItem(flask, helper.getLevel(), player);

            var appliedEffect = player.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(appliedEffect != null, "Drinking the flask did not apply regeneration");
            helper.assertTrue(appliedEffect != null && appliedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Glow Energy should still amplify drunk flask effects");
            helper.assertTrue(appliedEffect != null
                            && appliedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * (1.0F / 3.0F))),
                    "Glow Energy should reduce drunk flask duration by 1 / (1 + level)");
        });
    }
    static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );

            var emptiedFlask = SpellcastersFlask.copyAfterExtractingDoses(flask, 1);

            helper.assertTrue(emptiedFlask.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Extracting the last dose should keep the flask item");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(emptiedFlask) == 0,
                    "Batch extraction did not clear the stored dose count: " + SpellcastersFlask.getStoredDoseCount(emptiedFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(emptiedFlask).isEmpty(),
                    "Batch extraction left StoredItem behind");
        });
    }
    static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeHolder = helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe) recipeHolder.value();
            var flask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var craftingInput = createCraftingInput(flask, new ItemStack(Items.GLASS_BOTTLE));

            helper.assertTrue(recipe.matches(craftingInput, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should match a filled flask and glass bottle");

            var result = recipe.assemble(craftingInput, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingInput).get(0);

            helper.assertTrue(ItemStack.isSameItemSameComponents(
                            result,
                            createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get())),
                    "Spellcaster's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spellcaster's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Spellcaster's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Spellcaster's Flask extract recipe left StoredItem behind");
        });
    }
    static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Alchemist's Flask did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Alchemist's Flask spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Alchemist's Flask spell slot count mismatch: " + (spellContainer == null ? -1 : spellContainer.getMaxSpellCount()));
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Alchemist's Flask should not expose the imbued spell in the spell wheel");

            var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != SpellData.EMPTY, "Alchemist's Flask has no preset spell");
            helper.assertTrue(spellData.getSpell() == SpellRegistry.EXTRACT.get(),
                    "Alchemist's Flask preset spell mismatch: " + (spellData == SpellData.EMPTY ? "empty" : spellData.getSpell().getSpellResource()));
            helper.assertTrue(spellData.getLevel() == 1,
                    "Alchemist's Flask preset spell level mismatch: " + (spellData == SpellData.EMPTY ? -1 : spellData.getLevel()));
            helper.assertTrue(!spellData.canRemove(), "Alchemist's Flask preset spell should stay locked by default");
        });
    }
    static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Alchemist's Flask should allow instant spell imbuing");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.COMPOUND_PHIAL.get(), 1),
                    "Alchemist's Flask should allow long spell imbuing");
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Alchemist's Flask should allow continuous spell imbuing");
        });
    }
    static void alchemistsFlaskAcceptsSplashLingeringAndSimpleElixirButRejectsNormalPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var emptyFlask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionContentsHelper.createPotionStack(
                    Items.SPLASH_POTION,
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var lingeringPotion = PotionContentsHelper.createPotionStack(
                    Items.LINGERING_POTION,
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertFalse(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Alchemist's Flask accepted a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Alchemist's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Alchemist's Flask rejected a lingering potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, simpleElixir),
                    "Alchemist's Flask rejected a Simple Elixir");
        });
    }
    static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var splashPotion = PotionContentsHelper.createPotionStack(
                    Items.SPLASH_POTION,
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 16,
                    "Alchemist's Flask base capacity mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));

            registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.LARGE_MUG)
                    .ifPresent(enchantment -> {
                        flask.enchant(enchantment, 1);
                        helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 20,
                                "Alchemist's Flask Large Mug bonus mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));
                    });

            var filledFlask = SpellcastersFlask.copyWithAddedDoses(flask, splashPotion, 16);
            helper.assertTrue(!filledFlask.isEmpty(), "Alchemist's Flask failed to store sixteen splash potion doses");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(filledFlask) == 16,
                    "Alchemist's Flask stored dose count mismatch: " + SpellcastersFlask.getStoredDoseCount(filledFlask));

            var recipeHolder = helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe) recipeHolder.value();
            var craftingInput = createCraftingInput(
                    createFilledAlchemistsFlask(registryAccess, splashPotion, 1, 0),
                    new ItemStack(Items.GLASS_BOTTLE)
            );

            helper.assertTrue(recipe.matches(craftingInput, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should accept Alchemist's Flask");

            var result = recipe.assemble(craftingInput, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingInput).get(0);

            helper.assertTrue(ItemStack.isSameItemSameComponents(result, splashPotion),
                    "Alchemist's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Alchemist's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Alchemist's Flask extract recipe left StoredItem behind");
        });
    }
    static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var recipeHolder = helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"))
                    .orElseThrow();
            var recipe = (jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe) recipeHolder.value();

            var normalPotion = PotionContentsHelper.createPotionStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var spellcastersFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
            var enchantmentLookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
            enchantmentLookup.get(Enchantments.GUZZLE).ifPresent(enchantment -> spellcastersFlask.enchant(enchantment, 2));
            enchantmentLookup.get(Enchantments.LARGE_MUG).ifPresent(enchantment -> spellcastersFlask.enchant(enchantment, 1));
            enchantmentLookup.get(Enchantments.RED_ENERGY).ifPresent(enchantment -> spellcastersFlask.enchant(enchantment, 1));
            enchantmentLookup.get(Enchantments.GLOW_ENERGY).ifPresent(enchantment -> spellcastersFlask.enchant(enchantment, 1));
            var filledSpellcastersFlask = SpellcastersFlask.copyWithAddedDoses(spellcastersFlask, normalPotion, 2);
            var smithingInput = new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.EMERALD),
                    filledSpellcastersFlask,
                    new ItemStack(Items.GUNPOWDER)
            );

            helper.assertTrue(recipe.matches(smithingInput, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept a filled Spellcaster's Flask");

            var convertedFlask = recipe.assemble(smithingInput, registryAccess);
            helper.assertTrue(convertedFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask smithing recipe returned the wrong result item");
            helper.assertTrue(ISpellContainer.isSpellContainer(convertedFlask),
                    "Alchemist's Flask smithing recipe should preserve the preset spell container");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(convertedFlask) == 2,
                    "Alchemist's Flask smithing recipe should preserve stored dose count");

            var convertedStoredItem = SpellcastersFlask.getStoredItem(convertedFlask);
            helper.assertTrue(convertedStoredItem.is(Items.SPLASH_POTION),
                    "Filled Spellcaster's Flask should convert a regular potion into a splash potion");
            helper.assertTrue(PotionContentsHelper.getPotion(convertedStoredItem) == PotionContentsHelper.getPotion(normalPotion),
                    "Converted Alchemist's Flask should keep the original potion type");

            helper.assertTrue(Enchantments.getLevel(convertedFlask, Enchantments.GUZZLE) == 0,
                    "Alchemist's Flask smithing recipe should drop only Guzzle");
            helper.assertTrue(Enchantments.getLevel(convertedFlask, Enchantments.LARGE_MUG) == 1,
                    "Alchemist's Flask smithing recipe should keep Large Mug");
            helper.assertTrue(Enchantments.getLevel(convertedFlask, Enchantments.RED_ENERGY) == 1,
                    "Alchemist's Flask smithing recipe should keep Red Energy");
            helper.assertTrue(Enchantments.getLevel(convertedFlask, Enchantments.GLOW_ENERGY) == 1,
                    "Alchemist's Flask smithing recipe should keep Glow Energy");

            var emptyFlaskInput = new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.EMERALD),
                    new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(recipe.matches(emptyFlaskInput, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept an empty Spellcaster's Flask");
            var emptyFlaskResult = recipe.assemble(emptyFlaskInput, registryAccess);
            helper.assertTrue(emptyFlaskResult.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask smithing recipe should still create an empty Alchemist's Flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(emptyFlaskResult) == 0,
                    "Empty Spellcaster's Flask conversion should keep the result empty");
            helper.assertTrue(SpellcastersFlask.getStoredItem(emptyFlaskResult).isEmpty(),
                    "Empty Spellcaster's Flask conversion should not leave a stored item behind");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirInput = new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), simpleElixir, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(recipe.matches(simpleElixirInput, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept Simple Elixir");
            var simpleElixirResult = recipe.assemble(simpleElixirInput, registryAccess);
            helper.assertTrue(ItemStack.isSameItemSameComponents(SpellcastersFlask.getStoredItem(simpleElixirResult), simpleElixir),
                    "Alchemist's Flask smithing recipe should keep Simple Elixir unchanged");

            var fireAle = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_ALE.get());
            var fireAleInput = new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), fireAle, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(!recipe.matches(fireAleInput, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should reject unsupported stored items such as Fire Ale");
        });
    }
    static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var recipeHolder = helper.getLevel().getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"))
                    .orElseThrow();
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe) recipeHolder.value();

            var splashPotion = PotionContentsHelper.createPotionStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());
            var splashInput = createCraftingInput(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(registryAccess, splashPotion, 2, 1),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(splashInput, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a splash potion flask");

            var splashResult = recipe.assemble(splashInput, registryAccess);
            var splashRemainingFlask = recipe.getRemainingItems(splashInput).get(4);

            helper.assertTrue(splashResult.is(Items.TIPPED_ARROW) && splashResult.getCount() == 8,
                    "Alchemist's Flask tipped arrow recipe should return eight tipped arrows");
            helper.assertTrue(PotionContentsHelper.getPotion(splashResult) == PotionContentsHelper.getPotion(splashPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored splash potion");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(splashRemainingFlask) == 1,
                    "Alchemist's Flask tipped arrow recipe should consume exactly one dose");

            var lingeringPotion = PotionContentsHelper.createPotionStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var lingeringInput = createCraftingInput(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(registryAccess, lingeringPotion, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(lingeringInput, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a lingering potion flask");
            var lingeringResult = recipe.assemble(lingeringInput, registryAccess);
            var lingeringRemainingFlask = recipe.getRemainingItems(lingeringInput).get(4);

            helper.assertTrue(PotionContentsHelper.getPotion(lingeringResult) == PotionContentsHelper.getPotion(lingeringPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored lingering potion");
            helper.assertTrue(lingeringRemainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask tipped arrow recipe should return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(lingeringRemainingFlask) == 0,
                    "Alchemist's Flask tipped arrow recipe should empty the flask after the last dose");
            helper.assertTrue(SpellcastersFlask.getStoredItem(lingeringRemainingFlask).isEmpty(),
                    "Alchemist's Flask tipped arrow recipe should clear StoredItem after the last dose");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirInput = createCraftingInput(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(registryAccess, simpleElixir, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            helper.assertTrue(!recipe.matches(simpleElixirInput, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should reject Simple Elixir");
        });
    }
    static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionContentsHelper.createPotionStack(Items.SPLASH_POTION, PotionRegistry.INTELLIGENCE.get());
            var craftingInput = createCraftingInput(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(helper.getLevel().registryAccess(), splashPotion, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            var craftedStack = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, PotionRegistry.INTELLIGENCE.get());
            craftedStack.setCount(8);
            var advancement = helper.getLevel().getServer().getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/craft_tipped_arrow_by_flask")
            );

            helper.assertTrue(advancement != null, "Missing advancement for flask tipped arrow crafting");
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.event.AlchemistsFlaskAdvancementEvent.shouldAward(craftedStack, craftingInput),
                    "Crafting tipped arrows with Alchemist's Flask should satisfy the advancement award conditions"
            );
        });
    }
    static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var spell = (jp.aquafactory.apprenticecodex.spell.extract.Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_hand_test");
            var splashPotion = PotionContentsHelper.createPotionStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());
            var lingeringPotion = PotionContentsHelper.createPotionStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var magicData = MagicData.getPlayerMagicData(player);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(registryAccess, splashPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(registryAccess, lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when the main hand flask is filled");
            helper.assertTrue(magicData.getAdditionalCastData() instanceof jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData,
                    "Extract should store cast data for the selected flask");
            var mainCastData = (jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(mainCastData.hand() == InteractionHand.MAIN_HAND,
                    "Extract should prefer the main hand filled flask");
            helper.assertTrue(ItemStack.isSameItemSameComponents(mainCastData.storedItem(), splashPotion),
                    "Extract selected the wrong stored item from the main hand flask");

            magicData.setAdditionalCastData(null);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(registryAccess, lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when only the offhand flask is filled");
            var offhandCastData = (jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(offhandCastData.hand() == InteractionHand.OFF_HAND,
                    "Extract should fall back to the offhand filled flask when the main hand flask is empty");
            helper.assertTrue(ItemStack.isSameItemSameComponents(offhandCastData.storedItem(), lingeringPotion),
                    "Extract selected the wrong stored item from the offhand flask");
        });
    }
    static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should fail when no Alchemist's Flask is held");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should fail when the only Alchemist's Flask is empty");
        });
    }
    static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var spell = (jp.aquafactory.apprenticecodex.spell.extract.Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_cast_projectile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var lingeringPotion = PotionContentsHelper.createPotionStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(registryAccess, lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a lingering potion cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var lingeringProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(lingeringProjectile.getItem().is(Items.LINGERING_POTION),
                    "Extract should throw a lingering potion when the flask stores a lingering potion");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()) == 1,
                    "Extract should consume exactly one dose from the casting flask");

            lingeringProjectile.discard();
            magicData.setAdditionalCastData(null);

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(registryAccess, simpleElixir, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a Simple Elixir cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var splashProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(splashProjectile.getItem().is(Items.SPLASH_POTION),
                    "Extract should throw Simple Elixir contents as a splash potion");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.getStoredDoseCount(player.getOffhandItem()) == 1,
                    "Extract should consume exactly one offhand dose after a successful cast");
        });
    }
    static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var storedPotion = PotionContentsHelper.createPotionStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            var enchantmentLookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
            var redEnergy = enchantmentLookup.get(Enchantments.RED_ENERGY).orElse(null);
            if (redEnergy != null) {
                flask.enchant(redEnergy, 1);
            }
            var glowEnergy = enchantmentLookup.get(Enchantments.GLOW_ENERGY).orElse(null);
            if (glowEnergy != null) {
                flask.enchant(glowEnergy, 1);
            }
            flask = SpellcastersFlask.copyWithAddedDoses(flask, storedPotion, 1);

            var thrownPotion = jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.createExtractedPotionForThrow(flask, 1);
            var originalEffect = PotionContentsHelper.getMobEffects(storedPotion).get(0);
            var extractedEffect = PotionContentsHelper.getMobEffects(thrownPotion).get(0);

            helper.assertTrue(thrownPotion.is(Items.SPLASH_POTION),
                    "Extract should preserve non-lingering contents as splash potions");
            helper.assertTrue(extractedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Extract should add both Glow Energy and Extract amplification bonuses");
            helper.assertTrue(extractedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * 1.25F * 0.5F)),
                    "Extract should keep spell-side amplify while Glow Energy halves the Red Energy adjusted duration");
        });
    }
    static void spellDispenserValidatorAcceptsSingleMagicMissileScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a simple Magic Missile scroll");
            helper.assertTrue(validation.spellData().getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Spell Dispenser validator resolved the wrong spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }
    static void spellDispenserValidatorAcceptsNonIronsNamespaceScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.MAGE_LIGHT.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected an apprenticecodex spell scroll");
            helper.assertTrue(validation.spellData().getSpell() == SpellRegistry.MAGE_LIGHT.get(),
                    "Spell Dispenser validator resolved the wrong non-Iron's spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }
    static void spellDispenserValidatorAcceptsLongScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.COMPOUND_PHIAL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a LONG scroll");
            helper.assertTrue(validation.spellData().getSpell() == SpellRegistry.COMPOUND_PHIAL.get(),
                    "Spell Dispenser validator resolved the wrong LONG spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }
    static void spellDispenserValidatorRejectsDenylistedSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.ASSIST_WINGS.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a denylisted scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.DENYLISTED,
                    "Spell Dispenser validator returned the wrong failure reason for denylisted scroll: " + validation.failureReason());
        });
    }
    static void spellDispenserValidatorRejectsContinuousScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.LONG_STRIDE.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a CONTINUOUS scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_PROFILED,
                    "Spell Dispenser validator returned the wrong failure reason for profile-less CONTINUOUS scroll: " + validation.failureReason());
        });
    }
    static void spellDispenserValidatorAcceptsProfiledContinuousScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a profiled CONTINUOUS scroll");
            helper.assertTrue(validation.spellData().getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(),
                    "Spell Dispenser validator resolved the wrong profiled CONTINUOUS spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }
    static void spellDispenserValidatorRejectsRecastScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.HIGANBANA.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a recast spell scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.HAS_RECAST,
                    "Spell Dispenser validator returned the wrong failure reason for recast scroll: " + validation.failureReason());
        });
    }
    static void spellDispenserValidatorRejectsNonScrollSpellContainer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var nonScrollStack = createInitializedPresetStack(ItemRegistry.GRIMOIRE_MANIFEST.get());

            helper.assertTrue(ISpellContainer.isSpellContainer(nonScrollStack), "Prepared non-scroll test stack is not a spell container");
            var validation = SpellDispenserSpellValidator.validate(nonScrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a non-scroll spell container");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_SCROLL,
                    "Spell Dispenser validator returned the wrong failure reason for non-scroll spell container: " + validation.failureReason());
        });
    }
    static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_magic_missile_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a Magic Missile scroll");
            helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser Magic Missile cast did not mark that it reached onCast");
            helper.assertTrue(castResult.cooldownTicks() == spell.getSpellCooldown(),
                    "Spell Dispenser Magic Missile cast returned the wrong cooldown: " + castResult.cooldownTicks());
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser proxy caster was left behind after Magic Missile cast");
        });
    }
    static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    null
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser owner-optional cast failed without an owner profile");
            helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser owner-optional cast did not reach onCast");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser owner-optional cast left proxy state behind");
        });
    }
    static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.COMPOUND_PHIAL.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_long_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a LONG Compound Phial scroll");
            var expectedCooldown = spell.getSpellCooldown()
                    + spell.getEffectiveCastTime(1, new FakePlayer((ServerLevel) level, createSpellDispenserOwnerProfile("spell_dispenser_long_probe")));
            helper.assertTrue(castResult.cooldownTicks() == expectedCooldown,
                    "Spell Dispenser LONG cast returned the wrong cooldown: " + castResult.cooldownTicks() + " / expected " + expectedCooldown);
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser proxy caster was left behind after LONG cast");

            var projectileBox = new AABB(castPos).inflate(5.0D);
            var projectiles = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox);
            helper.assertTrue(!projectiles.isEmpty(), "Spell Dispenser LONG cast completed without spawning a Compound Phial projectile");
        });
    }
    static void spellDispenserCastHelperStopsContinuousCastAtDurationCap(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            var scrollStack = createSpellScroll(spell);
            var validation = createSpellDispenserValidation(scrollStack, spell);

            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    castPos,
                    Direction.NORTH,
                    validation,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_continuous_test")
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser cast helper failed to start a CONTINUOUS Blaze Storm scroll");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser cast helper did not return a CONTINUOUS session");

            var session = startResult.session();
            var maxTicks = session.magicData().getCastDuration();
            for (var tick = 0; tick <= maxTicks + 1 && !session.isFinished(); tick++) {
                SpellDispenserCastHelper.tickContinuousCast(level, session);
            }

            helper.assertTrue(session.isFinished(), "Spell Dispenser CONTINUOUS cast did not stop by its cast duration cap");
            helper.assertTrue(session.consumeFinishedCooldownTicks() == spell.getSpellCooldown(),
                    "Spell Dispenser CONTINUOUS cast did not record the expected cooldown after completion");
            var fireballBox = new AABB(castPos).inflate(16.0D);
            var fireballs = level.getEntitiesOfClass(SmallMagicFireball.class, fireballBox);
            helper.assertTrue(!fireballs.isEmpty(), "Spell Dispenser CONTINUOUS cast completed without spawning Blaze Storm fireballs");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser proxy caster was left behind after CONTINUOUS cast");
        });
    }
    static void spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var scrollStack = createSpellScroll(spell);
            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected Fire Breath profile");

            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    castPos,
                    Direction.NORTH,
                    validation,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_fire_breath_test")
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser cast helper failed to start Fire Breath");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser cast helper did not return a Fire Breath session");
            helper.assertTrue(startResult.session().spellCaster() instanceof SpellDispenserAnchorEntity,
                    "Spell Dispenser Fire Breath did not select a tracked living caster from its explicit profile: " + startResult.session().spellCaster());

            var session = startResult.session();
            for (var tick = 0; tick < 20; tick++) {
                SpellDispenserCastHelper.tickContinuousCast(level, session);
            }

            var coneProjectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(castPos).inflate(16.0D));
            helper.assertTrue(!coneProjectiles.isEmpty(), "Spell Dispenser Fire Breath never spawned its cone projectile");
            helper.assertTrue(coneProjectiles.get(0).getOwner() instanceof SpellDispenserAnchorEntity,
                    "Spell Dispenser Fire Breath did not rebind its cone owner to a tracked anchor: " + coneProjectiles.get(0).getOwner());

            SpellDispenserCastHelper.finishContinuousCast(level, session, true);
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser tracked anchor was left behind after Fire Breath cleanup");
        });
    }
    static void spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
        );
        var scrollStack = createSpellScroll(spell);
        spellDispenser.getInventory().setStackInSlot(0, scrollStack);
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_signal_stop_test"));

        helper.runAtTickTime(1, () -> {
            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    pos,
                    Direction.NORTH,
                    createSpellDispenserValidation(scrollStack, spell),
                    scrollStack,
                    spellDispenser.getOwnerProfile()
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser block entity failed to prepare a CONTINUOUS session");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser block entity did not receive a powered CONTINUOUS session");
            spellDispenser.startContinuousCast(startResult.session());
        });
        helper.runAtTickTime(2, () -> {
            SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            helper.assertTrue(spellDispenser.hasActiveContinuousCast(), "Spell Dispenser CONTINUOUS cast stopped immediately while powered");
            helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, false));
        });
        helper.runAtTickTime(3, () -> {
            SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            helper.assertFalse(spellDispenser.hasActiveContinuousCast(), "Spell Dispenser CONTINUOUS cast did not stop after redstone signal loss");
            assertNoSpellDispenserProxy(helper, pos, scrollStack, "Spell Dispenser proxy caster was left behind after signal loss");
            helper.succeed();
        });
    }
    static void spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
        );
        var scrollStack = createSpellScroll(spell);
        spellDispenser.getInventory().setStackInSlot(0, scrollStack);
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_signal_hold_test"));

        helper.runAtTickTime(1, () -> {
            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    pos,
                    Direction.NORTH,
                    createSpellDispenserValidation(scrollStack, spell),
                    scrollStack,
                    spellDispenser.getOwnerProfile()
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser block entity failed to prepare a held-signal CONTINUOUS session");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser block entity did not receive a held-signal CONTINUOUS session");
            spellDispenser.startContinuousCast(startResult.session());

            var maxTicks = startResult.session().magicData().getCastDuration();
            for (var tick = 0; tick <= maxTicks + 20; tick++) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                    "Spell Dispenser CONTINUOUS cast restarted even though the redstone signal never toggled off");
            assertNoSpellDispenserProxy(helper, pos, scrollStack, "Spell Dispenser proxy caster was left behind after held-signal completion");
            helper.succeed();
        });
    }
    static void spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
        helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
        );
        var scrollStack = createSpellScroll(spell);
        spellDispenser.getInventory().setStackInSlot(0, scrollStack);
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_reset_required_test"));

        helper.runAtTickTime(1, () -> {
            var startResult = SpellDispenserCastHelper.tryStartContinuousCast(
                    level,
                    pos,
                    Direction.NORTH,
                    createSpellDispenserValidation(scrollStack, spell),
                    scrollStack,
                    spellDispenser.getOwnerProfile()
            );
            helper.assertTrue(startResult.result().succeeded(), "Spell Dispenser block entity failed to prepare a reset-required CONTINUOUS session");
            helper.assertTrue(startResult.session() != null, "Spell Dispenser block entity did not receive a reset-required CONTINUOUS session");
            spellDispenser.startContinuousCast(startResult.session());

            var maxTicks = startResult.session().magicData().getCastDuration();
            for (var tick = 0; tick <= maxTicks + 20; tick++) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                    "Spell Dispenser CONTINUOUS cast stayed active after its held-signal duration cap");
            helper.assertTrue(spellDispenser.requiresContinuousReset(),
                    "Spell Dispenser did not record that CONTINUOUS needs a signal reset after held completion");
            helper.assertTrue(spellDispenser.isCoolingDown(),
                    "Spell Dispenser did not enter cooldown after a held CONTINUOUS cast completed");
            assertNoSpellDispenserProxy(helper, pos, scrollStack, "Spell Dispenser proxy caster was left behind after held completion reset state");
            helper.succeed();
        });
    }
    static void spellDispenserBlockEntityStartsAndClearsInstantCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scrollStack = createSpellScroll(spell);
            spellDispenser.getInventory().setStackInSlot(0, scrollStack);
            spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_cooldown_test"));

            var firstResult = spellDispenser.tryActivate();
            helper.assertTrue(firstResult.succeeded(), "Spell Dispenser failed to activate an INSTANT spell for cooldown test");
            helper.assertTrue(spellDispenser.getRemainingCooldownTicks() == spell.getSpellCooldown(),
                    "Spell Dispenser stored the wrong cooldown after an INSTANT cast: " + spellDispenser.getRemainingCooldownTicks());

            var blockedResult = spellDispenser.tryActivate();
            helper.assertTrue(!blockedResult.succeeded() && !blockedResult.reachedOnCast(),
                    "Spell Dispenser did not reject activation while cooling down");

            for (var tick = 0; tick < spell.getSpellCooldown(); tick++) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertFalse(spellDispenser.isCoolingDown(), "Spell Dispenser cooldown did not expire after the expected number of ticks");
        });
    }
    static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getItemHandler(Direction.UP);
            var manaFlask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var nonManaPotion = PotionContentsHelper.createPotionStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var nonManaFlask = createFilledSpellcastersFlask(helper.getLevel().registryAccess(), nonManaPotion, 1, 0);

            helper.assertTrue(itemHandler != null, "Spell Dispenser item capability was not exposed for flask slot validation");
            helper.assertTrue(
                    itemHandler != null && itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get())
                    ),
                    "Spell Dispenser flask slot rejected a mana potion"
            );
            helper.assertTrue(
                    itemHandler != null && itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            manaFlask
                    ),
                    "Spell Dispenser flask slot rejected a mana flask"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            new ItemStack(Items.GLASS_BOTTLE)
                    ),
                    "Spell Dispenser flask slot accepted a glass bottle from automation"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            nonManaPotion
                    ),
                    "Spell Dispenser flask slot accepted a non-mana potion"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            nonManaFlask
                    ),
                    "Spell Dispenser flask slot accepted a non-mana flask"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            new ItemStack(Items.STICK)
                    ),
                    "Spell Dispenser flask slot accepted an unrelated item"
            );
        });
    }
    static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            var absolutePos = helper.absolutePos(pos);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = level.getBlockEntity(absolutePos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var player = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "spell_dispenser_place_mana_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Spell Dispenser placement test could not resolve player mana data");
            magicData.setMana(1350.75F);
            var initialMana = magicData.getMana();

            var state = level.getBlockState(absolutePos);
            ((SpellDispenser) state.getBlock()).setPlacedBy(level, absolutePos, state, player, new ItemStack(ItemRegistry.SPELL_DISPENSER.get()));

            helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                    "Spell Dispenser placement should now start at zero mana: " + spellDispenser.getCurrentMana());
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Spell Dispenser placement should no longer drain player mana: " + magicData.getMana());
            helper.assertTrue(spellDispenser.hasOwnerProfile(), "Spell Dispenser placement did not store the owner profile");
        });
    }
    static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            var absolutePos = helper.absolutePos(pos);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = level.getBlockEntity(absolutePos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var player = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "spell_dispenser_place_sneak_test");
            player.setShiftKeyDown(true);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Spell Dispenser sneak placement test could not resolve player mana data");
            magicData.setMana(640.5F);
            var initialMana = magicData.getMana();

            var state = level.getBlockState(absolutePos);
            ((SpellDispenser) state.getBlock()).setPlacedBy(level, absolutePos, state, player, new ItemStack(ItemRegistry.SPELL_DISPENSER.get()));

            helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                    "Spell Dispenser sneak placement should start at zero mana: " + spellDispenser.getCurrentMana());
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Spell Dispenser sneak placement should not drain player mana: " + magicData.getMana());
            helper.assertTrue(spellDispenser.hasOwnerProfile(), "Spell Dispenser sneak placement did not store the owner profile");
        });
    }
    static void spellDispenserRefillsFromHighestManaPotionThatFits(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var overflowPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_THREE.get());
            var fittingPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_TWO.get());
            spellDispenser.setCurrentMana(780);
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, overflowPotion.copy());
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, fittingPotion.copy());

            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertTrue(spellDispenser.getCurrentMana() == 930,
                    "Spell Dispenser did not pick the highest refill that still fits: " + spellDispenser.getCurrentMana());
            helper.assertTrue(
                    ItemStack.isSameItemSameComponents(
                            spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START),
                            overflowPotion
                    ),
                    "Spell Dispenser consumed a potion that should have overflowed"
            );
            helper.assertTrue(
                    spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1).is(Items.GLASS_BOTTLE),
                    "Spell Dispenser did not leave a glass bottle after consuming a direct mana potion"
            );
        });
    }
    static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var glowEnergyFlask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    1
            );
            spellDispenser.setCurrentMana(700);
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, glowEnergyFlask);

            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertTrue(spellDispenser.getCurrentMana() == 850,
                    "Spell Dispenser did not apply Glow Energy to flask refill mana: " + spellDispenser.getCurrentMana());
            helper.assertTrue(
                    spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START).is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spell Dispenser replaced the flask item instead of emptying one dose"
            );
            helper.assertTrue(
                    !SpellcastersFlask.isFilled(spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START)),
                    "Spell Dispenser did not consume exactly one flask dose"
            );
        });
    }
    static void spellDispenserActivationFailsWhenManaIsInsufficient(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var spell = SpellRegistry.COMPOUND_PHIAL.get();
            var requiredMana = spell.getManaCost(1);
            spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
            spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_low_mana_test"));
            spellDispenser.setCurrentMana(Math.max(0, requiredMana - 1));

            var castResult = spellDispenser.tryActivate();
            helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated even though it had insufficient mana");
            helper.assertTrue(castResult.insufficientMana(), "Spell Dispenser returned the wrong failure for insufficient mana");
            helper.assertTrue(spellDispenser.getCurrentMana() == Math.max(0, requiredMana - 1),
                    "Spell Dispenser changed mana despite rejecting the cast");
        });
    }
    static void spellDispenserContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
            spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_continuous_low_mana_test"));
            spellDispenser.setCurrentMana(spell.getManaCost(1));

            var startResult = spellDispenser.tryActivate();
            helper.assertTrue(startResult.succeeded(), "Spell Dispenser failed to start a CONTINUOUS cast with exactly one cost worth of mana");
            helper.assertTrue(spellDispenser.hasActiveContinuousCast(), "Spell Dispenser did not keep the CONTINUOUS session active after start");
            helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                    "Spell Dispenser did not consume the initial CONTINUOUS mana cost at start: " + spellDispenser.getCurrentMana());

            for (var tick = 0; tick < 40 && spellDispenser.hasActiveContinuousCast(); ++tick) {
                SpellDispenserBlockEntity.serverTick(level, pos, helper.getBlockState(pos), spellDispenser);
            }

            helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                    "Spell Dispenser kept a CONTINUOUS cast active after its mana ran out");
            helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                    "Spell Dispenser mana changed unexpectedly after a mana-depleted CONTINUOUS stop");
        });
    }
    static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            var scrollStack = createSpellScroll(spell);
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_reset_test"));

            startCreateSpellDispenserMovement(harness);
            tickCreateSpellDispenserMovement(harness);
            helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser did not start CONTINUOUS casting when enabled");

            var maxTicks = spell.getEffectiveCastTime(1, new FakePlayer(level, createSpellDispenserOwnerProfile("spell_dispenser_create_reset_probe")));
            for (var tick = 0; tick <= maxTicks + 40 && hasCreateSpellDispenserContinuousCast(harness); tick++) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser CONTINUOUS cast did not stop at its duration cap");
            helper.assertTrue(createSpellDispenserRequiresReset(harness),
                    "Create-mounted Spell Dispenser did not enter reset-required state after held completion");
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not enter cooldown after held completion");

            tickCreateSpellDispenserMovement(harness);
            helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser restarted CONTINUOUS casting without being disabled first");

            setCreateSpellDispenserDisabled(harness, true);
            tickCreateSpellDispenserMovement(harness);
            helper.assertFalse(createSpellDispenserRequiresReset(harness),
                    "Create-mounted Spell Dispenser did not clear reset-required state when disabled");

            for (var tick = 0; tick < spell.getSpellCooldown() && createSpellDispenserIsCoolingDown(harness); tick++) {
                tickCreateSpellDispenserMovement(harness);
            }
            helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser cooldown did not expire after the expected ticks");

            setCreateSpellDispenserDisabled(harness, false);
            tickCreateSpellDispenserMovement(harness);
            helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser did not restart CONTINUOUS casting after disable/enable");

            stopCreateSpellDispenserMovement(harness);
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Create-mounted Spell Dispenser left proxy state behind after stopMoving");
        });
    }
    static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var scrollStack = createSpellScroll(spell);
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_disable_test"));

            startCreateSpellDispenserMovement(harness);
            tickCreateSpellDispenserMovement(harness);
            helper.assertTrue(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser did not start Fire Breath CONTINUOUS casting");

            for (var tick = 0; tick < 20; tick++) {
                tickCreateSpellDispenserMovement(harness);
            }

            setCreateSpellDispenserDisabled(harness, true);
            tickCreateSpellDispenserMovement(harness);
            helper.assertFalse(hasCreateSpellDispenserContinuousCast(harness),
                    "Create-mounted Spell Dispenser kept a CONTINUOUS session active after disable");
            helper.assertFalse(createSpellDispenserRequiresReset(harness),
                    "Create-mounted Spell Dispenser incorrectly required reset after disable cancellation");
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not enter cooldown after cancelling an already-casting CONTINUOUS spell");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Create-mounted Spell Dispenser left proxy state behind after disable");
        });
    }
    static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.COMPOUND_PHIAL.get();
            var scrollStack = createSpellScroll(spell);
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, createSpellDispenserOwnerProfile("spell_dispenser_create_instant_test"));

            startCreateSpellDispenserMovement(harness);
            visitCreateSpellDispenserPosition(harness, castPos);
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not enter cooldown after a non-continuous cast");

            var projectileBox = new AABB(castPos).inflate(5.0D);
            var firstProjectileCount = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox).size();
            helper.assertTrue(firstProjectileCount > 0,
                    "Create-mounted Spell Dispenser did not spawn a projectile on the first cast");

            visitCreateSpellDispenserPosition(harness, castPos);
            var secondProjectileCount = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox).size();
            helper.assertTrue(secondProjectileCount == firstProjectileCount,
                    "Create-mounted Spell Dispenser ignored cooldown and fired again immediately");

            var expectedCooldown = spell.getSpellCooldown()
                    + spell.getEffectiveCastTime(1, new FakePlayer(level, createSpellDispenserOwnerProfile("spell_dispenser_create_instant_probe")));
            for (var tick = 0; tick < expectedCooldown && createSpellDispenserIsCoolingDown(harness); tick++) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertFalse(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser cooldown did not expire after the expected ticks");
            visitCreateSpellDispenserPosition(harness, castPos);
            var thirdProjectileCount = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox).size();
            helper.assertTrue(thirdProjectileCount > secondProjectileCount,
                    "Create-mounted Spell Dispenser did not fire again after cooldown expired");
        });
    }
    static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            var mountedInventory = new ItemStackHandler(1);
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var harness = createSpellDispenserMovementHarness(level, castPos, mountedInventory, null);

            startCreateSpellDispenserMovement(harness);
            visitCreateSpellDispenserPosition(harness, castPos);
            helper.assertTrue(createSpellDispenserIsCoolingDown(harness),
                    "Create-mounted Spell Dispenser did not cast an owner-optional spell without an owner profile");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Create-mounted Spell Dispenser owner-optional cast left proxy state behind");
        });
    }
    static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var externalInventory = new ItemStackHandler(2);
            externalInventory.setStackInSlot(0,
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()));

            var harness = createSpellDispenserMovementHarness(
                    level,
                    castPos,
                    mountedInventory,
                    createSpellDispenserOwnerProfile("spell_dispenser_create_fuel_refill_test"),
                    800,
                    externalInventory,
                    true
            );

            startCreateSpellDispenserMovement(harness);
            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertTrue(externalInventory.getStackInSlot(0).is(Items.GLASS_BOTTLE),
                    "Create-mounted Spell Dispenser did not return a glass bottle to accessible contraption storage");
        });
    }
    static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var externalInventory = new ItemStackHandler(2);
            var manaPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            externalInventory.setStackInSlot(0, manaPotion.copy());

            var harness = createSpellDispenserMovementHarness(
                    level,
                    castPos,
                    mountedInventory,
                    createSpellDispenserOwnerProfile("spell_dispenser_create_protected_storage_test"),
                    800,
                    externalInventory,
                    false
            );

            startCreateSpellDispenserMovement(harness);
            for (var tick = 0; tick < SpellDispenserManaHelper.REFILL_INTERVAL_TICKS; ++tick) {
                tickCreateSpellDispenserMovement(harness);
            }

            helper.assertTrue(ItemStack.isSameItemSameComponents(externalInventory.getStackInSlot(0), manaPotion),
                    "Create-mounted Spell Dispenser consumed a potion from protected contraption storage");
        });
    }
    static void spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");

            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;
            spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get()));

            var castResult = spellDispenser.tryActivate();
            helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated without an owner profile");
            helper.assertTrue(castResult.missingOwnerProfile(), "Spell Dispenser returned the wrong failure for missing owner profile");
        });
    }
    static void spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");

            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            spellDispenser.getInventory().setStackInSlot(0, scrollStack);

            var castResult = spellDispenser.tryActivate();
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser rejected an owner-optional spell without an owner profile");
            assertNoSpellDispenserProxy(helper, pos, scrollStack, "Spell Dispenser block entity owner-optional cast left proxy state behind");
        });
    }
    static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;
            var itemHandler = helper.getLevel().getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(pos),
                    Direction.UP
            );
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.SPELL_SLOT_INDEX, scrollStack.copy());

            helper.assertTrue(itemHandler != null, "Spell Dispenser item capability was not exposed");
            helper.assertTrue(itemHandler != null && !itemHandler.isItemValid(0, scrollStack),
                    "Spell Dispenser sided automation exposed the scroll slot as insertable");

            var remainder = itemHandler == null ? scrollStack.copy() : itemHandler.insertItem(0, scrollStack.copy(), false);
            helper.assertTrue(ItemStack.isSameItemSameComponents(remainder, scrollStack),
                    "Spell Dispenser sided automation consumed a scroll while rejecting the protected slot");
            helper.assertTrue(itemHandler != null && ItemStack.isSameItemSameComponents(itemHandler.getStackInSlot(0), scrollStack),
                    "Spell Dispenser sided automation inserted a scroll into the protected slot");
            helper.assertTrue(itemHandler != null && itemHandler.extractItem(0, 1, false).isEmpty(),
                    "Spell Dispenser sided automation extracted the protected scroll slot");
        });
    }
    static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;
            var itemHandler = helper.getLevel().getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(pos),
                    Direction.UP
            );
            var filledFlask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, filledFlask);
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()));
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 2, new ItemStack(Items.GLASS_BOTTLE));

            helper.assertTrue(itemHandler != null, "Spell Dispenser sided automation capability was not exposed");
            helper.assertTrue(itemHandler != null && itemHandler.extractItem(SpellDispenserBlockEntity.FLASK_SLOT_START, 1, false).isEmpty(),
                    "Spell Dispenser sided automation extracted a filled mana container");
            helper.assertTrue(itemHandler != null
                            && itemHandler.extractItem(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, 1, false).is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spell Dispenser sided automation failed to extract an empty flask");
            helper.assertTrue(itemHandler != null
                            && itemHandler.extractItem(SpellDispenserBlockEntity.FLASK_SLOT_START + 2, 1, false).is(Items.GLASS_BOTTLE),
                    "Spell Dispenser sided automation failed to extract a glass bottle");
        });
    }
    static void spellDispenserMountedMenuKeepsContraptionInventoryAccessible(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spell_dispenser_mounted_menu_test"));
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var currentMana = new AtomicInteger(320);
            var ownerName = "spell_dispenser_mounted_menu_owner_test";

            var menu = SpellDispenserMenu.createMounted(0, new Inventory(player), BlockPos.ZERO, mountedInventory, true, ownerName, currentMana::get);
            helper.assertTrue(menu.stillValid(player), "Spell Dispenser mounted menu closed because it expected a world block entity");
            helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getSpellSource(), scrollStack),
                    "Spell Dispenser mounted menu did not expose the mounted inventory stack");
            helper.assertTrue(menu.isReadyToCast(player),
                    "Spell Dispenser mounted menu did not report a valid mounted scroll as ready");
            helper.assertTrue(menu.getCurrentMana() == 320,
                    "Spell Dispenser mounted menu did not expose the mounted mana value");
            helper.assertTrue(ownerName.equals(menu.getOwnerName()),
                    "Spell Dispenser mounted menu did not expose the mounted owner name");

            currentMana.set(180);
            helper.assertTrue(menu.getCurrentMana() == 180,
                    "Spell Dispenser mounted menu did not follow the live contraption mana source");

            var clientMenu = SpellDispenserMenu.createMounted(1, new Inventory(player), BlockPos.ZERO, mountedInventory, true, ownerName, 320);
            clientMenu.setData(0, 180);
            helper.assertTrue(clientMenu.getCurrentMana() == 180,
                    "Spell Dispenser mounted menu did not accept mana updates from menu data sync");
            helper.assertTrue(ownerName.equals(clientMenu.getOwnerName()),
                    "Spell Dispenser mounted client menu did not retain the synced owner name");
        });
    }
    static void spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spell_dispenser_owner_optional_menu_test"));
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            mountedInventory.setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()));

            var menu = SpellDispenserMenu.createMounted(0, new Inventory(player), BlockPos.ZERO, mountedInventory, false, null, 320);
            helper.assertTrue(menu.isReadyToCast(player),
                    "Spell Dispenser mounted menu still required an owner for an owner-optional spell");
        });
    }
    static void spellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellDispenserStack = new ItemStack(ItemRegistry.SPELL_DISPENSER.get());
            helper.assertTrue(spellDispenserStack.is(CREATE_CONTRAPTION_CONTROLLED),
                    "Spell Dispenser is missing create:contraption_controlled and cannot be selected by Contraption Controls");
        });
    }
    static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_tag_test");
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            blockEntity.setOwnerProfile(ownerProfile);

            var restoredOwner = SpellDispenserBlockEntity.readOwnerProfile(blockEntity.getUpdateTag(helper.getLevel().registryAccess()));
            helper.assertTrue(restoredOwner != null, "Spell Dispenser owner profile helper returned null");
            helper.assertTrue(restoredOwner != null && ownerProfile.getId().equals(restoredOwner.getId()),
                    "Spell Dispenser owner UUID helper returned the wrong value");
            helper.assertTrue(restoredOwner != null && ownerProfile.getName().equals(restoredOwner.getName()),
                    "Spell Dispenser owner name helper returned the wrong value");
        });
    }
    static void spellDispenserOwnerProfilePersistsThroughNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_save_test");
            var original = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            original.setOwnerProfile(ownerProfile);

            var restored = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            restored.loadWithComponents(original.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());

            helper.assertTrue(restored.hasOwnerProfile(), "Spell Dispenser owner profile was not restored from NBT");
            helper.assertTrue(restored.getOwnerProfile() != null && ownerProfile.getId().equals(restored.getOwnerProfile().getId()),
                    "Spell Dispenser owner UUID changed during NBT round-trip");
            helper.assertTrue(restored.getOwnerProfile() != null && ownerProfile.getName().equals(restored.getOwnerProfile().getName()),
                    "Spell Dispenser owner name changed during NBT round-trip");
        });
    }
    static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var scrollStack = createSpellScroll(SpellRegistry.COMPOUND_PHIAL.get());

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    new Vec3(1.0D, 0.0D, 1.0D),
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_diagonal_vector_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast from a diagonal forward vector");

            var projectileBox = new AABB(castPos).inflate(5.0D);
            var projectiles = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox);
            helper.assertTrue(!projectiles.isEmpty(), "Spell Dispenser diagonal vector cast did not spawn a Compound Phial projectile");

            var projectile = projectiles.get(0);
            var motion = projectile.getDeltaMovement();
            helper.assertTrue(Math.abs(motion.x) > 0.01D, "Spell Dispenser diagonal vector cast kept the projectile X motion at zero");
            helper.assertTrue(Math.abs(motion.z) > 0.01D, "Spell Dispenser diagonal vector cast kept the projectile Z motion at zero");
            helper.assertTrue(Math.signum(motion.x) == Math.signum(motion.z),
                    "Spell Dispenser diagonal vector cast did not preserve the intended diagonal quadrant: " + motion);
        });
    }
    static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var relativeCastPos = new BlockPos(0, 1, 0);
            var relativeTargetPos = new BlockPos(0, 1, 3);
            var castPos = helper.absolutePos(relativeCastPos);
            helper.setBlock(relativeTargetPos, Blocks.STONE);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.SOUTH,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get()),
                    createSpellDispenserOwnerProfile("spell_dispenser_spectral_hammer_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to start Spectral Hammer");

            var hammerBox = new AABB(castPos).inflate(8.0D);
            var hammers = level.getEntitiesOfClass(SpectralHammer.class, hammerBox);
            helper.assertTrue(!hammers.isEmpty(), "Spell Dispenser cast did not spawn Spectral Hammer");

            var hammer = hammers.get(0);
            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                hammer.tick();
            }

            helper.assertTrue(true, "Spectral Hammer ticked without crashing");
        });
    }
    static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 0), BlockRegistry.MAGE_LIGHT_TORCH.get(), BlockEntityRegistry.MAGE_LIGHT_TORCH.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 0), BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 0), BlockRegistry.ARCANUM_IN_A_JAR.get(), BlockEntityRegistry.ARCANUM_IN_A_JAR.get());
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 1), BlockRegistry.ESSENCE_SMOKER.get(), BlockEntityRegistry.ESSENCE_SMOKER.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 1), BlockRegistry.ATELIER_STATION.get(), BlockEntityRegistry.ATELIER_STATION.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 1), BlockRegistry.SPELL_DISPENSER.get(), BlockEntityRegistry.SPELL_DISPENSER.get());

            var level = helper.getLevel();
            for (var entityEntry : EntityRegistry.ENTITIES.getEntries()) {
                var entity = entityEntry.get().create(level);
                helper.assertTrue(entity != null, "Entity instantiation failed: " + entityEntry.getId());
            }
        });
    }
    static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var apprenticeEnabledSpells = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells().stream()
                    .filter(ApprenticeCodexGameTestScenarios::isApprenticeSpell)
                    .toList();
            var creativeTabSpells = CreativeTabRegistry.getCreativeTabSpells();

            helper.assertFalse(creativeTabSpells.isEmpty(), "No apprentice spells were exported to the creative tab");
            helper.assertTrue(creativeTabSpells.size() == apprenticeEnabledSpells.size(),
                    "Creative tab spell count mismatch: expected " + apprenticeEnabledSpells.size() + " but got " + creativeTabSpells.size());

            var schoolOrder = new LinkedHashMap<ResourceLocation, Integer>();
            var orderIndex = 0;
            for (var schoolType : io.redspace.ironsspellbooks.api.registry.SchoolRegistry.REGISTRY) {
                schoolOrder.putIfAbsent(schoolType.getId(), orderIndex++);
            }

            var previousSchoolIndex = -1;
            for (AbstractSpell spell : creativeTabSpells) {
                var spellId = spell.getSpellResource();
                helper.assertTrue(spellId != null, "Creative tab spell id is null");
                var schoolType = spell.getSchoolType();
                helper.assertTrue(schoolType != null, "Creative tab spell school is null: " + spellId);

                var schoolIndex = schoolOrder.getOrDefault(schoolType.getId(), Integer.MAX_VALUE);
                helper.assertTrue(previousSchoolIndex <= schoolIndex,
                        "Creative tab spell order is mixed across schools at " + spellId + " (" + schoolType.getId() + ")");
                previousSchoolIndex = schoolIndex;
            }
        });
    }
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
    static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lootTable = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SPAWN_BONUS_CHEST);
            var generatedLoot = new java.util.ArrayList<ItemStack>();
            var lootParams = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(new BlockPos(0, 1, 0)))
                    .create(LootContextParamSets.CHEST);
            lootTable.getRandomItems(lootParams, generatedLoot::add);

            helper.assertTrue(generatedLoot.stream().anyMatch(stack -> stack.is(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get())),
                    "Spawn bonus chest loot no longer contains Isekai Travel Guidebook: " + generatedLoot);
        });
    }
    static void senseEvilUsesSameCubeForSpawnersAndEntities(GameTestHelper helper) {
        var level = helper.getLevel();
        var casterPos = createRemoteIsolationOrigin(helper, new BlockPos(0, 14, 0), 768, 96);
        prepareAbsoluteIsolationPlatform(level, casterPos);
        var caster = createSenseEvilPlayer(level, casterPos, "sense_evil_spawner_cube_test");
        var spell = (SenseEvil) SpellRegistry.SENSE_EVIL.get();
        var range = getSenseEvilRange(spell, caster, 1);
        var diagonalOffset = Mth.floor(range * 0.75);

        helper.assertTrue(Math.sqrt(2.0 * diagonalOffset * diagonalOffset) > range,
                "Diagonal test offset must stay outside the old spherical spawner range");

        var zombieCenter = caster.getBoundingBox().getCenter().add(diagonalOffset, 0.0, diagonalOffset);
        prepareAbsoluteIsolationTargetPlatform(level, zombieCenter);
        var zombie = spawnPositionedZombie(level, zombieCenter);
        var spawnerPos = caster.blockPosition().offset(diagonalOffset, 0, diagonalOffset);
        placeZombieSpawner(level, spawnerPos);

        helper.runAtTickTime(5, () -> {
            var highlights = collectSenseEvilHighlights(spell, level, 1, caster);
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(BlockPos.containing(zombie.getBoundingBox().getCenter())), SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE,
                    "SenseEvil should still detect entities at the shared diagonal cube offset");
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(spawnerPos), SENSE_EVIL_HIGHLIGHT_POSITION_TOLERANCE,
                    "SenseEvil should detect spawners at the same diagonal cube offset as entities");
            helper.succeed();
        });
    }
    static void apprenticeCurioBonusLootTableContainsAllThreeItems(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAllItems(
                helper,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magic_items/basic_curios_bonus"),
                createEmptyLootParams(helper),
                256,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
        ));
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
    static void ominousVaultLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAllItems(
                helper,
                ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous"),
                createChestLootParams(helper),
                384,
                List.of(
                        ItemRegistry.SCARLET_THIRST.get(),
                        ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                        ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()
                )
        ));
    }
    static void catacombsLootIncludesScarletThirstBonusDrop(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAnyItem(
                helper,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/catacombs/coffin_loot"),
                createChestLootParams(helper),
                256,
                List.of(ItemRegistry.SCARLET_THIRST.get())
        ));
    }
    static void nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockedSpells = getNonLootableApprenticeSpells();
            var defaultSpellFilter = new io.redspace.ironsspellbooks.loot.SpellFilter();
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
    static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var item = (io.redspace.ironsspellbooks.item.UniqueSpellBook) stack.getItem();
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack),
                    "Isekai Travel Guidebook did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Isekai Travel Guidebook spell container is null");
            helper.assertTrue(spellContainer.getMaxSpellCount() == 2,
                    "Isekai Travel Guidebook spell slot count mismatch: " + spellContainer.getMaxSpellCount());

            var firstSpell = spellContainer.getSpellAtIndex(0);
            var secondSpell = spellContainer.getSpellAtIndex(1);
            helper.assertTrue(firstSpell != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Isekai Travel Guidebook first spell is empty");
            helper.assertTrue(secondSpell != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Isekai Travel Guidebook second spell is empty");
            helper.assertTrue(firstSpell.getSpell() == SpellRegistry.HEALING_BLOOM.get(),
                    "Isekai Travel Guidebook first spell mismatch: " + firstSpell.getSpell().getSpellResource());
            helper.assertTrue(secondSpell.getSpell() == SpellRegistry.COMPANION_TRUNK.get(),
                    "Isekai Travel Guidebook second spell mismatch: " + secondSpell.getSpell().getSpellResource());

            var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0));
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    pig,
                    0,
                    false,
                    true
            );
            var modifiers = item.getAttributeModifiers(
                    slotContext,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/isekai_travel_guidebook"),
                    stack
            );
            helper.assertTrue(modifiers.isEmpty(),
                    "Isekai Travel Guidebook should not add spellbook attributes: " + modifiers);
        });
    }
    static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            explorersCodexStack.set(DataComponents.CUSTOM_NAME, Component.literal("写本継承確認"));
            explorersCodexStack.set(DataComponents.REPAIR_COST, 7);
            var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
            var expectedUpgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    explorersCodexStack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.OFFHAND.getName()
            );
            var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(unbreaking, 1);
            EnchantmentHelper.setEnchantments(explorersCodexStack, enchantments.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingInput = createCraftingInput(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingInput, helper.getLevel()),
                    "Explorer's Codex + Isekai Travel Guidebook should match the transfer recipe");

            var result = recipe.assemble(craftingInput, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ItemRegistry.EXPLORERS_CODEX.get()),
                    "Transfer recipe should return Explorer's Codex but got " + BuiltInRegistries.ITEM.getKey(result.getItem()));
            helper.assertTrue("写本継承確認".equals(result.getHoverName().getString()),
                    "Explorer's Codex custom name was not preserved: " + result.getHoverName().getString());
            helper.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST, 0) == 7,
                    "Explorer's Codex repair cost was not preserved: " + result.getOrDefault(DataComponents.REPAIR_COST, 0));
            helper.assertTrue(UpgradeData.getUpgradeData(result).equals(expectedUpgradeData),
                    "Explorer's Codex upgrade data was not preserved: " + UpgradeData.getUpgradeData(result));
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(unbreaking, result) == 1,
                    "Explorer's Codex enchantments were not preserved");

            var resultSpellContainer = ISpellContainer.get(result);
            helper.assertTrue(resultSpellContainer != null, "Transferred Explorer's Codex lost its spell container");
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getMaxSpellCount() == 6,
                    "Transferred Explorer's Codex slot count mismatch: " + (resultSpellContainer == null ? -1 : resultSpellContainer.getMaxSpellCount()));
            assertSpellData(helper, resultSpellContainer, 0, SpellRegistry.ASSIST_WINGS.get(), 1, true, "Transferred Explorer's Codex first spell mismatch");
            assertSpellData(helper, resultSpellContainer, 1, SpellRegistry.MAGE_LIGHT.get(), 1, true, "Transferred Explorer's Codex second spell mismatch");
            assertSpellData(helper, resultSpellContainer, 2, SpellRegistry.SENSE_EVIL.get(), 1, true, "Transferred Explorer's Codex third spell mismatch");
            assertSpellData(helper, resultSpellContainer, 3, SpellRegistry.REMOTE_EYE.get(), 1, true, "Transferred Explorer's Codex fourth spell mismatch");
            assertSpellData(helper, resultSpellContainer, 4, SpellRegistry.HEALING_BLOOM.get(), 1, true, "Transferred Explorer's Codex transferred Healing Bloom mismatch");
            assertSpellData(helper, resultSpellContainer, 5, SpellRegistry.COMPANION_TRUNK.get(), 1, true, "Transferred Explorer's Codex transferred Companion Trunk mismatch");
        });
    }
    static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(5);
            helper.assertTrue(mutable.addSpell(SpellRegistry.HEALING_BLOOM.get(), 1, true),
                    "Failed to prepare duplicate Healing Bloom on Explorer's Codex");
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingInput = createCraftingInput(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingInput, helper.getLevel()),
                    "Recipe should still match when one guidebook spell is already present");

            var result = recipe.assemble(craftingInput, helper.getLevel().registryAccess());
            var resultSpellContainer = ISpellContainer.get(result);
            helper.assertTrue(resultSpellContainer != null, "Transferred Explorer's Codex lost its spell container");
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getMaxSpellCount() == 6,
                    "Duplicate-ignore result slot count mismatch: " + (resultSpellContainer == null ? -1 : resultSpellContainer.getMaxSpellCount()));
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getIndexForSpell(SpellRegistry.HEALING_BLOOM.get()) == 4,
                    "Healing Bloom should remain single and keep its prepared slot");
            assertSpellData(helper, resultSpellContainer, 5, SpellRegistry.COMPANION_TRUNK.get(), 1, true,
                    "Companion Trunk should still be transferred when Healing Bloom is already present");
        });
    }
    static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(14);
            fillSpellContainerToActiveCount(helper, mutable, 14);
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingInput = createCraftingInput(explorersCodexStack, guidebookStack);

            helper.assertFalse(recipe.matches(craftingInput, helper.getLevel()),
                    "Recipe should reject Explorer's Codex when transferred spells would exceed 15 slots");
            helper.assertTrue(recipe.assemble(craftingInput, helper.getLevel().registryAccess()).isEmpty(),
                    "Overflow recipe assembly should return empty result");
        });
    }
    static void copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Copper Spell Amplifier did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Copper Spell Amplifier spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Copper Spell Amplifier has no preset spell");
            helper.assertTrue(spellData.getSpell() == SpellRegistry.SHOCK.get(),
                    "Copper Spell Amplifier preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Copper Spell Amplifier preset spell level mismatch: " + spellData.getLevel());

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null, "Copper Spell Amplifier imbued school could not be resolved");

            // school ID の厳密一致ではなく、解決された spell power 属性に補正が積まれることを確認する.
            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Copper Spell Amplifier could not resolve spell power attribute for stacking: " + imbuedSchool.getId());

            assertModifierAmount(helper, item.getDefaultAttributeModifiers(stack), resolvedSpellPower, 0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Copper Spell Amplifier spell power bonus regression");

            var enchantmentRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantmentRegistry.getOrThrow(Enchantments.ATTUNEMENT), 1);
            assertModifierAmount(helper, item.getDefaultAttributeModifiers(stack), resolvedSpellPower, 0.14D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Copper Spell Amplifier + Attunement stacking regression");
        });
    }
    static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Copper Swingcast Staff did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Copper Swingcast Staff spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Copper Swingcast Staff has no preset spell");
            helper.assertTrue(spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(),
                    "Copper Swingcast Staff preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Copper Swingcast Staff preset spell level mismatch: " + spellData.getLevel());
        });
    }
    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var initialContainer = ISpellContainer.get(stack);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            helper.assertTrue(initialContainer != null, "Copper Swingcast Staff spell container is null");
            assertSpellData(helper, initialContainer, 0, io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, true,
                    "Copper Swingcast Staff preset spell should remain locked");

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var normalizedContainer = ISpellContainer.get(stack);
            helper.assertTrue(normalizedContainer != null, "Copper Swingcast Staff normalized spell container is null");
            assertSpellData(helper, normalizedContainer, 0, replacementSpell, 1, false,
                    "Copper Swingcast Staff replacement spell should be removable");
            helper.assertTrue(normalizedContainer.getSpellAtIndex(0).canRemove(),
                    "Copper Swingcast Staff replacement spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractImbueShieldItem) ItemRegistry.REFLECTCAST_SHIELD.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable after save/load");
        });
    }
    static void ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Iron Swingcast Staff imbued spell should remain extractable after save/load");
        });
    }

    static void copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Copper Swingcast Staff preset-equivalent save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Copper Swingcast Staff preset-equivalent imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Copper Swingcast Staff preset-equivalent imbued spell should remain extractable after save/load");
        });
    }
    static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractImbueShieldItem) ItemRegistry.REFLECTCAST_SHIELD.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable after save/load");
        });
    }
    static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var spellContainer = ISpellContainer.get(stack);
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var necklaceTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT)
            );

            helper.assertTrue(spellContainer != null, "Autocast Amulet default spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Autocast Amulet default slot count mismatch: " + (spellContainer == null ? -1 : spellContainer.getMaxSpellCount()));
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Autocast Amulet should stay hidden from the spell wheel");
            helper.assertTrue(stack.is(necklaceTag),
                    "Autocast Amulet should be tagged as curios:necklace");
            helper.assertTrue(item.canImbueSpell(apprenticeSpell, 1),
                    "Autocast Amulet should allow sense_evil by default");
            helper.assertTrue(item.canImbueSpell(ironsHeal, 1),
                    "Autocast Amulet should allow Iron's heal by default");
            helper.assertFalse(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Autocast Amulet should reject non-allowlisted spells");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().contains(apprenticeSpell.getSpellResource()),
                    "Autocast Amulet allowlist should contain sense_evil");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().contains(ironsHeal.getSpellResource()),
                    "Autocast Amulet allowlist should contain Iron's heal");
            helper.assertTrue(AutocastAmuletSpellListManager.getAllowlist().size() == 19,
                    "Autocast Amulet default allowlist size mismatch: " + AutocastAmuletSpellListManager.getAllowlist().size());
        });
    }

    static void manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            helper.assertTrue(stack.is(CURIOS_CHARM),
                    "Mana Shield Charm should be tagged for the Curios charm slot");
            helper.assertTrue(stack.getItem() instanceof ManaShieldCharm,
                    "Mana Shield Charm should resolve to the dedicated curio item implementation");
        });
    }

    static void manaShieldCharmKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertExactEnchantmentSurfaces(
                helper,
                new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()),
                registryIdSet(
                        Enchantments.SHELL,
                        Enchantments.SYNCHRONIZATION,
                        Enchantments.NEUTRALIZATION
                ),
                "Mana Shield Charm"
        ));
    }

    static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var shell = enchantmentLookup.getOrThrow(Enchantments.SHELL);
            var synchronization = enchantmentLookup.getOrThrow(Enchantments.SYNCHRONIZATION);
            var neutralization = enchantmentLookup.getOrThrow(Enchantments.NEUTRALIZATION);

            helper.assertFalse(Enchantment.areCompatible(shell, synchronization),
                    "Shell and Synchronization should stay mutually exclusive");
            helper.assertFalse(Enchantment.areCompatible(shell, neutralization),
                    "Shell and Neutralization should stay mutually exclusive");
            helper.assertFalse(Enchantment.areCompatible(synchronization, neutralization),
                    "Synchronization and Neutralization should stay mutually exclusive");
        });
    }

    static void manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_full_negate_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "full negate");

            var chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm full negate test could not resolve player mana data");
            magicData.setMana(100.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 2.0F);
            helper.assertTrue(event.isCanceled(),
                    "Mana Shield Charm should cancel the fully absorbed LivingAttackEvent");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Mana Shield Charm should keep health unchanged after fully negating damage");
            helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                    "Mana Shield Charm should spend 50 mana to negate 2 damage but got " + magicData.getMana());
            helper.assertTrue(chestplate.getDamageValue() == 0,
                    "Mana Shield Charm should not damage armor durability on a fully negated hit");
            helper.assertFalse(getManaShieldCharmState(player).cooldownActive,
                    "Mana Shield Charm should stay active while mana remains after a fully negated hit");
        });
    }

    static void manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_burned_out_full_negate_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "burned out full negate");

            var chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm burned-out full negate test could not resolve player mana data");
            magicData.setMana(25.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();

            var firstEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(firstEvent.isCanceled(),
                    "Mana Shield Charm should cancel the hit even when the last full negate burns out the shield");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Mana Shield Charm should keep health unchanged when the last full negate burns out the shield");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Mana Shield Charm should clamp mana to zero after the last full negate but got " + magicData.getMana());
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Mana Shield Charm should enter cooldown immediately after the last full negate burns out the shield");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Mana Shield Charm should still apply vanilla-style invulnerability time when the last full negate burns out the shield");
            helper.assertTrue(chestplate.getDamageValue() == 0,
                    "Mana Shield Charm should not damage armor durability when the burned-out hit is still fully negated");

            var secondEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(secondEvent.isCanceled(),
                    "Mana Shield Charm should still cancel repeated contact damage during the burned-out full-negate i-frame");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Mana Shield Charm should not leak damage during the burned-out full-negate i-frame");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Mana Shield Charm should not spend additional mana during the burned-out full-negate i-frame but got " + magicData.getMana());
        });
    }

    static void manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_low_mana_burnout_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "low mana burned out full negate");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Low mana Mana Shield Charm test could not resolve player mana data");
            magicData.setMana(24.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();

            var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(event.isCanceled(),
                    "Mana Shield Charm should still cancel a 1 damage hit when only 24 mana remains before cooldown");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Low mana Mana Shield Charm burnout should still leave health unchanged");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Low mana Mana Shield Charm burnout should clamp mana to zero but got " + magicData.getMana());
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Low mana Mana Shield Charm burnout should enter cooldown immediately");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Low mana Mana Shield Charm burnout should still apply vanilla-style invulnerability time");
        });
    }

    static void manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_iframe_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "iframe");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm iframe test could not resolve player mana data");
            magicData.setMana(100.0F);

            var firstEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(firstEvent.isCanceled(),
                    "Mana Shield Charm should cancel the first fully negated hit before starting its i-frame");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Mana Shield Charm should apply vanilla-style invulnerability time after a fully negated hit");
            helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                    "Mana Shield Charm should spend 25 mana on the first fully negated hit but got " + magicData.getMana());

            var secondEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(secondEvent.isCanceled(),
                    "Mana Shield Charm should also cancel repeated contact damage during its vanilla-style i-frame");
            helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                    "Mana Shield Charm should not spend additional mana during its vanilla-style i-frame but got " + magicData.getMana());
        });
    }

    static void manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var armored = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_partial_armor_test");
            var unarmored = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_partial_plain_test");

            equipCurio(armored, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            equipCurio(unarmored, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, armored, "partial armored");
            assertManaShieldCharmEquipped(helper, unarmored, "partial unarmored");
            armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));

            var armoredMana = MagicData.getPlayerMagicData(armored);
            var unarmoredMana = MagicData.getPlayerMagicData(unarmored);
            helper.assertTrue(armoredMana != null && unarmoredMana != null,
                    "Mana Shield Charm partial reduction test could not resolve player mana data");
            armoredMana.setMana(40.0F);
            unarmoredMana.setMana(40.0F);
            var armoredEvent = postLivingAttackEventForGameTest(armored, helper.getLevel().damageSources().lava(), 3.0F);
            var unarmoredEvent = postLivingAttackEventForGameTest(unarmored, helper.getLevel().damageSources().lava(), 3.0F);
            var expectedRemainingMana = resolveExpectedBarrierManaAfterHitForGameTest(3.0F, 40.0F);

            helper.assertTrue(Math.abs(armoredMana.getMana() - expectedRemainingMana) < 1.0e-4F,
                    "Mana Shield Charm partial reduction should apply the one-hit low mana rescue consistently for the armored player"
                            + " expectedMana=" + expectedRemainingMana
                            + " actualMana=" + armoredMana.getMana());
            helper.assertTrue(Math.abs(unarmoredMana.getMana() - expectedRemainingMana) < 1.0e-4F,
                    "Mana Shield Charm partial reduction should apply the one-hit low mana rescue consistently for the unarmored player"
                            + " expectedMana=" + expectedRemainingMana
                            + " actualMana=" + unarmoredMana.getMana());
            helper.assertTrue(armoredEvent.isCanceled(),
                    "Mana Shield Charm partial reduction should cancel the original armored LivingAttackEvent");
            helper.assertTrue(unarmoredEvent.isCanceled(),
                    "Mana Shield Charm partial reduction should cancel the original unarmored LivingAttackEvent");
            helper.assertTrue(getManaShieldCharmState(armored).cooldownActive == (expectedRemainingMana <= 0.0F),
                    "Mana Shield Charm armored partial reduction cooldown should match the rescued remaining mana expectation");
            helper.assertTrue(getManaShieldCharmState(unarmored).cooldownActive == (expectedRemainingMana <= 0.0F),
                    "Mana Shield Charm unarmored partial reduction cooldown should match the rescued remaining mana expectation");
        });
    }

    static void manaShieldCharmCooldownRecoversAtOneHundredMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_recovery_threshold_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "recovery");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm cooldown recovery test could not resolve player mana data");
            var state = getManaShieldCharmState(player);
            state.reset();
            state.cooldownActive = true;

            magicData.setMana(99.0F);
            var blockedEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(state.cooldownActive,
                    "Mana Shield Charm should stay disabled below the 100 mana recovery threshold");
            helper.assertFalse(blockedEvent.isCanceled(),
                    "Mana Shield Charm should not cancel the hit while cooldown remains locked below 100 mana");
            helper.assertTrue(Math.abs(magicData.getMana() - 99.0F) < 1.0e-4F,
                    "Mana Shield Charm should not spend mana while cooldown remains locked below 100 mana");

            state.cooldownActive = true;
            magicData.setMana(100.0F);
            var recoveredEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);

            helper.assertFalse(state.cooldownActive,
                    "Mana Shield Charm should recover immediately once mana reaches 100");
            helper.assertTrue(recoveredEvent.isCanceled(),
                    "Mana Shield Charm should cancel the recovered hit once the cooldown is lifted");
            helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                    "Mana Shield Charm should spend 25 mana after recovering at the threshold but got " + magicData.getMana());
        });
    }

    static void manaShieldCharmShellUsesArmorOnlyOnNormalDamageAndWearsArmor(GameTestHelper helper) {
        var armored = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_armored_test");
        var unarmored = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_shell_unarmored_test");
        var bypassArmor = createTrackedEquipmentTestPlayer(helper, new BlockPos(6, 2, 0), "mana_shield_shell_bypass_test");
        var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        var shellCharm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        shellCharm.enchant(enchantmentLookup.getOrThrow(Enchantments.SHELL), 1);
        equipCurio(armored, CuriosSlotConstants.CHARM, shellCharm.copy());
        equipCurio(unarmored, CuriosSlotConstants.CHARM, shellCharm.copy());
        equipCurio(bypassArmor, CuriosSlotConstants.CHARM, shellCharm.copy());

        var head = new ItemStack(Items.IRON_HELMET);
        var chest = new ItemStack(Items.IRON_CHESTPLATE);
        var legs = new ItemStack(Items.IRON_LEGGINGS);
        var boots = new ItemStack(Items.IRON_BOOTS);
        armored.setItemSlot(EquipmentSlot.HEAD, head);
        armored.setItemSlot(EquipmentSlot.CHEST, chest);
        armored.setItemSlot(EquipmentSlot.LEGS, legs);
        armored.setItemSlot(EquipmentSlot.FEET, boots);

        var bypassChest = new ItemStack(Items.IRON_CHESTPLATE);
        bypassArmor.setItemSlot(EquipmentSlot.CHEST, bypassChest);

        var armoredMana = MagicData.getPlayerMagicData(armored);
        var unarmoredMana = MagicData.getPlayerMagicData(unarmored);
        var bypassMana = MagicData.getPlayerMagicData(bypassArmor);
        helper.assertTrue(armoredMana != null && unarmoredMana != null && bypassMana != null,
                "Mana Shield Charm Shell test could not resolve player mana data");

        helper.runAtTickTime(1, () -> {
            // tracked player は 1tick 目までに自然回復が走ることがあり、先に mana を入れると burn-out 条件がぶれる。
            armoredMana.setMana(50.0F);
            unarmoredMana.setMana(50.0F);
            bypassMana.setMana(50.0F);
            armored.invulnerableTime = 0;
            unarmored.invulnerableTime = 0;
            bypassArmor.invulnerableTime = 0;
            var armoredInitialHealth = armored.getHealth();
            var unarmoredInitialHealth = unarmored.getHealth();
            var bypassInitialHealth = bypassArmor.getHealth();
            var armoredEvent = postLivingAttackEventForGameTest(armored, helper.getLevel().damageSources().lava(), 3.0F);
            var unarmoredEvent = postLivingAttackEventForGameTest(unarmored, helper.getLevel().damageSources().lava(), 3.0F);
            var bypassSource = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(helper.getLevel(), bypassArmor, DamageTypes.UNITE_LUNA);
            var bypassEvent = postLivingAttackEventForGameTest(bypassArmor, bypassSource, 2.0F);
            helper.assertTrue(armoredEvent.isCanceled() && unarmoredEvent.isCanceled() && bypassEvent.isCanceled(),
                    "Mana Shield Charm Shell test should cancel all intercepted LivingAttackEvent instances");
            helper.assertTrue(armored.getHealth() > unarmored.getHealth(),
                    "Shell should apply armor reduction before the normal mana shoulder path"
                            + " armoredHealth=" + armored.getHealth()
                            + " unarmoredHealth=" + unarmored.getHealth()
                            + " armoredMana=" + armoredMana.getMana()
                            + " unarmoredMana=" + unarmoredMana.getMana());
            helper.assertTrue(armoredMana.getMana() > unarmoredMana.getMana(),
                    "Shell should reduce barrier mana consumption when armor mitigates the intercepted hit"
                            + " armoredMana=" + armoredMana.getMana()
                            + " unarmoredMana=" + unarmoredMana.getMana());
            helper.assertTrue(Math.abs(unarmoredMana.getMana()) < 1.0e-4F,
                    "Shell should still burn out the unarmored player at 50 mana"
                            + " armoredMana=" + armoredMana.getMana()
                            + " unarmoredMana=" + unarmoredMana.getMana());
            helper.assertTrue(head.getDamageValue() == 1
                            && chest.getDamageValue() == 1
                            && legs.getDamageValue() == 1
                            && boots.getDamageValue() == 1,
                    "Shell should spend one durability on each equipped armor piece");
            helper.assertTrue(Math.abs(bypassArmor.getHealth() - bypassInitialHealth) < 1.0e-4F,
                    "Shell should not leak armor-bypass damage when base shield mana fully negates it");
            helper.assertTrue(Math.abs(bypassMana.getMana()) < 1.0e-4F,
                    "Shell should fall back to the normal 25 mana per damage path on armor-bypass hits");
            helper.assertTrue(bypassChest.getDamageValue() == 0,
                    "Shell should not damage armor durability on armor-bypass hits");
            helper.assertTrue(armored.getHealth() < armoredInitialHealth && unarmored.getHealth() < unarmoredInitialHealth,
                    "Shell normal damage test should leave residual health damage on both players");
            helper.succeed();
        });
    }

    static void manaShieldCharmShellLowManaBurnoutStillUsesArmorPath(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_low_mana_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(enchantmentLookup.getOrThrow(Enchantments.SHELL), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Shell low mana test could not resolve player mana data");
            magicData.setMana(24.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var armor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var toughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var source = helper.getLevel().damageSources().lava();
            var incomingDamage = findDamageForArmorReducedTarget(player, source, armor, toughness, 1.0F);
            var reducedDamage = CombatRules.getDamageAfterAbsorb(player, incomingDamage, source, armor, toughness);

            helper.assertTrue(Math.abs(reducedDamage - 1.0F) < 1.0e-3F,
                    "Shell low mana test should configure an armor-reduced hit worth exactly one barrier step"
                            + " reducedDamage=" + reducedDamage
                            + " incomingDamage=" + incomingDamage);

            var event = postLivingAttackEventForGameTest(player, source, incomingDamage);
            helper.assertTrue(event.isCanceled(),
                    "Shell should still cancel the hit when only the last armor-reduced barrier step can be rescued");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-3F,
                    "Shell low mana rescue should still keep health unchanged");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Shell low mana rescue should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Shell low mana rescue should enter cooldown");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Shell low mana rescue should still apply vanilla-style invulnerability time");
            helper.assertTrue(chestplate.getDamageValue() == 1,
                    "Shell low mana rescue should preserve armor durability loss on the armor path");
        });
    }

    static void manaShieldCharmSynchronizationChargesEnchantReductionBeforeNormalBarrier(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_cost_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(enchantmentLookup.getOrThrow(Enchantments.SYNCHRONIZATION), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);
            var allDamageProtection = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);

            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armorStack = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                armorStack.enchant(allDamageProtection, 4);
                player.setItemSlot(slot, armorStack);
            }

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization cost test could not resolve player mana data");
            magicData.setMana(120.0F);
            var availableMana = magicData.getMana();
            player.invulnerableTime = 0;

            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(helper.getLevel(), player, source);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(5.0F, protection);
            var expectedRemainingMana = resolveExpectedSynchronizationManaAfterHitForGameTest(5.0F, availableMana, protection);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            helper.assertTrue(event.isCanceled(),
                    "Synchronization should cancel the original LivingAttackEvent when it intercepts the hit");
            helper.assertTrue(Math.abs(magicData.getMana() - expectedRemainingMana) < 1.0e-4F,
                    "Synchronization should charge enchant mitigation before the normal barrier stage"
                            + " protection=" + protection
                            + " reducedDamage=" + reducedDamage
                            + " expectedMana=" + expectedRemainingMana
                            + " actualMana=" + magicData.getMana());
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive == (expectedRemainingMana <= 0.0F),
                    "Synchronization cooldown state did not match the remaining mana expectation"
                            + " expectedRemainingMana=" + expectedRemainingMana
                            + " cooldown=" + getManaShieldCharmState(player).cooldownActive);
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationBurnoutStopsAfterEnchantReduction(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_burnout_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(enchantmentLookup.getOrThrow(Enchantments.SYNCHRONIZATION), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);
            var allDamageProtection = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);

            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armorStack = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                armorStack.enchant(allDamageProtection, 4);
                player.setItemSlot(slot, armorStack);
            }

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization burnout test could not resolve player mana data");
            magicData.setMana(20.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var expectedArmor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var expectedToughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(helper.getLevel(), player, source);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            var expectedHealthLoss = CombatRules.getDamageAfterAbsorb(
                    player,
                    CombatRules.getDamageAfterMagicAbsorb(5.0F, protection),
                    source,
                    expectedArmor,
                    expectedToughness
            );

            helper.assertTrue(event.isCanceled(),
                    "Synchronization burnout test should still cancel the original LivingAttackEvent");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Synchronization burnout should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Synchronization burnout should enter cooldown during the enchant-reduction stage");
            helper.assertTrue(Math.abs((initialHealth - player.getHealth()) - expectedHealthLoss) < 1.0e-3F,
                    "Synchronization burnout should stop before the normal barrier stage and leave only enchant-reduced damage"
                            + " actualLoss=" + (initialHealth - player.getHealth())
                            + " expectedLoss=" + expectedHealthLoss
                            + " mana=" + magicData.getMana());
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationLowManaBurnoutStopsAfterEnchantStage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_low_mana_stage_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(enchantmentLookup.getOrThrow(Enchantments.SYNCHRONIZATION), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            equipProtectionIvIronArmor(helper, player);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization low mana enchant-stage test could not resolve player mana data");
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var expectedArmor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var expectedToughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(helper.getLevel(), player, source);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(5.0F, protection);
            var synchronizationSteps = countWholeDamageStepsForGameTest(5.0F - reducedDamage);
            helper.assertTrue(synchronizationSteps > 0,
                    "Synchronization low mana enchant-stage test should require at least one enchant mitigation cost step");
            magicData.setMana(synchronizationSteps * 30.0F - 1.0F);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            var expectedHealthLoss = CombatRules.getDamageAfterAbsorb(
                    player,
                    reducedDamage,
                    source,
                    expectedArmor,
                    expectedToughness
            );

            helper.assertTrue(event.isCanceled(),
                    "Synchronization low mana enchant-stage test should still cancel the original LivingAttackEvent");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Synchronization low mana enchant-stage rescue should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Synchronization low mana enchant-stage rescue should enter cooldown");
            helper.assertTrue(Math.abs((initialHealth - player.getHealth()) - expectedHealthLoss) < 1.0e-3F,
                    "Synchronization low mana enchant-stage rescue should stop before the normal barrier stage"
                            + " actualLoss=" + (initialHealth - player.getHealth())
                            + " expectedLoss=" + expectedHealthLoss
                            + " reducedDamage=" + reducedDamage
                            + " mana=" + magicData.getMana());
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationLowManaBurnoutAfterBarrierStage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_low_mana_barrier_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(enchantmentLookup.getOrThrow(Enchantments.SYNCHRONIZATION), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            equipProtectionIvIronArmor(helper, player);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization low mana barrier-stage test could not resolve player mana data");
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(helper.getLevel(), player, source);
            var incomingDamage = findDamageForMagicReducedTarget(protection, 1.0F);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(incomingDamage, protection);
            var synchronizationSteps = countWholeDamageStepsForGameTest(incomingDamage - reducedDamage);

            helper.assertTrue(Math.abs(reducedDamage - 1.0F) < 1.0e-3F,
                    "Synchronization low mana barrier-stage test should configure exactly one normal barrier step"
                            + " reducedDamage=" + reducedDamage
                            + " incomingDamage=" + incomingDamage);
            helper.assertTrue(synchronizationSteps > 0,
                    "Synchronization low mana barrier-stage test should still require enchant mitigation cost before the barrier");

            magicData.setMana(synchronizationSteps * 30.0F + 24.0F);
            var event = postLivingAttackEventForGameTest(player, source, incomingDamage);

            helper.assertTrue(event.isCanceled(),
                    "Synchronization low mana barrier-stage rescue should still cancel the original LivingAttackEvent");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-3F,
                    "Synchronization low mana barrier-stage rescue should keep health unchanged");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Synchronization low mana barrier-stage rescue should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Synchronization low mana barrier-stage rescue should enter cooldown");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Synchronization low mana barrier-stage rescue should still apply vanilla-style invulnerability time");
            helper.succeed();
        });
    }

    static void manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_neutralization_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(enchantmentLookup.getOrThrow(Enchantments.NEUTRALIZATION), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Neutralization test could not resolve player mana data");
            magicData.setMana(10.0F);
            var state = getManaShieldCharmState(player);
            state.reset();
            state.cooldownActive = true;
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var source = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(helper.getLevel(), player, DamageTypes.UNITE_LUNA);

            var event = postLivingAttackEventForGameTest(player, source, 2.0F);

            helper.assertTrue(event.isCanceled(),
                    "Neutralization should cancel armor-bypass damage even while cooldown is active");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Neutralization should fully negate armor-bypass damage");
            helper.assertTrue(Math.abs(magicData.getMana() - 60.0F) < 1.0e-4F,
                    "Neutralization should recover mana instead of consuming it");
            helper.assertTrue(state.cooldownActive,
                    "Neutralization should not clear cooldown until mana reaches the normal recovery threshold");
            helper.succeed();
        });
    }

    static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var stack = item.getDefaultInstance();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var mutable = ISpellContainer.create(5, false, false).mutableCopy();

            helper.assertTrue(mutable.addSpellAtIndex(apprenticeSpell, 1, 0, false),
                    "Failed to prepare allowlisted sense_evil for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 1, false),
                    "Failed to prepare blocked magic_missile for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(ironsHeal, 1, 2, false),
                    "Failed to prepare allowlisted heal for Autocast Amulet normalization test");
            helper.assertTrue(mutable.addSpellAtIndex(ironsGreaterHeal, 1, 3, false),
                    "Failed to prepare allowlisted greater_heal for Autocast Amulet normalization test");
            ISpellContainer.set(stack, mutable.toImmutable());

            item.normalizeImbuedSpellContainer(stack);

            var normalized = ISpellContainer.get(stack);
            helper.assertTrue(normalized != null, "Autocast Amulet normalized spell container is null");
            helper.assertTrue(normalized != null && normalized.getMaxSpellCount() == 3,
                    "Autocast Amulet normalization should clamp slot count to 3 but got " + (normalized == null ? -1 : normalized.getMaxSpellCount()));
            helper.assertTrue(normalized != null && normalized.getActiveSpellCount() == 3,
                    "Autocast Amulet normalization should keep only 3 allowlisted spells but got " + (normalized == null ? -1 : normalized.getActiveSpellCount()));
            assertSpellData(helper, normalized, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet normalization should preserve the first allowlisted spell");
            assertSpellData(helper, normalized, 1, ironsHeal, 1, false,
                    "Autocast Amulet normalization should compact later allowlisted spells");
            assertSpellData(helper, normalized, 2, ironsGreaterHeal, 1, false,
                    "Autocast Amulet normalization should preserve allowlisted order after filtering");

            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(1) - 1.0D) < 1.0e-9D,
                    "Autocast Amulet single-spell mana multiplier regression");
            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(2) - 1.44D) < 1.0e-9D,
                    "Autocast Amulet two-spell mana multiplier regression");
            helper.assertTrue(Math.abs(AutocastAmulet.getManaMultiplier(3) - 1.96D) < 1.0e-9D,
                    "Autocast Amulet three-spell mana multiplier regression");
            helper.assertTrue(AutocastAmulet.getScaledManaCost(ironsHeal, 1, 3) == 59,
                    "Autocast Amulet scaled mana cost should round heal to 59 at 3 active spells");
        });
    }
    static void autocastAmuletSpellSlotUpgradeStopsAtThreeAndKeepsOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(apprenticeSpell, 1)
            );

            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(), "Autocast Amulet should accept the first lesser spell slot upgrade");
            stack = item.createArcaneAnvilImbueResult(stack, new SpellData(ironsHeal, 1));
            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(), "Autocast Amulet should accept the second lesser spell slot upgrade");
            stack = item.createArcaneAnvilImbueResult(stack, new SpellData(ironsGreaterHeal, 1));

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Autocast Amulet upgraded spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 3,
                    "Autocast Amulet spell slot upgrade should stop at 3 slots");
            assertSpellData(helper, spellContainer, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet slot upgrade should preserve the first spell");
            assertSpellData(helper, spellContainer, 1, ironsHeal, 1, false,
                    "Autocast Amulet slot upgrade should preserve the second spell");
            assertSpellData(helper, spellContainer, 2, ironsGreaterHeal, 1, false,
                    "Autocast Amulet slot upgrade should append the third spell at the tail");
            helper.assertTrue(item.createSpellSlotUpgradeResult(stack, upgradeItem).isEmpty(),
                    "Autocast Amulet should reject a fourth spell slot upgrade");
        });
    }
    static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var apprenticeSpell = SpellRegistry.SENSE_EVIL.get();
            var ironsHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var ironsGreaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    3,
                    new SpellData(apprenticeSpell, 1),
                    new SpellData(ironsHeal, 1),
                    new SpellData(ironsGreaterHeal, 1)
            );
            var spellContainer = ISpellContainer.get(stack);

            helper.assertTrue(spellContainer != null, "Autocast Amulet workbench extraction spell container is null");
            var extractionIndex = item.getWorkbenchSpellExtractionIndex(stack, spellContainer);
            helper.assertTrue(extractionIndex == 2,
                    "Autocast Amulet workbench extraction should target the last filled slot but got " + extractionIndex);
            helper.assertTrue(item.canRemoveWorkbenchSpell(stack, spellContainer, extractionIndex, spellContainer.getSpellAtIndex(extractionIndex)),
                    "Autocast Amulet should allow removing its tail spell in Spellcaster Workbench");

            var mutable = spellContainer.mutableCopy();
            helper.assertTrue(mutable.removeSpellAtIndex(extractionIndex),
                    "Autocast Amulet tail spell should be removable from the mutable container");
            ISpellContainer.set(stack, mutable.toImmutable());
            item.normalizeImbuedSpellContainer(stack);

            var remaining = ISpellContainer.get(stack);
            helper.assertTrue(remaining != null, "Autocast Amulet remaining spell container is null after extraction");
            helper.assertTrue(remaining != null && remaining.getMaxSpellCount() == 3,
                    "Autocast Amulet should preserve max slot count after extraction");
            helper.assertTrue(remaining != null && remaining.getActiveSpellCount() == 2,
                    "Autocast Amulet should keep the first two spells after tail extraction");
            assertSpellData(helper, remaining, 0, apprenticeSpell, 1, false,
                    "Autocast Amulet should keep the first spell after tail extraction");
            assertSpellData(helper, remaining, 1, ironsHeal, 1, false,
                    "Autocast Amulet should keep the second spell after tail extraction");
            helper.assertTrue(remaining != null && remaining.getSpellAtIndex(2) == SpellData.EMPTY,
                    "Autocast Amulet should clear only the tail spell slot after extraction");
        });
    }
    static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_first_interval_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(200.0F);
            equipNecklaceCurio(player, stack);

            runAutocastAmuletServerTick(player, 19);
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet should stay idle before the first 20 tick interval");
            runAutocastAmuletServerTick(player, 20);
            helper.assertTrue(magicData.isCasting(),
                    "Autocast Amulet should start casting charge on the first castable interval after equip");
            helper.assertTrue(spell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Autocast Amulet should start the imbued charge spell on the first castable interval");
            helper.assertTrue(magicData.getCastingSpellLevel() == 1,
                    "Autocast Amulet should cast charge at the imbued spell level");
            helper.succeed();
        });
    }
    static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_mana_retry_test");

        helper.runAtTickTime(1, () -> {
            var expensiveSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var fallbackSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge")
            );
            var expensiveCost = AutocastAmulet.getScaledManaCost(expensiveSpell, 1, 2);
            var fallbackCost = AutocastAmulet.getScaledManaCost(fallbackSpell, 1, 2);
            helper.assertTrue(expensiveCost > fallbackCost,
                    "Autocast Amulet mana retry test requires the first spell to cost more mana than the fallback spell");

            var stack = createAutocastAmuletStack(
                    helper,
                    2,
                    new SpellData(expensiveSpell, 1),
                    new SpellData(fallbackSpell, 1)
            );
            equipNecklaceCurio(player, stack);
            var equippedStack = getEquippedAutocastAmulet(player);

            var magicData = MagicData.getPlayerMagicData(player);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 8.0F));
            magicData.getSyncedData().learnSpell(expensiveSpell, false);
            magicData.getSyncedData().learnSpell(fallbackSpell, false);
            magicData.setMana(fallbackCost);

            runAutocastAmuletServerTick(player, 20);
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet should stop immediately when the first spell lacks mana");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fallbackSpell),
                    "Autocast Amulet should not cast the fallback spell in the blocked mana sequence");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(equippedStack) == 80L,
                    "Autocast Amulet mana retry should wait exactly 60 ticks after the failed sequence");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(equippedStack) == 0,
                    "Autocast Amulet mana retry should skip the errored slot once on the delayed retry");

            runAutocastAmuletServerTick(player, 40);
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet should not retry again before the delayed retry sequence");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(fallbackSpell),
                    "Autocast Amulet should not cast the fallback spell before the delayed retry sequence");

            runAutocastAmuletServerTick(player, 80);
            helper.assertTrue(magicData.isCasting(),
                    "Autocast Amulet delayed retry should skip the errored slot and cast the fallback spell");
            helper.assertTrue(fallbackSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Autocast Amulet delayed retry should cast the next spell after skipping the errored slot");
            helper.assertTrue(magicData.getCastingSpellLevel() == 1,
                    "Autocast Amulet delayed retry should cast charge at the imbued spell level");
            helper.assertTrue(AutocastAmulet.getRetrySequenceTick(equippedStack) < 0L,
                    "Autocast Amulet should clear the delayed retry state after consuming the one-shot skipped sequence");
            helper.assertTrue(AutocastAmulet.getRetrySkipSlot(equippedStack) < 0,
                    "Autocast Amulet should clear the skipped slot marker after the delayed retry sequence");
            helper.succeed();
        });
    }
    static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_creative_mana_test");

        helper.runAtTickTime(1, () -> {
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            equipNecklaceCurio(player, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(0.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            runAutocastAmuletServerTick(player, 20);
            var cooldownInstance = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD,
                    getEquippedAutocastAmulet(player)
            );
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet creative test should still cast greater_heal with zero mana");
            helper.assertFalse(magicData.isCasting(),
                    "Autocast Amulet creative LONG cast should still complete immediately");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Autocast Amulet creative cast should not consume mana but got " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Autocast Amulet creative cast should still add spell cooldown");
            helper.assertTrue(cooldownInstance != null && cooldownInstance.getSpellCooldown() == expectedCooldown,
                    "Autocast Amulet creative cast should store the helper cooldown amount but got "
                            + (cooldownInstance == null ? "null" : cooldownInstance.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }
    static void autocastAmuletCooldownUsesHelperAmountWithoutSwordMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_cooldown_test");
            var stack = new ItemStack(ItemRegistry.AUTOCAST_AMULET.get());
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Autocast Amulet cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD,
                    stack
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Autocast Amulet cooldown event should use the helper cooldown amount but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            var swordCooldownMultiplier = io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CD_MULTIPLIER.get().floatValue();
            if (swordCooldownMultiplier != 1.0F) {
                helper.assertTrue(
                        cooldownEvent.getEffectiveCooldown() != io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                                spell,
                                player,
                                CastSource.SWORD
                        ),
                        "Autocast Amulet cooldown event should diverge from Iron's sword multiplier path when the config multiplier is not 1"
                );
            }
        });
    }
    static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "autocast_amulet_long_test");

        helper.runAtTickTime(1, () -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();
            var stack = createAutocastAmuletStack(
                    helper,
                    1,
                    new SpellData(spell, 1)
            );
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getSyncedData().learnSpell(spell, false);
            magicData.setMana(300.0F);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() - 10.0F));
            var healthBeforeCast = player.getHealth();

            helper.assertTrue(invokeAutocastBeginCast(
                            player,
                            magicData,
                            stack,
                            new SpellData(spell, 1),
                            1,
                            "necklace_0",
                            AutocastAmulet.getScaledManaCost(spell, 1, 1)
                    ),
                    "Autocast Amulet should start greater_heal from the auto-cast path");
            helper.assertTrue(player.getHealth() > healthBeforeCast,
                    "Autocast Amulet should resolve greater_heal immediately from the auto-cast path");
            helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(),
                    "Autocast Amulet LONG cast should complete immediately instead of leaving the player casting");
            helper.succeed();
        });
    }
    static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var icon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");

            controller.queueCooldownCast(100L, spellId, icon, 1300);

            var active = controller.getActiveNotification();
            helper.assertTrue(active != null, "Autocast Amulet notification controller should show the cast notification immediately");
            if (active != null) {
                helper.assertTrue(active.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST,
                        "Autocast Amulet cast notification should use the CAST kind");
                helper.assertTrue(active.displaySeconds() == 65,
                        "Autocast Amulet cast notification should display the rounded cooldown seconds");
                helper.assertTrue("65s".equals(active.displayText()),
                        "Autocast Amulet cast notification text should include the seconds suffix");
            }

            var scheduled = controller.getScheduledNotifications();
            helper.assertTrue(scheduled.size() == 3,
                    "Autocast Amulet 65 second cooldown should schedule 60/30/10 notifications but got " + scheduled.size());
            if (scheduled.size() == 3) {
                helper.assertTrue(scheduled.get(0).triggerTick() == 200L && scheduled.get(0).entry().displaySeconds() == 60,
                        "Autocast Amulet 60 second notification should trigger when 60 seconds remain");
                helper.assertTrue(scheduled.get(1).triggerTick() == 800L && scheduled.get(1).entry().displaySeconds() == 30,
                        "Autocast Amulet 30 second notification should trigger when 30 seconds remain");
                helper.assertTrue(scheduled.get(2).triggerTick() == 1200L && scheduled.get(2).entry().displaySeconds() == 10,
                        "Autocast Amulet 10 second notification should trigger when 10 seconds remain");
            }
        });
    }
    static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge");
            var icon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/charge.png");

            controller.queueCooldownCast(0L, spellId, icon, 500);

            var scheduled = controller.getScheduledNotifications();
            helper.assertTrue(scheduled.size() == 1,
                    "Autocast Amulet 25 second cooldown should only schedule the 10 second notification but got " + scheduled.size());
            if (scheduled.size() == 1) {
                helper.assertTrue(scheduled.get(0).triggerTick() == 300L,
                        "Autocast Amulet 25 second cooldown should trigger the 10 second notification after 15 seconds");
                helper.assertTrue(scheduled.get(0).entry().displaySeconds() == 10,
                        "Autocast Amulet short cooldown should keep the 10 second label");
            }
        });
    }
    static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var controller = new jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController();
            var healId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal");
            var healIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/greater_heal.png");
            var chargeId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge");
            var chargeIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/charge.png");
            var manaLowId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heal");
            var manaLowIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/heal.png");
            var delayedId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath");
            var delayedIcon = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/spells/fire_breath.png");

            controller.queueCooldownCast(0L, healId, healIcon, 1300);
            controller.queueCooldownCast(1L, chargeId, chargeIcon, 800);
            helper.assertTrue(controller.getPendingQueueSize() == 1,
                    "Autocast Amulet overlapping cast notifications should queue instead of drawing together");

            controller.advance(30L);
            var secondCast = controller.getActiveNotification();
            helper.assertTrue(secondCast != null && secondCast.spellId().equals(chargeId),
                    "Autocast Amulet queued cast notification should appear after the first cast display finishes");

            controller.queueManaLow(30L, manaLowId, manaLowIcon);
            controller.advance(60L);
            var manaLow = controller.getActiveNotification();
            helper.assertTrue(manaLow != null
                            && manaLow.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.MANA_LOW
                            && "MP!".equals(manaLow.displayText()),
                    "Autocast Amulet mana-low notification should use the dedicated minimal overlay text");

            controller.queueCooldownCast(85L, delayedId, delayedIcon, 400);
            controller.advance(100L);
            var stillBlockedByQueue = controller.getActiveNotification();
            helper.assertTrue(stillBlockedByQueue != null
                            && stillBlockedByQueue.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.CAST
                            && stillBlockedByQueue.spellId().equals(delayedId),
                    "Autocast Amulet threshold notification should wait until earlier queued notifications finish");

            controller.advance(130L);
            var delayedThreshold = controller.getActiveNotification();
            helper.assertTrue(delayedThreshold != null
                            && delayedThreshold.type() == jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletNotificationController.NotificationType.THRESHOLD
                            && "60s".equals(delayedThreshold.displayText()),
                    "Autocast Amulet delayed threshold notification should keep the original 60 second label");
        });
    }
    static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Spellcaster Gun should stay cleared after save/load");
        });
    }
    static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Swingcast Staff should stay cleared after save/load");
        });
    }
    static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun legacy locked replacement should be recovered after save/load");
        });
    }
    static void ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(helper, stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff legacy locked replacement should be recovered after save/load");
        });
    }
    static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var diamondItem = ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get();
            var diamondStack = new ItemStack(diamondItem);
            assertModifierAmount(
                    helper,
                    diamondItem.getDefaultAttributeModifiers(diamondStack),
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.value(),
                    0.25D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Diamond Spell Amplifier casting move speed bonus regression"
            );

            var netheriteItem = ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get();
            var netheriteStack = new ItemStack(netheriteItem);
            assertModifierAmount(
                    helper,
                    netheriteItem.getDefaultAttributeModifiers(netheriteStack),
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.value(),
                    0.50D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Netherite Spell Amplifier casting move speed bonus regression"
            );
        });
    }
    static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()),
                    "Ender Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "Elemental Bow should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get()),
                    "AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.PHOTON_SIPHON.get()),
                    "Direct AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    "AbstractSpellGunItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    "Crystal Bladed Staff should remain upgradeable after the 1.21.1 StaffItem migration");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get()),
                    "Indirect AbstractRightClickMagicWeaponItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get()),
                    "New swing magic weapon descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                    "Charged Twin Blade Staff should be upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    "Mana Force Blade should be upgradeable via explicit whitelist entry");

            var shieldStack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertFalse(shieldStack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                    "Reflectcast Shield should not be in the upgrade whitelist");
            helper.assertFalse(Utils.canBeUpgraded(shieldStack),
                    "Reflectcast Shield should remain excluded from the upgrade system");
        });
    }
    static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractSpellGunItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Spell Gun");

            for (var stack : stacks) {
                var expectedEnchantments = expectedSpellGunEnchantments(stack);
                assertExactEnchantmentSurfaces(
                        helper,
                        stack,
                        expectedEnchantments,
                        expectedEnchantments,
                        expectedEnchantments,
                        expectedBookEnchantments,
                        expectedEnchantments,
                        "Spell Gun " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                );
            }
        });
    }
    static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractOffhandMagicItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Offhand Magic Item");

            for (var stack : stacks) {
                // Malum の soul_hunter_weapon は main hand 前提なので、
                // offhand 系は 1.21.1 でも従来の enchant 面を維持する前提で固定する。
                assertExactEnchantmentSurfaces(
                        helper,
                        stack,
                        expectedOffhandEnchantments(),
                        expectedOffhandEnchantments(),
                        expectedOffhandEnchantments(),
                        expectedBookEnchantments,
                        expectedOffhandEnchantments(),
                        "Offhand Magic Item " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                    );
            }
        });
    }
    static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat regression test");

            var spellbreakerAttributes = net.bettercombat.logic.WeaponRegistry.getAttributes(new ItemStack(spellbreaker));
            helper.assertTrue(spellbreakerAttributes != null && spellbreakerAttributes.isTwoHanded(),
                    "Better Combat spellbreaker should resolve as a two-handed weapon but got " + spellbreakerAttributes);

            var amplifierStack = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var amplifierEvent = new ItemAttributeModifierEvent(
                    amplifierStack,
                    amplifierStack.getItem().getDefaultAttributeModifiers(amplifierStack)
            );
            NeoForge.EVENT_BUS.post(amplifierEvent);
            var amplifierModifiers = toModifierMultimap(amplifierEvent.build());

            var spellPowerBonus = sumModifierAmount(
                    amplifierModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(spellPowerBonus - 0.05D) < 1.0e-9D,
                    "Iron Spell Amplifier should expose +0.05 spell power in offhand modifiers but got "
                            + spellPowerBonus + " modifiers=" + describeModifiers(amplifierModifiers));
        });
    }
    static void chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ChargedTwinBladeStaff) ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get();
            var stack = new ItemStack(item);
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ATTACK_DAMAGE,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    stack.getItem().getDefaultAttributeModifiers(stack)
            );
            NeoForge.EVENT_BUS.post(event);
            var modifiers = toModifierMultimap(event.build());

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D,
                    "Charged Twin Blade Staff melee damage upgrade should be a single display modifier"
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(modifiers)
            );
        });
    }
    static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get();
            setSingleUnlockedSpell(helper, stack, spell, 1);
            stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.ATTUNEMENT), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            var upgradeKey = findUpgradeKeyForPowerAttribute(attunementAttribute);
            helper.assertTrue(upgradeKey != null,
                    "Mana Force Blade test could not resolve a matching upgrade orb for " + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute));

            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    upgradeKey,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    stack.getItem().getDefaultAttributeModifiers(stack)
            );
            NeoForge.EVENT_BUS.post(event);
            var modifiers = toModifierMultimap(event.build());

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.09D,
                    "Mana Force Blade Attunement and matching upgrade should merge into one display modifier"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + BuiltInRegistries.ATTRIBUTE.getKey(attunementAttribute)
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(modifiers)
            );
        });
    }
    static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            var ironAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            ironAmplifier.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            var rescuedIronModifiers = BetterCombatOffhandAttributeRescueCompat.buildRescueModifiers(ironAmplifier);

            var rescuedSpellPowerBonus = sumModifierAmount(
                    rescuedIronModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(rescuedSpellPowerBonus - 0.07D) < 1.0e-9D,
                    "Better Combat rescue should keep Iron Spell Amplifier + Surge at +0.07 spell power but got "
                            + rescuedSpellPowerBonus + " modifiers=" + describeModifiers(rescuedIronModifiers));

            var copperAmplifier = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            copperAmplifier.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);
            var rescuedCopperModifiers = BetterCombatOffhandAttributeRescueCompat.buildRescueModifiers(copperAmplifier);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(copperAmplifier);
            helper.assertTrue(imbuedSchool != null,
                    "Copper Spell Amplifier rescue test could not resolve imbued school");
            var imbuedSpellPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Copper Spell Amplifier rescue test could not resolve school spell power attribute");

            var rescuedAttunementBonus = sumModifierAmount(
                    rescuedCopperModifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(imbuedSpellPowerAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(rescuedAttunementBonus - 0.14D) < 1.0e-9D,
                    "Better Combat rescue should keep Copper Spell Amplifier base + Attunement at +0.14 but got "
                            + rescuedAttunementBonus + " modifiers=" + describeModifiers(rescuedCopperModifiers));
        });
    }
    static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "better_combat_hidden_offhand_attribute_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker but returned " + player.getOffhandItem());

            var physicalOffhand = BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player);
            helper.assertTrue(
                    physicalOffhand.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "Physical offhand resolver should keep Silver Spell Amplifier but got " + physicalOffhand
            );
            helper.assertTrue(
                    BetterCombatOffhandAttributeRescueCompat.isRescueActive(player),
                    "Better Combat rescue should stay active while physical offhand stack exists"
            );

            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var expectedMaxManaBonus = sumModifierAmount(
                    BetterCombatOffhandAttributeRescueCompat.buildRescueModifiers(physicalOffhand).get(maxManaAttribute),
                    AttributeModifier.Operation.ADD_VALUE
            );
            helper.assertTrue(expectedMaxManaBonus > 0.0D,
                    "Silver Spell Amplifier Better Combat rescue should provide positive max mana but got "
                            + expectedMaxManaBonus);

            var maxManaInstance = player.getAttribute(maxManaAttribute);
            helper.assertTrue(maxManaInstance != null,
                    "FakePlayer is missing max mana attribute for Better Combat rescue test");
            var baseMaxMana = maxManaInstance.getValue();
            BetterCombatOffhandAttributeRescueCompat.sync(player);
            var rescuedMaxMana = maxManaInstance.getValue();
            helper.assertTrue(Math.abs((rescuedMaxMana - baseMaxMana) - expectedMaxManaBonus) < 1.0e-9D,
                    "Better Combat rescue should restore Silver Spell Amplifier max mana by "
                            + expectedMaxManaBonus + " but changed from " + baseMaxMana + " to " + rescuedMaxMana);

            BetterCombatOffhandAttributeRescueCompat.clear(player);
        });
    }
    static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            ).orElse(null);
            helper.assertTrue(spellbreaker != null && spellbreaker != Items.AIR,
                    "Missing irons_spellbooks:spellbreaker for Better Combat spell rescue test");

            var copperAmplifier = createInitializedPresetStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var expectedSpell = ISpellContainer.get(copperAmplifier).getSpellAtIndex(0);
            helper.assertTrue(expectedSpell != SpellData.EMPTY,
                    "Copper Spell Amplifier should expose a fixed offhand spell for Better Combat spell rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    copperAmplifier,
                    "better_combat_hidden_offhand_spell_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker spell rescue but returned "
                            + player.getOffhandItem());

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var offhandSelections = selectionManager.getSpellsForSlot(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND);
            helper.assertTrue(offhandSelections.size() == 1,
                    "Better Combat spell rescue should add exactly one fixed offhand spell but got "
                            + offhandSelections.size() + " selections=" + offhandSelections);

            var rescuedSpell = selectionManager.getSpellForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    0
            );
            helper.assertTrue(
                    rescuedSpell != SpellData.EMPTY
                            && rescuedSpell.getSpell().equals(expectedSpell.getSpell())
                            && rescuedSpell.getLevel() == expectedSpell.getLevel(),
                    "Better Combat spell rescue should restore Copper Spell Amplifier fixed spell "
                            + expectedSpell + " but got " + rescuedSpell
            );
        });
    }
    static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var expectedEnchantments = expectedEnchantedCircletEnchantments();
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    allRegisteredEnchantmentIds(helper.getLevel().registryAccess()),
                    expectedEnchantments,
                    "Enchanted Circlet"
            );
        });
    }
    static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var item = (top.theillusivec4.curios.api.type.capability.ICurioItem) stack.getItem();
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.HEAD,
                    helper.spawn(EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    Attributes.ATTACK_DAMAGE,
                    -0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet attack damage penalty regression"
            );

            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ALACRITY), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.REFLUX), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.RESERVOIR), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.SURGE), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.ATTUNEMENT), 1);
            stack.enchant(enchantmentLookup.getOrThrow(Enchantments.TENSE), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null, "Enchanted Circlet imbued school could not be resolved");

            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Enchanted Circlet could not resolve spell power attribute for Attunement: " + imbuedSchool.getId());

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION,
                    0.02D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Alacrity regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Reflux regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA,
                    20.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Enchanted Circlet Reservoir regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER,
                    0.02D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Surge regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(resolvedSpellPower),
                    0.04D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Attunement regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Enchanted Circlet Tense regression"
            );
        });
    }
    static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Enchanted Circlet should be extractable in Spellcaster Workbench");
            helper.assertFalse(new ItemStack(ItemRegistry.ASHEN_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Ashen Circlet should remain non-extractable in Spellcaster Workbench");
        });
    }
    static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "enchanted_circlet_wisdom_test"));
            var baseExperience = 20;

            var withoutCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0)), player, baseExperience);
            NeoForge.EVENT_BUS.post(withoutCirclet);
            helper.assertTrue(withoutCirclet.getDroppedExperience() == baseExperience,
                    "Wisdom baseline should stay unchanged without enchanted circlet");

            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            circletStack.enchant(enchantmentLookup.getOrThrow(Enchantments.WISDOM), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var withCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0)), player, baseExperience);
            NeoForge.EVENT_BUS.post(withCirclet);
            helper.assertTrue(withCirclet.getDroppedExperience() == 21,
                    "Enchanted Circlet Wisdom should match armor rate (+5% at level 1) but got " + withCirclet.getDroppedExperience());

            var roundedUp = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0)), player, 1);
            NeoForge.EVENT_BUS.post(roundedUp);
            helper.assertTrue(roundedUp.getDroppedExperience() == 2,
                    "Wisdom should round enemy experience up from 1 to 2 at +5% but got " + roundedUp.getDroppedExperience());
        });
    }
    static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            var baselinePlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_baseline_test"));
            var baselineExperience = createBlockDropsExperienceEvent(level, new BlockPos(0, 2, 0), state, baselinePlayer, ItemStack.EMPTY, 3);
            WisdomExperienceDropEvent.onBlockDrops(baselineExperience);
            helper.assertTrue(baselineExperience.getDroppedExperience() == 3,
                    "Block experience should stay unchanged without Wisdom but got " + baselineExperience.getDroppedExperience());

            var curioPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_curio_test"));
            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            circletStack.enchant(enchantmentLookup.getOrThrow(Enchantments.WISDOM), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(curioPlayer)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for block wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var roundedCurioExperience = createBlockDropsExperienceEvent(level, new BlockPos(1, 2, 0), state, curioPlayer, ItemStack.EMPTY, 1);
            WisdomExperienceDropEvent.onBlockDrops(roundedCurioExperience);
            helper.assertTrue(roundedCurioExperience.getDroppedExperience() == 2,
                    "Curio Wisdom should round block experience up from 1 to 2 at +5% but got " + roundedCurioExperience.getDroppedExperience());

            var heldPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_held_test"));
            var spellGunStack = new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
            spellGunStack.enchant(enchantmentLookup.getOrThrow(Enchantments.WISDOM), 1);
            heldPlayer.setItemInHand(InteractionHand.MAIN_HAND, spellGunStack);

            var heldExperience = createBlockDropsExperienceEvent(level, new BlockPos(2, 2, 0), state, heldPlayer, spellGunStack, 3);
            WisdomExperienceDropEvent.onBlockDrops(heldExperience);
            helper.assertTrue(heldExperience.getDroppedExperience() == 4,
                    "Held Wisdom should increase block experience from 3 to 4 at +20% but got " + heldExperience.getDroppedExperience());
        });
    }

    private static BlockDropsEvent createBlockDropsExperienceEvent(ServerLevel level, BlockPos pos, BlockState state, Player breaker, ItemStack tool, int droppedExperience) {
        var event = new BlockDropsEvent(level, pos, state, null, new ArrayList<>(), breaker, tool);
        event.setDroppedExperience(droppedExperience);
        return event;
    }
    static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Right Click Magic Weapon",
                // 1.21.1 では Crystal Bladed Staff が StaffItem 化され、このカテゴリから外れている。
                item -> item instanceof AbstractRightClickMagicWeaponItem,
                stack -> expectedRightClickMagicWeaponEnchantments(helper.getLevel().registryAccess(), stack)
        ));
    }
    static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            assertReferenceItemEnchantmentsWithRequiredExtras(
                    helper,
                    stack,
                    new ItemStack(Items.BOW),
                    requiredElementalBowExtraEnchantments(),
                    "Elemental Bow"
            );
        });
    }
    static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_view_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var regenerationArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.REGENERATION.value());
            var healingId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(healingArrow));
            var regenerationId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(regenerationArrow));
            helper.assertTrue(healingId != null && regenerationId != null,
                    "Elemental Bow selection view test could not resolve tipped arrow potion ids");
            var availablePotionIds = new LinkedHashSet<ResourceLocation>();
            if (healingId != null) {
                availablePotionIds.add(healingId);
            }
            if (regenerationId != null) {
                availablePotionIds.add(regenerationId);
            }
            var expectedPotionOrder = java.util.stream.StreamSupport.stream(BuiltInRegistries.POTION.spliterator(), false)
                    .map(BuiltInRegistries.POTION::getKey)
                    .filter(id -> id != null && availablePotionIds.contains(id))
                    .toList();

            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));
            player.getInventory().setItem(2, healingArrow);
            player.getInventory().setItem(3, regenerationArrow);

            var views = ElementalBow.getAvailableSelectionViews(player, stack);
            var actualSelections = views.stream()
                    .map(ApprenticeCodexGameTestScenarios::describeElementalBowSelectionView)
                    .toList();
            var expectedSelections = new ArrayList<String>();
            expectedSelections.add("normal");
            expectedSelections.add("arrow");
            expectedSelections.add("special:minecraft:spectral_arrow");
            for (var potionId : expectedPotionOrder) {
                expectedSelections.add("special:" + potionId);
            }
            expectedSelections.add("magic:" + SchoolRegistry.FIRE_RESOURCE);
            expectedSelections.add("magic:" + SchoolRegistry.ENDER_RESOURCE);
            expectedSelections.add("magic:" + SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(actualSelections.equals(expectedSelections),
                    "Elemental Bow selection view order mismatch: expected=" + expectedSelections + ", actual=" + actualSelections);
            helper.assertTrue(views.get(0).iconStack().is(Items.BOW),
                    "Elemental Bow vanilla mode selection should render as a bow icon");
            helper.assertTrue(views.get(0).badgeText() == null,
                    "Elemental Bow vanilla mode selection should not show an ammo badge");
            helper.assertTrue(views.get(1).iconStack().is(Items.ARROW),
                    "Elemental Bow arrow-only selection should render as an arrow icon");
            helper.assertTrue("∞".equals(views.get(1).badgeText()),
                    "Elemental Bow arrow-only selection should show infinity while Infinity is enchanted: " + views.get(1).badgeText());

            var fireView = views.stream()
                    .filter(view -> "magic".equals(view.selection().shotMode()) && SchoolRegistry.FIRE_RESOURCE.equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(fireView != null, "Elemental Bow selection view should include Fire magic");
            if (fireView != null) {
                helper.assertTrue(fireView.iconKind() == ElementalBow.SelectionIconKind.SPELL,
                        "Elemental Bow magic selection should render as a spell icon");
                helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get().getSpellIconResource().equals(fireView.spellIcon()),
                        "Elemental Bow Fire magic selection should use the Fire Arrow spell icon");
            }
        });
    }
    static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(ElementalBow.getInventoryOverlayView(stack) == null,
                    "Elemental Bow normal mode should not expose an inventory overlay");

            setElementalBowShotSelection(stack, "arrow", null);
            var arrowOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(arrowOverlay != null,
                    "Elemental Bow arrow-only selection should expose an inventory overlay");
            if (arrowOverlay != null) {
                helper.assertTrue(arrowOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow arrow-only selection should render as an item overlay");
                helper.assertTrue(arrowOverlay.iconStack().is(Items.ARROW),
                        "Elemental Bow arrow-only selection should render the arrow icon");
            }

            var spectralArrowId = ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow");
            setElementalBowShotSelection(stack, "special", spectralArrowId);
            var spectralOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(spectralOverlay != null,
                    "Elemental Bow spectral selection should expose an inventory overlay");
            if (spectralOverlay != null) {
                helper.assertTrue(spectralOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow spectral selection should render as an item overlay");
                helper.assertTrue(spectralOverlay.iconStack().is(Items.SPECTRAL_ARROW),
                        "Elemental Bow spectral selection should render the spectral arrow icon");
            }

            var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.HEALING.value());
            var healingId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(healingArrow));
            helper.assertTrue(healingId != null,
                    "Elemental Bow overlay test could not resolve the healing arrow potion id");
            if (healingId != null) {
                setElementalBowShotSelection(stack, "special", healingId);
                var tippedOverlay = ElementalBow.getInventoryOverlayView(stack);
                helper.assertTrue(tippedOverlay != null,
                        "Elemental Bow tipped arrow selection should expose an inventory overlay");
                if (tippedOverlay != null) {
                    helper.assertTrue(tippedOverlay.iconStack().is(Items.TIPPED_ARROW),
                            "Elemental Bow tipped arrow selection should render a tipped arrow icon");
                    helper.assertTrue(PotionContentsHelper.getPotion(tippedOverlay.iconStack()) == net.minecraft.world.item.alchemy.Potions.HEALING.value(),
                            "Elemental Bow tipped arrow overlay should keep the selected potion");
                }
            }

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            var fireOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(fireOverlay != null,
                    "Elemental Bow magic selection should expose an inventory overlay");
            if (fireOverlay != null) {
                helper.assertTrue(fireOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow magic selection should render as an item overlay");
                helper.assertTrue(fireOverlay.iconStack().is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                        "Elemental Bow Fire mode should render the Fire rune icon");
            }
        });
    }
    static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_overheat_overlay_test");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        setElementalBowMode(stack, "fire");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        helper.runAtTickTime(1, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null, "Elemental Bow overheat overlay test should expose the Fire magic selection");
            if (fireView != null) {
                helper.assertFalse(fireView.overheatActive(),
                        "Elemental Bow Fire selection should not be overheated before any cast");
                helper.assertTrue(fireView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Fire selection should start with an empty overheat overlay");
            }

            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    40
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.NATURE_RESOURCE,
                    20
            );

            var overheatedFireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(overheatedFireView != null && overheatedFireView.overheatActive(),
                    "Elemental Bow Fire selection should report active overheat immediately after cast");
            if (overheatedFireView != null) {
                helper.assertTrue(overheatedFireView.overheatFillRatio() == 1.0F,
                        "Elemental Bow Fire selection should start with a full overheat overlay: " + overheatedFireView.overheatFillRatio());
            }
        });

        helper.runAtTickTime(11, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null && fireView.overheatActive(),
                    "Elemental Bow Fire selection should still be overheated mid-cooldown");
            if (fireView != null) {
                helper.assertTrue(Mth.equal(fireView.overheatFillRatio(), 0.75F),
                        "Elemental Bow Fire selection should decay based on its own cooldown: " + fireView.overheatFillRatio());
            }

            var natureView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(natureView != null && natureView.overheatActive(),
                    "Elemental Bow Nature selection should track its own overheat independently");
            if (natureView != null) {
                helper.assertTrue(Mth.equal(natureView.overheatFillRatio(), 0.5F),
                        "Elemental Bow Nature selection should show its shorter cooldown independently: " + natureView.overheatFillRatio());
            }

            var enderView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.ENDER_RESOURCE);
            helper.assertTrue(enderView != null, "Elemental Bow overheat overlay test should expose the Ender magic selection");
            if (enderView != null) {
                helper.assertFalse(enderView.overheatActive(),
                        "Elemental Bow Ender selection should stay inactive when untouched");
                helper.assertTrue(enderView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Ender selection should not show an overheat overlay");
            }
        });

        helper.runAtTickTime(42, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null, "Elemental Bow Fire selection should remain in the selection list after cooldown");
            if (fireView != null) {
                helper.assertFalse(fireView.overheatActive(),
                        "Elemental Bow Fire selection should clear overheat after cooldown expires");
                helper.assertTrue(fireView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Fire selection overlay should be empty after cooldown expires");
            }
        });

        helper.runAtTickTime(43, helper::succeed);
    }
    static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_mana_gate_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow mana gate test could not resolve player mana data");
            magicData.setMana(0.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow should fail to start drawing when mana is insufficient: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow should not enter use state without enough mana");
        });
    }
    static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_partial_release_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow partial release test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Elemental Bow should start drawing when mana and ammo are available: " + useResult.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 19);
            helper.assertTrue(stack.getDamageValue() == 0, "Elemental Bow should not lose durability before full draw");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should not consume arrows before full draw");
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Elemental Bow should not consume mana before full draw: " + magicData.getMana());
        });
    }
    static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start vanilla draw with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow should enter use state for Infinity vanilla draw");
        });
    }
    static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow arrow-only mode should enter use state for Infinity draw");
        });
    }
    static void elementalBowVanillaModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_vanilla_infinity_preserve_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with Infinity while normal arrows exist: " + result.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow vanilla mode should not consume normal arrows while Infinity is enchanted");
        });
    }
    static void elementalBowArrowModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_preserve_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity while normal arrows exist: " + result.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow arrow-only mode should not consume normal arrows while Infinity is enchanted");
        });
    }
    static void elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_special_arrow_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing while the selected arrow exists: " + firstUse.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow special mode should consume the selected arrow even with Infinity");

            var secondUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(secondUse.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow special mode should fail after the selected arrow runs out: " + secondUse.getResult());
            assertElementalBowSelection(helper, stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"),
                    "Elemental Bow special mode should keep the selected arrow after ammo loss");
        });
    }
    static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow magic mode should fail to start without ammo even with Infinity: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow magic mode should not enter use state without ammo");
        });
    }
    static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var synthesis = enchantmentLookup.getOrThrow(Enchantments.SYNTHESIS);
            var infinity = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var mending = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());

            helper.assertTrue(stack.getItem().supportsEnchantment(stack, synthesis),
                    "Elemental Bow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(synthesis)),
                    "Elemental Bow should accept Synthesis from enchanted books");
            helper.assertTrue(synthesis.value().canEnchant(stack),
                    "Elemental Bow should be included in the Synthesis supported_items tag");
            helper.assertFalse(Enchantment.areCompatible(synthesis, infinity),
                    "Synthesis should be incompatible with Infinity");
            helper.assertFalse(Enchantment.areCompatible(synthesis, mending),
                    "Synthesis should be incompatible with Mending");

            stack.enchant(synthesis, 1);
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, tooltipLines, TooltipFlag.Default.NORMAL);
            var hasSynthesisTooltip = tooltipLines.stream()
                    .anyMatch(component -> component.getContents() instanceof TranslatableContents translatableContents
                            && "item.apprenticecodex.elemental_bow.with_synthesis".equals(translatableContents.getKey()));
            helper.assertTrue(hasSynthesisTooltip,
                    "Elemental Bow should add the Synthesis tooltip line while Synthesis is enchanted");
        });
    }
    static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var synthesis = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_empty_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(synthesis, 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start without arrows when Synthesis is enchanted: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(stack.getDamageValue() == 1,
                    "Elemental Bow Synthesis magic shot should still damage the bow after a successful cast");
            helper.assertTrue(magicData.getMana() < initialMana,
                    "Elemental Bow Synthesis magic shot should still consume spell mana");
        });
    }
    static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var synthesis = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_ammo_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(synthesis, 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis ammo test could not resolve player mana data");
            magicData.setMana(250.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start with Synthesis while arrows are present: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow Synthesis magic shot should not consume arrows even when arrows are available");
        });
    }
    static void spellcasterQuiverUsesBackSlotAndCapsStoredArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            helper.assertTrue(quiverStack.is(CURIOS_BACK),
                    "Spellcaster Quiver should be tagged for the Curios back slot");

            var firstInsert = SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 300));
            var secondInsert = SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 300));
            helper.assertTrue(firstInsert == 300, "Spellcaster Quiver should store the full first stack");
            helper.assertTrue(secondInsert == 212,
                    "Spellcaster Quiver should stop at 512 arrows but inserted " + secondInsert);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 512,
                    "Spellcaster Quiver should cap total storage at 512");

            var removalOrderQuiver = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(removalOrderQuiver, new ItemStack(Items.ARROW, 32));
            SpellcasterQuiver.store(removalOrderQuiver, new ItemStack(Items.SPECTRAL_ARROW, 7));
            var removed = SpellcasterQuiver.removeOneStack(removalOrderQuiver);
            helper.assertTrue(removed.is(Items.SPECTRAL_ARROW) && removed.getCount() == 7,
                    "Spellcaster Quiver should remove the smallest stored arrow stack first");
        });
    }
    static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_pickup_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var itemEntity = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.ARROW, 12));
            SpellcasterQuiverPickupEvent.onEntityItemPickup(new ItemEntityPickupEvent.Pre(player, itemEntity));

            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 12,
                    "Equipped Spellcaster Quiver should auto-store picked up arrows");
            helper.assertTrue(itemEntity.isRemoved(),
                    "Spellcaster Quiver pickup handling should finish the ItemEntity when all arrows were stored");
        });
    }
    static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_quiver_priority_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(bowStack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start drawing when only the equipped Spellcaster Quiver provides arrows");

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 4,
                    "Elemental Bow should consume the equipped Spellcaster Quiver arrow first");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should leave loose inventory arrows untouched while the quiver has arrows");
        });
    }
    static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_quiver_selection_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 3));
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var normalView = findElementalBowSelectionView(player, bowStack, "arrow", null);
            helper.assertTrue(normalView != null, "Elemental Bow should expose normal arrow selection");
            helper.assertTrue(normalView != null && "3".equals(normalView.badgeText()),
                    "Elemental Bow selection badge should count normal arrows stored in Spellcaster Quiver");

            var view = findElementalBowSelectionView(player, bowStack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));
            helper.assertTrue(view != null, "Elemental Bow should expose spectral arrow selection from Spellcaster Quiver contents");
            helper.assertTrue(view != null && "2".equals(view.badgeText()),
                    "Elemental Bow selection badge should count Spellcaster Quiver arrows");
        });
    }
    static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_quiver_priority_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when Spellcaster Quiver provides arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 4,
                    "Vanilla Bow should consume the Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Vanilla Bow should leave loose inventory arrows untouched while the quiver has arrows");
        });
    }
    static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_held_special_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when only the held special arrow should be selected: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Vanilla Bow should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Vanilla Bow should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }
    static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_normal_priority_test");
            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing when normal arrows exist outside the quiver: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Vanilla Bow should consume the lone normal arrow before more numerous Spellcaster Quiver special arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow should not consume Spellcaster Quiver special arrows while a normal arrow existed");
        });
    }
    static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_infinity_quiver_test");
            var bowStack = new ItemStack(Items.BOW);
            bowStack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow Infinity fallback should stop at normal arrow mode and leave Spellcaster Quiver special arrows untouched");
        });
    }
    static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_held_special_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 1));

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 5));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing when a held special arrow exists: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Elemental Bow vanilla mode should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Elemental Bow vanilla mode should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }
    static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            bowStack.enchant(infinity, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player) - 20);
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Elemental Bow vanilla mode Infinity fallback should leave Spellcaster Quiver special arrows untouched");
        });
    }
    static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_slowdown_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var bowStack = new ItemStack(Items.BOW);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));

            bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should activate while a bow is being drawn");

            player.stopUsingItem();
            helper.assertFalse(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should stop once bow use ends");
        });
    }
    static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_quiver_focus_staffbow_slowdown_test");
            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.startUsingItem(InteractionHand.MAIN_HAND);

            helper.assertTrue(FocusStaffbow.isBowDrawUse(player),
                    "Focus Staffbow draw helper should activate while the item is being held");
            helper.assertTrue(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should activate while Focus Staffbow is being drawn");

            player.stopUsingItem();
            helper.assertFalse(FocusStaffbow.isBowDrawUse(player),
                    "Focus Staffbow draw helper should stop once use ends");
            helper.assertFalse(SpellcasterQuiver.shouldIgnoreBowSlowdown(player),
                    "Spellcaster Quiver slowdown helper should stop once Focus Staffbow use ends");
        });
    }
    static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));

            item.initializeSpellContainer(stack);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should not expose a spell container outside magic mode");
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack) == null,
                    "Elemental Bow should not expose a displayed spell profile outside magic mode");
        });
    }
    static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_empty_selection_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));

            var selectedViews = ElementalBow.getAvailableSelectionViews(player, stack);
            var spectralView = selectedViews.stream()
                    .filter(view -> "special".equals(view.selection().shotMode())
                            && ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow").equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(spectralView != null, "Elemental Bow should keep the empty current special selection in the UI");
            if (spectralView != null) {
                helper.assertTrue("0".equals(spectralView.badgeText()),
                        "Elemental Bow empty current special selection should show 0 ammo");
                helper.assertTrue(spectralView.badgeColor() == 0xFF5555,
                        "Elemental Bow empty current special selection should render its ammo count in red");
            }

            setElementalBowShotSelection(stack, "normal", null);
            var normalViews = ElementalBow.getAvailableSelectionViews(player, stack);
            helper.assertTrue(normalViews.stream().noneMatch(view ->
                            "special".equals(view.selection().shotMode())
                                    && ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow").equals(view.selection().selectionId())),
                    "Elemental Bow should drop the empty special selection after another mode is chosen");
            var arrowView = normalViews.stream()
                    .filter(view -> "arrow".equals(view.selection().shotMode()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(arrowView != null, "Elemental Bow should always expose the arrow-only selection");
            if (arrowView != null) {
                helper.assertTrue("0".equals(arrowView.badgeText()),
                        "Elemental Bow arrow-only selection should show 0 ammo while empty");
                helper.assertTrue(arrowView.badgeColor() == 0xFF5555,
                        "Elemental Bow arrow-only selection should render empty ammo in red even while another mode is selected");
            }
        });
    }
    static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_vanilla_special_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with only special arrows available: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow vanilla mode should consume the special arrow that vanilla resolution selected");
        });
    }
    static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_only_mode_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow arrow-only mode should fail when only special arrows are available: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow arrow-only mode should not enter use state without normal arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Elemental Bow arrow-only mode should not consume special arrows");
            assertElementalBowSelection(helper, stack, "arrow", null,
                    "Elemental Bow arrow-only mode should keep its selection while empty");
        });
    }
    static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_offhand_reject_test");
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when used from offhand but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when offhand use is rejected");
        });
    }
    static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_offhand_selection_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow offhand selection test could not resolve player mana data");
            if (magicData != null) {
                magicData.setMana(100.0F);
            }

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var selection = selectionManager.getSelection();
            helper.assertTrue(selection != null
                            && io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(selection.slot),
                    "Focus Staffbow offhand selection test should resolve offhand spell selection but got " + selection);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() != net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow mainhand use should remain available even when selected spell slot is offhand but got "
                            + result.getResult());
        });
    }
    static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_pending_summon_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should enter pending cast for LONG summon spells but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should keep a pending cast state while charging");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).size() == 1,
                    "Focus Staffbow should expose the summon weapon during pending charge");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should not consume its catalyst arrow before the LONG cast completes");
            helper.assertTrue(player.isUsingItem(), "Focus Staffbow should still be in use while the summon weapon is pending");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow pending state should clear after the charged cast completes");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow charged cast should clear simulated additional cast data after completion");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow should consume exactly one catalyst arrow after the LONG cast completes");
        });
    }
    static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_pending_cancel_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow cancel test should start a pending cast but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                helper.assertTrue(
                        getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).size() == 1,
                        "Focus Staffbow cancel test should spawn the summon weapon during pending charge"
                )
        );
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - (jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player) - 1)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow cancel test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should clear the pending state when released before the required charge");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity.class).isEmpty(),
                    "Focus Staffbow should remove the simulated summon weapon when the charge is cancelled");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow should clear simulated additional cast data when the charge is cancelled");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should keep its catalyst arrow when the LONG cast is cancelled early");
        });
    }
    static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_helper_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var cooldownAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION);
            helper.assertTrue(cooldownAttribute != null, "Elemental Bow cooldown helper test could not resolve cooldown attribute");
            cooldownAttribute.addPermanentModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "elemental_bow_cooldown_helper_test"),
                    0.35D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

            var expectedCooldown = (int) (fireArrow.getSpellCooldown() * (2 - Utils.softCapFormula(
                    player.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION)
            )));
            var helperCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD,
                    stack
            );
            helper.assertTrue(helperCooldown == expectedCooldown,
                    "Elemental Bow cooldown helper should keep player cooldown reduction but ignore sword multiplier: "
                            + helperCooldown + " / expected " + expectedCooldown);

            var vanillaCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            var fallbackCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD,
                    ItemStack.EMPTY
            );
            helper.assertTrue(fallbackCooldown == vanillaCooldown,
                    "Non-opt-in cooldown helper path should match Iron's default cooldown calculation");

            var swordCooldownMultiplier = io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CD_MULTIPLIER.get().floatValue();
            if (swordCooldownMultiplier != 1.0F) {
                helper.assertTrue(helperCooldown != vanillaCooldown,
                        "Elemental Bow cooldown helper should diverge from the sword multiplier path when the config multiplier is not 1");
            }
        });
    }
    static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_mana_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow overheat mana test could not resolve player mana data");

            var item = (ElementalBow) stack.getItem();
            item.initializeSpellContainer(stack);
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow overheat mana test should resolve the active Fire profile");
            var fireArrow = fireProfile != null
                    ? fireProfile.spell()
                    : io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var baseMana = fireProfile != null ? fireProfile.spell().getManaCost(fireProfile.spellLevel()) : fireArrow.getManaCost(1);

            magicData.setMana(300.0F);
            var initialMana = magicData.getMana();

            magicData.setPlayerCastingItem(stack.copy());
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(cooldownEvent);
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            fireArrow.getSpellCooldown()
                    )
            );

            var extraMana = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    baseMana
            );
            helper.assertTrue(extraMana > 0.0F, "Elemental Bow should charge extra mana once Fire overheat is active");

            var overheatedUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(overheatedUseResult.getResult().consumesAction(),
                    "Elemental Bow should still allow a second overheated draw: " + overheatedUseResult.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            var manaAfterOverheatedShot = magicData.getMana();
            helper.assertTrue(Math.abs(manaAfterOverheatedShot - (initialMana - baseMana - extraMana)) < 1.0e-3F,
                    "Elemental Bow overheated shot consumed the wrong mana: " + manaAfterOverheatedShot);
            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(state.active() && state.chainDepth() >= 2,
                    "Elemental Bow overheated shot should keep Fire overheat active and deepen the chain: " + state);
        });
    }
    static void elementalBowOverheatTracksSchoolsSeparately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_school_test");
            var fireStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(fireStack, "magic", SchoolRegistry.FIRE_RESOURCE);

            var natureStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(natureStack, "magic", SchoolRegistry.NATURE_RESOURCE);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow school overheat test could not resolve player mana data");

            magicData.setPlayerCastingItem(fireStack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            160,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            0
                    )
            );

            magicData.setPlayerCastingItem(natureStack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            120,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.POISON_ARROW_SPELL.get(),
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.NATURE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.NATURE_RESOURCE,
                            0
                    )
            );

            var fireState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireState.active() && fireState.chainDepth() == 1,
                    "Elemental Bow fire overheat should stay isolated at depth 1: " + fireState);

            var natureState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(natureState.active() && natureState.chainDepth() == 1,
                    "Elemental Bow nature overheat should stay isolated at depth 1: " + natureState);

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                            player,
                            SchoolRegistry.ENDER_RESOURCE,
                            10.0F
                    ) == 0.0F,
                    "Elemental Bow should not leak overheat into untouched schools"
            );
        });
    }
    static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_refresh_test");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
        var magicData = MagicData.getPlayerMagicData(player);
        var firstExpire = new java.util.concurrent.atomic.AtomicLong();
        var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
        var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                fireArrow,
                player,
                CastSource.SWORD,
                stack
        );

        helper.assertTrue(magicData != null, "Elemental Bow overheat refresh test could not resolve player mana data");

        helper.runAtTickTime(1, () -> {
            magicData.setPlayerCastingItem(stack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                            fireArrow,
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            expectedCooldown
                    )
            );
            firstExpire.set(jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE).expireGameTime());
        });

        helper.runAtTickTime(40, () -> {
            magicData.setPlayerCastingItem(stack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                            fireArrow,
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            expectedCooldown
                    )
            );

            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(state.active(), "Elemental Bow repeated cast should keep fire overheat active");
            helper.assertTrue(state.chainDepth() == 2, "Elemental Bow repeated cast should raise overheat chain depth to 2: " + state.chainDepth());
            helper.assertTrue(state.expireGameTime() > firstExpire.get(),
                    "Elemental Bow repeated cast should refresh overheat expiry but got " + state.expireGameTime() + " <= " + firstExpire.get());
            helper.assertTrue(state.expireGameTime() - helper.getLevel().getGameTime() == expectedCooldown,
                    "Elemental Bow repeated cast should reset overheat duration from the latest cast");
        });

        helper.runAtTickTime(41, helper::succeed);
    }
    static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_hold_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(10000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous test should store a CONTINUOUS cast state");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous test should keep Iron's casting state active after start");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should consume one catalyst arrow as soon as casting starts");
            helper.assertTrue(player.isUsingItem(),
                    "Focus Staffbow continuous test should keep the player in use state while held");
        });
        helper.runAtTickTime(3, () -> {
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous multiplier test could not resolve spell power attribute");
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            helper.assertTrue(modifier != null && modifier.amount() > 0.0D,
                    "Focus Staffbow continuous multiplier should start rising immediately after cast start");
        });
        helper.runAtTickTime(101, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous duration test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should stay active past the spell's normal duration cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep Iron's casting state active past the normal duration cap");
            helper.assertTrue(magicData.getCastDurationRemaining() < 10,
                    "Focus Staffbow continuous cast should have passed Iron's normal remaining-duration stop window: " + magicData.getCastDurationRemaining());
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous midpoint test could not resolve spell power attribute");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var expectedMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(
                    continuousState.getElapsedTicks(player.level().getGameTime())
            );
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.amount();
            helper.assertTrue(Math.abs(actualAmount - (expectedMultiplier - 1.0D)) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should match the fixed early-stage curve: " + actualAmount);
            helper.assertTrue(Math.abs(expectedMultiplier - 1.5D) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should reach 1.5x after 100 ticks: " + expectedMultiplier);
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow continuous cast should not keep consuming arrows while the button stays held");
        });
        helper.runAtTickTime(251, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous cap test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous cast should remain active after reaching the 2x cap");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous cast should keep running after reaching the 2x cap while mana remains");
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous cap test could not resolve spell power attribute");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            var expectedMultiplier = jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.computeContinuousChargeMultiplier(
                    continuousState.getElapsedTicks(player.level().getGameTime())
            );
            AttributeModifier modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.amount();
            helper.assertTrue(Math.abs(expectedMultiplier - 2.0D) < 1.0e-9D,
                    "Focus Staffbow continuous multiplier should cap at 2.0x after 250 ticks: " + expectedMultiplier);
            helper.assertTrue(Math.abs(actualAmount - 1.0D) < 1.0e-9D,
                    "Focus Staffbow continuous spell power bonus should stop at +100%: " + actualAmount);
        });
        helper.runAtTickTime(252, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 251
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous release test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous cast state should clear after releasing the button");
            helper.assertFalse(magicData.isCasting(),
                    "Focus Staffbow continuous release should clear Iron's casting state");
        });
    }
    static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_arrow_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should fail immediately when no catalyst arrow is available but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state without a catalyst arrow");
        });
    }
    static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(15.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous mana test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous mana test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous mana test should start with a CONTINUOUS cast state");
            helper.assertTrue(magicData.isCasting(),
                    "Focus Staffbow continuous mana test should still be casting immediately after start");
        });
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous mana stop test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous cast should stop once it cannot pay the next tick's mana cost");
            helper.assertFalse(magicData.isCasting(),
                    "Focus Staffbow continuous mana stop should clear Iron's casting state");
            helper.assertTrue(magicData.getMana() >= 0.0F,
                    "Focus Staffbow continuous mana stop should not drive mana below zero: " + magicData.getMana());
            helper.assertTrue(magicData.getMana() <= 15.0F,
                    "Focus Staffbow continuous mana stop consumed an unexpected amount of mana: " + magicData.getMana());
        });
    }
    static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_standard_time_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(300.0F);

        var castTimeReductionAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION);
        helper.assertTrue(castTimeReductionAttribute != null,
                "Focus Staffbow continuous standard time test could not resolve cast time reduction attribute");
        if (castTimeReductionAttribute != null) {
            castTimeReductionAttribute.addPermanentModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "focus_staffbow_continuous_standard_time_test"),
                    0.75D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow continuous standard time test should start casting but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous standard time test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isContinuous(),
                    "Focus Staffbow continuous standard time test should store a CONTINUOUS cast state");
            var continuousState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
            helper.assertTrue(continuousState.requiredCastTicks == spell.getCastTime(1),
                    "Focus Staffbow continuous standard time should ignore cast-time attributes and use the spell's base castTime");
            helper.assertTrue(magicData.getCastDuration() == spell.getCastTime(1),
                    "Focus Staffbow continuous standard time should sync Iron's cast duration with the base castTime");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 2
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow continuous standard time release test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow continuous standard time test should clear after release");
        });
    }
    static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_instant_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow instant base mana test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost + 40.0F);
        var initialMana = magicData.getMana();

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow instant test should start charging immediately but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow instant immediate release should only consume base mana: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow instant cast should consume one catalyst arrow on release");
        });
    }
    static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_short_long_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow short LONG base mana test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost + 60.0F);
        var initialMana = magicData.getMana();

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow short LONG test should enter pending charge but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow short LONG test lost spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).chargeBaselineTicks
                            == jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeLogic.MINIMUM_OVERCHARGE_BASELINE_TICKS,
                    "Focus Staffbow short LONG test should clamp the overcharge baseline to one second");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - spell.getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - baseManaCost)) < 1.0e-4F,
                    "Focus Staffbow short LONG release should stay at base mana within the first second: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow short LONG cast should still consume only one catalyst arrow after completion");
        });
    }
    static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_base_mana_gate_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Focus Staffbow mana gate test could not resolve player mana data");
            magicData.setMana(spell.getManaCost(1) - 1.0F);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should still fail immediately when base mana is insufficient but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state when even base mana is missing");
        });
    }
    static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_repay_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow loan test could not resolve player mana data");
        var baseManaCost = spell.getManaCost(1);
        magicData.setMana(baseManaCost);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow loan test should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan test lost spell data capability before release");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).requiredCastTicks == 0,
                    "Focus Staffbow loan test should treat INSTANT spells as zero required cast ticks");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 120
                )
        );
        helper.runAtTickTime(4, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan test lost spell data capability after cast");
            var loanState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
            helper.assertTrue(loanState.remainingLoanMana >= 119.0F,
                    "Focus Staffbow loan test should create roughly eight base-cost worth of debt at x3 but got "
                            + loanState.remainingLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan test should leave current mana at zero after borrowed cast: " + magicData.getMana());
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow borrowed cast should still consume exactly one catalyst arrow");
            magicData.setMana(10.0F);
            jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager.tickLoanRepayment(player);
            helper.assertTrue(loanState.remainingLoanMana >= 109.0F && loanState.remainingLoanMana <= 111.0F,
                    "Focus Staffbow loan repay test should consume recovered mana into the debt first but got "
                            + loanState.remainingLoanMana);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Focus Staffbow loan repay test should keep displayed mana at zero while debt remains: " + magicData.getMana());
            helper.succeed();
        });
    }
    static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_creative_overcharge_test");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Focus Staffbow creative overcharge test could not resolve player mana data");
        magicData.setMana(17.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow creative overcharge test should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - 120
                )
        );
        helper.runAtTickTime(4, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow creative overcharge test lost spell data capability");
            var loanState = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE);
            helper.assertFalse(loanState.hasOutstandingLoan(),
                    "Focus Staffbow creative overcharge test should not create loan mana");
            helper.assertTrue(Math.abs(magicData.getMana() - 17.0F) < 1.0e-4F,
                    "Focus Staffbow creative overcharge test should leave mana unchanged but got " + magicData.getMana());
            helper.succeed();
        });
    }
    static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_loan_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow loan block test could not resolve spell data capability");
            if (spellData != null) {
                spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE, state -> state.addLoan(7.0F));
            }

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject new casts while borrowed mana remains but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not remain in use state while a loan blocks casting");
        });
    }
    static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_cooldown_block_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
            MagicData.getPlayerMagicData(player).setMana(200.0F);
            var selection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null, "Focus Staffbow cooldown test could not resolve the selected spell");
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Focus Staffbow should reject use while the selected spell is on cooldown but got " + result.getResult());
            helper.assertFalse(player.isUsingItem(),
                    "Focus Staffbow should not enter use state while spell cooldown blocks casting");
        });
    }
    static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = jp.aquafactory.apprenticecodex.item.FocusStaffbow.createLoanBlockedMessage(5.1F);
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.loan_mana",
                    "Focus Staffbow loan block message should use the dedicated translation key"
            );
        });
    }
    static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = jp.aquafactory.apprenticecodex.item.FocusStaffbow.createInsufficientArrowMessage();
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.insufficient_arrow",
                    "Focus Staffbow insufficient arrow message should use the dedicated translation key"
            );
        });
    }
    static void focusStaffbowFallsBackToSpecialArrowWhenNormalArrowIsMissing(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_special_arrow_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start when only a special arrow is available but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() ->
                helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                        "Focus Staffbow should consume the special arrow when normal arrows are unavailable")
        );
    }
    static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_synthesis_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        bowStack.enchant(
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SYNTHESIS),
                1
        );
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start without arrows when Synthesis is enchanted but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player)
                )
        );
        helper.succeedWhen(() ->
                helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                        "Focus Staffbow Synthesis path should not require or consume a catalyst arrow")
        );
    }
    static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_quiver_priority_test");
            var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            setSingleUnlockedSpell(helper, amplifierStack, jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get(), 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 2));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should start when Spellcaster Quiver holds the catalyst arrow but got " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration(player));
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 1,
                    "Focus Staffbow should consume the equipped Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Focus Staffbow should leave loose inventory arrows untouched while the quiver still has arrows");
        });
    }
    static void focusStaffbowAcceptsSynthesisEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var synthesis = enchantmentLookup.getOrThrow(Enchantments.SYNTHESIS);
            var infinity = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            var transcendence = enchantmentLookup.getOrThrow(Enchantments.TRANSCENDENCE);
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var item = (FocusStaffbow) stack.getItem();

            helper.assertTrue(stack.getItem().supportsEnchantment(stack, synthesis),
                    "Focus Staffbow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(synthesis)),
                    "Focus Staffbow should accept Synthesis from enchanted books");
            helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, synthesis),
                    "Focus Staffbow should allow Synthesis through anvil merges");

            helper.assertFalse(stack.getItem().supportsEnchantment(stack, infinity),
                    "Focus Staffbow should reject Infinity at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(infinity)),
                    "Focus Staffbow should reject Infinity from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, infinity),
                    "Focus Staffbow should reject Infinity through anvil merges");

            helper.assertFalse(stack.getItem().supportsEnchantment(stack, transcendence),
                    "Focus Staffbow should reject Transcendence at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(transcendence)),
                    "Focus Staffbow should reject Transcendence from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, transcendence),
                    "Focus Staffbow should reject Transcendence through anvil merges");

            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = enchantmentLookup.get(ResourceKey.create(Registries.ENCHANTMENT, MALUM_HAUNTED)).orElse(null);
            helper.assertTrue(haunted != null, "Missing malum:haunted enchantment");
            if (haunted != null) {
                helper.assertTrue(stack.getItem().supportsEnchantment(stack, haunted),
                        "Focus Staffbow should allow malum:haunted at the enchanting table");
                helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(haunted)),
                        "Focus Staffbow should allow malum:haunted from enchanted books");
                helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, haunted),
                        "Focus Staffbow should allow malum:haunted through anvil merges");
            }

            var animated = enchantmentLookup.get(ResourceKey.create(Registries.ENCHANTMENT, MALUM_ANIMATED)).orElse(null);
            helper.assertTrue(animated != null, "Missing malum:animated enchantment");
            if (animated != null) {
                helper.assertFalse(stack.getItem().supportsEnchantment(stack, animated),
                        "Focus Staffbow should keep rejecting malum:animated at the enchanting table");
                helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(animated)),
                        "Focus Staffbow should keep rejecting malum:animated from enchanted books");
                helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, animated),
                        "Focus Staffbow should keep rejecting malum:animated through anvil merges");
            }
        });
    }
    static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var modifiers = toModifierMultimap(stack.getAttributeModifiers());

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_VALUE
            ) - 3.0D) < 1.0e-9D, "Focus Staffbow attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Focus Staffbow attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get((Holder<Attribute>) io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ) - 0.10D) < 1.0e-9D, "Focus Staffbow spell power regression: " + describeModifiers(modifiers));
        });
    }
    static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedChargedTwinBladeStaffEnchantments(helper.getLevel().registryAccess()),
                    "Charged Twin Blade Staff"
            );
        });
    }
    static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var modifiers = toModifierMultimap(stack.getAttributeModifiers());

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_VALUE
            ) - 10.0D) < 1.0e-9D, "Charged Twin Blade Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get((Holder<Attribute>) io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ) - 0.10D) < 1.0e-9D, "Charged Twin Blade Staff spell power regression: " + describeModifiers(modifiers));
        });
    }
    static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var baseStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_damage_test");
            var genericTarget = new ArmorStand(level, 0.0D, 2.0D, 0.0D);
            var undeadTarget = EntityType.ZOMBIE.create(level);
            var arthropodTarget = EntityType.SPIDER.create(level);
            var aquaticTarget = EntityType.DROWNED.create(level);
            helper.assertTrue(undeadTarget != null, "Charged Twin Blade Staff damage test could not create undead target");
            helper.assertTrue(arthropodTarget != null, "Charged Twin Blade Staff damage test could not create arthropod target");
            helper.assertTrue(aquaticTarget != null, "Charged Twin Blade Staff damage test could not create aquatic target");

            var damageSource = level.damageSources().playerAttack(player);
            var baseDamage = ChargedTwinBladeStaff.resolveThrownDamage(baseStack);
            helper.assertTrue(Math.abs(baseDamage - 11.0D) < 1.0e-9D,
                    "Charged Twin Blade Staff base thrown damage regression: " + baseDamage);

            var sharpnessStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            sharpnessStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    sharpnessStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff sharpness thrown damage regression"
            );

            var smiteStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            smiteStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    undeadTarget,
                    damageSource,
                    "Charged Twin Blade Staff smite thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff smite fallback damage regression"
            );

            var baneStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            baneStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    arthropodTarget,
                    damageSource,
                    "Charged Twin Blade Staff bane thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff bane fallback damage regression"
            );

            var impalingStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            impalingStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.IMPALING), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    aquaticTarget,
                    damageSource,
                    "Charged Twin Blade Staff impaling thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff impaling fallback damage regression"
            );
        });
    }
    static void chargedTwinBladeStaffThrowConsumesMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_throw_mana_test");
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff normal throw should consume 100 mana but left " + magicData.getMana());
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(), "Charged Twin Blade Staff throw did not spawn its projectile");
        });
    }
    static void chargedTwinBladeStaffLoyaltyReducesThrowManaCost(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_loyalty_mana_test");
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        var loyalty = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOYALTY);
        stack.enchant(loyalty, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff loyalty mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - 100.0F / 3.0F)) < 1.0e-3F,
                "Charged Twin Blade Staff loyalty mana discount regressed: " + magicData.getMana()));
    }
    static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_riptide_test");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var riptide = helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE);
            stack.enchant(riptide, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff riptide test could not resolve player mana data");
            magicData.setMana(50.0F);

            stack.getItem().releaseUsing(
                    stack,
                    helper.getLevel(),
                    player,
                    stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
            );
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff riptide should consume 50 mana on dry ground");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.01D,
                    "Charged Twin Blade Staff riptide should propel the player even without rain or water");
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff riptide should not spawn a projectile");
        });
    }
    static void chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var historyResolved = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(4.0D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(1.0D, 0.0D, 0.0D)
            );
            helper.assertTrue(historyResolved.distanceTo(new Vec3(1.0D, 0.0D, 0.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should prefer recent flight history: " + historyResolved);

            var shortHistoryFallback = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(0.001D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(0.0D, 0.0D, 1.0D)
            );
            helper.assertTrue(shortHistoryFallback.distanceTo(new Vec3(0.0D, 0.0D, 1.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should fall back when history is too short: " + shortHistoryFallback);

            var reversedHistoryFallback = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(-4.0D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(1.0D, 0.0D, 0.0D)
            );
            helper.assertTrue(reversedHistoryFallback.distanceTo(new Vec3(1.0D, 0.0D, 0.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should fall back when history reverses initial throw: " + reversedHistoryFallback);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_cast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact cast test could not resolve player mana data");
            magicData.setMana(200.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var forward = new Vec3(0.0D, 0.0D, 1.0D);

            var instantPayload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, instantPayload, impactPos, forward
                    ),
                    "Charged Twin Blade Staff impact manager failed to cast an INSTANT payload"
            );
            var instantProjectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!instantProjectiles.isEmpty(),
                    "Charged Twin Blade Staff INSTANT impact cast did not spawn Magic Missile projectiles");

            var longPayload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("apprenticecodex", "compound_phial"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, longPayload, impactPos, forward
                    ),
                    "Charged Twin Blade Staff impact manager failed to cast a LONG payload"
            );
            var longProjectiles = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, new AABB(impactPos, impactPos).inflate(12.0D));
            helper.assertTrue(!longProjectiles.isEmpty(),
                    "Charged Twin Blade Staff LONG impact cast did not spawn Compound Phial projectiles");
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_self_profile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff self profile test could not resolve player mana data");
            magicData.setMana(200.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff self profile failed to cast Oakskin"
            );
            helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get())),
                    "Charged Twin Blade Staff self profile should apply Oakskin to the real player");
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesDispenserProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_dispenser_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative dispenser profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff creative impact cast should use Spell Dispenser profile with zero mana"
            );
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff creative dispenser profile should spawn Magic Missile projectiles");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative dispenser profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_staff_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative staff profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff creative impact cast should use staff profile with zero mana"
            );
            helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get())),
                    "Charged Twin Blade Staff creative staff profile should apply Oakskin to the real player");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative staff profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead test could not resolve player mana data");
            magicData.setMana(500.0F);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead profile failed its initial cast"
            );
            var summons = level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            );
            helper.assertTrue(!summons.isEmpty(),
                    "Charged Twin Blade Staff Raise Dead profile should summon mobs near the impact");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get()),
                    "Charged Twin Blade Staff Raise Dead profile should register recast on the real player");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get()),
                    "Charged Twin Blade Staff Raise Dead profile should not add a normal cooldown for a recast spell");
            summons.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_recast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead recast test could not resolve player mana data");
            magicData.setMana(500.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead recast setup failed"
            );
            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead should not recast while an initial recast is active"
            );
            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            ).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_selection_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead selection test could not resolve player mana data");
            magicData.setMana(500.0F);

            var amplifierStack = new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var mutable = ISpellContainer.create(2, true, false).mutableCopy();
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 0, false),
                    "Failed to prepare first wheel spell for Raise Dead selection regression");
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(), 1, 1, false),
                    "Failed to prepare Raise Dead wheel spell for selection regression");
            ISpellContainer.set(amplifierStack, mutable.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            magicData.getSyncedData().setSpellSelection(new io.redspace.ironsspellbooks.gui.overlays.SpellSelection(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    1
            ));

            var beforeSelection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(beforeSelection != null
                            && beforeSelection.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(),
                    "Raise Dead selection regression setup should select Raise Dead but got " + beforeSelection);

            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead selection regression failed its initial cast"
            );

            var afterSelection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(afterSelection != null
                            && afterSelection.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(),
                    "Raise Dead impact cast should preserve the selected wheel spell but got " + afterSelection);
            var recastPayload = jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload.capture(afterSelection, player);
            helper.assertFalse(recastPayload.isPresent(),
                    "Raise Dead active recast should not fall back to a different wheel spell payload");

            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            ).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_unprofiled_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff unprofiled test could not resolve player mana data");
            magicData.setMana(500.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blood_step"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff should reject spells without a staff or Spell Dispenser profile"
            );
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_continuous_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff continuous impact test could not resolve player mana data");
        magicData.setMana(200.0F);
        var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath"),
                1,
                CastSource.SWORD.name(),
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );

        helper.runAtTickTime(1, () -> helper.assertTrue(
                jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                ),
                "Charged Twin Blade Staff impact manager failed to start a CONTINUOUS payload"
        ));
        helper.succeedWhen(() -> {
            var projectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(impactPos, impactPos).inflate(16.0D));
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff CONTINUOUS impact cast did not spawn Fire Breath projectiles");
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact fail test could not resolve player mana data");
            magicData.setMana(0.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff impact manager should skip casts when the owner cannot pay the spell mana"
            );
        });
    }
    static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var expectedStoredCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD,
                    stack
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 0,
                    "Elemental Bow should suppress elemental arrow cooldowns but got " + cooldownEvent.getEffectiveCooldown());
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            0
                    ) == expectedStoredCooldown,
                    "Elemental Bow should store the helper cooldown for overheat timing"
            );

            var controlEvent = new SpellCooldownAddedEvent.Pre(
                    160,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get(),
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == 160,
                    "Elemental Bow cooldown suppression should not affect unrelated spells");
        });
    }
    static void reflectcastShieldKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Reflectcast Shield is missing malum:soul_hunter_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedReflectcastShieldEnchantments(helper.getLevel().registryAccess(), stack),
                    "Reflectcast Shield"
            );
        });
    }
    static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spellcasters Flask",
                item -> item instanceof SpellcastersFlask,
                expectedFlaskEnchantments()
        ));
    }
    static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Alchemists Flask",
                item -> item instanceof AlchemistsFlask,
                expectedAlchemistsFlaskEnchantments()
        ));
    }
    static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertApprenticeEnchantmentFlags(helper, Enchantments.ALACRITY, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.REFLUX, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.RESERVOIR, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SURGE, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.ATTUNEMENT, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.TENSE, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.WISDOM, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.PLUNDER, false, true, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.TRANSCENDENCE, true, false, true, true);
            assertApprenticeEnchantmentFlags(helper, Enchantments.GUZZLE, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.LARGE_MUG, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.RED_ENERGY, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.GLOW_ENERGY, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SYNTHESIS, false, true, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SHELL, false, false, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.SYNCHRONIZATION, false, false, false, false);
            assertApprenticeEnchantmentFlags(helper, Enchantments.NEUTRALIZATION, false, false, false, false);
        });
    }
    static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var function = EnchantRandomlyFunction.randomApplicableEnchantment(helper.getLevel().registryAccess()).build();
            var seenApprenticeEnchantments = new LinkedHashSet<ResourceLocation>();
            var excludedEnchantments = new LinkedHashSet<>(expectedFlaskEnchantments());
            excludedEnchantments.add(Enchantments.SYNTHESIS.location());

            for (long seed = 0L; seed < 4096L; ++seed) {
                var result = function.apply(new ItemStack(Items.BOOK), createEmptyLootContext(helper, seed));
                var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
                helper.assertTrue(result.is(Items.ENCHANTED_BOOK),
                        "Random applicable enchantment loot should convert books into enchanted books");
                helper.assertTrue(enchantments.size() == 1,
                        "Random applicable enchantment loot should apply exactly one enchantment: " + enchantments);

                for (var enchantment : enchantments.keySet()) {
                    var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
                    if (enchantmentId == null || !ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
                        continue;
                    }

                    helper.assertFalse(excludedEnchantments.contains(enchantmentId),
                            "Random applicable enchantment loot included excluded enchantment: " + enchantmentId + " at seed " + seed);
                    seenApprenticeEnchantments.add(enchantmentId);
                }
            }

            var expectedEnchantments = expectedRandomBookLootEnchantments();
            helper.assertTrue(seenApprenticeEnchantments.containsAll(expectedEnchantments),
                    "Random applicable enchantment loot lost apprentice enchantments: "
                            + describeEnchantmentDifference(expectedEnchantments, seenApprenticeEnchantments));
        });
    }
    static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCategoryEnchantments(
                    helper,
                    "Enchantress Robe",
                    item -> item instanceof EnchantressRobeItem,
                    stack -> expectedEnchantressRobeEnchantments(helper.getLevel().registryAccess(), stack)
            );
            assertCategoryEnchantments(
                    helper,
                    "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    stack -> expectedStealthRuneArmorEnchantments(helper.getLevel().registryAccess(), stack)
            );
        });
    }
    static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER;
            var lightningSpellPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get().getSchoolType()
            );
            helper.assertTrue(lightningSpellPowerAttribute != null,
                    "Enchantress Robe test could not resolve lightning school spell power attribute");
            var lightningSpellPowerHolder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(lightningSpellPowerAttribute);
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get(),
                    ArmorItem.Type.LEGGINGS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADD_VALUE);
                helper.assertTrue(Math.abs(maxManaBonus - EnchantressRobeStats.MAX_MANA_BONUS_PER_PIECE) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - 0.10D) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " spell power regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Enchantress Robe " + armorType + " imbue surface regression: hasImbueSlot="
                                + item.hasImbueSlot() + " stack=" + stack);

                var lightningSpellPowerBonus = sumModifierAmount(
                        modifiers.get(lightningSpellPowerHolder),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                );
                helper.assertTrue(Math.abs(lightningSpellPowerBonus) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " should not gain school spell power before imbue: "
                                + describeModifiers(modifiers));
            }
        });
    }
    static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Enchantress Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Enchantress Robe chestplate test could not resolve school spell power attribute");

            var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
            var globalSpellPowerBonus = sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(globalSpellPowerBonus - 0.10D) < 1.0e-9D,
                    "Enchantress Robe chestplate should keep +0.10 spell power after imbue: " + describeModifiers(modifiers));

            var imbuedSchoolSpellPowerBonus = sumModifierAmount(
                    modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(imbuedSpellPowerAttribute)),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            helper.assertTrue(Math.abs(imbuedSchoolSpellPowerBonus - 0.05D) < 1.0e-9D,
                    "Enchantress Robe chestplate should add +0.05 imbued school spell power: " + describeModifiers(modifiers));
        });
    }
    static void pastelStaffKeepsItsExtraMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            assertRequiredExtraEnchantments(
                    helper,
                    stack,
                    requiredPastelStaffExtraEnchantments(),
                    null,
                    "Pastel Staff required extra enchantment"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    rejectedPastelStaffExtraEnchantments(),
                    null,
                    "Pastel Staff should keep rejecting"
            );

            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                helper.assertTrue(stack.is(MALUM_MAGIC_CAPABLE_WEAPON),
                        "Pastel Staff is missing malum:magic_capable_weapon");
                assertRequiredExtraEnchantments(
                        helper,
                        stack,
                        requiredMalumMagicCapableWeaponEnchantments(),
                        null,
                        "Pastel Staff malum extra enchantment"
                );
            }
        });
    }
    static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            var item = (CrystalBladedStaff) stack.getItem();
            assertRequiredExtraEnchantments(
                    helper,
                    stack,
                    requiredCrystalBladedStaffExtraEnchantments(),
                    true,
                    "Crystal Bladed Staff required extra enchantment"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    rejectedCrystalBladedStaffExtraEnchantments(),
                    false,
                    "Crystal Bladed Staff should keep rejecting"
            );

            helper.assertTrue(item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Crystal Bladed Staff should keep accepting diamonds as its repair material");

            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                helper.assertTrue(stack.is(MALUM_MAGIC_CAPABLE_WEAPON),
                        "Crystal Bladed Staff is missing malum:magic_capable_weapon");
                assertRequiredExtraEnchantments(
                        helper,
                        stack,
                        requiredMalumMagicCapableWeaponEnchantments(),
                        true,
                        "Crystal Bladed Staff malum extra enchantment"
                );
            }
        });
    }
    static void hauntedBonusDamageTypeStaysOnMagicDamageTagPath(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var attacker = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var source = CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS);

            helper.assertTrue(source.is(DamageTypes.HAUNTED_BONUS),
                    "Haunted bonus should use apprenticecodex:haunted_bonus");
            helper.assertTrue(source.is(DamageTypeTagGenerator.MAGIC_DAMAGE),
                    "Haunted bonus should stay on the magic damage tag path");
            helper.assertTrue(source.is(COMMON_IS_MAGIC),
                    "Haunted bonus should stay on the c:is_magic path for Lodestone magic_proficiency");
            helper.assertTrue(source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "Haunted bonus should bypass cooldown-based I-Frame checks");
        });
    }
    static void hauntedBonusDamageActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(LODESTONE_MOD_ID)) {
                return;
            }

            var magicProficiency = BuiltInRegistries.ATTRIBUTE.getOptional(LODESTONE_MAGIC_PROFICIENCY).orElse(null);
            helper.assertTrue(magicProficiency != null, "lodestone:magic_proficiency is not registered");

            var attacker = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var proficiencyInstance = attacker.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(magicProficiency));
            helper.assertTrue(proficiencyInstance != null, "Attacker is missing lodestone:magic_proficiency");

            var baselineTarget = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(1, 2, 0));
            var amplifiedTarget = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(2, 2, 0));
            var baseDamage = 4.0F;

            var baselineHealth = baselineTarget.getHealth();
            helper.assertTrue(baselineTarget.hurt(
                            CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS),
                            baseDamage),
                    "Baseline haunted bonus damage should apply");
            var baselineTaken = baselineHealth - baselineTarget.getHealth();
            helper.assertTrue(Math.abs(baselineTaken - baseDamage) < 1.0e-4F,
                    "Baseline haunted bonus damage should stay unscaled at proficiency 1.0, actual=" + baselineTaken);

            proficiencyInstance.setBaseValue(1.5D);
            var amplifiedHealth = amplifiedTarget.getHealth();
            helper.assertTrue(amplifiedTarget.hurt(
                            CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS),
                            baseDamage),
                    "Amplified haunted bonus damage should apply");
            var amplifiedTaken = amplifiedHealth - amplifiedTarget.getHealth();
            helper.assertTrue(Math.abs(amplifiedTaken - 6.0F) < 1.0e-4F,
                    "Amplified haunted bonus damage should scale to 6.0 at proficiency 1.5, actual=" + amplifiedTaken);
            helper.assertTrue(amplifiedTaken > baselineTaken,
                    "Amplified haunted bonus damage should exceed baseline damage");
        });
    }
    static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.UniteLunaStaff) ItemRegistry.UNITE_LUNA_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Unite Luna Staff did not initialize a spell container");
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Unite Luna Staff spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Unite Luna Staff has no preset spell");
            helper.assertTrue(spellData.getSpell() == jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA.get(),
                    "Unite Luna Staff preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Unite Luna Staff preset spell level mismatch: " + spellData.getLevel());

            var modifiers = item.getDefaultAttributeModifiers(stack);
            assertModifierAmount(helper, modifiers, Attributes.ATTACK_DAMAGE.value(), EquipmentSlotGroup.MAINHAND, 12.0D,
                    AttributeModifier.Operation.ADD_VALUE, "Unite Luna Staff attack damage regression");
            assertModifierAmount(helper, modifiers, Attributes.ATTACK_SPEED.value(), EquipmentSlotGroup.MAINHAND, -3.2D,
                    AttributeModifier.Operation.ADD_VALUE, "Unite Luna Staff attack speed regression");
            assertModifierAmount(helper, modifiers, Attributes.ENTITY_INTERACTION_RANGE.value(), EquipmentSlotGroup.MAINHAND, 0.5D,
                    AttributeModifier.Operation.ADD_VALUE, "Unite Luna Staff entity reach regression");
            assertModifierAmount(helper, modifiers, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Unite Luna Staff spell power regression");
            assertModifierAmount(helper, modifiers, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.value(),
                    EquipmentSlotGroup.MAINHAND, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    "Unite Luna Staff holy spell power regression");
        });
    }
    static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var stack = new ItemStack(item);
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(stack, item.getDefaultAttributeModifiers(stack));
            NeoForge.EVENT_BUS.post(event);
            var upgradedModifiers = event.build();

            assertModifierAmount(
                    helper,
                    upgradedModifiers,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.value(),
                    50.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Offhand upgrade bridge regression: expected +50 max mana from mainhand-stored upgrade but got "
                            + upgradeData
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
    static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCastingMoveSpeedAdjustment(helper, 0.0D, 0.8D, "No external bonus should keep full cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.25D, 0.55D, "Diamond-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.50D, 0.30D, "Netherite-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.75D, 0.05D, "Small remaining headroom should stay positive");
            assertCastingMoveSpeedAdjustment(helper, 0.80D, 0.0D, "Exact cap should stop adding more casting move speed");
            assertCastingMoveSpeedAdjustment(helper, 1.10D, 0.0D, "External overshoot should not become a negative correction");
        });
    }
    static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "long_stride_base_bonus_test");
            var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(movementSpeed != null, "LongStride base bonus test could not resolve movement speed attribute");

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectRegistry.LONG_STRIDE_MOBILITY, 200, 0));
            helper.assertTrue(movementSpeed != null, "LongStride base bonus test lost movement speed attribute after addEffect");

            var actualAmount = movementSpeed.getModifiers().stream()
                    .filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .mapToDouble(AttributeModifier::amount)
                    .sum();
            helper.assertTrue(Math.abs(actualAmount - 0.15D) < 1.0e-9D,
                    "LongStride movement speed bonus regression: expected 0.15 but got " + actualAmount);
        });
    }
    static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dynamic_casting_movespeed_rebalance_test");
            var effectHolder = EffectRegistry.LONG_STRIDE_MOBILITY;
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.value();
            var castingMoveSpeed = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED);
            helper.assertTrue(castingMoveSpeed != null,
                    "Dynamic casting mobility test could not resolve the CASTING_MOVESPEED attribute");

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effectHolder, 200, 0));
            helper.assertTrue(castingMoveSpeed != null, "Dynamic casting mobility test lost CASTING_MOVESPEED after addEffect");
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    null,
                    0.8D,
                    "Dynamic casting mobility effect should initially fill the full cancellation headroom"
            );

            castingMoveSpeed.addTransientModifier(new AttributeModifier(
                    CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                    0.5D,
                    AttributeModifier.Operation.ADD_VALUE
            ));
            effect.applyEffectTick(player, 0);
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID,
                    0.3D,
                    "Dynamic casting mobility effect should shrink after an external casting move speed bonus is added"
            );

            castingMoveSpeed.removeModifier(CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID);
            effect.applyEffectTick(player, 0);
            assertCastingMoveSpeedModifierAmount(
                    helper,
                    castingMoveSpeed,
                    null,
                    0.8D,
                    "Dynamic casting mobility effect should recover once the external casting move speed bonus is removed"
            );
        });
    }
    static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = new ItemStack(ItemRegistry.COMFORT_BERRIES.get()).getFoodProperties(null);
            helper.assertTrue(foodProperties != null, "Comfort Berries should remain edible");
            helper.assertTrue(foodProperties != null && foodProperties.nutrition() == 4,
                    "Comfort Berries nutrition regression: " + (foodProperties == null ? "null" : foodProperties.nutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.saturation() - 9.6f) < 1.0e-6F,
                    "Comfort Berries saturation regression: "
                            + (foodProperties == null ? "null" : foodProperties.saturation()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Berries should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<net.minecraft.world.food.FoodProperties.PossibleEffect>of()
                    : foodProperties.effects().stream()
                    .filter(effectPair -> effectPair.effect().getEffect() == EffectRegistry.MANA_REGENERATION)
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Berries should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.effect().getDuration() == 20 * 30,
                    "Comfort Berries mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.effect().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.effect().getAmplifier() == 0,
                    "Comfort Berries mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.effect().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.probability() - 1.0f) < 1.0e-6F,
                    "Comfort Berries mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.probability()));
        });
    }
    static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "mana_regeneration_test"));
            var manaRegenAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN);
            helper.assertTrue(manaRegenAttribute != null, "Player is missing the mana regen attribute");

            var baseValue = manaRegenAttribute == null ? Double.NaN : manaRegenAttribute.getValue();
            helper.assertTrue(!Double.isNaN(baseValue) && baseValue > 0.0D,
                    "Mana regen base value must be positive for regression testing: " + baseValue);

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectRegistry.MANA_REGENERATION, 20 * 30, 0));
            var levelOneValue = manaRegenAttribute == null ? Double.NaN : manaRegenAttribute.getValue();
            helper.assertTrue(Math.abs(levelOneValue - (baseValue * 1.25D)) < 1.0e-9D,
                    "Mana Regeneration Lv1 regression: expected " + (baseValue * 1.25D) + " but got " + levelOneValue);

            player.removeEffect(EffectRegistry.MANA_REGENERATION);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(EffectRegistry.MANA_REGENERATION, 20 * 30, 1));
            var levelTwoValue = manaRegenAttribute == null ? Double.NaN : manaRegenAttribute.getValue();
            helper.assertTrue(Math.abs(levelTwoValue - (baseValue * 1.50D)) < 1.0e-9D,
                    "Mana Regeneration Lv2 regression: expected " + (baseValue * 1.50D) + " but got " + levelTwoValue);
        });
    }
    static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertPotionEffect(helper, PotionRegistry.MEDITATION.get(), "apprenticecodex:meditation", 20 * 60 * 3, 0);
            assertPotionEffect(helper, PotionRegistry.LONG_MEDITATION.get(), "apprenticecodex:long_meditation", 20 * 60 * 8, 0);
            assertPotionEffect(helper, PotionRegistry.STRONG_MEDITATION.get(), "apprenticecodex:strong_meditation", 20 * 90, 1);
        });
    }
    static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var instantSpell = SpellRegistry.AUTO_MAGNET.get();
            var longSpell = SpellRegistry.ARCANE_BLAST.get();
            var continuousSpell = SpellRegistry.BULLET_STREAM.get();

            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Iron Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Copper Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.SILVER_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Silver Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.GOLD_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Gold Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Diamond Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.NETHERITE_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Netherite Swingcast Staff"
            );
        });
    }
    static void healingBloomLightHasReducedLevelAndNoOutline(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = BlockRegistry.HEALING_BLOOM_LIGHT.get().defaultBlockState();
            var pos = new BlockPos(0, 2, 0);
            var shape = state.getShape(level, pos, CollisionContext.empty());
            helper.assertTrue(shape.isEmpty(),
                    "Healing Bloom light outline should be empty so it cannot be removed by hand");
            helper.assertTrue(state.getLightEmission(level, pos) == 11,
                    "Healing Bloom light should now emit light level 11");
        });
    }
    static void healingBloomLightSelfCleansWithoutBloom(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var pos = new BlockPos(0, 2, 0);
            helper.setBlock(pos, BlockRegistry.HEALING_BLOOM_LIGHT.get());

            for (int i = 0; i < 25; ++i) {
                if (level.getBlockState(pos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get())) {
                    var blockEntity = helper.getBlockEntity(pos);
                    helper.assertTrue(blockEntity instanceof HealingBloomLightBlockEntity,
                            "Healing Bloom light is missing its block entity");
                    HealingBloomLightBlockEntity.serverTick(
                            level,
                            pos,
                            level.getBlockState(pos),
                            (HealingBloomLightBlockEntity) blockEntity
                    );
                }
            }

            helper.assertTrue(!level.getBlockState(pos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get()),
                    "Healing Bloom light should self-clean without a bloom");
        });
    }
    static void healingBloomAcceptsOwnerDamageAndStaysSavable(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_owner_test"));
            var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
            bloom.setOwner(owner);
            bloom.setAnchorPos(new BlockPos(0, 2, 0));
            helper.assertTrue(bloom.shouldBeSaved(), "Healing Bloom should now be saved with the world");
            helper.assertTrue(bloom.hurt(level.damageSources().playerAttack(owner), 2.0f),
                    "Healing Bloom should now accept damage from its owner");
        });
    }
    static void healingBloomRootLossUsesDeathState(GameTestHelper helper) {
        var level = helper.getLevel();
        var relativeAnchorPos = new BlockPos(0, 2, 0);
        var anchorPos = helper.absolutePos(relativeAnchorPos);
        helper.setBlock(relativeAnchorPos.below(), Blocks.DIRT);

        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_root_loss_test"));
        var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
        bloom.setOwner(owner);
        bloom.setAnchorPos(anchorPos);
        bloom.setBloomMaxHealth(10.0f);
        bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
        helper.getLevel().addFreshEntity(bloom);

        helper.runAtTickTime(1, () -> helper.setBlock(relativeAnchorPos.below(), Blocks.AIR));
        helper.runAtTickTime(3, () -> {
            var blooms = level.getEntitiesOfClass(HealingBloomEntity.class, new net.minecraft.world.phys.AABB(anchorPos).inflate(1.5));
            helper.assertTrue(!blooms.isEmpty(),
                    "Healing Bloom should remain as a dead entity for a short time instead of silently disappearing when its root is lost");
            helper.assertTrue(blooms.stream().allMatch(entity -> !entity.isAlive() || entity.isDeadOrDying()),
                    "Healing Bloom should enter its death state when its root is lost");
            helper.succeed();
        });
    }
    static void healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(GameTestHelper helper) {
        var level = helper.getLevel();
        var relativeAnchorPos = new BlockPos(0, 2, 0);
        var anchorPos = helper.absolutePos(relativeAnchorPos);
        helper.setBlock(relativeAnchorPos.below(), Blocks.STONE);

        var owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "healing_bloom_regen_test"));
        var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), level);
        bloom.setOwner(owner);
        bloom.setAnchorPos(anchorPos);
        bloom.setBloomMaxHealth(10.0f);
        bloom.setHealth(5.0f);
        bloom.moveTo(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(bloom);

        helper.runAtTickTime(45, () -> {
            helper.assertFalse(bloom.hasEffect(MobEffects.REGENERATION),
                    "Healing Bloom should not grant its own regeneration effect to itself");
            helper.assertTrue(Math.abs(bloom.getHealth() - 5.0f) < 0.01f,
                    "Healing Bloom should not recover before its low-speed natural heal ticks");
        });
        helper.runAtTickTime(81, () -> {
            helper.assertTrue(Math.abs(bloom.getHealth() - 6.0f) < 0.01f,
                    "Healing Bloom should recover exactly one point from low-speed natural healing after 80 ticks");
            helper.succeed();
        });
    }
    static void healingBloomCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_slab_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB);

            castHealingBloom(helper, owner, 1, anchorPos, false);

            var bloom = getSingleLivingHealingBloom(helper, owner);
            helper.assertTrue(Math.abs(bloom.getY() - (helper.absolutePos(anchorPos).below().getY() + 0.5)) < 0.01,
                    "Healing Bloom should now sit on top of a slab support instead of refusing the placement");
        });
    }
    static void healingBloomNormalRecastFailsForSameOwner(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_recast_same_owner_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(2, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, owner, 1, firstAnchor, false);
            var firstBloom = getSingleLivingHealingBloom(helper, owner);

            castHealingBloom(helper, owner, 1, secondAnchor, false);

            var blooms = getOwnedHealingBlooms(helper, owner);
            helper.assertTrue(blooms.size() == 1,
                    "Healing Bloom should still allow only one active bloom for the same owner");
            helper.assertTrue(blooms.get(0) == firstBloom,
                    "Healing Bloom should keep the original bloom when recast without sneaking");
            helper.assertTrue(firstBloom.blockPosition().equals(helper.absolutePos(firstAnchor)),
                    "Healing Bloom should remain at the original anchor after a blocked recast");
        });
    }
    static void healingBloomAllowsDifferentOwnersToEachHaveOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var firstOwner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_owner_a_test");
            var secondOwner = createHealingBloomPlayer(helper, new BlockPos(4, 2, 0), "healing_bloom_owner_b_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var secondAnchor = new BlockPos(4, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, firstOwner, 1, firstAnchor, false);
            castHealingBloom(helper, secondOwner, 1, secondAnchor, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, firstOwner).size() == 1,
                    "The first owner should keep exactly one Healing Bloom");
            helper.assertTrue(getOwnedHealingBlooms(helper, secondOwner).size() == 1,
                    "A different owner should be able to place a separate Healing Bloom");
        });
    }
    static void healingBloomMissingManagedBloomDoesNotBlockRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_missing_state_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);

            var spellData = jp.aquafactory.apprenticecodex.capability.Capabilities.getSpellDataOrNull(owner);
            helper.assertTrue(spellData != null,
                    "Healing Bloom stale-state test could not resolve spell data capability");
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, state -> state.setBloomUuid(UUID.randomUUID()));

            castHealingBloom(helper, owner, 1, anchorPos, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, owner).size() == 1,
                    "A missing managed Healing Bloom should not block recasting for the same owner");
        });
    }
    static void healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var firstOwner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_force_owner_a_test");
            var secondOwner = createHealingBloomPlayer(helper, new BlockPos(4, 2, 0), "healing_bloom_force_owner_b_test");
            var firstAnchor = new BlockPos(0, 2, 0);
            var replacementAnchor = new BlockPos(2, 2, 0);
            var secondAnchor = new BlockPos(4, 2, 0);
            helper.setBlock(firstAnchor.below(), Blocks.STONE);
            helper.setBlock(replacementAnchor.below(), Blocks.STONE);
            helper.setBlock(secondAnchor.below(), Blocks.STONE);

            castHealingBloom(helper, firstOwner, 1, firstAnchor, false);
            var previousBloom = getSingleLivingHealingBloom(helper, firstOwner);
            castHealingBloom(helper, secondOwner, 1, secondAnchor, false);
            var otherOwnersBloom = getSingleLivingHealingBloom(helper, secondOwner);

            castHealingBloom(helper, firstOwner, 1, replacementAnchor, true);

            var currentBloom = getSingleLivingHealingBloom(helper, firstOwner);
            helper.assertTrue(currentBloom != previousBloom,
                    "Sneak casting should create a new Healing Bloom for the owner");
            helper.assertTrue(currentBloom.blockPosition().equals(helper.absolutePos(replacementAnchor)),
                    "The replacement Healing Bloom should appear at the new anchor");
            helper.assertTrue(!previousBloom.isAlive() || previousBloom.isDeadOrDying(),
                    "The previous Healing Bloom should enter its death state after the replacement bloom is created");
            helper.assertTrue(otherOwnersBloom.isAlive() && otherOwnersBloom.blockPosition().equals(helper.absolutePos(secondAnchor)),
                    "Replacing your own Healing Bloom should not affect blooms owned by other players");
        });
    }
    static void archerMultipleTimeoutWithGreaterConjurersTalismanSkipsCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createArcherMultiplePlayer(helper, playerPos, "archer_multiple_greater_conjurer_timeout_test");
            equipGreaterConjurersTalisman(player);

            castArcherMultiple(helper, player, 1);

            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            var magicData = MagicData.getPlayerMagicData(player);
            var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "Archer Multiple should create a recast instance on initial cast");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple recast should remain active before timeout completion");

            magicData.getPlayerRecasts().removeRecast(recast, io.redspace.ironsspellbooks.capabilities.magic.RecastResult.TIMEOUT);

            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple recast should be removed after timeout completion");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Greater Conjurer's Talisman should suppress Archer Multiple cooldown when the recast ends by timeout");
        });
    }
    static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        var playerPos = new BlockPos(0, 12, 0);
        prepareSummonedEntityIsolationArea(helper, playerPos);
        var player = createArcherMultiplePlayer(helper, playerPos, "archer_multiple_all_bows_removed_test");
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        var magicData = MagicData.getPlayerMagicData(player);

        helper.runAtTickTime(1, () -> {
            castArcherMultiple(helper, player, 1);
            helper.assertTrue(getOwnedArcherMultipleBows(helper, player).size() == 4,
                    "Archer Multiple should summon all bows before the removal test starts");
        });
        helper.runAtTickTime(3, () -> getOwnedArcherMultipleBows(helper, player).forEach(bow -> bow.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED)));

        helper.succeedWhen(() -> {
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Archer Multiple recast should end once every summoned bow has disappeared");
            helper.assertTrue(getOwnedArcherMultipleBows(helper, player).isEmpty(),
                    "Archer Multiple bows should all be gone after the forced removal");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Archer Multiple should start its normal cooldown when every summoned bow disappears");
        });
    }
    static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_external_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var touchDigSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get();
            var spectralHammerSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get();

            var touchDigManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.TOUCH_DIG_SPELL_ID,
                    1,
                    15,
                    touchDigSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(touchDigManaEvent);
            helper.assertTrue(touchDigManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Touch Dig mana to 8 but got " + touchDigManaEvent.getManaCost());
            var expectedTouchDigBaseCooldown = Math.max(1, touchDigSpell.getSpellCooldown() / 3);
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(touchDigSpell.getSpellCooldown(), player) == expectedTouchDigBaseCooldown,
                    "CraftsmansDelight should reduce Touch Dig base cooldown to one third before player modifiers");

            var touchDigCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    10,
                    touchDigSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(touchDigCooldownEvent);
            helper.assertTrue(touchDigCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(touchDigSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Touch Dig cooldown through the reduced cooldown helper");

            var spectralHammerManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.SPECTRAL_HAMMER_SPELL_ID,
                    1,
                    15,
                    spectralHammerSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(spectralHammerManaEvent);
            helper.assertTrue(spectralHammerManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Spectral Hammer mana to 8 but got " + spectralHammerManaEvent.getManaCost());
            var expectedSpectralHammerBaseCooldown = Math.max(1, spectralHammerSpell.getSpellCooldown() / 3);
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(spectralHammerSpell.getSpellCooldown(), player) == expectedSpectralHammerBaseCooldown,
                    "CraftsmansDelight should reduce Spectral Hammer base cooldown to one third before player modifiers");

            var spectralHammerCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    40,
                    spectralHammerSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(spectralHammerCooldownEvent);
            helper.assertTrue(spectralHammerCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(spectralHammerSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Spectral Hammer cooldown through the reduced cooldown helper");
        });
    }
    static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = new TouchDigSpell();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_range_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var targetPos = helper.absolutePos(new BlockPos(0, 23, 0));

            helper.assertTrue(magicData != null, "Touch Dig range test could not resolve player mana data");
            player.setYRot(0.0f);
            player.setXRot(-90.0f);
            player.setYHeadRot(0.0f);
            player.setYBodyRot(0.0f);
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var uniqueInfo = spell.getUniqueInfo(1, player).stream()
                    .map(Component::getString)
                    .collect(Collectors.joining(", "));
            helper.assertTrue(uniqueInfo.contains("16"),
                    "Touch Dig unique info should display 16 block range while CraftsmansDelight is equipped but got: " + uniqueInfo);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped"
                            + " [equipped=" + CraftsmansDelight.isEquippedBy(player)
                            + ", range=" + CraftsmansDelight.getTouchDigRange(player)
                            + ", info=" + uniqueInfo + "]");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }
    static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var fortune = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_ring_enchant_merge_test");
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(fortune, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            setCraftsmansDelightEnchantments(player, enchantments -> enchantments.set(fortune, 3));

            var mergedFortuneTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            mergedFortuneTool,
                            net.minecraft.world.item.enchantment.Enchantments.FORTUNE
                    ) == 3,
                    "Touch Dig should prefer the higher Fortune level from the ring");

            setCraftsmansDelightEnchantments(player, enchantments -> enchantments.set(silkTouch, 1));
            var equippedRing = getEquippedCraftsmansDelight(player);
            var mergedSilkTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            mergedSilkTool,
                            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                    ) == 1,
                    "Touch Dig should inherit Silk Touch from the ring"
                            + " [equippedRingSilk=" + jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                                    equippedRing,
                                    net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                            )
                            + ", mergedSilk=" + jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                                    mergedSilkTool,
                                    net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                            )
                            + "]");
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            mergedSilkTool,
                            net.minecraft.world.item.enchantment.Enchantments.FORTUNE
                    ) == 0,
                    "Touch Dig should drop Fortune when Silk Touch is present");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 1));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(blockPos).inflate(1.5D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig with ring Silk Touch should not drop cobblestone");
        });
    }
    static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_bare_hand_ring_enchant_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(silkTouch, 1);
            equipRingCurio(player, ringStack);

            var synthesizedTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertFalse(synthesizedTool.isEmpty(),
                    "Touch Dig should synthesize a mining tool when the caster is bare-handed but the ring has mining enchantments");
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            synthesizedTool,
                            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                    ) == 1,
                    "Touch Dig should copy Silk Touch onto the synthesized bare-hand tool");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(blockPos).inflate(1.5D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should not drop cobblestone");
        });
    }
    static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "spectral_hammer_ring_enchant_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            setCraftsmansDelightEnchantments(player, enchantments -> enchantments.set(silkTouch, 1));

            var targetPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            var hammer = new SpectralHammer(
                    helper.getLevel(),
                    player,
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.NORTH, targetPos, false),
                    0,
                    1
            );
            var hammerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 12, 1)));
            hammer.setPos(hammerPos.x, hammerPos.y, hammerPos.z);
            helper.getLevel().addFreshEntity(hammer);

            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                hammer.tick();
            }

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(targetPos).inflate(2.0D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should not drop cobblestone");
        });
    }
    static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_vanilla_menu_test");
        var shelfPos = new BlockPos(0, 1, 0);
        placeAndAssertBlockEntity(helper, shelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
        var absoluteShelfPos = helper.absolutePos(shelfPos);

        helper.succeedIf(() -> {
            var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
            shelf.setShelfData(player, false, Direction.NORTH);
            shelf.setLifeData(20 * 60, 10.0);
            var personalInventory = jp.aquafactory.apprenticecodex.capability.Capabilities.getPersonalInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing personal inventory for Personal Shelf GameTest"));

            personalInventory.getHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            for (var slot = 1; slot < 54; ++slot) {
                personalInventory.getHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            player.getInventory().setItem(0, new ItemStack(Items.DIRT));

            var menu = shelf.createMenu(1, player.getInventory(), player);
            helper.assertTrue(menu instanceof ChestMenu chestMenu && chestMenu.getRowCount() == 6,
                    "Personal Shelf should expose a vanilla six-row chest menu");
            var chestMenu = (ChestMenu) menu;
            helper.assertTrue(chestMenu.getSlot(0).getItem().is(Items.DIAMOND),
                    "Personal Shelf chest menu should read from the opener's personal inventory");

            var quickMoved = chestMenu.quickMoveStack(player, 81);
            helper.assertTrue(quickMoved.isEmpty(),
                    "Full Personal Shelf quick move should fail cleanly instead of looping");
            helper.assertTrue(player.getInventory().getItem(0).is(Items.DIRT),
                    "Failed Personal Shelf quick move should leave the player's stack in place");
        });
    }
    static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_expire_close_test");
        var shelfPos = new BlockPos(0, 1, 0);
        var absoluteShelfPos = castPersonalShelf(helper, player, shelfPos, false, Direction.NORTH);

        helper.runAtTickTime(1, () -> {
            var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
            helper.assertTrue(player.openMenu(shelf).isPresent(), "Personal Shelf should open before the expiration check");
            helper.assertTrue(player.containerMenu instanceof ChestMenu,
                    "Personal Shelf should still be using ChestMenu during the expiration check");
            shelf.setLifeData(1, 8.0);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(player.containerMenu == player.inventoryMenu,
                    "Expired Personal Shelf should close the opened menu");
            helper.assertBlockNotPresent(BlockRegistry.PERSONAL_SHELF_CHEST.get(), shelfPos);
        });
    }
    static void companionTrunkRecastRecallsLoadedTrunkWhenFar(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createCompanionTrunkPlayer(helper, playerPos);
            castCompanionTrunk(helper, player, 1);

            var trunk = getSingleCompanionTrunk(helper, player);
            trunk.setItem(0, new ItemStack(Items.DIAMOND));
            trunk.moveTo(player.getX() + 5.0, player.getY(), player.getZ(), 0.0f, 0.0f);

            castCompanionTrunk(helper, player, 1);

            helper.assertTrue(trunk.isAlive(), "Companion Trunk should stay active when recalled with items inside");
            helper.assertTrue(Math.abs(trunk.blockPosition().getX() - player.blockPosition().getX()) <= 1
                            && Math.abs(trunk.blockPosition().getZ() - player.blockPosition().getZ()) <= 1,
                    "Companion Trunk should be recalled within one block of the caster");
            helper.assertFalse(trunk.isEmpty(), "Companion Trunk should keep its items after recall");
        });
    }
    static void companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareSummonedEntityIsolationArea(helper, playerPos);
            var player = createCompanionTrunkPlayer(helper, playerPos);
            castCompanionTrunk(helper, player, 1);

            var trunk = getSingleCompanionTrunk(helper, player);
            trunk.setItem(0, new ItemStack(Items.EMERALD));
            trunk.moveTo(player.getX() + 1.0, player.getY(), player.getZ(), 0.0f, 0.0f);
            var before = trunk.position();

            castCompanionTrunk(helper, player, 1);

            helper.assertTrue(trunk.position().distanceTo(before) < 0.01,
                    "Companion Trunk should not move when recast while already within two blocks");
            helper.assertFalse(trunk.isEmpty(), "Companion Trunk should stay loaded after the failed dismiss");
        });
    }
    static void companionTrunkDeathStoresItemsInChestWhenSpaceExists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
            var trunkPos = new BlockPos(0, 2, 0);
            var trunk = createCompanionTrunk(helper, owner, trunkPos);
            trunk.setItem(0, new ItemStack(Items.DIAMOND, 3));

            trunk.dropAllContentsAndDiscard();

            var chestPos = findCompanionTrunkChest(helper, trunkPos);
            helper.assertTrue(chestPos != null, "Companion Trunk should create a vanilla chest when there is space nearby");
            helper.assertBlockPresent(Blocks.CHEST, chestPos);

            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
            helper.assertTrue(blockEntity instanceof ChestBlockEntity, "Death chest should use the vanilla chest block entity");
            helper.assertTrue(blockEntity instanceof ChestBlockEntity chest && containsItem(chest, Items.DIAMOND, 3),
                    "Death chest should receive the Companion Trunk inventory");
        });
    }
    static void companionTrunkDeathDropsItemsWhenNoChestSpaceExists(GameTestHelper helper) {
        var trunkPos = new BlockPos(0, 2, 0);
        for (var y = -1; y <= 1; ++y) {
            for (var x = -1; x <= 1; ++x) {
                for (var z = -1; z <= 1; ++z) {
                    helper.setBlock(trunkPos.offset(x, y, z), Blocks.STONE);
                }
            }
        }

        helper.succeedIf(() -> {
            var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 4, 0));
            var trunk = createCompanionTrunk(helper, owner, trunkPos);
            trunk.setItem(0, new ItemStack(Items.EMERALD, 2));

            trunk.dropAllContentsAndDiscard();

            helper.assertTrue(findCompanionTrunkChest(helper, trunkPos) == null,
                    "Companion Trunk should not create a chest when every candidate position is blocked");
            helper.assertItemEntityPresent(Items.EMERALD, trunkPos, 2.5);
        });
    }
    static void companionTrunkIgnoresFireAndRescuesFromVoid(GameTestHelper helper) {
        var player = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
        var trunk = createCompanionTrunk(helper, player, new BlockPos(0, 2, 0));
        trunk.setCompanionMaxHealth(10.0f);

        helper.assertFalse(trunk.hurt(helper.getLevel().damageSources().lava(), 4.0f),
                "Companion Trunk should ignore lava damage");
        trunk.igniteForSeconds(5.0f);

        helper.runAtTickTime(1, () -> {
            var belowWorld = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, -2, 0)));
            trunk.moveTo(belowWorld.x, belowWorld.y, belowWorld.z, 0.0f, 0.0f);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.isAlive(), "Companion Trunk should survive lava and void rescue");
            helper.assertTrue(trunk.getRemainingFireTicks() <= 0, "Companion Trunk should not stay ignited");
            helper.assertTrue(Math.abs(trunk.blockPosition().getX() - player.blockPosition().getX()) <= 2
                            && Math.abs(trunk.blockPosition().getZ() - player.blockPosition().getZ()) <= 2,
                    "Companion Trunk should return near its owner after falling below the world");
        });
    }
    static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var matureCropPos = new BlockPos(3, 2, 0);
        var immatureCropPos = new BlockPos(4, 2, 0);
        // age リセット収穫の検証が目的。
        // 小麦系は GameTest/FakePlayer 環境だと明るさ・耕地維持・更新順のノイズを受けやすいため、
        // ここでは Soul Sand だけで生存条件を満たせるネザーウォートで収穫ロジック自体を検証する。
        helper.setBlock(matureCropPos.below(), Blocks.SOUL_SAND);
        helper.setBlock(immatureCropPos.below(), Blocks.SOUL_SAND);
        helper.setBlock(matureCropPos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
        helper.setBlock(immatureCropPos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 2));

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(3, () -> {
            helper.assertBlockProperty(matureCropPos, NetherWartBlock.AGE, 0);
            helper.assertBlockProperty(immatureCropPos, NetherWartBlock.AGE, 2);
            helper.assertItemEntityPresent(Items.NETHER_WART, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.NETHER_WART, matureCropPos, 1.5);
            helper.succeed();
        });
    }
    static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var tomatoPos = new BlockPos(3, 2, 0);

        if (!ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var tomatoBlock = requireForgeBlock(helper, FARMERS_DELIGHT_TOMATO_BLOCK);
        var tomatoItem = requireForgeItem(helper, FARMERS_DELIGHT_TOMATO_ITEM);
        helper.setBlock(tomatoPos.below(), Blocks.FARMLAND);
        helper.setBlock(tomatoPos, withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 3));

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.runAtTickTime(3, () -> {
            var harvestedState = helper.getBlockState(tomatoPos);
            helper.assertTrue(harvestedState.is(tomatoBlock), "Farmer's Delight tomato should remain planted after HarvestMoon");
            helper.assertTrue(getIntegerPropertyValue(helper, harvestedState, "age") == 0,
                    "Farmer's Delight tomato should reset to age 0 after HarvestMoon but got " + harvestedState);
            helper.assertItemEntityPresent(tomatoItem, casterPos, 1.5);
            helper.assertItemEntityNotPresent(tomatoItem, tomatoPos, 1.5);
            helper.succeed();
        });
    }
    static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var baseTomatoPos = new BlockPos(3, 2, 0);
        var ropeTomatoPos = baseTomatoPos.above();

        if (!ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var tomatoBlock = requireForgeBlock(helper, FARMERS_DELIGHT_TOMATO_BLOCK);
        var tomatoItem = requireForgeItem(helper, FARMERS_DELIGHT_TOMATO_ITEM);
        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.setBlock(baseTomatoPos.below(), Blocks.FARMLAND);
        // rope 付きトマトは上段で天井扱いになりやすいため、GameTest でも補助光を置く。
        helper.setBlock(baseTomatoPos.east(), Blocks.GLOWSTONE);
        helper.setBlock(baseTomatoPos, withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 0));
        helper.setBlock(
                ropeTomatoPos,
                withBooleanProperty(
                        helper,
                        withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 3),
                        "ropelogged",
                        true
                )
        );

        // HarvestMoon が依存している右クリック経路そのものを直接通し、
        // 成熟した rope 付き上段トマトを収穫しても rope 状態が壊れないことを検証する。
        var result = BlockTools.useBlockByPlayerMainHand(helper.getLevel(), player, helper.absolutePos(ropeTomatoPos), new ItemStack(Items.STICK));
        helper.assertTrue(result.consumesAction(), "Farmer's Delight rope tomato block use should consume the action but got " + result);

        var baseState = helper.getBlockState(baseTomatoPos);
        var ropeState = helper.getBlockState(ropeTomatoPos);
        helper.assertTrue(baseState.is(tomatoBlock), "Lower tomato support should remain planted after right click harvest but got " + baseState);
        helper.assertTrue(getIntegerPropertyValue(helper, baseState, "age") == 0,
                "Lower tomato support should stay at age 0 after right click harvest but got " + baseState);
        helper.assertTrue(ropeState.is(tomatoBlock),
                "Harvested rope tomato should stay planted after right click harvest but got " + ropeState);
        helper.assertTrue(getBooleanPropertyValue(helper, ropeState, "ropelogged"),
                "Right click harvest should preserve Farmer's Delight tomato rope state but got " + ropeState);
        helper.assertTrue(getIntegerPropertyValue(helper, ropeState, "age") == 0,
                "Harvested rope tomato should reset to age 0 after right click harvest but got " + ropeState);
        helper.assertItemEntityPresent(tomatoItem, ropeTomatoPos, 1.5);
        helper.succeed();
    }
    static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var stemPos = new BlockPos(2, 2, 1);
        var fruitPos = new BlockPos(3, 2, 1);
        helper.setBlock(stemPos.below(), Blocks.FARMLAND);
        helper.setBlock(stemPos, Blocks.ATTACHED_MELON_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        helper.setBlock(fruitPos, Blocks.MELON);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.MELON, fruitPos);
            helper.assertBlockPresent(Blocks.MELON_STEM, stemPos);
            helper.assertItemEntityPresent(Items.MELON_SLICE, casterPos, 1.5);
        });
    }
    static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var bambooBase = new BlockPos(4, 2, 1);
        var blockBudgetPerTick = getHarvestMoonBlockBudgetPerTick();
        helper.setBlock(bambooBase.below(), Blocks.DIRT);
        helper.setBlock(bambooBase, Blocks.BAMBOO);
        for (var offset = 1; offset <= 6; ++offset) {
            helper.setBlock(bambooBase.above(offset), Blocks.BAMBOO);
        }

        var cropPositions = new ArrayList<BlockPos>();
        // GameTest の runAtTickTime から最初の観測までに HarvestMoonJob が複数 tick 進む環境があるため、
        // 3～4 tick 分進んでも未処理が残るだけの作物数を置いて false negative を避ける。
        for (var x = 1; x <= 9; ++x) {
            for (var z = -8; z <= 8; ++z) {
                var pos = new BlockPos(x, 3, z);
                if (pos.getX() == bambooBase.getX() && pos.getZ() == bambooBase.getZ()) {
                    continue;
                }
                cropPositions.add(pos);
                helper.setBlock(pos.below(), Blocks.SOUL_SAND);
                helper.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
            }
        }
        helper.assertTrue(cropPositions.size() > blockBudgetPerTick * 4,
                "HarvestMoon tick budget test requires more than four ticks of crops");

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(2, () -> helper.assertTrue(countMatureHarvestMoonPlants(helper, cropPositions) > 0,
                "HarvestMoon should leave some crops for later ticks"));

        helper.succeedWhen(() -> {
            helper.assertTrue(countMatureHarvestMoonPlants(helper, cropPositions) == 0,
                    "HarvestMoon should eventually harvest every queued mature crop");
            helper.assertBlockPresent(Blocks.BAMBOO, bambooBase);
            for (var offset = 1; offset <= 6; ++offset) {
                helper.assertBlockNotPresent(Blocks.BAMBOO, bambooBase.above(offset));
            }
            helper.assertItemEntityPresent(Items.BAMBOO, casterPos, 1.5);
        });
    }
    static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var kelpBase = new BlockPos(3, 2, 0);
        helper.setBlock(kelpBase.below(), Blocks.DIRT);
        helper.setBlock(kelpBase, Blocks.KELP_PLANT);
        for (var offset = 1; offset <= 5; ++offset) {
            helper.setBlock(kelpBase.above(offset), Blocks.KELP_PLANT);
        }
        helper.setBlock(kelpBase.above(6), Blocks.KELP);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.succeedWhen(() -> {
            var baseState = helper.getBlockState(kelpBase);
            helper.assertTrue(baseState.is(Blocks.KELP) || baseState.is(Blocks.KELP_PLANT),
                    "HarvestMoon should keep the bottom kelp block");
            for (var offset = 1; offset <= 6; ++offset) {
                var state = helper.getBlockState(kelpBase.above(offset));
                helper.assertTrue(!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT),
                        "HarvestMoon should remove upper kelp blocks even outside the initial Y slice");
            }
            helper.assertItemEntityPresent(Items.KELP, casterPos, 1.5);
        });
    }

    private static FakePlayer createHarvestMoonPlayer(GameTestHelper helper, BlockPos pos, ItemStack mainHandStack) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "harvest_moon_test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());
        return player;
    }

    private static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static FakePlayer createEquipmentTestPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        return player;
    }

    private static void setFocusStaffbowArrowCatalyst(FakePlayer player, ItemStack arrowStack) {
        player.getInventory().setItem(1, arrowStack.copy());
    }

    private static int getFocusStaffbowArrowCount(Player player) {
        int count = 0;
        for (var stack : player.getInventory().items) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        for (var stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void equipCurio(FakePlayer player, String slotId, ItemStack stack) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(slotId, 0, stack);
    }

    private static void assertManaShieldCharmEquipped(GameTestHelper helper, ServerPlayer player, String context) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Mana Shield Charm " + context + " test"));
        helper.assertTrue(curiosInventory.isEquipped(ItemRegistry.MANA_SHIELD_CHARM.get()),
                "Mana Shield Charm should be recognized as equipped in Curios during " + context + " test");
        helper.assertTrue(curiosInventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()).isPresent(),
                "Mana Shield Charm should be discoverable via findFirstCurio during " + context + " test");
    }

    private static net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent postLivingAttackEventForGameTest(
            ServerPlayer player,
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
        var container = new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount);
        var event = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player, container);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    private static void equipRingCurio(FakePlayer player, ItemStack ringStack) {
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.RING_SLOT, ringStack);
    }

    private static void prepareWideSearchIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareElevatedStonePlatform(helper, centerPos);
    }

    private static void prepareMiningSpellIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareElevatedStonePlatform(helper, centerPos);
    }

    private static void prepareSummonedEntityIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareElevatedStonePlatform(helper, centerPos);
    }

    private static ItemStack getEquippedCraftsmansDelight(FakePlayer player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.CRAFTSMANS_DELIGHT.get())
                        .map(slotResult -> slotResult.stack().copy()))
                .orElse(ItemStack.EMPTY);
    }

    private static void setCraftsmansDelightEnchantments(
            FakePlayer player,
            java.util.function.Consumer<ItemEnchantments.Mutable> enchantmentApplier
    ) {
        var equippedRing = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.CRAFTSMANS_DELIGHT.get())
                        .map(top.theillusivec4.curios.api.SlotResult::stack))
                .orElseThrow(() -> new IllegalStateException("Missing equipped CraftsmansDelight for GameTest"));
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantmentApplier.accept(enchantments);
        EnchantmentHelper.setEnchantments(equippedRing, enchantments.toImmutable());
    }

    private static void equipNecklaceCurio(FakePlayer player, ItemStack necklaceStack) {
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, necklaceStack);
    }

    private static FakePlayer createTrackedEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = createEquipmentTestPlayer(helper, pos, profileName);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static ItemStack getEquippedAutocastAmulet(FakePlayer player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof AutocastAmulet).stream()
                        .findFirst()
                        .map(top.theillusivec4.curios.api.SlotResult::stack)
                        .orElseThrow(() -> new IllegalStateException("Missing equipped Autocast Amulet for GameTest")))
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Autocast Amulet GameTest"));
    }

    private static ManaShieldCharmState getManaShieldCharmState(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            throw new IllegalStateException("Missing spell data for Mana Shield Charm GameTest");
        }
        return spellData.get(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE);
    }

    private static void invokeTouchDigDestroyBlock(TouchDigSpell spell, Level level, BlockPos pos, Player player) {
        try {
            var method = TouchDigSpell.class.getDeclaredMethod("doDestroyBlock", Level.class, BlockPos.class, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            method.invoke(spell, level, pos, player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Touch Dig destroy helper for GameTest", exception);
        }
    }

    private static List<ItemEntity> getFreshItemDrops(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).inflate(radius),
                itemEntity -> itemEntity.getAge() <= 1
        );
    }
    private static FakePlayer createSpellDispenserPlacer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static FakePlayer createBetterCombatHiddenOffhandPlayer(
            GameTestHelper helper,
            ItemStack mainHandStack,
            ItemStack offhandStack,
            String profileName
    ) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 0)));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack.copy());

        // Better Combat に空扱いされても inventory.offhand[0] 自体は保持されるため、
        // 救済系テストは実スロットへ直接積んで隠蔽前提を再現する。
        player.getInventory().offhand.set(0, offhandStack.copy());
        return player;
    }

    private static void castHarvestMoon(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.HARVEST_MOON.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    private static int getHarvestMoonBlockBudgetPerTick() {
        try {
            var field = HarvestMoon.class.getDeclaredField("BLOCK_BUDGET_PER_TICK");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read HarvestMoon block budget for GameTest", exception);
        }
    }

    private static FakePlayer createSenseEvilPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static FakePlayer createSenseEvilPlayer(ServerLevel level, BlockPos absolutePos, String profileName) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absoluteVec = Vec3.atBottomCenterOf(absolutePos);
        player.setPos(absoluteVec.x, absoluteVec.y, absoluteVec.z);
        level.addFreshEntity(player);
        return player;
    }

    private static net.minecraft.world.entity.LivingEntity spawnPositionedZombie(ServerLevel level, Vec3 targetCenter) {
        forceLoadChunk(level, BlockPos.containing(targetCenter));
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create zombie for SenseEvil GameTest");
        }
        var floorPos = BlockPos.containing(targetCenter.x, targetCenter.y - zombie.getBbHeight() * 0.5, targetCenter.z);
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(floorPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floorPos.above(2), Blocks.AIR.defaultBlockState(), 3);
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        zombie.moveTo(targetCenter.x, floorPos.getY() + 1.0, targetCenter.z, 0.0f, 0.0f);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static void placeZombieSpawner(ServerLevel level, BlockPos pos) {
        forceLoadChunk(level, pos);
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 3);
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawner)) {
            throw new IllegalStateException("Failed to place spawner for SenseEvil GameTest at " + pos);
        }
        spawner.setEntityId(EntityType.ZOMBIE, level.getRandom());
        spawner.setChanged();
    }

    private static void forceLoadChunk(ServerLevel level, BlockPos pos) {
        var chunkX = SectionPos.blockToSectionCoord(pos.getX());
        var chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        level.setChunkForced(chunkX, chunkZ, true);
        level.getChunk(chunkX, chunkZ);
    }

    private static double getSenseEvilRange(SenseEvil spell, net.minecraft.world.entity.LivingEntity caster, int spellLevel) {
        try {
            var method = SenseEvil.class.getDeclaredMethod("getRange", int.class, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            return (double) method.invoke(spell, spellLevel, caster);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read SenseEvil range for GameTest", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<SenseEvilHighlightsPacket.TargetData> collectSenseEvilHighlights(
            SenseEvil spell,
            ServerLevel level,
            int spellLevel,
            net.minecraft.world.entity.LivingEntity caster
    ) {
        try {
            var method = SenseEvil.class.getDeclaredMethod("collectHighlights", ServerLevel.class, int.class, net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            return (List<SenseEvilHighlightsPacket.TargetData>) method.invoke(spell, level, spellLevel, caster);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to collect SenseEvil highlights for GameTest", exception);
        }
    }

    private static void assertSenseEvilHighlightPresent(
            GameTestHelper helper,
            List<SenseEvilHighlightsPacket.TargetData> highlights,
            Vec3 expectedPosition,
            double tolerance,
            String message
    ) {
        var found = highlights.stream()
                .anyMatch(target -> target.position().distanceTo(expectedPosition) <= tolerance);
        helper.assertTrue(found, message + " / expected near " + expectedPosition + " but got " + highlights);
    }

    private static FakePlayer createHealingBloomPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static void castHealingBloom(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos, boolean forceReplace) {
        var spell = (HealingBloom) SpellRegistry.HEALING_BLOOM.get();
        var castData = new HealingBloom.HealingBloomCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        tag.putBoolean("ForceReplace", forceReplace);
        castData.deserializeNBT(helper.getLevel().registryAccess(), tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    private static java.util.List<HealingBloomEntity> getOwnedHealingBlooms(GameTestHelper helper, FakePlayer owner) {
        var blooms = new java.util.ArrayList<HealingBloomEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof HealingBloomEntity bloom
                    && bloom.isAlive()
                    && owner.getUUID().equals(bloom.getOwnerUuid())) {
                blooms.add(bloom);
            }
        }
        return blooms;
    }

    private static HealingBloomEntity getSingleLivingHealingBloom(GameTestHelper helper, FakePlayer owner) {
        var blooms = getOwnedHealingBlooms(helper, owner);
        helper.assertTrue(blooms.size() == 1, "Expected exactly one living Healing Bloom but found " + blooms.size());
        return blooms.get(0);
    }

    private static FakePlayer createArcherMultiplePlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        // SummonManager は owner を level lookup で引き直して recast cleanup するため、
        // Archer Multiple の summon 消滅テストでは FakePlayer もワールドへ参加させる。
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(io.redspace.ironsspellbooks.compat.Curios.NECKLACE_SLOT, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    private static void castArcherMultiple(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    private static List<ArcherMultipleBowEntity> getOwnedArcherMultipleBows(GameTestHelper helper, FakePlayer owner) {
        return helper.getLevel().getEntitiesOfClass(
                ArcherMultipleBowEntity.class,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                bow -> {
                    var summonOwner = bow.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    private static <T extends jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity> List<T> getOwnedSummonWeapons(
            GameTestHelper helper,
            FakePlayer owner,
            Class<T> weaponType
    ) {
        return helper.getLevel().getEntitiesOfClass(
                weaponType,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                weapon -> {
                    var summonOwner = weapon.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    private static FakePlayer createCompanionTrunkPlayer(GameTestHelper helper, BlockPos pos) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "companion_trunk_test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static void prepareElevatedStonePlatform(GameTestHelper helper, BlockPos centerPos) {
        // basic_floor は 5x3x5 と小さく、batch 近接配置の地形へ探索やレイが吸われやすい。
        // 足場を高所へ自前で作って、各テストが自分の 5x5 領域だけを参照するように固定する。
        var floorY = centerPos.getY() - 1;
        for (var x = -2; x <= 2; ++x) {
            for (var z = -2; z <= 2; ++z) {
                helper.setBlock(new BlockPos(centerPos.getX() + x, floorY, centerPos.getZ() + z), Blocks.STONE);
                helper.setBlock(new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z), Blocks.AIR);
                helper.setBlock(new BlockPos(centerPos.getX() + x, centerPos.getY() + 1, centerPos.getZ() + z), Blocks.AIR);
            }
        }
    }

    private static BlockPos createRemoteIsolationOrigin(GameTestHelper helper, BlockPos relativePos, int xOffset, int zOffset) {
        return helper.absolutePos(relativePos).offset(xOffset, 0, zOffset);
    }

    private static void prepareAbsoluteIsolationPlatform(ServerLevel level, BlockPos centerPos) {
        var floorY = centerPos.getY() - 1;
        for (var x = -2; x <= 2; ++x) {
            for (var z = -2; z <= 2; ++z) {
                var floorPos = new BlockPos(centerPos.getX() + x, floorY, centerPos.getZ() + z);
                var lowerAirPos = new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z);
                var upperAirPos = new BlockPos(centerPos.getX() + x, centerPos.getY() + 1, centerPos.getZ() + z);
                forceLoadChunk(level, floorPos);
                level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(lowerAirPos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(upperAirPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void prepareAbsoluteIsolationTargetPlatform(ServerLevel level, Vec3 targetCenter) {
        var floorPos = BlockPos.containing(targetCenter.x, targetCenter.y - 1.0D, targetCenter.z);
        forceLoadChunk(level, floorPos);
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(floorPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floorPos.above(2), Blocks.AIR.defaultBlockState(), 3);
    }

    private static FakePlayer createPersonalShelfPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        // owner lookup と openers の close 対象探索が server 側の player list / level lookup を使うため、
        // Personal Shelf の GameTest では FakePlayer もワールドへ参加させる。
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static BlockPos castPersonalShelf(GameTestHelper helper, FakePlayer player, BlockPos shelfPos, boolean exportMode, Direction exportFacing) {
        var spell = (PersonalShelf) SpellRegistry.PERSONAL_SHELF.get();
        var castData = new PersonalShelf.PersonalShelfCastData();
        var absoluteShelfPos = helper.absolutePos(shelfPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteShelfPos.getX());
        tag.putInt("PositionY", absoluteShelfPos.getY());
        tag.putInt("PositionZ", absoluteShelfPos.getZ());
        tag.putBoolean("ExportMode", exportMode);
        tag.putInt("ExportFacing", exportFacing.get3DDataValue());
        castData.deserializeNBT(helper.getLevel().registryAccess(), tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
        return absoluteShelfPos;
    }

    private static PersonalShelfChestBlockEntity getPersonalShelfBlockEntity(GameTestHelper helper, BlockPos absoluteShelfPos) {
        var blockEntity = helper.getLevel().getBlockEntity(absoluteShelfPos);
        helper.assertTrue(blockEntity instanceof PersonalShelfChestBlockEntity,
                "Expected Personal Shelf block entity but found " + blockEntity);
        return (PersonalShelfChestBlockEntity) blockEntity;
    }

    private static void castCompanionTrunk(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.COMPANION_TRUNK.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    private static CompanionTrunkEntity createCompanionTrunk(GameTestHelper helper, FakePlayer owner, BlockPos pos) {
        var trunk = new CompanionTrunkEntity(EntityRegistry.COMPANION_TRUNK.get(), helper.getLevel(), owner);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        trunk.moveTo(absolutePos.x, absolutePos.y, absolutePos.z, 0.0f, 0.0f);
        helper.getLevel().addFreshEntity(trunk);
        return trunk;
    }

    private static CompanionTrunkEntity getSingleCompanionTrunk(GameTestHelper helper, FakePlayer owner) {
        var trunks = helper.getLevel().getEntitiesOfClass(
                CompanionTrunkEntity.class,
                new AABB(owner.position(), owner.position()).inflate(16.0),
                trunk -> owner.getUUID().equals(trunk.getOwnerUuid())
        );
        helper.assertTrue(trunks.size() == 1, "Expected exactly one Companion Trunk but found " + trunks.size());
        return trunks.get(0);
    }

    private static BlockPos findCompanionTrunkChest(GameTestHelper helper, BlockPos center) {
        for (var y = -1; y <= 1; ++y) {
            for (var x = -1; x <= 1; ++x) {
                for (var z = -1; z <= 1; ++z) {
                    var candidate = center.offset(x, y, z);
                    if (helper.getBlockState(candidate).is(Blocks.CHEST)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean containsItem(ChestBlockEntity chest, Item item, int count) {
        for (var slot = 0; slot < chest.getContainerSize(); ++slot) {
            var stack = chest.getItem(slot);
            if (stack.is(item) && stack.getCount() == count) {
                return true;
            }
        }
        return false;
    }

    private static int countMatureHarvestMoonPlants(GameTestHelper helper, List<BlockPos> cropPositions) {
        var count = 0;
        for (var pos : cropPositions) {
            var state = helper.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                ++count;
                continue;
            }
            if (state.is(Blocks.NETHER_WART)
                    && state.hasProperty(NetherWartBlock.AGE)
                    && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
                ++count;
            }
        }
        return count;
    }

    private static Block requireForgeBlock(GameTestHelper helper, ResourceLocation id) {
        var block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        helper.assertTrue(block != null, "Missing required block for GameTest: " + id);
        return block;
    }

    private static Item requireForgeItem(GameTestHelper helper, ResourceLocation id) {
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        helper.assertTrue(item != null, "Missing required item for GameTest: " + id);
        return item;
    }

    private static BlockState withIntegerProperty(GameTestHelper helper, BlockState state, String propertyName, int value) {
        var property = findIntegerProperty(helper, state, propertyName);
        helper.assertTrue(property.getPossibleValues().contains(value),
                "Property " + propertyName + " does not accept " + value + " on " + state);
        return state.setValue(property, value);
    }

    private static int getIntegerPropertyValue(GameTestHelper helper, BlockState state, String propertyName) {
        return state.getValue(findIntegerProperty(helper, state, propertyName));
    }

    private static BlockState withBooleanProperty(GameTestHelper helper, BlockState state, String propertyName, boolean value) {
        return state.setValue(findBooleanProperty(helper, state, propertyName), value);
    }

    private static boolean getBooleanPropertyValue(GameTestHelper helper, BlockState state, String propertyName) {
        return state.getValue(findBooleanProperty(helper, state, propertyName));
    }

    private static IntegerProperty findIntegerProperty(GameTestHelper helper, BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && integerProperty.getName().equals(propertyName)) {
                return integerProperty;
            }
        }
        helper.fail("Missing integer property " + propertyName + " on " + state);
        throw new IllegalStateException("Unreachable after helper.fail");
    }

    private static BooleanProperty findBooleanProperty(GameTestHelper helper, BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && booleanProperty.getName().equals(propertyName)) {
                return booleanProperty;
            }
        }
        helper.fail("Missing boolean property " + propertyName + " on " + state);
        throw new IllegalStateException("Unreachable after helper.fail");
    }

    private static boolean isApprenticeSpell(AbstractSpell spell) {
        var spellId = spell.getSpellResource();
        return spellId != null && ApprenticeCodex.MODID.equals(spellId.getNamespace());
    }

    private static void assertCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<Item> itemPredicate,
            Set<ResourceLocation> expectedEnchantments
    ) {
        assertCategoryEnchantments(helper, categoryName, itemPredicate, stack -> expectedEnchantments);
    }

    private static void assertCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<Item> itemPredicate,
            java.util.function.Function<ItemStack, Set<ResourceLocation>> expectedEnchantmentsResolver
    ) {
        var stacks = getRegisteredItemStacks(itemPredicate);
        helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: " + categoryName);

        for (var stack : stacks) {
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantmentsResolver.apply(stack),
                    categoryName + " " + BuiltInRegistries.ITEM.getKey(stack.getItem())
            );
        }
    }

    private static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantments,
            String itemName
    ) {
        assertExactEnchantmentSurfaces(
                helper,
                stack,
                expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    expectedEnchantments,
                    itemName
        );
    }

    private static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedPrimaryEnchantments,
            Set<ResourceLocation> expectedSupportedEnchantments,
            Set<ResourceLocation> expectedDefinitionEnchantments,
            Set<ResourceLocation> expectedBookEnchantments,
            Set<ResourceLocation> expectedAnvilEnchantments,
            String itemName
    ) {
        var item = stack.getItem();
        var registryAccess = helper.getLevel().registryAccess();

        var actualPrimaryEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.isPrimaryItemFor(stack, enchantment)
        );
        helper.assertTrue(actualPrimaryEnchantments.equals(expectedPrimaryEnchantments),
                itemName + " primary enchantments changed: "
                        + describeEnchantmentDifference(expectedPrimaryEnchantments, actualPrimaryEnchantments));

        var actualSupportedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.supportsEnchantment(stack, enchantment)
        );
        helper.assertTrue(actualSupportedEnchantments.equals(expectedSupportedEnchantments),
                itemName + " supported enchantments changed: "
                        + describeEnchantmentDifference(expectedSupportedEnchantments, actualSupportedEnchantments));

        var actualDefinitionEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(stack)
        );
        helper.assertTrue(actualDefinitionEnchantments.equals(expectedDefinitionEnchantments),
                itemName + " enchantment definition support changed: "
                        + describeEnchantmentDifference(expectedDefinitionEnchantments, actualDefinitionEnchantments));

        var actualBookEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> item.isBookEnchantable(stack, createEnchantedBook(enchantment))
        );
        helper.assertTrue(actualBookEnchantments.equals(expectedBookEnchantments),
                itemName + " book enchantments changed: "
                        + describeEnchantmentDifference(expectedBookEnchantments, actualBookEnchantments));

        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            var actualAnvilEnchantments = collectAllowedEnchantments(
                    registryAccess,
                    enchantment -> mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment)
            );
            helper.assertTrue(actualAnvilEnchantments.equals(expectedAnvilEnchantments),
                    itemName + " anvil enchantments changed: "
                            + describeEnchantmentDifference(expectedAnvilEnchantments, actualAnvilEnchantments));
        }
    }

    private static void assertReferenceItemEnchantmentsWithRequiredExtras(
            GameTestHelper helper,
            ItemStack stack,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments,
            String itemName
    ) {
        var registryAccess = helper.getLevel().registryAccess();
        var expectedPrimaryEnchantments = expectedReferencePrimaryEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedSupportedEnchantments = expectedReferenceSupportedEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedDefinitionEnchantments = expectedReferenceDefinitionEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);
        var expectedBookEnchantments = expectedReferenceBookEnchantments(registryAccess, referenceStack, requiredExtraEnchantments);

        assertExactEnchantmentSurfaces(
                helper,
                stack,
                expectedPrimaryEnchantments,
                expectedSupportedEnchantments,
                expectedDefinitionEnchantments,
                expectedBookEnchantments,
                expectedSupportedEnchantments,
                itemName
        );

        var enchantmentLookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        for (var enchantmentId : requiredExtraEnchantments) {
            assertSingleEnchantmentSurfaces(
                    helper,
                    stack,
                    enchantmentLookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)),
                    true,
                    true,
                    true,
                    true,
                    stack.getItem() instanceof NonDamageableAnvilMergeItem ? Boolean.TRUE : null,
                    itemName + " required extra enchantment"
            );
        }
    }

    private static void assertRequiredExtraEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> requiredEnchantments,
            @Nullable Boolean expectedAnvil,
            String message
    ) {
        assertExtraEnchantments(helper, stack, requiredEnchantments, true, true, true, true, expectedAnvil, message);
    }

    private static void assertRejectedExtraEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> rejectedEnchantments,
            @Nullable Boolean expectedAnvil,
            String message
    ) {
        assertExtraEnchantments(helper, stack, rejectedEnchantments, false, false, false, false, expectedAnvil, message);
    }

    private static void assertExtraEnchantments(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> enchantmentIds,
            boolean expectedPrimary,
            boolean expectedSupported,
            boolean expectedDefinitionSupport,
            boolean expectedBook,
            @Nullable Boolean expectedAnvil,
            String message
    ) {
        var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var enchantmentId : enchantmentIds) {
            assertSingleEnchantmentSurfaces(
                    helper,
                    stack,
                    enchantmentLookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId)),
                    expectedPrimary,
                    expectedSupported,
                    expectedDefinitionSupport,
                    expectedBook,
                    expectedAnvil,
                    message
            );
        }
    }

    private static void assertSingleEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            net.minecraft.core.Holder<Enchantment> enchantment,
            boolean expectedPrimary,
            boolean expectedSupported,
            boolean expectedDefinitionSupport,
            boolean expectedBook,
            Boolean expectedAnvil,
            String message
    ) {
        var item = stack.getItem();
        var enchantmentId = enchantment.unwrapKey().orElseThrow().location();

        helper.assertTrue(item.isPrimaryItemFor(stack, enchantment) == expectedPrimary,
                message + " primary rule changed for " + enchantmentId + ": expected " + expectedPrimary);
        helper.assertTrue(item.supportsEnchantment(stack, enchantment) == expectedSupported,
                message + " supported rule changed for " + enchantmentId + ": expected " + expectedSupported);
        helper.assertTrue(enchantment.value().canEnchant(stack) == expectedDefinitionSupport,
                message + " definition support changed for " + enchantmentId + ": expected " + expectedDefinitionSupport);
        helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(enchantment)) == expectedBook,
                message + " book rule changed for " + enchantmentId + ": expected " + expectedBook);

        if (expectedAnvil != null) {
            helper.assertTrue(item instanceof NonDamageableAnvilMergeItem,
                    message + " anvil expectation requires NonDamageableAnvilMergeItem");
            var actualAnvil = ((NonDamageableAnvilMergeItem) item).isAnvilMergeEnchantmentAllowed(stack, enchantment);
            helper.assertTrue(actualAnvil == expectedAnvil,
                    message + " anvil rule changed for " + enchantmentId + ": expected " + expectedAnvil);
        }
    }

    private static List<ItemStack> getRegisteredItemStacks(Predicate<Item> itemPredicate) {
        return ItemRegistry.ITEMS.getEntries().stream()
                .map(DeferredHolder::get)
                .filter(itemPredicate)
                .sorted(Comparator.comparing(item -> String.valueOf(BuiltInRegistries.ITEM.getKey(item))))
                .map(ItemStack::new)
                .toList();
    }

    private static Set<ResourceLocation> expectedSpellGunEnchantments(ItemStack stack) {
        var expectedEnchantments = registryIdSet(
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedOffhandEnchantments() {
        return registryIdSet(
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TENSE,
                Enchantments.TRANSCENDENCE
        );
    }

    private static Set<ResourceLocation> expectedEnchantedCircletEnchantments() {
        var expectedEnchantments = new LinkedHashSet<>(expectedOffhandEnchantments());
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedReferencePrimaryEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().isPrimaryItemFor(referenceStack, enchantment)
        );
    }

    private static Set<ResourceLocation> expectedReferenceSupportedEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().supportsEnchantment(referenceStack, enchantment)
        );
    }

    private static Set<ResourceLocation> expectedReferenceDefinitionEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> enchantment.value().canEnchant(referenceStack)
        );
    }

    private static Set<ResourceLocation> expectedReferenceBookEnchantments(
            RegistryAccess registryAccess,
            ItemStack referenceStack,
            Set<ResourceLocation> requiredExtraEnchantments
    ) {
        return expectedReferenceEnchantments(
                registryAccess,
                requiredExtraEnchantments,
                enchantment -> referenceStack.getItem().isBookEnchantable(referenceStack, createEnchantedBook(enchantment))
        );
    }

    private static Set<ResourceLocation> expectedReferenceEnchantments(
            RegistryAccess registryAccess,
            Set<ResourceLocation> requiredExtraEnchantments,
            Predicate<net.minecraft.core.Holder<Enchantment>> predicate
    ) {
        var expectedEnchantments = collectAllowedEnchantments(registryAccess, predicate);
        expectedEnchantments.addAll(requiredExtraEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedRightClickMagicWeaponEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM
        ));
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedChargedTwinBladeStaffEnchantments(RegistryAccess registryAccess) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.TRIDENT))
                        && !isDurabilityTargetEnchantment(enchantment)
        ));
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.WISDOM
        ));
        return expectedEnchantments;
    }

    private static void assertChargedTwinBladeStaffThrownDamage(
            GameTestHelper helper,
            ItemStack stack,
            net.minecraft.world.entity.Entity target,
            net.minecraft.world.damagesource.DamageSource damageSource,
            String failureMessage
    ) {
        var level = helper.getLevel();
        var expectedDamage = EnchantmentHelper.modifyDamage(level, stack, target, damageSource, (float) ChargedTwinBladeStaff.resolveThrownDamage(stack));
        var actualDamage = ChargedTwinBladeStaff.resolveThrownDamage(level, stack, target, damageSource);
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-9D,
                failureMessage + ": target=" + EntityType.getKey(target.getType()) + ", expected=" + expectedDamage + ", actual=" + actualDamage);
    }

    private static Set<ResourceLocation> expectedReflectcastShieldEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.SHIELD))
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> requiredElementalBowExtraEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER,
                Enchantments.SYNTHESIS
        );
    }

    private static Set<ResourceLocation> requiredPastelStaffExtraEnchantments() {
        return registryIdSet(
                net.minecraft.world.item.enchantment.Enchantments.FORTUNE,
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
        );
    }

    private static Set<ResourceLocation> rejectedPastelStaffExtraEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM
        );
    }

    private static Set<ResourceLocation> requiredCrystalBladedStaffExtraEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM
        );
    }

    private static Set<ResourceLocation> rejectedCrystalBladedStaffExtraEnchantments() {
        return registryIdSet(
                net.minecraft.world.item.enchantment.Enchantments.FORTUNE,
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
        );
    }

    private static Set<ResourceLocation> requiredMalumMagicCapableWeaponEnchantments() {
        return Set.of(MALUM_HAUNTED, MALUM_ANIMATED);
    }

    private static Set<ResourceLocation> expectedFlaskEnchantments() {
        return registryIdSet(
                Enchantments.GUZZLE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY
        );
    }

    private static Set<ResourceLocation> expectedAlchemistsFlaskEnchantments() {
        return registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY
        );
    }

    private static Set<ResourceLocation> expectedRandomBookLootEnchantments() {
        return registryIdSet(
                Enchantments.ALACRITY,
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TENSE,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER
        );
    }

    private static Set<ResourceLocation> expectedEnchantressRobeEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedStealthRuneArmorEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(Enchantments.WISDOM));
        return expectedEnchantments;
    }

    private static ItemStack createArmorProbeStack(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            throw new IllegalArgumentException("Expected armor item for enchantment probe: " + stack);
        }

        return switch (armorItem.getType()) {
            case HELMET -> new ItemStack(Items.LEATHER_HELMET);
            case CHESTPLATE -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGGINGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case BOOTS -> new ItemStack(Items.LEATHER_BOOTS);
            default -> throw new IllegalArgumentException("Unsupported armor type for enchantment probe: " + armorItem.getType());
        };
    }

    @SafeVarargs
    private static Set<ResourceLocation> registryIdSet(ResourceKey<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            ids.add(enchantment.location());
        }
        return ids;
    }

    private static float getEquippedAttributeTotal(Player player, Holder<Attribute> attribute) {
        var total = 0.0F;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            total += (float) stack.getAttributeModifiers().modifiers().stream()
                    .filter(entry -> entry.slot().test(slot))
                    .filter(entry -> entry.attribute().equals(attribute))
                    .filter(entry -> entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                    .mapToDouble(entry -> entry.modifier().amount())
                    .sum();
        }
        return total;
    }

    private static void equipProtectionIvIronArmor(GameTestHelper helper, ServerPlayer player) {
        var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var allDamageProtection = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
            armorStack.enchant(allDamageProtection, 4);
            player.setItemSlot(slot, armorStack);
        }
    }

    private static float findDamageForArmorReducedTarget(
            ServerPlayer player,
            net.minecraft.world.damagesource.DamageSource source,
            float armor,
            float toughness,
            float targetReducedDamage
    ) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterAbsorb(player, high, source, armor, toughness) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterAbsorb(player, mid, source, armor, toughness);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static float findDamageForMagicReducedTarget(float protection, float targetReducedDamage) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterMagicAbsorb(high, protection) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(mid, protection);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static float resolveExpectedBarrierManaAfterHitForGameTest(float incomingDamage, float availableMana) {
        var remainingDamage = incomingDamage;
        var remainingMana = availableMana;

        while (remainingDamage >= 1.0F) {
            if (remainingMana >= 25.0F) {
                remainingDamage -= 1.0F;
                remainingMana -= 25.0F;
                continue;
            }
            if (remainingMana > 0.0F) {
                remainingDamage -= 1.0F;
                remainingMana = 0.0F;
            }
            break;
        }

        return Math.max(remainingMana, 0.0F);
    }

    private static float resolveExpectedSynchronizationManaAfterHitForGameTest(
            float incomingDamage,
            float availableMana,
            float protection
    ) {
        var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(incomingDamage, protection);
        var remainingMitigatedDamage = Math.max(incomingDamage - reducedDamage, 0.0F);
        var remainingMana = availableMana;

        while (remainingMitigatedDamage >= 1.0F && remainingMana >= 30.0F) {
            remainingMitigatedDamage -= 1.0F;
            remainingMana -= 30.0F;
        }

        if (remainingMitigatedDamage >= 1.0F) {
            return 0.0F;
        }

        return resolveExpectedBarrierManaAfterHitForGameTest(reducedDamage, remainingMana);
    }

    private static int countWholeDamageStepsForGameTest(float damage) {
        var remainingDamage = damage;
        var count = 0;
        while (remainingDamage >= 1.0F) {
            remainingDamage -= 1.0F;
            ++count;
        }
        return count;
    }

    private static void assertElementalBowMode(GameTestHelper helper, ItemStack stack, String expectedMode, String message) {
        var tag = getCustomDataTag(stack);
        var actualMode = tag != null && tag.contains("ElementalBowMode") ? tag.getString("ElementalBowMode") : null;
        helper.assertTrue(
                java.util.Objects.equals(actualMode, normalizeElementalBowModeId(expectedMode)),
                message + ": expected " + expectedMode + " but got " + actualMode
        );
    }

    private static void setElementalBowMode(ItemStack stack, String mode) {
        var normalizedMode = normalizeElementalBowModeId(mode);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowMode", normalizedMode));
    }

    private static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = getCustomDataTag(stack);
        var actualShotMode = tag != null && tag.contains("ElementalBowShotMode")
                ? tag.getString("ElementalBowShotMode")
                : null;
        ResourceLocation actualSelectionId = null;
        if (tag != null) {
            if ("magic".equals(actualShotMode) && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            } else if (tag.contains("ElementalBowAmmoSelection")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowAmmoSelection"));
            } else if (actualShotMode == null && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            }
        }
        helper.assertTrue(
                Objects.equals(actualShotMode, expectedShotMode) && Objects.equals(actualSelectionId, expectedSelectionId),
                message + ": expected shotMode=" + expectedShotMode + ", selection=" + expectedSelectionId
                        + " but got shotMode=" + actualShotMode + ", selection=" + actualSelectionId
        );
    }

    private static String describeElementalBowSelectionView(ElementalBow.ModeSelectionView view) {
        return view.selection().selectionId() == null
                ? view.selection().shotMode()
                : view.selection().shotMode() + ":" + view.selection().selectionId();
    }

    @Nullable
    private static ElementalBow.ModeSelectionView findElementalBowSelectionView(
            ServerPlayer player,
            ItemStack stack,
            String shotMode,
            @Nullable ResourceLocation selectionId
    ) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals(selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    private static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("ElementalBowShotMode", shotMode);
            if ("magic".equals(shotMode)) {
                if (selectionId != null) {
                    tag.putString("ElementalBowMode", selectionId.toString());
                }
                tag.remove("ElementalBowAmmoSelection");
                return;
            }

            if (selectionId != null) {
                tag.putString("ElementalBowAmmoSelection", selectionId.toString());
            } else {
                tag.remove("ElementalBowAmmoSelection");
            }
            tag.remove("ElementalBowMode");
        });
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static @Nullable String normalizeElementalBowModeId(@Nullable String mode) {
        if (mode == null || mode.isBlank()) {
            return mode;
        }
        return mode.contains(":")
                ? mode
                : ResourceLocation.fromNamespaceAndPath("irons_spellbooks", mode).toString();
    }

    private static Set<ResourceLocation> collectAllowedEnchantments(
            RegistryAccess registryAccess,
            Predicate<net.minecraft.core.Holder<Enchantment>> predicate
    ) {
        var allowedEnchantments = new LinkedHashSet<ResourceLocation>();
        var enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .sorted(Comparator.comparing(holder -> holder.key().location().toString()))
                .toList();

        for (var enchantment : enchantments) {
            if (predicate.test(enchantment)) {
                allowedEnchantments.add(enchantment.key().location());
            }
        }
        return allowedEnchantments;
    }

    private static Set<ResourceLocation> allRegisteredEnchantmentIds(RegistryAccess registryAccess) {
        return collectAllowedEnchantments(registryAccess, enchantment -> true);
    }

    private static ItemStack createEnchantedBook(net.minecraft.core.Holder<Enchantment> enchantment) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(enchantment, 1);
        return book;
    }

    private static boolean isDurabilityTargetEnchantment(net.minecraft.core.Holder<Enchantment> enchantment) {
        return enchantment.value().canEnchant(new ItemStack(Items.ELYTRA));
    }

    private static void addExpectedMalumSpiritPlunderIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID) && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            expectedEnchantments.add(MALUM_SPIRIT_PLUNDER);
        }
    }

    private static void addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID) && stack.is(MALUM_MAGIC_CAPABLE_WEAPON)) {
            expectedEnchantments.add(MALUM_HAUNTED);
            expectedEnchantments.add(MALUM_ANIMATED);
        }
    }

    private static String describeEnchantmentDifference(
            Set<ResourceLocation> expectedEnchantments,
            Set<ResourceLocation> actualEnchantments
    ) {
        var missingEnchantments = new LinkedHashSet<>(expectedEnchantments);
        missingEnchantments.removeAll(actualEnchantments);

        var unexpectedEnchantments = new LinkedHashSet<>(actualEnchantments);
        unexpectedEnchantments.removeAll(expectedEnchantments);

        return "missing=" + missingEnchantments + ", unexpected=" + unexpectedEnchantments;
    }

    private static void assertApprenticeEnchantmentFlags(
            GameTestHelper helper,
            ResourceKey<Enchantment> enchantmentKey,
            boolean expectedTreasureOnly,
            boolean expectedInEnchantingTable,
            boolean expectedTradeable,
            boolean expectedOnRandomLoot
    ) {
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
        var enchantmentId = enchantmentKey.location();
        var actualTreasureOnly = enchantment.is(EnchantmentTags.TREASURE);
        var actualInEnchantingTable = enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE);
        var actualTradeable = enchantment.is(EnchantmentTags.TRADEABLE);
        var actualOnRandomLoot = enchantment.is(EnchantmentTags.ON_RANDOM_LOOT);
        helper.assertTrue(actualTreasureOnly == expectedTreasureOnly,
                "Treasure tag changed for " + enchantmentId + ": expected " + expectedTreasureOnly + " but got " + actualTreasureOnly);
        helper.assertTrue(actualInEnchantingTable == expectedInEnchantingTable,
                "Enchanting-table tag changed for " + enchantmentId + ": expected " + expectedInEnchantingTable + " but got " + actualInEnchantingTable);
        helper.assertTrue(actualTradeable == expectedTradeable,
                "Tradeable tag changed for " + enchantmentId + ": expected " + expectedTradeable + " but got " + actualTradeable);
        helper.assertTrue(actualOnRandomLoot == expectedOnRandomLoot,
                "Random-loot tag changed for " + enchantmentId + ": expected " + expectedOnRandomLoot + " but got " + actualOnRandomLoot);
    }

    private static void assertBaseAttackModifier(
            GameTestHelper helper,
            ResourceLocation itemId,
            Item item,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation expectedModifierId
    ) {
        var stack = new ItemStack(item);
        var modifiers = item.getDefaultAttributeModifiers(stack).modifiers().stream()
                .filter(entry -> entry.attribute().equals(attribute) && entry.slot().equals(EquipmentSlotGroup.MAINHAND))
                .toList();
        helper.assertTrue(modifiers.size() == 1,
                "Expected exactly one " + attribute.unwrapKey().map(key -> key.location()).orElse(ResourceLocation.withDefaultNamespace("unknown"))
                        + " modifier on " + itemId + " but got " + modifiers.size());
        helper.assertTrue(modifiers.getFirst().modifier().id().equals(expectedModifierId),
                "Unexpected modifier id for " + itemId + " / "
                        + attribute.unwrapKey().map(key -> key.location()).orElse(ResourceLocation.withDefaultNamespace("unknown"))
                        + ": " + modifiers.getFirst().modifier().id());
    }

    private static void assertSwingcastStaffTier(
            GameTestHelper helper,
            AbstractSwingcastStaffItem item,
            Set<SpellGunCastType> expectedCastTypes,
            SwingcastCooldownMode expectedCooldownMode,
            AbstractSpell instantSpell,
            AbstractSpell longSpell,
            AbstractSpell continuousSpell,
            String itemName
    ) {
        var tier = item.getSwingcastStaffTier();
        helper.assertTrue(tier.supportedCastTypes().equals(expectedCastTypes),
                itemName + " cast type regression: expected " + expectedCastTypes + " but got " + tier.supportedCastTypes());
        helper.assertTrue(tier.swingcastCooldownMode() == expectedCooldownMode,
                itemName + " cooldown mode regression: expected " + expectedCooldownMode + " but got " + tier.swingcastCooldownMode());

        var allowsLong = expectedCastTypes.contains(SpellGunCastType.LONG);
        helper.assertTrue(item.canImbueSpell(instantSpell, 1),
                itemName + " should allow instant spell imbuing");
        helper.assertTrue(item.canImbueSpell(longSpell, 1) == allowsLong,
                itemName + " long spell imbue regression: expected " + allowsLong);
        helper.assertFalse(item.canImbueSpell(continuousSpell, 1),
                itemName + " should continue rejecting continuous spells");
    }

    private static void assertModifierAmount(
            GameTestHelper helper,
            ItemAttributeModifiers modifiers,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        assertModifierAmount(helper, modifiers, attribute, EquipmentSlotGroup.OFFHAND, expectedAmount, operation, message);
    }

    private static void assertModifierAmount(
            GameTestHelper helper,
            ItemAttributeModifiers modifiers,
            Attribute attribute,
            EquipmentSlotGroup slotGroup,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var actualAmount = modifiers.modifiers().stream()
                .filter(entry -> entry.slot().equals(slotGroup))
                .filter(entry -> entry.attribute().value() == attribute)
                .filter(entry -> entry.modifier().operation() == operation)
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    private static void assertCurioModifierAmount(
            GameTestHelper helper,
            top.theillusivec4.curios.api.type.capability.ICurioItem item,
            top.theillusivec4.curios.api.SlotContext slotContext,
            ItemStack stack,
            Holder<Attribute> attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var slotId = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "gametest/curio/%s_%d".formatted(slotContext.identifier(), slotContext.index())
        );
        var modifiers = item.getAttributeModifiers(slotContext, slotId, stack);
        var actualAmount = sumModifierAmount(modifiers.get(attribute), operation);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(modifiers));
    }

    private static double sumModifierAmount(
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.operation() == operation)
                .mapToDouble(AttributeModifier::amount)
                .sum();
    }

    private static String describeModifiers(com.google.common.collect.Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().value()) + "="
                        + entry.getValue().amount() + "@" + entry.getValue().operation())
                .collect(Collectors.joining(", "));
    }

    private static com.google.common.collect.Multimap<Holder<Attribute>, AttributeModifier> toModifierMultimap(
            ItemAttributeModifiers modifiers
    ) {
        var builder = com.google.common.collect.ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        for (var entry : modifiers.modifiers()) {
            builder.put(entry.attribute(), entry.modifier());
        }
        return builder.build();
    }

    private static void assertCastingMoveSpeedAdjustment(
            GameTestHelper helper,
            double externalBonus,
            double expectedAmount,
            String message
    ) {
        var actualAmount = CastingMoveSpeedAdjustment.computeAvailableBonus(externalBonus);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount + " for external bonus " + externalBonus);
    }

    private static void assertCastingMoveSpeedModifierAmount(
            GameTestHelper helper,
            net.minecraft.world.entity.ai.attributes.AttributeInstance attributeInstance,
            @org.jetbrains.annotations.Nullable ResourceLocation excludedModifierId,
            double expectedAmount,
            String message
    ) {
        var actualAmount = attributeInstance.getModifiers().stream()
                .filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_VALUE)
                .filter(modifier -> excludedModifierId == null || !excludedModifierId.equals(modifier.id()))
                .mapToDouble(AttributeModifier::amount)
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + attributeInstance.getModifiers());
    }

    private static void assertTranslatableKey(
            GameTestHelper helper,
            Component component,
            String expectedKey,
            String message
    ) {
        helper.assertTrue(
                component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()),
                message + ": expected " + expectedKey + " but got " + component
        );
    }

    private static void assertMainhandUpgradeBridge(
            GameTestHelper helper,
            ItemStack stack,
            String message
    ) {
        var upgradeData = createUpgradeData(
                helper.getLevel().registryAccess(),
                stack,
                io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                EquipmentSlot.MAINHAND.getName()
        );

        var event = new ItemAttributeModifierEvent(stack, stack.getItem().getDefaultAttributeModifiers(stack));
        NeoForge.EVENT_BUS.post(event);

        assertModifierAmount(
                helper,
                event.build(),
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.value(),
                EquipmentSlotGroup.MAINHAND,
                50.0D,
                AttributeModifier.Operation.ADD_VALUE,
                message + ": expected +50 max mana from " + upgradeData
        );
    }

    private static void assertUpgradeable(GameTestHelper helper, ItemStack stack, String message) {
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        helper.assertTrue(stack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                message + " (missing upgrade whitelist tag on " + itemId + ")");
        helper.assertTrue(Utils.canBeUpgraded(stack),
                message + " (Utils.canBeUpgraded returned false for " + itemId + ")");
    }

    private static UpgradeData createUpgradeData(
            RegistryAccess registryAccess,
            ItemStack stack,
            ResourceKey<io.redspace.ironsspellbooks.item.armor.UpgradeOrbType> upgradeKey,
            String slotName
    ) {
        var upgradeRegistry = registryAccess.registryOrThrow(io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY);
        var upgradeHolder = upgradeRegistry.getHolder(upgradeKey)
                .orElseThrow(() -> new IllegalStateException("Missing upgrade orb type: " + upgradeKey.location()));
        var upgradeData = new UpgradeData(java.util.Map.of(upgradeHolder, 1), slotName);
        UpgradeData.set(stack, upgradeData);
        return upgradeData;
    }

    @Nullable
    private static net.minecraft.resources.ResourceKey<io.redspace.ironsspellbooks.item.armor.UpgradeOrbType> findUpgradeKeyForPowerAttribute(
            Attribute spellPowerAttribute
    ) {
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.FIRE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ICE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.LIGHTNING_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.HOLY_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ENDER_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ENDER_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.BLOOD_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.EVOCATION_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.EVOCATION_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.NATURE_SPELL_POWER.value())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.NATURE_SPELL_POWER;
        }
        return null;
    }

    private static void assertSingleModifierAmount(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifiers = modifiers.stream()
                .filter(modifier -> modifier.operation() == operation)
                .toList();
        helper.assertTrue(matchingModifiers.size() == 1,
                message + ": expected exactly one " + operation + " modifier but got " + matchingModifiers);
        var actualAmount = matchingModifiers.get(0).amount();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    private static void placeAndAssertBlockEntity(
            GameTestHelper helper,
            BlockPos pos,
            net.minecraft.world.level.block.Block block,
            net.minecraft.world.level.block.entity.BlockEntityType<?> expectedType
    ) {
        helper.setBlock(pos, block);
        helper.assertBlockPresent(block, pos);

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity != null, "Missing block entity for " + BuiltInRegistries.BLOCK.getKey(block));
        helper.assertTrue(blockEntity.getType() == expectedType,
                "Block entity type mismatch for " + BuiltInRegistries.BLOCK.getKey(block) + ": " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    private static void assertRecipeLoaded(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            net.minecraft.world.item.crafting.RecipeSerializer<?> expectedSerializer,
            net.minecraft.world.item.crafting.RecipeType<?> expectedType
    ) {
        var recipeHolder = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipeHolder != null, "Missing recipe: " + recipeId);

        var recipe = recipeHolder.value();
        helper.assertTrue(recipe.getSerializer() == expectedSerializer,
                "Recipe serializer mismatch for " + recipeId + ": " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
        if (expectedType != null) {
            helper.assertTrue(recipe.getType() == expectedType,
                    "Recipe type mismatch for " + recipeId + ": " + BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()));
        }
    }

    private static void assertSearchBeaconTarget(GameTestHelper helper, Item item, String expectedTarget) {
        var definition = SearchBeaconTargetManager.getDefinition(new ItemStack(item));
        helper.assertTrue(definition != null, "SearchBeacon target missing for " + BuiltInRegistries.ITEM.getKey(item));
        helper.assertTrue(
                definition != null
                        && definition.targets().contains(new SearchBeaconTargetList.TargetReference(false, ResourceLocation.parse(expectedTarget))),
                "SearchBeacon target mismatch for " + BuiltInRegistries.ITEM.getKey(item)
        );
    }

    private static void assertVillageHousePoolContains(
            GameTestHelper helper,
            Registry<StructureTemplatePool> templatePoolRegistry,
            ResourceLocation poolId,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId,
            int expectedWeight
    ) {
        var pool = templatePoolRegistry.get(poolId);
        helper.assertTrue(pool != null, "Missing village house pool: " + poolId);

        var rawTemplates = ((StructureTemplatePoolAccessor) pool).apprenticecodex$getRawTemplates();
        var matchingRawEntries = rawTemplates.stream()
                .filter(pair -> isMatchingSinglePoolElement(pair.getFirst(), expectedStructureId, expectedProcessorId))
                .toList();

        helper.assertTrue(matchingRawEntries.size() == 1,
                "Expected exactly one Errand Mage entry in " + poolId + " but found " + matchingRawEntries.size());
        helper.assertTrue(
                !matchingRawEntries.isEmpty() && matchingRawEntries.get(0).getSecond() == expectedWeight,
                "Unexpected Errand Mage weight in " + poolId + ": " + (matchingRawEntries.isEmpty() ? "missing" : matchingRawEntries.get(0).getSecond())
        );

        long expandedMatchCount = pool.getShuffledTemplates(RandomSource.create(0L)).stream()
                .filter(element -> isMatchingSinglePoolElement(element, expectedStructureId, expectedProcessorId))
                .count();
        helper.assertTrue(expandedMatchCount == expectedWeight,
                "Expanded template count mismatch in " + poolId + ": " + expandedMatchCount);
    }

    private static boolean isMatchingSinglePoolElement(
            StructurePoolElement element,
            ResourceLocation expectedStructureId,
            ResourceLocation expectedProcessorId
    ) {
        if (!(element instanceof SinglePoolElement singlePoolElement)) {
            return false;
        }

        var accessor = (SinglePoolElementAccessor) singlePoolElement;
        var structureId = accessor.apprenticecodex$getTemplate().left().orElse(null);
        var processorId = accessor.apprenticecodex$getProcessors().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        return expectedStructureId.equals(structureId) && expectedProcessorId.equals(processorId);
    }

    private static void assertVillageHouseTemplateLoadsWithJigsaws(
            GameTestHelper helper,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager,
            ResourceLocation structureId,
            ResourceLocation expectedVillagerPool
    ) {
        var template = structureTemplateManager.get(structureId).orElse(null);
        helper.assertTrue(template != null, "Missing village house template: " + structureId);

        var jigsawBlocks = template != null
                ? template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true)
                : List.<StructureTemplate.StructureBlockInfo>of();
        helper.assertTrue(jigsawBlocks.size() == 2,
                "Unexpected jigsaw count for " + structureId + ": " + jigsawBlocks.size());

        boolean hasBottom = false;
        boolean hasEntrance = false;
        for (var jigsawBlock : jigsawBlocks) {
            var nbt = jigsawBlock.nbt();
            helper.assertTrue(nbt != null, "Village house jigsaw is missing NBT: " + structureId + " at " + jigsawBlock.pos());
            if (nbt == null) {
                continue;
            }

            var name = ResourceLocation.tryParse(nbt.getString("name"));
            var target = ResourceLocation.tryParse(nbt.getString("target"));
            var pool = ResourceLocation.tryParse(nbt.getString("pool"));
            if (ResourceLocation.withDefaultNamespace("bottom").equals(name)
                    && ResourceLocation.withDefaultNamespace("bottom").equals(target)
                    && expectedVillagerPool.equals(pool)) {
                hasBottom = true;
            }
            if (ResourceLocation.withDefaultNamespace("building_entrance").equals(name)
                    && ResourceLocation.withDefaultNamespace("building_entrance").equals(target)
                    && ResourceLocation.withDefaultNamespace("empty").equals(pool)) {
                hasEntrance = true;
            }
        }

        helper.assertTrue(hasBottom, "Village house is missing villager spawn jigsaw: " + structureId);
        helper.assertTrue(hasEntrance, "Village house is missing building entrance jigsaw: " + structureId);
    }

    private static LootParams createChestLootParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(new BlockPos(0, 1, 0)))
                .create(LootContextParamSets.CHEST);
    }

    private static LootParams createEmptyLootParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
    }

    private static LootContext createEmptyLootContext(GameTestHelper helper, long seed) {
        return new LootContext.Builder(createEmptyLootParams(helper))
                .withOptionalRandomSeed(seed)
                .create(Optional.of(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/random_applicable_enchantment")));
    }

    private static List<AbstractSpell> getNonLootableApprenticeSpells() {
        return List.of(
                SpellRegistry.EXTRACT.get(),
                SpellRegistry.UNITE_LUNA.get(),
                SpellRegistry.ILLUMINATE_STELLAR.get(),
                SpellRegistry.MANIFESTATION_GRIMOIRE.get(),
                SpellRegistry.MANA_SLASH.get()
        );
    }

    private static void assertLootTableGeneratesAllItems(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<Item> expectedItems
    ) {
        var remainingItems = expectedItems.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> remainingItems.remove(stack.getItem()));

        helper.assertTrue(remainingItems.isEmpty(),
                "Loot table " + lootTableId + " did not generate expected items within " + attempts + " attempts: "
                        + remainingItems.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    private static void assertLootTableGeneratesAnyItem(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<Item> expectedItems
    ) {
        var seenItems = new LinkedHashSet<Item>();
        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> {
            if (expectedItems.contains(stack.getItem())) {
                seenItems.add(stack.getItem());
            }
        });

        helper.assertTrue(!seenItems.isEmpty(),
                "Loot table " + lootTableId + " did not generate any of the expected items within " + attempts + " attempts: "
                        + expectedItems.stream().map(BuiltInRegistries.ITEM::getKey).toList());
    }

    private static void assertLootTableNeverGeneratesBlockedSpells(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            List<AbstractSpell> blockedSpells
    ) {
        var blockedSpellIds = blockedSpells.stream()
                .map(AbstractSpell::getSpellResource)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var seenBlockedSpells = new LinkedHashSet<ResourceLocation>();
        var scrollSampleCount = new AtomicInteger();

        sampleLootTable(helper, lootTableId, lootParams, attempts, stack -> {
            var spell = getScrollSpell(stack);
            if (spell == null) {
                return;
            }

            scrollSampleCount.incrementAndGet();
            var spellId = spell.getSpellResource();
            if (spellId != null && blockedSpellIds.contains(spellId)) {
                seenBlockedSpells.add(spellId);
            }
        });

        helper.assertTrue(scrollSampleCount.get() > 0,
                "Loot table " + lootTableId + " did not generate any spell scrolls within " + attempts + " attempts");
        helper.assertTrue(seenBlockedSpells.isEmpty(),
                "Loot table " + lootTableId + " generated blocked apprentice scrolls: " + seenBlockedSpells);
    }

    private static void sampleLootTable(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            java.util.function.Consumer<ItemStack> stackConsumer
    ) {
        var lootTable = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, lootTableId)
        );
        for (var i = 0; i < attempts; i++) {
            lootTable.getRandomItems(lootParams, stackConsumer);
        }
    }

    private static ExplorersCodexGuidebookTransferRecipe getExplorersCodexGuidebookTransferRecipe(GameTestHelper helper) {
        var recipeHolder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"))
                .orElse(null);
        var recipe = recipeHolder == null ? null : recipeHolder.value();
        helper.assertTrue(recipe instanceof ExplorersCodexGuidebookTransferRecipe,
                "Missing Explorer's Codex guidebook transfer recipe: " + recipe);
        return (ExplorersCodexGuidebookTransferRecipe) recipe;
    }

    private static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, stack);
        return stack;
    }

    private static @Nullable AbstractSpell getScrollSpell(ItemStack stack) {
        if (!stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        return spellData.getSpell();
    }

    private static ItemStack createInstantManaPotion(net.minecraft.world.item.alchemy.Potion potion) {
        return PotionContentsHelper.createPotionStack(Items.POTION, potion);
    }

    private static void assertPotionEffect(
            GameTestHelper helper,
            net.minecraft.world.item.alchemy.Potion potion,
            String expectedPotionId,
            int expectedDuration,
            int expectedAmplifier
    ) {
        var potionId = ResourceLocation.parse(expectedPotionId);
        helper.assertTrue(BuiltInRegistries.POTION.get(potionId) == potion,
                "Missing potion registry entry: " + potionId);

        var effects = potion.getEffects();
        helper.assertTrue(effects.size() == 1,
                "Potion " + potionId + " should have exactly one effect but got " + effects.size());

        var effect = effects.isEmpty() ? null : effects.get(0);
        helper.assertTrue(effect != null && effect.getEffect().value() == EffectRegistry.MANA_REGENERATION.get(),
                "Potion " + potionId + " should grant mana regeneration");
        helper.assertTrue(effect != null && effect.getDuration() == expectedDuration,
                "Potion " + potionId + " duration regression: "
                        + (effect == null ? "missing" : effect.getDuration()));
        helper.assertTrue(effect != null && effect.getAmplifier() == expectedAmplifier,
                "Potion " + potionId + " amplifier regression: "
                        + (effect == null ? "missing" : effect.getAmplifier()));
    }

    private static ItemStack createFilledSpellcastersFlask(
            RegistryAccess registryAccess,
            ItemStack storedItem,
            int doseCount,
            int glowEnergyLevel
    ) {
        var flask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
        if (glowEnergyLevel > 0) {
            registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.GLOW_ENERGY)
                    .ifPresent(enchantment -> flask.enchant(enchantment, glowEnergyLevel));
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }

    private static ItemStack createFilledAlchemistsFlask(
            RegistryAccess registryAccess,
            ItemStack storedItem,
            int doseCount,
            int glowEnergyLevel
    ) {
        var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
        if (glowEnergyLevel > 0) {
            registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.GLOW_ENERGY)
                    .ifPresent(enchantment -> flask.enchant(enchantment, glowEnergyLevel));
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }

    private static FakePlayer createExtractPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity getSingleExtractProjectile(
            GameTestHelper helper,
            FakePlayer owner
    ) {
        var projectiles = helper.getLevel().getEntitiesOfClass(
                jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity.class,
                new AABB(owner.blockPosition()).inflate(16.0D),
                projectile -> projectile.isAlive() && projectile.getOwner() == owner
        );
        helper.assertTrue(projectiles.size() == 1,
                "Expected exactly one Extract projectile but found " + projectiles.size());
        return projectiles.get(0);
    }

    private static SpellDispenserSpellValidator.ValidationResult createSpellDispenserValidation(ItemStack stack, AbstractSpell spell) {
        return new SpellDispenserSpellValidator.ValidationResult(stack, new SpellData(spell, 1), SpellDispenserSpellValidator.FailureReason.NONE);
    }

    private static GameProfile createSpellDispenserOwnerProfile(String name) {
        return new GameProfile(UUID.randomUUID(), name);
    }

    private static boolean skipWhenCreateMissing(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) {
            return false;
        }

        // optional 依存の absent 環境では Create 専用テストを成功扱いで抜け、通常検証の起動性を優先する。
        helper.succeed();
        return true;
    }

    private static void assertNoSpellDispenserProxy(GameTestHelper helper, BlockPos castPos, ItemStack spellSource, String message) {
        var proxyBox = new AABB(castPos).inflate(3.0D);
        var remainingProxies = helper.getLevel().getEntitiesOfClass(ArmorStand.class, proxyBox, stand ->
                stand.isInvisible() && stand.getMainHandItem().is(spellSource.getItem()));
        helper.assertTrue(remainingProxies.isEmpty(), message + ": " + remainingProxies.size());

        var remainingAnchors = helper.getLevel().getEntitiesOfClass(SpellDispenserAnchorEntity.class, proxyBox);
        helper.assertTrue(remainingAnchors.isEmpty(), message + " (tracked anchors): " + remainingAnchors.size());
    }

    private static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile
    ) {
        return invokeCreateGameTestHook(
                "createSpellDispenserMovementHarness",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStackHandler.class, GameProfile.class},
                level, worldPos, mountedInventory, ownerProfile
        );
    }

    private static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile,
            int currentMana,
            ItemStackHandler externalInventory,
            boolean externalAvailableForFuel
    ) {
        return invokeCreateGameTestHook(
                "createSpellDispenserMovementHarness",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStackHandler.class, GameProfile.class, int.class, ItemStackHandler.class, boolean.class},
                level, worldPos, mountedInventory, ownerProfile, currentMana, externalInventory, externalAvailableForFuel
        );
    }

    private static void startCreateSpellDispenserMovement(Object harness) {
        invokeCreateGameTestHook("startMoving", new Class<?>[]{Object.class}, harness);
    }

    private static void tickCreateSpellDispenserMovement(Object harness) {
        invokeCreateGameTestHook("tick", new Class<?>[]{Object.class}, harness);
    }

    private static void visitCreateSpellDispenserPosition(Object harness, BlockPos pos) {
        invokeCreateGameTestHook("visitNewPosition", new Class<?>[]{Object.class, BlockPos.class}, harness, pos);
    }

    private static void stopCreateSpellDispenserMovement(Object harness) {
        invokeCreateGameTestHook("stopMoving", new Class<?>[]{Object.class}, harness);
    }

    private static void setCreateSpellDispenserDisabled(Object harness, boolean disabled) {
        invokeCreateGameTestHook("setDisabled", new Class<?>[]{Object.class, boolean.class}, harness, disabled);
    }

    private static boolean hasCreateSpellDispenserContinuousCast(Object harness) {
        return invokeCreateGameTestHookBoolean("hasRunningContinuousCast", harness);
    }

    private static boolean createSpellDispenserRequiresReset(Object harness) {
        return invokeCreateGameTestHookBoolean("requiresContinuousReset", harness);
    }

    private static boolean createSpellDispenserIsCoolingDown(Object harness) {
        return invokeCreateGameTestHookBoolean("isCoolingDown", harness);
    }

    private static boolean invokeCreateGameTestHookBoolean(String methodName, Object harness) {
        return (boolean) invokeCreateGameTestHook(methodName, new Class<?>[]{Object.class}, harness);
    }

    private static Object invokeCreateGameTestHook(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            var hooksClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            return hooksClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest helper call failed: " + methodName, exception);
        }
    }

    private static ItemStack createInitializedPresetStack(Item item) {
        var stack = new ItemStack(item);
        if (item instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        }
        return stack;
    }

    private static void applyRestrictedImbueNormalization(
            GameTestHelper helper,
            ItemStack stack,
            RestrictedSpellImbuableItem item,
            AbstractSpell spell,
            int spellLevel
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before restricted imbue normalization test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before restricted imbue normalization test");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare unlocked spell data before restricted imbue normalization test");
        ISpellContainer.set(stack, mutable.toImmutable());
        item.normalizeImbuedSpellContainer(stack);
    }

    private static void setSingleUnlockedSpell(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before Focus Staffbow spell setup");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before Focus Staffbow spell setup");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare Focus Staffbow spell setup");
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    private static void applyPresetSpellExtraction(GameTestHelper helper, ItemStack stack) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before preset extraction test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before preset extraction test");
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        PresetSpellContainerStateHelper.rememberCleared(stack);
    }

    private static void applyLegacyLockedReplacement(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before legacy replacement test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before legacy replacement test");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, true),
                "Failed to prepare legacy locked replacement spell");
        ISpellContainer.set(stack, mutable.toImmutable());
        PresetSpellContainerStateHelper.clearRememberedState(stack);
    }

    private static ItemStack roundTripItemStack(GameTestHelper helper, ItemStack stack) {
        return ItemStack.parseOptional(
                helper.getLevel().registryAccess(),
                (CompoundTag) stack.saveOptional(helper.getLevel().registryAccess())
        );
    }

    private static void repairPresetSpellContainerStateIfNeeded(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            spellGunItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractRightClickMagicWeaponItem magicWeaponItem) {
            magicWeaponItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractImbueShieldItem imbueShieldItem) {
            imbueShieldItem.repairPresetSpellContainerStateIfNeeded(stack);
        }
    }

    private static void assertClearedSpellContainer(GameTestHelper helper, ItemStack stack, String message) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + ": spell container is null");
        helper.assertTrue(spellContainer.getActiveSpellCount() <= 0,
                message + ": expected no active spells but got " + spellContainer.getActiveSpellCount());
        helper.assertTrue(spellContainer.getSpellAtIndex(0) == SpellData.EMPTY,
                message + ": slot 0 unexpectedly contains " + spellContainer.getSpellAtIndex(0).getSpell().getSpellResource());
    }

    private static CraftingInput createCraftingInput(ItemStack... stacks) {
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; ++i) {
            items.set(i, stacks[i]);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static void fillSpellContainerToActiveCount(
            GameTestHelper helper,
            io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable mutable,
            int targetActiveCount
    ) {
        for (var spell : io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells()) {
            if (mutable.getActiveSpellCount() >= targetActiveCount) {
                break;
            }
            if (mutable.getIndexForSpell(spell) >= 0) {
                continue;
            }

            helper.assertTrue(mutable.addSpell(spell, 1, false),
                    "Failed to prepare overflow test spell: " + spell.getSpellResource());
        }

        helper.assertTrue(mutable.getActiveSpellCount() == targetActiveCount,
                "Failed to prepare overflow Explorer's Codex: expected " + targetActiveCount + " active spells but got "
                        + mutable.getActiveSpellCount());
    }

    private static ItemStack createAutocastAmuletStack(GameTestHelper helper, int spellSlotCount, SpellData... spells) {
        var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
        var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
        var stack = item.getDefaultInstance();

        while (true) {
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Autocast Amulet setup lost its spell container");
            if (spellContainer != null && spellContainer.getMaxSpellCount() >= spellSlotCount) {
                break;
            }

            stack = item.createSpellSlotUpgradeResult(stack, upgradeItem);
            helper.assertFalse(stack.isEmpty(),
                    "Failed to prepare Autocast Amulet with " + spellSlotCount + " spell slots");
        }

        for (var spellData : spells) {
            stack = item.createArcaneAnvilImbueResult(stack, spellData);
            helper.assertFalse(stack.isEmpty(),
                    "Failed to imbue Autocast Amulet with " + spellData.getSpell().getSpellResource());
        }

        return stack;
    }

    private static List<AutoMagnetFamiliarEntity> getOwnedAutoMagnetFamiliars(GameTestHelper helper, FakePlayer owner) {
        return helper.getLevel().getEntitiesOfClass(
                AutoMagnetFamiliarEntity.class,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                familiar -> {
                    var summonOwner = familiar.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    private static boolean invokeAutocastBeginCast(
            ServerPlayer player,
            MagicData magicData,
            ItemStack stack,
            SpellData spellData,
            int spellLevel,
            String castingSlot,
            int scaledManaCost
    ) {
        try {
            var method = AutocastAmuletAutoCastEvent.class.getDeclaredMethod(
                    "beginAutoCast",
                    ServerPlayer.class,
                    MagicData.class,
                    ItemStack.class,
                    SpellData.class,
                    int.class,
                    String.class,
                    int.class
            );
            method.setAccessible(true);
            return (boolean) method.invoke(null, player, magicData, stack, spellData, spellLevel, castingSlot, scaledManaCost);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Autocast Amulet auto-cast helper for GameTest", exception);
        }
    }

    private static void runAutocastAmuletServerTick(FakePlayer player, int tickCount) {
        player.tickCount = tickCount;
        AutocastAmuletAutoCastEvent.onPlayerTick(new PlayerTickEvent.Post(player));
        jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onPlayerTick(
                new PlayerTickEvent.Post(player)
        );
    }

    private static void assertSpellData(
            GameTestHelper helper,
            ISpellContainer spellContainer,
            int index,
            AbstractSpell expectedSpell,
            int expectedLevel,
            boolean expectedLocked,
            String message
    ) {
        var spellData = spellContainer.getSpellAtIndex(index);
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                message + " (spell slot is empty at index " + index + ")");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
        helper.assertTrue(spellData.isLocked() == expectedLocked,
                message + " (locked mismatch: " + spellData.isLocked() + ")");
    }

    private static void assertRecipePresent(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId
    ) {
        helper.assertTrue(recipeManager.byKey(recipeId).isPresent(), "Missing recipe: " + recipeId);
    }

    private static void assertEnchantmentsRegistered(GameTestHelper helper) {
        var enchantmentRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var key : getRegisteredEnchantments()) {
            helper.assertTrue(enchantmentRegistry.get(key).isPresent(), "Missing enchantment registry entry: " + key.location());
        }
    }

    private static List<ResourceKey<Enchantment>> getRegisteredEnchantments() {
        return List.of(
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.ALACRITY,
                Enchantments.TENSE,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER,
                Enchantments.GUZZLE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY,
                Enchantments.SYNTHESIS
        );
    }

    private static <T> void assertBuiltinRegistryEntries(
            GameTestHelper helper,
            String registryName,
            Registry<T> registry,
            Collection<? extends DeferredHolder<T, ? extends T>> entries
    ) {
        for (var entry : entries) {
            var id = entry.getId();
            helper.assertTrue(id != null, "Missing " + registryName + " id");
            helper.assertTrue(registry.get(id) == entry.get(),
                    "Missing " + registryName + " registry entry: " + id);
        }
    }
}
