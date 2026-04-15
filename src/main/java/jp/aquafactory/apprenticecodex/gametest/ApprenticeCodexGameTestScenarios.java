package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
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
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.mixin.SinglePoolElementAccessor;
import jp.aquafactory.apprenticecodex.mixin.StructureTemplatePoolAccessor;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSearchService;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeStats;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PoiTypeRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.utility.ApprenticeEnchantmentAvailability;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.worldgen.ErrandMageVillageAddition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";
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
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("create", "contraption_controlled")
    );
    private static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();
    private static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");

    private ApprenticeCodexGameTestScenarios() {
    }

    @GameTest(template = TEMPLATE)
    public static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertForgeRegistryEntries(helper, "item", net.minecraftforge.registries.ForgeRegistries.ITEMS, ItemRegistry.ITEMS.getEntries());
            assertForgeRegistryEntries(helper, "block", net.minecraftforge.registries.ForgeRegistries.BLOCKS, BlockRegistry.BLOCKS.getEntries());
            assertForgeRegistryEntries(helper, "block entity", net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES, BlockEntityRegistry.BLOCK_ENTITY_TYPES.getEntries());
            assertForgeRegistryEntries(helper, "entity", net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES, EntityRegistry.ENTITIES.getEntries());
            assertForgeRegistryEntries(helper, "mob effect", net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS, EffectRegistry.EFFECTS.getEntries());
            assertForgeRegistryEntries(helper, "enchantment", net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS, EnchantmentRegistry.ENCHANTMENTS.getEntries());
            assertForgeRegistryEntries(helper, "attribute", net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES, ApprenticeAttributeRegistry.ATTRIBUTES.getEntries());
            assertForgeRegistryEntries(helper, "recipe serializer", net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, RecipeRegistry.RECIPE_SERIALIZERS.getEntries());
            assertForgeRegistryEntries(helper, "point of interest type", net.minecraftforge.registries.ForgeRegistries.POI_TYPES, PoiTypeRegistry.POI_TYPES.getEntries());
            assertForgeRegistryEntries(helper, "villager profession", net.minecraftforge.registries.ForgeRegistries.VILLAGER_PROFESSIONS, VillagerProfessionRegistry.VILLAGER_PROFESSIONS.getEntries());

            assertBuiltinRegistryEntries(helper, "potion", BuiltInRegistries.POTION, PotionRegistry.POTIONS.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe type", BuiltInRegistries.RECIPE_TYPE, RecipeRegistry.RECIPE_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "creative tab", BuiltInRegistries.CREATIVE_MODE_TAB, CreativeTabRegistry.TABS.getEntries());

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
                helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.REGISTRY.get().getValue(spellId) == spell,
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

            var divinePearl = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "divine_pearl"));
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

    @GameTest(template = TEMPLATE)
    public static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var damagedCrown = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get());
            damagedCrown.setDamageValue(2);
            var taggedCrownCost = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get());
            taggedCrownCost.getOrCreateTag().putString("apprenticecodex_test", "cost");
            var crownOffer = new net.minecraft.world.item.trading.MerchantOffer(
                    taggedCrownCost,
                    new ItemStack(Items.EMERALD),
                    16,
                    2,
                    0.05F
            );
            helper.assertTrue(crownOffer.satisfiedBy(damagedCrown, ItemStack.EMPTY),
                    "Damaged crown should satisfy the errand mage buy offer even when the saved cost stack has tags");

            var taggedScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            taggedScroll.getOrCreateTag().putString("apprenticecodex_test", "tagged");
            var taggedScrollCost = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            taggedScrollCost.getOrCreateTag().putString("apprenticecodex_test", "cost");
            var scrollOffer = new net.minecraft.world.item.trading.MerchantOffer(
                    taggedScrollCost,
                    new ItemStack(Items.EMERALD, 16),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                    3,
                    5,
                    0.05F
            );
            helper.assertTrue(scrollOffer.satisfiedBy(taggedScroll, new ItemStack(Items.EMERALD, 16)),
                    "Tagged scroll should satisfy the errand mage ink trade even when the saved cost stack has tags");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void customRecipeDataIsLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_transfer"),
                    RecipeRegistry.SPELLCASTERS_FLASK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"),
                    RecipeRegistry.SPELLCASTERS_FLASK_EXTRACT_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "irons_guide_book_repair"),
                    RecipeRegistry.IRONS_GUIDE_BOOK_REPAIR_SERIALIZER.get(), null);
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

            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).isEmpty(),
                    "No Essence Smoker recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).isEmpty(),
                    "No Grind Runner recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).isEmpty(),
                    "No Spellcaster Workbench recipes were loaded");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskRejectsSplashAndLingeringPotions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
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

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = createFilledSpellcastersFlask(
                    PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION),
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

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var originalEffect = PotionUtils.getMobEffects(storedPotion).get(0);
            var flask = createFilledSpellcastersFlask(storedPotion, 1, 2);
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

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var flask = createFilledSpellcastersFlask(
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

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var flask = createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var craftingContainer = createCraftingContainer(flask, new ItemStack(Items.GLASS_BOTTLE));

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should match a filled flask and glass bottle");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingContainer).get(0);

            helper.assertTrue(ItemStack.isSameItemSameTags(result,
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

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskAcceptsSplashLingeringAndSimpleElixirButRejectsNormalPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var emptyFlask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
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

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 16,
                    "Alchemist's Flask base capacity mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));

            if (EnchantmentRegistry.LARGE_MUG.isPresent()) {
                flask.enchant(EnchantmentRegistry.LARGE_MUG.get(), 1);
                helper.assertTrue(SpellcastersFlask.getMaxDoseCapacity(flask) == 20,
                        "Alchemist's Flask Large Mug bonus mismatch: " + SpellcastersFlask.getMaxDoseCapacity(flask));
            }

            var filledFlask = SpellcastersFlask.copyWithAddedDoses(flask, splashPotion, 16);
            helper.assertTrue(!filledFlask.isEmpty(), "Alchemist's Flask failed to store sixteen splash potion doses");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(filledFlask) == 16,
                    "Alchemist's Flask stored dose count mismatch: " + SpellcastersFlask.getStoredDoseCount(filledFlask));

            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"))
                    .orElseThrow();
            var craftingContainer = createCraftingContainer(
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()), splashPotion, 1),
                    new ItemStack(Items.GLASS_BOTTLE)
            );

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Spellcaster's Flask extract recipe should accept Alchemist's Flask");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            var remainingFlask = recipe.getRemainingItems(craftingContainer).get(0);

            helper.assertTrue(ItemStack.isSameItemSameTags(result, splashPotion),
                    "Alchemist's Flask extract recipe returned the wrong potion");
            helper.assertTrue(remainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask extract recipe did not return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(remainingFlask) == 0,
                    "Alchemist's Flask extract recipe left dose count behind: " + SpellcastersFlask.getStoredDoseCount(remainingFlask));
            helper.assertTrue(SpellcastersFlask.getStoredItem(remainingFlask).isEmpty(),
                    "Alchemist's Flask extract recipe left StoredItem behind");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (jp.aquafactory.apprenticecodex.spell.extract.Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_precast_hand_test");
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var magicData = MagicData.getPlayerMagicData(player);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(splashPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when the main hand flask is filled");
            helper.assertTrue(magicData.getAdditionalCastData() instanceof jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData,
                    "Extract should store cast data for the selected flask");
            var mainCastData = (jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(mainCastData.hand() == InteractionHand.MAIN_HAND,
                    "Extract should prefer the main hand filled flask");
            helper.assertTrue(ItemStack.isSameItemSameTags(mainCastData.storedItem(), splashPotion),
                    "Extract selected the wrong stored item from the main hand flask");

            magicData.setAdditionalCastData(null);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should cast when only the offhand flask is filled");
            var offhandCastData = (jp.aquafactory.apprenticecodex.spell.extract.Extract.ExtractCastData) magicData.getAdditionalCastData();
            helper.assertTrue(offhandCastData.hand() == InteractionHand.OFF_HAND,
                    "Extract should fall back to the offhand filled flask when the main hand flask is empty");
            helper.assertTrue(ItemStack.isSameItemSameTags(offhandCastData.storedItem(), lingeringPotion),
                    "Extract selected the wrong stored item from the offhand flask");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (jp.aquafactory.apprenticecodex.spell.extract.Extract) SpellRegistry.EXTRACT.get();
            var player = createExtractPlayer(helper, new BlockPos(0, 2, 0), "extract_cast_projectile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);

            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(lingeringPotion, 2, 0));
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
            player.setItemInHand(InteractionHand.OFF_HAND, createFilledAlchemistsFlask(simpleElixir, 2, 0));
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

    @GameTest(template = TEMPLATE)
    public static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            if (EnchantmentRegistry.RED_ENERGY.isPresent()) {
                flask.enchant(EnchantmentRegistry.RED_ENERGY.get(), 1);
            }
            if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 1);
            }
            flask = SpellcastersFlask.copyWithAddedDoses(flask, storedPotion, 1);

            var thrownPotion = jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.createExtractedPotionForThrow(flask, 1);
            var originalEffect = PotionUtils.getMobEffects(storedPotion).get(0);
            var extractedEffect = PotionUtils.getMobEffects(thrownPotion).get(0);

            helper.assertTrue(thrownPotion.is(Items.SPLASH_POTION),
                    "Extract should preserve non-lingering contents as splash potions");
            helper.assertTrue(extractedEffect.getAmplifier() == originalEffect.getAmplifier() + 2,
                    "Extract should add both Glow Energy and Extract amplification bonuses");
            helper.assertTrue(extractedEffect.getDuration() == Math.max(1, Math.round(originalEffect.getDuration() * 1.25F * 0.5F)),
                    "Extract should keep spell-side amplify while Glow Energy halves the Red Energy adjusted duration");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsSingleMagicMissileScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a simple Magic Missile scroll");
            helper.assertTrue(validation.spellData().getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Spell Dispenser validator resolved the wrong spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsNonIronsNamespaceScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.MAGE_LIGHT.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected an apprenticecodex spell scroll");
            helper.assertTrue(validation.spellData().getSpell() == SpellRegistry.MAGE_LIGHT.get(),
                    "Spell Dispenser validator resolved the wrong non-Iron's spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsLongScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.COMPOUND_PHIAL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a LONG scroll");
            helper.assertTrue(validation.spellData().getSpell() == SpellRegistry.COMPOUND_PHIAL.get(),
                    "Spell Dispenser validator resolved the wrong LONG spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsDenylistedSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.ASSIST_WINGS.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a denylisted scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.DENYLISTED,
                    "Spell Dispenser validator returned the wrong failure reason for denylisted scroll: " + validation.failureReason());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsContinuousScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.LONG_STRIDE.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a CONTINUOUS scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_PROFILED,
                    "Spell Dispenser validator returned the wrong failure reason for profile-less CONTINUOUS scroll: " + validation.failureReason());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsProfiledContinuousScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(validation.isSupported(), "Spell Dispenser validator rejected a profiled CONTINUOUS scroll");
            helper.assertTrue(validation.spellData().getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(),
                    "Spell Dispenser validator resolved the wrong profiled CONTINUOUS spell: " + validation.spellData().getSpell().getSpellResource());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsRecastScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(SpellRegistry.HIGANBANA.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a recast spell scroll");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.HAS_RECAST,
                    "Spell Dispenser validator returned the wrong failure reason for recast scroll: " + validation.failureReason());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsNonScrollSpellContainer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var nonScrollStack = createInitializedPresetStack(ItemRegistry.GRIMOIRE_MANIFEST.get());

            helper.assertTrue(ISpellContainer.isSpellContainer(nonScrollStack), "Prepared non-scroll test stack is not a spell container");
            var validation = SpellDispenserSpellValidator.validate(nonScrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted a non-scroll spell container");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.NOT_SCROLL,
                    "Spell Dispenser validator returned the wrong failure reason for non-scroll spell container: " + validation.failureReason());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperStopsContinuousCastAtDurationCap(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStartsAndClearsInstantCooldown(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            var manaFlask = createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var alchemistsManaFlask = createFilledAlchemistsFlask(
                    PotionUtils.setPotion(
                            new ItemStack(Items.SPLASH_POTION),
                            io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
                    ),
                    1,
                    0
            );
            var nonManaPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var nonManaFlask = createFilledSpellcastersFlask(nonManaPotion, 1, 0);

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
                            alchemistsManaFlask
                    ),
                    "Spell Dispenser flask slot accepted an Alchemist's Flask"
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromHighestManaPotionThatFits(GameTestHelper helper) {
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
                    ItemStack.isSameItemSameTags(
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var glowEnergyFlask = createFilledSpellcastersFlask(
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
            helper.assertTrue(
                    SpellcastersFlask.getStoredItem(
                            spellDispenser.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START)
                    ).isEmpty(),
                    "Spell Dispenser left StoredItem behind after consuming the last mana flask dose"
            );
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserActivationFailsWhenManaIsInsufficient(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
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

            helper.assertTrue(ItemStack.isSameItemSameTags(externalInventory.getStackInSlot(0), manaPotion),
                    "Create-mounted Spell Dispenser consumed a potion from protected contraption storage");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get());
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.SPELL_SLOT_INDEX, scrollStack.copy());

            helper.assertTrue(itemHandler != null, "Spell Dispenser item capability was not exposed");
            helper.assertTrue(itemHandler != null && !itemHandler.isItemValid(0, scrollStack),
                    "Spell Dispenser sided automation exposed the scroll slot as insertable");

            var remainder = itemHandler == null ? scrollStack.copy() : itemHandler.insertItem(0, scrollStack.copy(), false);
            helper.assertTrue(ItemStack.isSameItemSameTags(remainder, scrollStack),
                    "Spell Dispenser sided automation inserted a scroll into the protected slot");
            helper.assertTrue(itemHandler != null && ItemStack.isSameItemSameTags(itemHandler.getStackInSlot(0), scrollStack),
                    "Spell Dispenser sided automation did not expose the protected scroll contents");
            helper.assertTrue(itemHandler != null && itemHandler.extractItem(0, 1, false).isEmpty(),
                    "Spell Dispenser sided automation extracted the protected scroll slot");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            var filledFlask = createFilledSpellcastersFlask(
                    createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, filledFlask);
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()));
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 2, new ItemStack(Items.GLASS_BOTTLE));

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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuKeepsContraptionInventoryAccessible(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spell_dispenser_mounted_menu_test"));
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var currentMana = new AtomicInteger(320);
            var ownerName = "spell_dispenser_mounted_menu_owner_test";

            var menu = SpellDispenserMenu.createMounted(0, new Inventory(player), BlockPos.ZERO, mountedInventory, true, ownerName, currentMana::get);
            helper.assertTrue(menu.stillValid(player), "Spell Dispenser mounted menu closed because it expected a world block entity");
            helper.assertTrue(ItemStack.isSameItemSameTags(menu.getSpellSource(), scrollStack),
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spell_dispenser_owner_optional_menu_test"));
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            mountedInventory.setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()));

            var menu = SpellDispenserMenu.createMounted(0, new Inventory(player), BlockPos.ZERO, mountedInventory, false, null, 320);
            helper.assertTrue(menu.isReadyToCast(player),
                    "Spell Dispenser mounted menu still required an owner for an owner-optional spell");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellDispenserStack = new ItemStack(ItemRegistry.SPELL_DISPENSER.get());
            helper.assertTrue(spellDispenserStack.is(CREATE_CONTRAPTION_CONTROLLED),
                    "Spell Dispenser is missing create:contraption_controlled and cannot be selected by Contraption Controls");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_tag_test");
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            blockEntity.setOwnerProfile(ownerProfile);

            var restoredOwner = SpellDispenserBlockEntity.readOwnerProfile(blockEntity.getUpdateTag());
            helper.assertTrue(restoredOwner != null, "Spell Dispenser owner profile helper returned null");
            helper.assertTrue(restoredOwner != null && ownerProfile.getId().equals(restoredOwner.getId()),
                    "Spell Dispenser owner UUID helper returned the wrong value");
            helper.assertTrue(restoredOwner != null && ownerProfile.getName().equals(restoredOwner.getName()),
                    "Spell Dispenser owner name helper returned the wrong value");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfilePersistsThroughNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_save_test");
            var original = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            original.setOwnerProfile(ownerProfile);

            var restored = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            restored.load(original.getUpdateTag());

            helper.assertTrue(restored.hasOwnerProfile(), "Spell Dispenser owner profile was not restored from NBT");
            helper.assertTrue(restored.getOwnerProfile() != null && ownerProfile.getId().equals(restored.getOwnerProfile().getId()),
                    "Spell Dispenser owner UUID changed during NBT round-trip");
            helper.assertTrue(restored.getOwnerProfile() != null && ownerProfile.getName().equals(restored.getOwnerProfile().getName()),
                    "Spell Dispenser owner name changed during NBT round-trip");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 0), BlockRegistry.MAGE_LIGHT_TORCH.get(), BlockEntityRegistry.MAGE_LIGHT_TORCH.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 0), BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 0), BlockRegistry.RIFT_HOLE.get(), BlockEntityRegistry.RIFT_HOLE.get());
            placeAndAssertBlockEntity(helper, new BlockPos(3, 1, 0), BlockRegistry.ARCANUM_IN_A_JAR.get(), BlockEntityRegistry.ARCANUM_IN_A_JAR.get());
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

    @GameTest(template = TEMPLATE)
    public static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
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
            for (var schoolType : io.redspace.ironsspellbooks.api.registry.SchoolRegistry.REGISTRY.get().getValues()) {
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

    @GameTest(template = TEMPLATE)
    public static void senseEvilExpandsHorizontalReachToCube(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var caster = createSenseEvilPlayer(helper, new BlockPos(0, 2, 0), "sense_evil_horizontal_cube_test");
            var spell = (SenseEvil) SpellRegistry.SENSE_EVIL.get();
            var range = getSenseEvilRange(spell, caster, 1);
            var oldHorizontalHalfExtent = range + caster.getBbWidth() * 0.5;
            var zombieCenter = caster.getBoundingBox().getCenter().add(oldHorizontalHalfExtent + 0.5, 0.0, 0.0);
            var zombie = spawnPositionedZombie(level, zombieCenter);

            var highlights = collectSenseEvilHighlights(spell, level, 1, caster);
            assertSenseEvilHighlightPresent(helper, highlights, zombie.getBoundingBox().getCenter(), 0.25,
                    "SenseEvil should detect undead in the added X direction cube band");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void senseEvilUsesSameCubeForSpawnersAndEntities(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var caster = createSenseEvilPlayer(helper, new BlockPos(0, 2, 0), "sense_evil_spawner_cube_test");
            var spell = (SenseEvil) SpellRegistry.SENSE_EVIL.get();
            var range = getSenseEvilRange(spell, caster, 1);
            var diagonalOffset = Mth.floor(range * 0.75);

            helper.assertTrue(Math.sqrt(2.0 * diagonalOffset * diagonalOffset) > range,
                    "Diagonal test offset must stay outside the old spherical spawner range");

            var zombieCenter = caster.getBoundingBox().getCenter().add(diagonalOffset, 0.0, diagonalOffset);
            var zombie = spawnPositionedZombie(level, zombieCenter);
            var spawnerPos = caster.blockPosition().offset(diagonalOffset, 0, diagonalOffset);
            placeZombieSpawner(level, spawnerPos);

            var highlights = collectSenseEvilHighlights(spell, level, 1, caster);
            assertSenseEvilHighlightPresent(helper, highlights, zombie.getBoundingBox().getCenter(), 0.25,
                    "SenseEvil should still detect entities at the shared diagonal cube offset");
            assertSenseEvilHighlightPresent(helper, highlights, Vec3.atCenterOf(spawnerPos), 0.25,
                    "SenseEvil should detect spawners at the same diagonal cube offset as entities");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeCurioBonusLootTableContainsAllThreeItems(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void genericLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void ironsStructureLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void catacombsLootIncludesScarletThirstBonusDrop(GameTestHelper helper) {
        helper.succeedIf(() -> assertLootTableGeneratesAnyItem(
                helper,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/catacombs/coffin_loot"),
                createChestLootParams(helper),
                256,
                List.of(ItemRegistry.SCARLET_THIRST.get())
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
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
            var modifiers = item.getAttributeModifiers(slotContext, UUID.randomUUID(), stack);
            helper.assertTrue(modifiers.isEmpty(),
                    "Isekai Travel Guidebook should not add spellbook attributes: " + describeModifiers(modifiers));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            explorersCodexStack.setHoverName(Component.literal("写本継承確認"));
            explorersCodexStack.setRepairCost(7);
            var expectedUpgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    explorersCodexStack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.OFFHAND.getName()
            );
            EnchantmentHelper.setEnchantments(Map.of(Enchantments.UNBREAKING, 1), explorersCodexStack);

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingContainer = createCraftingContainer(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Explorer's Codex + Isekai Travel Guidebook should match the transfer recipe");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ItemRegistry.EXPLORERS_CODEX.get()),
                    "Transfer recipe should return Explorer's Codex but got " + ForgeRegistries.ITEMS.getKey(result.getItem()));
            helper.assertTrue("写本継承確認".equals(result.getHoverName().getString()),
                    "Explorer's Codex custom name was not preserved: " + result.getHoverName().getString());
            helper.assertTrue(result.getBaseRepairCost() == 7,
                    "Explorer's Codex repair cost was not preserved: " + result.getBaseRepairCost());
            helper.assertTrue(UpgradeData.getUpgradeData(result).equals(expectedUpgradeData),
                    "Explorer's Codex upgrade data was not preserved: " + UpgradeData.getUpgradeData(result));
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, result) == 1,
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

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(5);
            helper.assertTrue(mutable.addSpell(SpellRegistry.HEALING_BLOOM.get(), 1, true),
                    "Failed to prepare duplicate Healing Bloom on Explorer's Codex");
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingContainer = createCraftingContainer(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingContainer, helper.getLevel()),
                    "Recipe should still match when one guidebook spell is already present");

            var result = recipe.assemble(craftingContainer, helper.getLevel().registryAccess());
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

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(14);
            fillSpellContainerToActiveCount(helper, mutable, 14);
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingContainer = createCraftingContainer(explorersCodexStack, guidebookStack);

            helper.assertFalse(recipe.matches(craftingContainer, helper.getLevel()),
                    "Recipe should reject Explorer's Codex when transferred spells would exceed 15 slots");
            helper.assertTrue(recipe.assemble(craftingContainer, helper.getLevel().registryAccess()).isEmpty(),
                    "Overflow recipe assembly should return empty result");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(GameTestHelper helper) {
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

            // ここでは school ID の厳密一致ではなく、
            // 実装が解決した spell power 属性へ bonus / Attunement が正しく合算されることを回帰検知する.
            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Copper Spell Amplifier could not resolve spell power attribute for additive stacking: " + imbuedSchool.getId());

            assertModifierAmount(helper, item, stack, resolvedSpellPower, 0.10D, AttributeModifier.Operation.MULTIPLY_BASE,
                    "Copper Spell Amplifier additive spell power bonus regression");

            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            assertModifierAmount(helper, item, stack, resolvedSpellPower, 0.14D, AttributeModifier.Operation.MULTIPLY_BASE,
                    "Copper Spell Amplifier + Attunement stacking regression");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var diamondItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get();
            var diamondStack = new ItemStack(diamondItem);
            assertModifierAmount(
                    helper,
                    diamondItem,
                    diamondStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.get(),
                    0.25D,
                    AttributeModifier.Operation.ADDITION,
                    "Diamond Spell Amplifier casting move speed bonus regression"
            );

            var netheriteItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get();
            var netheriteStack = new ItemStack(netheriteItem);
            assertModifierAmount(
                    helper,
                    netheriteItem,
                    netheriteStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.get(),
                    0.50D,
                    AttributeModifier.Operation.ADDITION,
                    "Netherite Spell Amplifier casting move speed bonus regression"
            );
        });
    }

    @GameTest(template = TEMPLATE)
    public static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()),
                    "Ender Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get()),
                    "AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.PHOTON_SIPHON.get()),
                    "Direct AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    "AbstractSpellGunItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    "AbstractRightClickMagicWeaponItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get()),
                    "Indirect AbstractRightClickMagicWeaponItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get()),
                    "New swing magic weapon descendants should be upgradeable");

            var shieldStack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertFalse(shieldStack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                    "Reflectcast Shield should not be in the upgrade whitelist");
            helper.assertFalse(Utils.canBeUpgraded(shieldStack),
                    "Reflectcast Shield should remain excluded from the upgrade system");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
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

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), AttributeModifier.Operation.ADDITION) - 12.0D) < 1.0e-9D,
                    "Unite Luna Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED), AttributeModifier.Operation.ADDITION) - (-3.2D)) < 1.0e-9D,
                    "Unite Luna Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get()), AttributeModifier.Operation.ADDITION) - 0.5D) < 1.0e-9D,
                    "Unite Luna Staff entity reach regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()), AttributeModifier.Operation.MULTIPLY_BASE) - 0.05D) < 1.0e-9D,
                    "Unite Luna Staff spell power regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.get()), AttributeModifier.Operation.MULTIPLY_BASE) - 0.10D) < 1.0e-9D,
                    "Unite Luna Staff holy spell power regression: " + describeModifiers(modifiers));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    EquipmentSlot.OFFHAND,
                    ItemRegistry.COPPER_SPELL_AMPLIFIER.get().getAttributeModifiers(EquipmentSlot.OFFHAND, stack)
            );
            MinecraftForge.EVENT_BUS.post(event);

            var maxManaAmount = sumModifierAmount(
                    event.getModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()),
                    AttributeModifier.Operation.ADDITION
            );
            helper.assertTrue(Math.abs(maxManaAmount - 50.0D) < 1.0e-9D,
                    "Offhand upgrade bridge regression: expected +50 max mana from mainhand-stored upgrade but got "
                            + maxManaAmount + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(event.getModifiers()));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat regression test");
            var spellbreakerAttributes = net.bettercombat.logic.WeaponRegistry.getAttributes(new ItemStack(spellbreaker));
            helper.assertTrue(spellbreakerAttributes != null && spellbreakerAttributes.isTwoHanded(),
                    "Better Combat spellbreaker should resolve as a two-handed weapon but got " + spellbreakerAttributes);

            var amplifierStack = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var amplifierEvent = new ItemAttributeModifierEvent(
                    amplifierStack,
                    EquipmentSlot.OFFHAND,
                    ItemRegistry.IRON_SPELL_AMPLIFIER.get().getAttributeModifiers(EquipmentSlot.OFFHAND, amplifierStack)
            );
            MinecraftForge.EVENT_BUS.post(amplifierEvent);

            var spellPowerBonus = sumModifierAmount(
                    amplifierEvent.getModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(spellPowerBonus - 0.05D) < 1.0e-9D,
                    "Iron Spell Amplifier should expose +0.05 spell power in offhand modifiers but got "
                            + spellPowerBonus + " modifiers=" + describeModifiers(amplifierEvent.getModifiers()));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var ironAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            ironAmplifier.enchant(EnchantmentRegistry.SURGE.get(), 1);
            var rescuedIronModifiers =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .buildRescueModifiers(ironAmplifier);

            var rescuedSpellPowerBonus = sumModifierAmount(
                    rescuedIronModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(rescuedSpellPowerBonus - 0.07D) < 1.0e-9D,
                    "Better Combat rescue should keep Iron Spell Amplifier + Surge at +0.07 spell power but got "
                            + rescuedSpellPowerBonus + " modifiers=" + describeModifiers(rescuedIronModifiers));

            var copperAmplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var copperAmplifier = new ItemStack(copperAmplifierItem);
            copperAmplifierItem.initializeSpellContainer(copperAmplifier);
            copperAmplifier.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            var rescuedCopperModifiers =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .buildRescueModifiers(copperAmplifier);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(copperAmplifier);
            helper.assertTrue(imbuedSchool != null,
                    "Copper Spell Amplifier rescue test could not resolve imbued school");
            var imbuedSpellPowerAttribute =
                    jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Copper Spell Amplifier rescue test could not resolve school spell power attribute");

            var rescuedAttunementBonus = sumModifierAmount(
                    rescuedCopperModifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(rescuedAttunementBonus - 0.14D) < 1.0e-9D,
                    "Better Combat rescue should keep Copper Spell Amplifier base + Attunement at +0.14 but got "
                            + rescuedAttunementBonus + " modifiers=" + describeModifiers(rescuedCopperModifiers));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "better_combat_hidden_offhand_attribute_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker but returned " + player.getOffhandItem());

            var physicalOffhand =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .getPhysicalOffhandStack(player);
            helper.assertTrue(
                    physicalOffhand.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "Physical offhand resolver should keep Silver Spell Amplifier but got " + physicalOffhand
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .isRescueActive(player),
                    "Better Combat rescue should stay active while physical offhand stack exists"
            );

            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var expectedMaxManaBonus = sumModifierAmount(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .buildRescueModifiers(physicalOffhand)
                            .get(maxManaAttribute),
                    AttributeModifier.Operation.ADDITION
            );
            helper.assertTrue(expectedMaxManaBonus > 0.0D,
                    "Silver Spell Amplifier Better Combat rescue should provide positive max mana but got "
                            + expectedMaxManaBonus);

            var baseMaxMana = player.getAttributeValue(maxManaAttribute);
            jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat.sync(player);
            var rescuedMaxMana = player.getAttributeValue(maxManaAttribute);
            helper.assertTrue(Math.abs((rescuedMaxMana - baseMaxMana) - expectedMaxManaBonus) < 1.0e-9D,
                    "Better Combat rescue should restore Silver Spell Amplifier max mana by "
                            + expectedMaxManaBonus + " but changed from " + baseMaxMana + " to " + rescuedMaxMana);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat spell rescue test");

            var copperAmplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var copperAmplifier = new ItemStack(copperAmplifierItem);
            copperAmplifierItem.initializeSpellContainer(copperAmplifier);
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

    @GameTest(template = TEMPLATE)
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCastingMoveSpeedAdjustment(helper, 0.0D, 0.8D, "No external bonus should keep full cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.25D, 0.55D, "Diamond-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.50D, 0.30D, "Netherite-equivalent bonus should reduce shared cancellation");
            assertCastingMoveSpeedAdjustment(helper, 0.75D, 0.05D, "Small remaining headroom should stay positive");
            assertCastingMoveSpeedAdjustment(helper, 0.80D, 0.0D, "Exact cap should stop adding more casting move speed");
            assertCastingMoveSpeedAdjustment(helper, 1.10D, 0.0D, "External overshoot should not become a negative correction");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.get();
            var movementSpeedModifier = effect.getAttributeModifiers().get(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            helper.assertTrue(movementSpeedModifier != null, "LongStride is missing the movement speed attribute modifier");

            var actualAmount = effect.getAttributeModifierValue(0, movementSpeedModifier);
            helper.assertTrue(Math.abs(actualAmount - 0.15D) < 1.0e-9D,
                    "LongStride movement speed bonus regression: expected 0.15 but got " + actualAmount);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var foodProperties = ItemRegistry.COMFORT_BERRIES.get().getFoodProperties();
            helper.assertTrue(foodProperties != null, "Comfort Berries should remain edible");
            helper.assertTrue(foodProperties != null && foodProperties.getNutrition() == 4,
                    "Comfort Berries nutrition regression: " + (foodProperties == null ? "null" : foodProperties.getNutrition()));
            helper.assertTrue(foodProperties != null && Math.abs(foodProperties.getSaturationModifier() - 1.2f) < 1.0e-6F,
                    "Comfort Berries saturation modifier regression: "
                            + (foodProperties == null ? "null" : foodProperties.getSaturationModifier()));
            helper.assertTrue(foodProperties != null && foodProperties.canAlwaysEat(),
                    "Comfort Berries should remain edible even when full");

            var matchingEffects = foodProperties == null ? List.<com.mojang.datafixers.util.Pair<net.minecraft.world.effect.MobEffectInstance, Float>>of()
                    : foodProperties.getEffects().stream()
                    .filter(effectPair -> effectPair.getFirst().getEffect() == EffectRegistry.MANA_REGENERATION.get())
                    .toList();
            helper.assertTrue(matchingEffects.size() == 1,
                    "Comfort Berries should grant exactly one mana regeneration effect but got " + matchingEffects.size());

            var effectPair = matchingEffects.isEmpty() ? null : matchingEffects.get(0);
            helper.assertTrue(effectPair != null && effectPair.getFirst().getDuration() == 20 * 30,
                    "Comfort Berries mana regeneration duration regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getDuration()));
            helper.assertTrue(effectPair != null && effectPair.getFirst().getAmplifier() == 0,
                    "Comfort Berries mana regeneration level regression: "
                            + (effectPair == null ? "missing" : effectPair.getFirst().getAmplifier()));
            helper.assertTrue(effectPair != null && Math.abs(effectPair.getSecond() - 1.0f) < 1.0e-6F,
                    "Comfort Berries mana regeneration chance regression: "
                            + (effectPair == null ? "missing" : effectPair.getSecond()));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var effect = (jp.aquafactory.apprenticecodex.effect.ManaRegeneration) EffectRegistry.MANA_REGENERATION.get();
            var manaRegenModifier = effect.getAttributeModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get());
            helper.assertTrue(manaRegenModifier != null, "Mana Regeneration is missing the mana regen attribute modifier");
            helper.assertTrue(manaRegenModifier != null
                            && manaRegenModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL,
                    "Mana Regeneration should use MULTIPLY_TOTAL but got "
                            + (manaRegenModifier == null ? "missing" : manaRegenModifier.getOperation()));

            var levelOneAmount = manaRegenModifier == null ? Double.NaN : effect.getAttributeModifierValue(0, manaRegenModifier);
            helper.assertTrue(Math.abs(levelOneAmount - 0.25D) < 1.0e-9D,
                    "Mana Regeneration Lv1 regression: expected 0.25 but got " + levelOneAmount);

            var levelTwoAmount = manaRegenModifier == null ? Double.NaN : effect.getAttributeModifierValue(1, manaRegenModifier);
            helper.assertTrue(Math.abs(levelTwoAmount - 0.50D) < 1.0e-9D,
                    "Mana Regeneration Lv2 regression: expected 0.50 but got " + levelTwoAmount);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertPotionEffect(helper, PotionRegistry.MEDITATION.get(), "apprenticecodex:meditation", 20 * 60 * 3, 0);
            assertPotionEffect(helper, PotionRegistry.LONG_MEDITATION.get(), "apprenticecodex:long_meditation", 20 * 60 * 8, 0);
            assertPotionEffect(helper, PotionRegistry.STRONG_MEDITATION.get(), "apprenticecodex:strong_meditation", 20 * 90, 1);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spell Gun",
                item -> item instanceof AbstractSpellGunItem,
                ApprenticeCodexGameTestScenarios::expectedSpellGunEnchantments
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds();
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractOffhandMagicItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Offhand Magic Item");

            for (var stack : stacks) {
                // 1.20.1 の offhand 系は isBookEnchantable を個別制限していないため、
                // 本判定だけは広く通る。Malum 側は main hand 前提で soul_hunter_weapon を使うため、
                // 実際に固定したい付与面はエンチャント台と独自金床側の offhand 非対応面。
                assertExactEnchantmentSurfaces(
                        helper,
                        stack,
                        expectedOffhandEnchantments(stack),
                        expectedBookEnchantments,
                        expectedOffhandEnchantments(stack),
                        "Offhand Magic Item " + ForgeRegistries.ITEMS.getKey(stack.getItem())
                );
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantedCircletEnchantments(stack),
                    allRegisteredEnchantmentIds(),
                    expectedEnchantedCircletEnchantments(stack),
                    "Enchanted Circlet"
            );
        });
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var item = (top.theillusivec4.curios.api.type.capability.ICurioItem) stack.getItem();
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.HEAD,
                    helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    -0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet attack damage penalty regression"
            );

            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);
            stack.enchant(EnchantmentRegistry.ALACRITY.get(), 1);
            stack.enchant(EnchantmentRegistry.REFLUX.get(), 1);
            stack.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            stack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            stack.enchant(EnchantmentRegistry.TENSE.get(), 1);

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
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get(),
                    0.02D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Alacrity regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get(),
                    0.05D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Reflux regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    20.0D,
                    AttributeModifier.Operation.ADDITION,
                    "Enchanted Circlet Reservoir regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get(),
                    0.02D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Surge regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    resolvedSpellPower,
                    0.04D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Attunement regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get(),
                    0.05D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Tense regression"
            );
        });
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Enchanted Circlet should be extractable in Spellcaster Workbench");
            helper.assertFalse(new ItemStack(ItemRegistry.ASHEN_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Ashen Circlet should remain non-extractable in Spellcaster Workbench");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "enchanted_circlet_wisdom_test"));
            var baseExperience = 20;

            var withoutCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0)), player, baseExperience);
            MinecraftForge.EVENT_BUS.post(withoutCirclet);
            helper.assertTrue(withoutCirclet.getDroppedExperience() == baseExperience,
                    "Wisdom baseline should stay unchanged without enchanted circlet");

            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            circletStack.enchant(EnchantmentRegistry.WISDOM.get(), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var withCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0)), player, baseExperience);
            MinecraftForge.EVENT_BUS.post(withCirclet);
            helper.assertTrue(withCirclet.getDroppedExperience() == 21,
                    "Enchanted Circlet Wisdom should match armor rate (+5% at level 1) but got " + withCirclet.getDroppedExperience());

            var roundedUp = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0)), player, 1);
            MinecraftForge.EVENT_BUS.post(roundedUp);
            helper.assertTrue(roundedUp.getDroppedExperience() == 2,
                    "Wisdom should round enemy experience up from 1 to 2 at +5% but got " + roundedUp.getDroppedExperience());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();

            var baselinePlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_baseline_test"));
            var baselineExperience = new BlockEvent.BreakEvent(level, new BlockPos(0, 2, 0), state, baselinePlayer);
            baselineExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(baselineExperience);
            helper.assertTrue(baselineExperience.getExpToDrop() == 3,
                    "Block experience should stay unchanged without Wisdom but got " + baselineExperience.getExpToDrop());

            var curioPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_curio_test"));
            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            circletStack.enchant(EnchantmentRegistry.WISDOM.get(), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(curioPlayer)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for block wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var roundedCurioExperience = new BlockEvent.BreakEvent(level, new BlockPos(1, 2, 0), state, curioPlayer);
            roundedCurioExperience.setExpToDrop(1);
            WisdomExperienceDropEvent.onBlockBreak(roundedCurioExperience);
            helper.assertTrue(roundedCurioExperience.getExpToDrop() == 2,
                    "Curio Wisdom should round block experience up from 1 to 2 at +5% but got " + roundedCurioExperience.getExpToDrop());

            var heldPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_held_test"));
            var spellGunStack = new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
            spellGunStack.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            heldPlayer.setItemInHand(InteractionHand.MAIN_HAND, spellGunStack);

            var heldExperience = new BlockEvent.BreakEvent(level, new BlockPos(2, 2, 0), state, heldPlayer);
            heldExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(heldExperience);
            helper.assertTrue(heldExperience.getExpToDrop() == 4,
                    "Held Wisdom should increase block experience from 3 to 4 at +20% but got " + heldExperience.getExpToDrop());
        });
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createCraftsmansDelightPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_external_spell_discount_test");
            equipCraftsmansDelight(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
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

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = new TouchDigSpell();
            var player = createCraftsmansDelightPlayer(helper, new BlockPos(0, 2, 0), "touch_dig_range_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var targetPos = helper.absolutePos(new BlockPos(0, 3, 12));

            helper.assertTrue(magicData != null, "Touch Dig range test could not resolve player mana data");
            player.setYRot(0.0f);
            player.setXRot(0.0f);
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipCraftsmansDelight(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped");
            helper.assertTrue(spell.getUniqueInfo(1, player).stream().anyMatch(component -> component.getString().contains("16")),
                    "Touch Dig unique info should display 16 block range while CraftsmansDelight is equipped");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createCraftsmansDelightPlayer(helper, new BlockPos(0, 2, 0), "touch_dig_ring_enchant_merge_test");
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(Enchantments.BLOCK_FORTUNE, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            equipCraftsmansDelight(player, ringStack);

            var mergedFortuneTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(mergedFortuneTool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) == 3,
                    "Touch Dig should prefer the higher Fortune level from the ring");

            ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipCraftsmansDelight(player, ringStack);

            var mergedSilkTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(mergedSilkTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "Touch Dig should inherit Silk Touch from the ring");
            helper.assertTrue(mergedSilkTool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) == 0,
                    "Touch Dig should drop Fortune when Silk Touch is present");

            var blockPos = helper.absolutePos(new BlockPos(0, 2, 1));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(blockPos).inflate(1.5D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig with ring Silk Touch should not drop cobblestone");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createCraftsmansDelightPlayer(helper, new BlockPos(0, 2, 0), "touch_dig_bare_hand_ring_enchant_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipCraftsmansDelight(player, ringStack);

            var synthesizedTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertFalse(synthesizedTool.isEmpty(),
                    "Touch Dig should synthesize a mining tool when the caster is bare-handed but the ring has mining enchantments");
            helper.assertTrue(synthesizedTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "Touch Dig should copy Silk Touch onto the synthesized bare-hand tool");

            var blockPos = helper.absolutePos(new BlockPos(0, 2, 2));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(blockPos).inflate(1.5D));
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should not drop cobblestone");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createCraftsmansDelightPlayer(helper, new BlockPos(0, 2, 0), "spectral_hammer_ring_enchant_test");
            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipCraftsmansDelight(player, ringStack);

            var targetPos = helper.absolutePos(new BlockPos(0, 2, 2));
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            var hammer = new SpectralHammer(
                    helper.getLevel(),
                    player,
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.NORTH, targetPos, false),
                    0,
                    1
            );
            var hammerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 1)));
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

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Right Click Magic Weapon",
                // 1.21.1申し送り事項:
                // 1.20.1 では StaffItem にしていない武器でも、1.21.1 側では StaffItem 化する場合がある。
                // ここは 1.20.1 の AbstractRightClickMagicWeaponItem 系の付与面を固定し、
                // port 時に StaffItem へ寄せた結果の差分を意図的に見えるようにしておく。
                item -> item instanceof AbstractRightClickMagicWeaponItem,
                ApprenticeCodexGameTestScenarios::expectedRightClickMagicWeaponEnchantments
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Reflectcast Shield is missing malum:soul_hunter_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedReflectcastShieldEnchantments(stack),
                    "Reflectcast Shield"
            );
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spellcasters Flask",
                item -> item.getClass() == SpellcastersFlask.class,
                expectedFlaskEnchantments()
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Alchemists Flask",
                item -> item.getClass() == AlchemistsFlask.class,
                expectedAlchemistsFlaskEnchantments()
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.ALACRITY, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.REFLUX, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.RESERVOIR, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SURGE, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.ATTUNEMENT, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.TENSE, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.WISDOM, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.PLUNDER, false, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.TRANSCENDENCE, true, true, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.GUZZLE, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.LARGE_MUG, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.RED_ENERGY, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.GLOW_ENERGY, false, false, true);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var function = EnchantRandomlyFunction.randomApplicableEnchantment().build();
            var seenApprenticeEnchantments = new LinkedHashSet<ResourceLocation>();

            for (long seed = 0L; seed < 4096L; ++seed) {
                var result = function.apply(new ItemStack(Items.BOOK), createEmptyLootContext(helper, seed));
                var enchantments = EnchantmentHelper.getEnchantments(result);
                helper.assertTrue(result.is(Items.ENCHANTED_BOOK),
                        "Random applicable enchantment loot should convert books into enchanted books");
                helper.assertTrue(enchantments.size() == 1,
                        "Random applicable enchantment loot should apply exactly one enchantment: " + enchantments);

                for (var enchantment : enchantments.keySet()) {
                    var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                    if (enchantmentId == null || !ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
                        continue;
                    }

                    helper.assertFalse(ApprenticeEnchantmentAvailability.isFlaskExclusiveEnchantment(enchantment),
                            "Random applicable enchantment loot included flask enchantment: " + enchantmentId + " at seed " + seed);
                    seenApprenticeEnchantments.add(enchantmentId);
                }
            }

            var expectedEnchantments = expectedRandomBookLootEnchantments();
            helper.assertTrue(seenApprenticeEnchantments.containsAll(expectedEnchantments),
                    "Random applicable enchantment loot lost apprentice enchantments: "
                            + describeEnchantmentDifference(expectedEnchantments, seenApprenticeEnchantments));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCategoryEnchantments(
                    helper,
                    "Enchantress Robe",
                    item -> item instanceof EnchantressRobeItem,
                    ApprenticeCodexGameTestScenarios::expectedEnchantressRobeEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    ApprenticeCodexGameTestScenarios::expectedStealthRuneArmorEnchantments
            );
        });
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var lightningSpellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get();
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

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - EnchantressRobeStats.MAX_MANA_BONUS_PER_PIECE) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - 0.10D) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " spell power regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Enchantress Robe " + armorType + " imbue surface regression: hasImbueSlot="
                                + item.hasImbueSlot() + " stack=" + stack);

                var lightningSpellPowerBonus = sumModifierAmount(
                        modifiers.get(lightningSpellPowerAttribute),
                        AttributeModifier.Operation.MULTIPLY_BASE
                );
                helper.assertTrue(Math.abs(lightningSpellPowerBonus) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " should not gain school spell power before imbue: "
                                + describeModifiers(modifiers));
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (EnchantressRobeItem) ItemRegistry.ENCHANTRESS_ROBE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Enchantress Robe chestplate test could not resolve imbued school");
            var imbuedSpellPowerAttribute =
                    jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Enchantress Robe chestplate test could not resolve school spell power attribute");

            var modifiers = item.getAttributeModifiers(EquipmentSlot.CHEST, stack);
            var globalSpellPowerBonus = sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(globalSpellPowerBonus - 0.10D) < 1.0e-9D,
                    "Enchantress Robe chestplate should keep +0.10 spell power after imbue: " + describeModifiers(modifiers));

            var imbuedSchoolSpellPowerBonus = sumModifierAmount(
                    modifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(imbuedSchoolSpellPowerBonus - 0.05D) < 1.0e-9D,
                    "Enchantress Robe chestplate should add +0.05 imbued school spell power: " + describeModifiers(modifiers));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void pastelStaffKeepsItsLocalEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            var item = stack.getItem();
            var expectedVanillaEnchantments = Set.of(
                    ResourceLocation.withDefaultNamespace("fortune"),
                    ResourceLocation.withDefaultNamespace("knockback"),
                    ResourceLocation.withDefaultNamespace("looting"),
                    ResourceLocation.withDefaultNamespace("silk_touch")
            );

            var actualAllowedVanillaEnchantments = collectAllowedEnchantments(
                    stack,
                    enchantment -> {
                        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                        return enchantmentId != null
                                && VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())
                                && item.canApplyAtEnchantingTable(stack, enchantment);
                    }
            );
            helper.assertTrue(actualAllowedVanillaEnchantments.equals(expectedVanillaEnchantments),
                    "Pastel Staff allowed vanilla enchantments changed: "
                            + describeEnchantmentDifference(expectedVanillaEnchantments, actualAllowedVanillaEnchantments));

            // Iron's StaffItem 側の広い互換性は 1.21.1 で揺れやすいため固定せず、
            // この mod が明示したバニラ武器許可と耐久系拒否だけを回帰監視する。
            for (var enchantment : getRegisteredEnchantments()) {
                var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                if (enchantmentId == null) {
                    continue;
                }

                var expectedVanillaAllowed = VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())
                        && expectedVanillaEnchantments.contains(enchantmentId);
                if (VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())) {
                    helper.assertTrue(item.canApplyAtEnchantingTable(stack, enchantment) == expectedVanillaAllowed,
                            "Pastel Staff vanilla enchanting-table rule changed for " + enchantmentId
                                    + ": expected " + expectedVanillaAllowed);
                    helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(enchantment)) == expectedVanillaAllowed,
                            "Pastel Staff vanilla book rule changed for " + enchantmentId
                                    + ": expected " + expectedVanillaAllowed);
                }

                if (isDurabilityTargetEnchantment(enchantment)) {
                    helper.assertFalse(item.canApplyAtEnchantingTable(stack, enchantment),
                            "Pastel Staff should keep rejecting durability-target enchantments at the enchanting table: "
                                    + enchantmentId);
                    helper.assertFalse(item.isBookEnchantable(stack, createEnchantedBook(enchantment)),
                            "Pastel Staff should keep rejecting durability-target enchantments from books: "
                                    + enchantmentId);
                }

                if (MALUM_HAUNTED.equals(enchantmentId)) {
                    helper.assertTrue(item.canApplyAtEnchantingTable(stack, enchantment),
                            "Pastel Staff should allow malum:haunted at the enchanting table");
                    helper.assertTrue(item.isBookEnchantable(stack, createEnchantedBook(enchantment)),
                            "Pastel Staff should allow malum:haunted from books");
                }

                if (MALUM_ANIMATED.equals(enchantmentId)) {
                    helper.assertFalse(item.canApplyAtEnchantingTable(stack, enchantment),
                            "Pastel Staff should keep rejecting malum:animated at the enchanting table");
                    helper.assertFalse(item.isBookEnchantable(stack, createEnchantedBook(enchantment)),
                            "Pastel Staff should keep rejecting malum:animated from books");
                }
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void malumHauntedBonusResolvesFromSupportedMainhandWeapons(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = MalumHauntedCompat.getHauntedEnchantment();
            helper.assertTrue(haunted != null, "malum:haunted is not registered");

            var pastelStaff = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            pastelStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(pastelStaff),
                    "Pastel Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(pastelStaff) > 0.0D,
                    "Pastel Staff should resolve a positive Haunted magic damage bonus");

            var crystalBladedStaff = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            crystalBladedStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(crystalBladedStaff),
                    "Crystal Bladed Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(crystalBladedStaff) > 0.0D,
                    "Crystal Bladed Staff should resolve a positive Haunted magic damage bonus");

            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get())),
                    "Spellgun should stay outside Haunted support");
            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get())),
                    "Reflectcast Shield should stay outside Haunted support");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void malumHauntedBonusUsesDedicatedDamageType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var attacker = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var source = MalumHauntedCompat.createHauntedBonusDamageSource(attacker);
            helper.assertTrue(source.is(DamageTypes.HAUNTED_BONUS),
                    "Haunted bonus should use apprenticecodex:haunted_bonus");
            helper.assertTrue(source.is(DamageTypeTagGenerator.MAGIC_DAMAGE),
                    "Haunted bonus should stay on the magic damage tag path");
            helper.assertTrue(source.is(DamageTypeTagGenerator.FORGE_IS_MAGIC),
                    "Haunted bonus should stay on the forge:is_magic path for Lodestone magic_proficiency");
            helper.assertTrue(source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "Haunted bonus should bypass cooldown-based I-Frame checks");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void magicDamageTagActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(LODESTONE_MOD_ID)) {
                return;
            }

            var magicProficiency = ForgeRegistries.ATTRIBUTES.getValue(LODESTONE_MAGIC_PROFICIENCY);
            helper.assertTrue(magicProficiency != null, "lodestone:magic_proficiency is not registered");

            var attacker = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var proficiencyInstance = attacker.getAttribute(magicProficiency);
            helper.assertTrue(proficiencyInstance != null, "Attacker is missing lodestone:magic_proficiency");

            var baselineTarget = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(1, 2, 0));
            var amplifiedTarget = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(2, 2, 0));
            var baseDamage = 4.0F;

            var baselineHealth = baselineTarget.getHealth();
            helper.assertTrue(baselineTarget.hurt(MalumHauntedCompat.createHauntedBonusDamageSource(attacker), baseDamage),
                    "Baseline haunted bonus damage should apply");
            var baselineTaken = baselineHealth - baselineTarget.getHealth();
            helper.assertTrue(Math.abs(baselineTaken - baseDamage) < 1.0e-4F,
                    "Baseline haunted bonus damage should stay unscaled at proficiency 1.0, actual=" + baselineTaken);

            proficiencyInstance.setBaseValue(1.5D);
            var amplifiedHealth = amplifiedTarget.getHealth();
            helper.assertTrue(amplifiedTarget.hurt(MalumHauntedCompat.createHauntedBonusDamageSource(attacker), baseDamage),
                    "Amplified haunted bonus damage should apply");
            var amplifiedTaken = amplifiedHealth - amplifiedTarget.getHealth();
            helper.assertTrue(Math.abs(amplifiedTaken - 6.0F) < 1.0e-4F,
                    "Amplified haunted bonus damage should scale to 6.0 at proficiency 1.5, actual=" + amplifiedTaken);
            helper.assertTrue(amplifiedTaken > baselineTaken,
                    "Amplified haunted bonus damage should exceed baseline damage");
        });
    }

    public static void healingBloomLightHasReducedLevelAndNoOutline(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomLightSelfCleansWithoutBloom(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomAcceptsOwnerDamageAndStaysSavable(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomRootLossUsesDeathState(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomCanBePlacedOnSupportedSlab(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomNormalRecastFailsForSameOwner(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomAllowsDifferentOwnersToEachHaveOne(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void healingBloomMissingManagedBloomDoesNotBlockRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createHealingBloomPlayer(helper, new BlockPos(0, 2, 0), "healing_bloom_missing_state_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);

            var spellData = Capabilities.getSpellDataOrNull(owner);
            helper.assertTrue(spellData != null,
                    "Healing Bloom stale-state test could not resolve spell data capability");
            spellData.edit(CodexSpellStateTypeRegister.HEALING_BLOOM_STATE, state -> state.setBloomUuid(UUID.randomUUID()));

            castHealingBloom(helper, owner, 1, anchorPos, false);

            helper.assertTrue(getOwnedHealingBlooms(helper, owner).size() == 1,
                    "A missing managed Healing Bloom should not block recasting for the same owner");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void archerMultipleTimeoutWithGreaterConjurersTalismanSkipsCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createArcherMultiplePlayer(helper, new BlockPos(0, 2, 0), "archer_multiple_greater_conjurer_timeout_test");
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

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        var player = createArcherMultiplePlayer(helper, new BlockPos(0, 2, 0), "archer_multiple_all_bows_removed_test");
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

    @GameTest(template = TEMPLATE)
    public static void companionTrunkRecastRecallsLoadedTrunkWhenFar(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
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

    @GameTest(template = TEMPLATE)
    public static void companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
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

    @GameTest(template = TEMPLATE)
    public static void companionTrunkDeathStoresItemsInChestWhenSpaceExists(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void companionTrunkDeathDropsItemsWhenNoChestSpaceExists(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void companionTrunkIgnoresFireAndRescuesFromVoid(GameTestHelper helper) {
        var player = createCompanionTrunkPlayer(helper, new BlockPos(0, 2, 0));
        var trunk = createCompanionTrunk(helper, player, new BlockPos(0, 2, 0));
        trunk.setCompanionMaxHealth(10.0f);

        helper.assertFalse(trunk.hurt(helper.getLevel().damageSources().lava(), 4.0f),
                "Companion Trunk should ignore lava damage");
        trunk.setSecondsOnFire(5);

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

    @GameTest(template = TEMPLATE)
    public static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var bambooBase = new BlockPos(4, 2, 1);
        helper.setBlock(bambooBase.below(), Blocks.DIRT);
        helper.setBlock(bambooBase, Blocks.BAMBOO);
        for (var offset = 1; offset <= 6; ++offset) {
            helper.setBlock(bambooBase.above(offset), Blocks.BAMBOO);
        }

        var cropPositions = new ArrayList<BlockPos>();
        for (var x = 2; x <= 9; ++x) {
            for (var z = -4; z <= 4; ++z) {
                var pos = new BlockPos(x, 3, z);
                if (pos.getX() == bambooBase.getX() && pos.getZ() == bambooBase.getZ()) {
                    continue;
                }
                cropPositions.add(pos);
                helper.setBlock(pos.below(), Blocks.SOUL_SAND);
                helper.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
            }
        }
        helper.assertTrue(cropPositions.size() > 64, "HarvestMoon tick budget test requires more than 64 crops");

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

    @GameTest(template = TEMPLATE)
    public static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
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

    private static FakePlayer createCraftsmansDelightPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static void equipCraftsmansDelight(FakePlayer player, ItemStack ringStack) {
        var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for CraftsmansDelight test"));
        curiosInventory.setEquippedCurio(io.redspace.ironsspellbooks.compat.Curios.RING_SLOT, 0, ringStack);
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

    private static FakePlayer createExtractPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
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

        // Better Combat に空扱いされても inventory.offhand[0] 自体は保持されるので、
        // 救済系テストは実スロットへ直接積んで隠蔽前提を再現する。
        player.getInventory().offhand.set(0, offhandStack.copy());
        return player;
    }

    private static void castHarvestMoon(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.HARVEST_MOON.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    private static FakePlayer createSenseEvilPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static net.minecraft.world.entity.LivingEntity spawnPositionedZombie(ServerLevel level, Vec3 targetCenter) {
        forceLoadChunk(level, BlockPos.containing(targetCenter));
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create zombie for SenseEvil GameTest");
        }
        zombie.moveTo(targetCenter.x, targetCenter.y - zombie.getBbHeight() * 0.5, targetCenter.z, 0.0f, 0.0f);
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
        level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
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
        castData.deserializeNBT(tag);
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

    private static FakePlayer createCompanionTrunkPlayer(GameTestHelper helper, BlockPos pos) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "companion_trunk_test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    private static void castCompanionTrunk(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.COMPANION_TRUNK.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    private static jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity getSingleExtractProjectile(
            GameTestHelper helper,
            FakePlayer owner
    ) {
        var projectiles = helper.getLevel().getEntitiesOfClass(
                jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity.class,
                new AABB(owner.position(), owner.position()).inflate(16.0),
                projectile -> projectile.getOwner() == owner
        );
        helper.assertTrue(projectiles.size() == 1,
                "Expected exactly one Extract projectile but found " + projectiles.size());
        return projectiles.get(0);
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
        var block = ForgeRegistries.BLOCKS.getValue(id);
        helper.assertTrue(block != null, "Missing required block for GameTest: " + id);
        return block;
    }

    private static Item requireForgeItem(GameTestHelper helper, ResourceLocation id) {
        var item = ForgeRegistries.ITEMS.getValue(id);
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
            Predicate<net.minecraft.world.item.Item> itemPredicate,
            Set<ResourceLocation> expectedEnchantments
    ) {
        assertCategoryEnchantments(helper, categoryName, itemPredicate, stack -> expectedEnchantments);
    }

    private static void assertCategoryEnchantments(
            GameTestHelper helper,
            String categoryName,
            Predicate<net.minecraft.world.item.Item> itemPredicate,
            java.util.function.Function<ItemStack, Set<ResourceLocation>> expectedEnchantmentsResolver
    ) {
        var stacks = getRegisteredItemStacks(itemPredicate);
        helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: " + categoryName);

        for (var stack : stacks) {
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantmentsResolver.apply(stack),
                    categoryName + " " + ForgeRegistries.ITEMS.getKey(stack.getItem())
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
                itemName
        );
    }

    private static void assertExactEnchantmentSurfaces(
            GameTestHelper helper,
            ItemStack stack,
            Set<ResourceLocation> expectedEnchantingTableEnchantments,
            Set<ResourceLocation> expectedBookEnchantments,
            Set<ResourceLocation> expectedAnvilEnchantments,
            String itemName
    ) {
        var item = stack.getItem();
        var actualEnchantingTableEnchantments = collectAllowedEnchantments(
                stack,
                enchantment -> item.canApplyAtEnchantingTable(stack, enchantment)
        );
        helper.assertTrue(actualEnchantingTableEnchantments.equals(expectedEnchantingTableEnchantments),
                itemName + " enchanting-table enchantments changed: "
                        + describeEnchantmentDifference(expectedEnchantingTableEnchantments, actualEnchantingTableEnchantments));

        var actualBookEnchantments = collectAllowedEnchantments(
                stack,
                enchantment -> item.isBookEnchantable(stack, createEnchantedBook(enchantment))
        );
        helper.assertTrue(actualBookEnchantments.equals(expectedBookEnchantments),
                itemName + " book enchantments changed: "
                        + describeEnchantmentDifference(expectedBookEnchantments, actualBookEnchantments));

        if (item instanceof NonDamageableAnvilMergeItem mergeItem) {
            var actualAnvilEnchantments = collectAllowedEnchantments(
                    stack,
                    enchantment -> mergeItem.isAnvilMergeEnchantmentAllowed(stack, enchantment)
            );
            helper.assertTrue(actualAnvilEnchantments.equals(expectedAnvilEnchantments),
                    itemName + " anvil enchantments changed: "
                            + describeEnchantmentDifference(expectedAnvilEnchantments, actualAnvilEnchantments));
        }
    }

    private static List<ItemStack> getRegisteredItemStacks(Predicate<net.minecraft.world.item.Item> itemPredicate) {
        return ItemRegistry.ITEMS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(itemPredicate)
                .sorted(Comparator.comparing(item -> String.valueOf(ForgeRegistries.ITEMS.getKey(item))))
                .map(ItemStack::new)
                .toList();
    }

    private static Set<ResourceLocation> expectedSpellGunEnchantments(ItemStack stack) {
        var expectedEnchantments = registryIdSet(
                EnchantmentRegistry.ALACRITY,
                EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR,
                EnchantmentRegistry.SURGE,
                EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE,
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedOffhandEnchantments(ItemStack stack) {
        return registryIdSet(
                EnchantmentRegistry.ALACRITY,
                EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR,
                EnchantmentRegistry.SURGE,
                EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE,
                EnchantmentRegistry.TRANSCENDENCE
        );
    }

    private static Set<ResourceLocation> expectedEnchantedCircletEnchantments(ItemStack stack) {
        var expectedEnchantments = new LinkedHashSet<>(expectedOffhandEnchantments(stack));
        expectedEnchantments.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedRightClickMagicWeaponEnchantments(ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                new ItemStack(Items.DIAMOND_SWORD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM
        ));
        addExpectedMalumHauntedIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedReflectcastShieldEnchantments(ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                new ItemStack(Items.SHIELD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.SHIELD))
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedFlaskEnchantments() {
        return registryIdSet(
                EnchantmentRegistry.GUZZLE,
                EnchantmentRegistry.LARGE_MUG,
                EnchantmentRegistry.RED_ENERGY,
                EnchantmentRegistry.GLOW_ENERGY
        );
    }

    private static Set<ResourceLocation> expectedAlchemistsFlaskEnchantments() {
        return registryIdSet(
                EnchantmentRegistry.LARGE_MUG,
                EnchantmentRegistry.RED_ENERGY,
                EnchantmentRegistry.GLOW_ENERGY,
                EnchantmentRegistry.TRANSCENDENCE
        );
    }

    private static Set<ResourceLocation> expectedRandomBookLootEnchantments() {
        return registryIdSet(
                EnchantmentRegistry.ALACRITY,
                EnchantmentRegistry.REFLUX,
                EnchantmentRegistry.RESERVOIR,
                EnchantmentRegistry.SURGE,
                EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.TENSE,
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER
        );
    }

    private static Set<ResourceLocation> expectedEnchantressRobeEnchantments(ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                probeStack,
                enchantment -> enchantment.canApplyAtEnchantingTable(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedStealthRuneArmorEnchantments(ItemStack stack) {
        var probeStack = createArmorProbeStack(stack);
        var expectedEnchantments = collectAllowedEnchantments(
                probeStack,
                enchantment -> enchantment.canApplyAtEnchantingTable(probeStack)
        );
        expectedEnchantments.addAll(registryIdSet(EnchantmentRegistry.WISDOM));
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
        };
    }

    private static Set<ResourceLocation> registryIdSet(RegistryObject<Enchantment>... enchantments) {
        var ids = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : enchantments) {
            var id = enchantment.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static List<Enchantment> getRegisteredEnchantments() {
        return ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .sorted(Comparator.comparing(enchantment -> String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment))))
                .toList();
    }

    private static Set<ResourceLocation> allRegisteredEnchantmentIds() {
        return collectAllowedEnchantments(ItemStack.EMPTY, enchantment -> true);
    }

    private static Set<ResourceLocation> collectAllowedEnchantments(
            ItemStack stack,
            Predicate<Enchantment> predicate
    ) {
        var allowedEnchantments = new LinkedHashSet<ResourceLocation>();
        for (var enchantment : getRegisteredEnchantments()) {
            var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (enchantmentId == null || !predicate.test(enchantment)) {
                continue;
            }
            allowedEnchantments.add(enchantmentId);
        }
        return allowedEnchantments;
    }

    private static ItemStack createEnchantedBook(Enchantment enchantment) {
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, 1));
        return book;
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.ELYTRA));
    }

    private static void addExpectedMalumSpiritPlunderIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID) && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            expectedEnchantments.add(MALUM_SPIRIT_PLUNDER);
        }
    }

    private static void addExpectedMalumHauntedIfPresent(ItemStack stack, Set<ResourceLocation> expectedEnchantments) {
        if (ModList.get().isLoaded(MALUM_MOD_ID) && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            expectedEnchantments.add(MALUM_HAUNTED);
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
            RegistryObject<Enchantment> enchantmentRegistryObject,
            boolean expectedTreasureOnly,
            boolean expectedTradeable,
            boolean expectedDiscoverable
    ) {
        var enchantment = enchantmentRegistryObject.get();
        var enchantmentId = String.valueOf(enchantmentRegistryObject.getId());
        helper.assertTrue(enchantment.isTreasureOnly() == expectedTreasureOnly,
                "Treasure flag changed for " + enchantmentId + ": expected " + expectedTreasureOnly + " but got " + enchantment.isTreasureOnly());
        helper.assertTrue(enchantment.isTradeable() == expectedTradeable,
                "Tradeable flag changed for " + enchantmentId + ": expected " + expectedTradeable + " but got " + enchantment.isTradeable());
        helper.assertTrue(enchantment.isDiscoverable() == expectedDiscoverable,
                "Discoverable flag changed for " + enchantmentId + ": expected " + expectedDiscoverable + " but got " + enchantment.isDiscoverable());
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
            jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem item,
            ItemStack stack,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var actualAmount = sumModifierAmount(
                item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack).get(attribute),
                operation
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack)));
    }

    private static void assertCurioModifierAmount(
            GameTestHelper helper,
            top.theillusivec4.curios.api.type.capability.ICurioItem item,
            top.theillusivec4.curios.api.SlotContext slotContext,
            ItemStack stack,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var modifiers = item.getAttributeModifiers(slotContext, UUID.randomUUID(), stack);
        var actualAmount = sumModifierAmount(modifiers.get(attribute), operation);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(modifiers));
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

    private static void assertUpgradeable(GameTestHelper helper, ItemStack stack, String message) {
        helper.assertTrue(stack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                message + " (missing upgrade whitelist tag on " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");
        helper.assertTrue(Utils.canBeUpgraded(stack),
                message + " (Utils.canBeUpgraded returned false for " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");
    }

    private static UpgradeData createUpgradeData(
            RegistryAccess registryAccess,
            ItemStack stack,
            net.minecraft.resources.ResourceKey<io.redspace.ironsspellbooks.item.armor.UpgradeOrbType> upgradeKey,
            String slotName
    ) {
        io.redspace.ironsspellbooks.api.backwards_compat.UpgradeTypeCache.doCache(registryAccess);
        var upgradeRegistry = registryAccess.registryOrThrow(io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY);
        var upgradeHolder = upgradeRegistry.getHolder(upgradeKey)
                .orElseThrow(() -> new IllegalStateException("Missing upgrade orb type: " + upgradeKey.location()));
        var upgradeData = new UpgradeData(java.util.Map.of(upgradeHolder, 1), slotName);
        UpgradeData.set(stack, upgradeData);
        return upgradeData;
    }

    private static double sumModifierAmount(
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    private static String describeModifiers(com.google.common.collect.Multimap<Attribute, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()) + "="
                        + entry.getValue().getAmount() + "@" + entry.getValue().getOperation())
                .collect(Collectors.joining(", "));
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
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe != null, "Missing recipe: " + recipeId);
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
                .create(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/random_applicable_enchantment"));
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

    private static void sampleLootTable(
            GameTestHelper helper,
            ResourceLocation lootTableId,
            LootParams lootParams,
            int attempts,
            java.util.function.Consumer<ItemStack> stackConsumer
    ) {
        var lootTable = helper.getLevel().getServer().getLootData().getLootTable(lootTableId);
        for (var i = 0; i < attempts; i++) {
            lootTable.getRandomItems(lootParams, stackConsumer);
        }
    }

    private static ExplorersCodexGuidebookTransferRecipe getExplorersCodexGuidebookTransferRecipe(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"))
                .orElse(null);
        helper.assertTrue(recipe instanceof ExplorersCodexGuidebookTransferRecipe,
                "Missing Explorer's Codex guidebook transfer recipe: " + recipe);
        return (ExplorersCodexGuidebookTransferRecipe) recipe;
    }

    private static ItemStack createSpellScroll(AbstractSpell spell) {
        var stack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, stack);
        return stack;
    }

    private static ItemStack createInstantManaPotion(net.minecraft.world.item.alchemy.Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
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
        helper.assertTrue(effect != null && effect.getEffect() == EffectRegistry.MANA_REGENERATION.get(),
                "Potion " + potionId + " should grant mana regeneration");
        helper.assertTrue(effect != null && effect.getDuration() == expectedDuration,
                "Potion " + potionId + " duration regression: "
                        + (effect == null ? "missing" : effect.getDuration()));
        helper.assertTrue(effect != null && effect.getAmplifier() == expectedAmplifier,
                "Potion " + potionId + " amplifier regression: "
                        + (effect == null ? "missing" : effect.getAmplifier()));
    }

    private static ItemStack createFilledSpellcastersFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }

    private static ItemStack createFilledAlchemistsFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
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

    private static CraftingContainer createCraftingContainer(ItemStack... stacks) {
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };

        var container = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < stacks.length; ++i) {
            container.setItem(i, stacks[i]);
        }
        return container;
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

    private static <T> void assertForgeRegistryEntries(
            GameTestHelper helper,
            String registryName,
            IForgeRegistry<T> registry,
            Collection<? extends RegistryObject<? extends T>> entries
    ) {
        for (var entry : entries) {
            var id = entry.getId();
            helper.assertTrue(id != null, "Missing " + registryName + " id");
            helper.assertTrue(registry.getValue(id) == entry.get(),
                    "Missing " + registryName + " registry entry: " + id);
        }
    }

    private static <T> void assertBuiltinRegistryEntries(
            GameTestHelper helper,
            String registryName,
            Registry<T> registry,
            Collection<? extends RegistryObject<? extends T>> entries
    ) {
        for (var entry : entries) {
            var id = entry.getId();
            helper.assertTrue(id != null, "Missing " + registryName + " id");
            helper.assertTrue(registry.get(id) == entry.get(),
                    "Missing " + registryName + " registry entry: " + id);
        }
    }
}
