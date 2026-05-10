package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaFluidHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.event.ErrandMageVillagerTradesEvent;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaffCastEvent;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellListManager;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverPickupEvent;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
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
import jp.aquafactory.apprenticecodex.spell.earthforge.EarthForge;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconSearchService;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackJob;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.item.armor.ApprenticeMageRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressStats;
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
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.worldgen.ErrandMageVillageAddition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StairBlock;
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
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
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
    private static final UUID FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = UUID.fromString("a7dc54b6-a83c-4a5f-ae93-0cb49780fc8f");
    private static final UUID CASTING_MOVESPEED_DYNAMIC_TEST_EXTERNAL_MODIFIER_ID =
            UUID.fromString("04a46352-a09b-44fb-b504-92ab5f69f969");
    private static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();
    private static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");

    private ApprenticeCodexGameTestScenarios() {
    }
    static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
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
    static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pottedComfortBerryBush = (FlowerPotBlock) BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get();
            helper.assertTrue(pottedComfortBerryBush.getContent() == BlockRegistry.COMFORT_BERRY_BUSH.get(),
                    "Potted Comfort Berry Bush should contain the Comfort Berry Bush block");

            var potPos = new BlockPos(1, 1, 1);
            var absolutePotPos = helper.absolutePos(potPos);
            helper.setBlock(potPos, Blocks.FLOWER_POT);

            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "comfort_berry_pot_test"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.COMFORT_BERRIES.get()));
            var result = helper.getLevel().getBlockState(absolutePotPos).use(
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolutePotPos), Direction.UP, absolutePotPos, false)
            );

            helper.assertTrue(result.consumesAction(), "Comfort Berries should be accepted by a vanilla Flower Pot");
            helper.assertBlockPresent(BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get(), potPos);
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "Potted Comfort Berries should consume one berry item outside creative mode");
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
    static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
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
    static void errandMageTradesMatchExpectedOffers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var trades = createEmptyVillagerTrades();
            ErrandMageVillagerTradesEvent.onVillagerTrades(
                    new VillagerTradesEvent(trades, VillagerProfessionRegistry.ERRAND_MAGE.get())
            );

            var level1Offers = createOffers(trades.get(1), 0L);
            helper.assertFalse(hasBaseCostItem(level1Offers,
                            io.redspace.ironsspellbooks.registries.ItemRegistry.FROZEN_BONE_SHARD.get()),
                    "Errand Mage level 1 trades should not buy Frozen Bone Shards");
            assertContainsOffer(helper, level1Offers,
                    new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 32),
                    ItemStack.EMPTY,
                    new ItemStack(Items.EMERALD),
                    16,
                    "Errand Mage level 1 trades should buy Rapid Spellcaster Rounds");

            var level2Listings = trades.get(2);
            var exclusiveItems = new LinkedHashSet<Item>();
            var level2Offers = createOffers(level2Listings, 0L);
            helper.assertTrue(countBaseCostItems(level2Offers,
                            io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get(),
                            io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_PEARL.get()) == 1,
                    "Errand Mage level 2 trades should choose either Shriving Stone or Divine Pearl");
            var exclusiveRandom = RandomSource.create(0L);
            var exclusiveListing = level2Listings.get(0);
            for (var attempt = 0; attempt < 64 && exclusiveItems.size() < 2; attempt++) {
                var offer = exclusiveListing.getOffer(null, exclusiveRandom);
                helper.assertTrue(offer != null, "Errand Mage level 2 exclusive trade should create an offer");
                exclusiveItems.add(offer.getBaseCostA().getItem());
            }
            helper.assertTrue(exclusiveItems.size() == 2,
                    "Errand Mage level 2 exclusive trade should be able to choose both configured items");
            assertContainsOffer(helper, level2Offers,
                    new ItemStack(Items.EMERALD),
                    ItemStack.EMPTY,
                    new ItemStack(ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 8),
                    12,
                    "Errand Mage level 2 trades should always sell Basic Spellcaster Rounds");

            assertContainsOffer(helper, createOffers(trades.get(4), 0L),
                    new ItemStack(Items.EMERALD, 32),
                    ItemStack.EMPTY,
                    new ItemStack(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()),
                    12,
                    "Errand Mage level 4 trades should sell Spellstained Arcane Ingots");
            assertContainsOffer(helper, createOffers(trades.get(5), 0L),
                    new ItemStack(Items.EMERALD, 64),
                    new ItemStack(Items.WRITABLE_BOOK),
                    new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get()),
                    3,
                    "Errand Mage level 5 trades should sell Isekai Travel Guidebooks");
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
            var guidebookRecipeId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "isekai_travel_guidebook");
            helper.assertTrue(recipeManager.byKey(guidebookRecipeId).isEmpty(),
                    "Isekai Travel Guidebook crafting recipe should not be loaded: " + guidebookRecipeId);

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
    static void spellcastersFlaskAcceptsAllVanillaPotionTypes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var emptyFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Spellcaster's Flask rejected a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Spellcaster's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Spellcaster's Flask rejected a lingering potion");
            helper.assertTrue(!SpellcastersFlask.copyWithAddedDoses(emptyFlask, splashPotion, 1).isEmpty(),
                    "Spellcaster's Flask failed to store a splash potion");
            helper.assertTrue(!SpellcastersFlask.copyWithAddedDoses(emptyFlask, lingeringPotion, 1).isEmpty(),
                    "Spellcaster's Flask failed to store a lingering potion");
        });
    }
    static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
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
    static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
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
    static void spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var storedPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var twoDoseFlask = createFilledSpellcastersFlask(storedPotion, 2, 0);
            var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), "spellcasters_flask_mismatch_drink_test"));

            twoDoseFlask.getItem().finishUsingItem(twoDoseFlask, helper.getLevel(), player);

            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(twoDoseFlask) == 0,
                    "Mismatched Spellcaster's Flask drink should consume two doses");
            helper.assertTrue(SpellcastersFlask.getStoredItem(twoDoseFlask).isEmpty(),
                    "Mismatched Spellcaster's Flask should clear StoredItem when extra consumption empties it");

            var oneDoseFlask = createFilledSpellcastersFlask(storedPotion, 1, 0);
            oneDoseFlask.getItem().finishUsingItem(oneDoseFlask, helper.getLevel(), player);
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(oneDoseFlask) == 0,
                    "Mismatched Spellcaster's Flask drink should still work with one remaining dose");
        });
    }
    static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
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
    static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
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
    static void alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var emptyFlask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
            var normalPotion = createInstantManaPotion(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get());
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, normalPotion),
                    "Alchemist's Flask rejected a regular potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, splashPotion),
                    "Alchemist's Flask rejected a splash potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, lingeringPotion),
                    "Alchemist's Flask rejected a lingering potion");
            helper.assertTrue(SpellcastersFlask.canAddDoseFromItem(emptyFlask, simpleElixir),
                    "Alchemist's Flask rejected a Simple Elixir");
        });
    }
    static void flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            assertTooltipKeyUsesColor(
                    helper,
                    createFilledSpellcastersFlask(splashPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    ChatFormatting.YELLOW,
                    "Spellcaster's Flask should warn for stored splash potions"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    createFilledAlchemistsFlask(normalPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    ChatFormatting.YELLOW,
                    "Alchemist's Flask should warn for stored regular potions"
            );
            assertTooltipKeyAbsent(
                    helper,
                    createFilledSpellcastersFlask(normalPotion, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    "Spellcaster's Flask should not warn for regular potions"
            );
            assertTooltipKeyAbsent(
                    helper,
                    createFilledAlchemistsFlask(simpleElixir, 1, 0),
                    "item.apprenticecodex.flask_system.mismatch_flask_type",
                    "Alchemist's Flask should not warn for Simple Elixir"
            );
        });
    }
    static void flaskAutomaticFillTypeGateRejectsMismatchedEmptyVanillaPotion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());

            helper.assertFalse(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                            splashPotion),
                    "Atelier Station should not auto-fill an empty Spellcaster's Flask with splash potions");
            helper.assertFalse(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            normalPotion),
                    "Atelier Station should not auto-fill an empty Alchemist's Flask with regular potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                            normalPotion),
                    "Atelier Station should auto-fill an empty Spellcaster's Flask with regular potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            splashPotion),
                    "Atelier Station should auto-fill an empty Alchemist's Flask with splash potions");
            helper.assertTrue(AbstractPotionFlaskItem.canAcceptRepresentativeForAutomaticFill(
                            new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                            simpleElixir),
                    "Atelier Station should keep non-vanilla Alchemist's Flask items outside mismatch penalties");
        });
    }
    static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
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
    static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"))
                    .orElseThrow();

            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var spellcastersFlask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
            if (EnchantmentRegistry.GUZZLE.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.GUZZLE.get(), 2);
            }
            if (EnchantmentRegistry.LARGE_MUG.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.LARGE_MUG.get(), 1);
            }
            if (EnchantmentRegistry.RED_ENERGY.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.RED_ENERGY.get(), 1);
            }
            if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                spellcastersFlask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 1);
            }
            var filledSpellcastersFlask = SpellcastersFlask.copyWithAddedDoses(spellcastersFlask, normalPotion, 2);
            filledSpellcastersFlask = SpellcastersFlask.copyWithToggledEffectParticles(filledSpellcastersFlask);
            helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(filledSpellcastersFlask),
                    "Spellcaster's Flask test input should start with suppressed particles");
            var smithingContainer = new net.minecraft.world.SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    filledSpellcastersFlask,
                    new ItemStack(Items.GUNPOWDER)
            );

            helper.assertTrue(recipe.matches(smithingContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept a filled Spellcaster's Flask");

            var convertedFlask = recipe.assemble(smithingContainer, helper.getLevel().registryAccess());
            helper.assertTrue(convertedFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask smithing recipe returned the wrong result item");
            helper.assertTrue(ISpellContainer.isSpellContainer(convertedFlask),
                    "Alchemist's Flask smithing recipe should preserve the preset spell container");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(convertedFlask) == 2,
                    "Alchemist's Flask smithing recipe should preserve stored dose count");

            var convertedStoredItem = SpellcastersFlask.getStoredItem(convertedFlask);
            helper.assertTrue(convertedStoredItem.is(Items.POTION),
                    "Filled Spellcaster's Flask should keep a regular potion as a mismatch-usable potion");
            helper.assertTrue(PotionUtils.getPotion(convertedStoredItem) == PotionUtils.getPotion(normalPotion),
                    "Converted Alchemist's Flask should keep the original potion type");
            helper.assertTrue(!SpellcastersFlask.isEffectParticlesSuppressed(convertedFlask),
                    "Alchemist's Flask smithing recipe should reset suppressed particles");

            var convertedEnchantments = EnchantmentHelper.getEnchantments(convertedFlask);
            helper.assertTrue(!EnchantmentRegistry.GUZZLE.isPresent()
                            || !convertedEnchantments.containsKey(EnchantmentRegistry.GUZZLE.get()),
                    "Alchemist's Flask smithing recipe should drop only Guzzle");
            helper.assertTrue(!EnchantmentRegistry.LARGE_MUG.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.LARGE_MUG.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Large Mug");
            helper.assertTrue(!EnchantmentRegistry.RED_ENERGY.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.RED_ENERGY.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Red Energy");
            helper.assertTrue(!EnchantmentRegistry.GLOW_ENERGY.isPresent()
                            || convertedEnchantments.getOrDefault(EnchantmentRegistry.GLOW_ENERGY.get(), 0) == 1,
                    "Alchemist's Flask smithing recipe should keep Glow Energy");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirContainer = new net.minecraft.world.SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), simpleElixir, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(recipe.matches(simpleElixirContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should accept Simple Elixir");
            var simpleElixirResult = recipe.assemble(simpleElixirContainer, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.isSameItemSameTags(SpellcastersFlask.getStoredItem(simpleElixirResult), simpleElixir),
                    "Alchemist's Flask smithing recipe should keep Simple Elixir unchanged");

            var fireAle = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_ALE.get());
            var fireAleContainer = new net.minecraft.world.SimpleContainer(
                    new ItemStack(Items.EMERALD),
                    SpellcastersFlask.copyWithAddedDoses(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()), fireAle, 1),
                    new ItemStack(Items.GUNPOWDER)
            );
            helper.assertTrue(!recipe.matches(fireAleContainer, helper.getLevel()),
                    "Alchemist's Flask smithing recipe should reject unsupported stored items such as Fire Ale");
        });
    }
    static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = (jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe) helper.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"))
                    .orElseThrow();

            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var splashFlask = createFilledAlchemistsFlask(splashPotion, 2, 1);
            var splashContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    splashFlask,
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(splashContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a splash potion flask");

            var splashResult = recipe.assemble(splashContainer, helper.getLevel().registryAccess());
            var splashRemainingFlask = recipe.getRemainingItems(splashContainer).get(4);

            helper.assertTrue(splashResult.is(Items.TIPPED_ARROW) && splashResult.getCount() == 8,
                    "Alchemist's Flask tipped arrow recipe should return eight tipped arrows");
            helper.assertTrue(PotionUtils.getPotion(splashResult) == PotionUtils.getPotion(splashPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored splash potion");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(splashRemainingFlask) == 1,
                    "Alchemist's Flask tipped arrow recipe should consume exactly one dose");

            var lingeringPotion = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.Potions.HEALING);
            var lingeringFlask = createFilledAlchemistsFlask(lingeringPotion, 1, 0);
            var lingeringContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    lingeringFlask,
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );

            helper.assertTrue(recipe.matches(lingeringContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should accept a lingering potion flask");
            var lingeringResult = recipe.assemble(lingeringContainer, helper.getLevel().registryAccess());
            var lingeringRemainingFlask = recipe.getRemainingItems(lingeringContainer).get(4);

            helper.assertTrue(PotionUtils.getPotion(lingeringResult) == PotionUtils.getPotion(lingeringPotion),
                    "Alchemist's Flask tipped arrow recipe should keep the stored lingering potion");
            helper.assertTrue(lingeringRemainingFlask.is(ItemRegistry.ALCHEMISTS_FLASK.get()),
                    "Alchemist's Flask tipped arrow recipe should return the flask");
            helper.assertTrue(SpellcastersFlask.getStoredDoseCount(lingeringRemainingFlask) == 0,
                    "Alchemist's Flask tipped arrow recipe should empty the flask after the last dose");
            helper.assertTrue(SpellcastersFlask.getStoredItem(lingeringRemainingFlask).isEmpty(),
                    "Alchemist's Flask tipped arrow recipe should clear StoredItem after the last dose");

            var simpleElixir = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get());
            var simpleElixirContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(simpleElixir, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            helper.assertTrue(!recipe.matches(simpleElixirContainer, helper.getLevel()),
                    "Alchemist's Flask tipped arrow recipe should reject Simple Elixir");
        });
    }
    static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var splashPotion = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), PotionRegistry.INTELLIGENCE.get());
            var craftingContainer = createCraftingContainer(
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    createFilledAlchemistsFlask(splashPotion, 1, 0),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW),
                    new ItemStack(Items.ARROW)
            );
            var craftedStack = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 8), PotionRegistry.INTELLIGENCE.get());
            var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/craft_tipped_arrow_by_flask")
            );

            helper.assertTrue(advancement != null, "Missing advancement for flask tipped arrow crafting");
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.event.AlchemistsFlaskAdvancementEvent.shouldAward(craftedStack, craftingContainer),
                    "Crafting tipped arrows with Alchemist's Flask should satisfy the advancement award conditions"
            );
        });
    }
    static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
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

            var normalPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            player.setItemInHand(InteractionHand.MAIN_HAND, createFilledAlchemistsFlask(normalPotion, 2, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Extract should prepare a force-splash normal potion cast");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var forcedSplashProjectile = getSingleExtractProjectile(helper, player);
            helper.assertTrue(forcedSplashProjectile.getItem().is(Items.SPLASH_POTION),
                    "Extract should throw normal potion contents as a splash potion");
            helper.assertTrue(jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()) == 0,
                    "Extract should consume two doses when force-splashing a normal potion");
            forcedSplashProjectile.discard();
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
    static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
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
    static void spellDispenserValidatorRejectsRayOfSiphoning(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAY_OF_SIPHONING_SPELL.get());

            var validation = SpellDispenserSpellValidator.validate(scrollStack);
            helper.assertTrue(!validation.isSupported(), "Spell Dispenser validator accepted Ray of Siphoning");
            helper.assertTrue(validation.failureReason() == SpellDispenserSpellValidator.FailureReason.DENYLISTED,
                    "Spell Dispenser validator returned the wrong failure reason for Ray of Siphoning: " + validation.failureReason());
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
    static void spellDispenserCastHelperTicksLongCastBeforeCompletion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.PRECISION_JACK.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_precision_jack_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a Precision Jack scroll");

            var knifeBox = new AABB(castPos).inflate(6.0D);
            var knives = level.getEntitiesOfClass(PrecisionJackKnifeEntity.class, knifeBox);
            helper.assertTrue(!knives.isEmpty(), "Spell Dispenser Precision Jack cast completed without spawning a knife");
            helper.assertTrue(knives.stream().anyMatch(PrecisionJackKnifeEntity::isTrailActive),
                    "Spell Dispenser LONG cast completed without calling Precision Jack's server cast tick");
        });
    }
    static void spellDispenserPrecisionJackLowManaCleansUpKnife(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(0, 1, 0);
        var absolutePos = helper.absolutePos(pos);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

        var spell = SpellRegistry.PRECISION_JACK.get();
        var requiredMana = spell.getManaCost(1);
        spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
        spellDispenser.setOwnerProfile(createSpellDispenserOwnerProfile("spell_dispenser_precision_jack_low_mana_test"));
        spellDispenser.setCurrentMana(Math.max(0, requiredMana - 1));

        var castResult = spellDispenser.tryActivate();
        helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated Precision Jack with insufficient mana");
        helper.assertTrue(castResult.insufficientMana(), "Spell Dispenser returned the wrong Precision Jack low-mana failure");

        helper.runAtTickTime(20, () -> {
            var knifeBox = new AABB(absolutePos).inflate(8.0D);
            var knives = level.getEntitiesOfClass(PrecisionJackKnifeEntity.class, knifeBox);
            helper.assertTrue(knives.isEmpty(), "Low-mana Precision Jack left knife entities behind: " + knives.size());
            helper.succeed();
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
    static void spellDispenserFluidAcceptsOnlyRegularManaPotions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).resolve().orElse(null);
            var partialManaFluid = createIronsManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(), 100);
            var remainingPartialManaFluid = createIronsManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(), 150);
            var strongerManaFluid = createIronsManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_TWO.get(), 250);
            var splashPotion = PotionUtils.setPotion(
                    new ItemStack(Items.SPLASH_POTION),
                    io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
            );
            var splashManaFluid = io.redspace.ironsspellbooks.fluids.PotionFluid.from(splashPotion);
            var healingFluid = createIronsManaPotionFluid(net.minecraft.world.item.alchemy.Potions.HEALING, 250);

            blockEntity.setCurrentMana(950);
            helper.assertTrue(fluidHandler != null, "Spell Dispenser fluid capability was not exposed");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(healingFluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "Spell Dispenser accepted a non-mana potion fluid");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(splashManaFluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "Spell Dispenser accepted a splash mana potion fluid");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(partialManaFluid, IFluidHandler.FluidAction.EXECUTE) == 100,
                    "Spell Dispenser rejected a partial regular mana potion fluid");
            helper.assertTrue(blockEntity.getStoredManaPotionFluid().getAmount() == 100,
                    "Spell Dispenser did not retain a partial mana potion fluid");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(strongerManaFluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "Spell Dispenser accepted a different mana potion fluid into the same tank");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(remainingPartialManaFluid, IFluidHandler.FluidAction.EXECUTE) == 150,
                    "Spell Dispenser rejected matching mana potion fluid after a partial fill");
            helper.assertTrue(blockEntity.getStoredManaPotionFluid().getAmount() == 250,
                    "Spell Dispenser consumed a potion fluid that should not fit in remaining mana capacity");
        });
    }
    static void spellDispenserFluidConsumesPotionDoseImmediately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, null).resolve().orElse(null);
            var manaFluid = createIronsManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(), 250);

            blockEntity.setCurrentMana(850);
            helper.assertTrue(fluidHandler != null, "Spell Dispenser fluid capability was not exposed");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Spell Dispenser did not accept a regular mana potion fluid");
            helper.assertTrue(blockEntity.getCurrentMana() == 925,
                    "Spell Dispenser did not immediately recover mana from potion fluid: " + blockEntity.getCurrentMana());
            helper.assertTrue(blockEntity.getStoredManaPotionFluid().isEmpty(),
                    "Spell Dispenser left consumed mana potion fluid in the tank");
        });
    }
    static void spellDispenserFluidDrainsUnconsumedPotionOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).resolve().orElse(null);
            var manaFluid = createIronsManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(), 500);

            blockEntity.setCurrentMana(950);
            helper.assertTrue(fluidHandler != null, "Spell Dispenser fluid capability was not exposed");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 500,
                    "Spell Dispenser did not accept unconsumed mana potion fluid");

            var drained = fluidHandler == null ? FluidStack.EMPTY : fluidHandler.drain(500, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(drained.getAmount() == 500,
                    "Spell Dispenser did not drain all unconsumed potion fluid: " + drained.getAmount());
            helper.assertTrue(blockEntity.getCurrentMana() == 950,
                    "Spell Dispenser converted internal mana into drained potion fluid");
            helper.assertTrue(fluidHandler == null || fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "Spell Dispenser drained potion fluid after the tank was empty");
        });
    }
    static void spellDispenserFluidPersistsThroughNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var original = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = original.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.SOUTH).resolve().orElse(null);
            var manaFluid = createIronsManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(), 250);

            original.setCurrentMana(950);
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Spell Dispenser did not accept mana potion fluid before NBT round-trip");

            var restored = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            restored.load(original.getUpdateTag());
            helper.assertTrue(restored.getStoredManaPotionFluid().getAmount() == 250,
                    "Spell Dispenser did not restore stored mana potion fluid from NBT");
            helper.assertTrue(SpellDispenserManaFluidHelper.isSameFluidAndTags(restored.getStoredManaPotionFluid(), manaFluid),
                    "Spell Dispenser changed stored potion fluid identity during NBT round-trip");
        });
    }
    static void spellDispenserFluidAcceptsCreateManaPotion(GameTestHelper helper) {
        if (skipWhenCreateMissing(helper)) {
            return;
        }

        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).resolve().orElse(null);
            var manaFluid = createCreateManaPotionFluid(io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(), 250);

            blockEntity.setCurrentMana(850);
            helper.assertTrue(!manaFluid.isEmpty(), "Create potion fluid was not registered");
            helper.assertTrue(fluidHandler != null && fluidHandler.fill(manaFluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Spell Dispenser rejected a Create regular mana potion fluid");
            helper.assertTrue(blockEntity.getCurrentMana() == 925,
                    "Spell Dispenser did not recover mana from Create potion fluid: " + blockEntity.getCurrentMana());
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
    static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
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

            helper.assertTrue(ItemStack.isSameItemSameTags(externalInventory.getStackInSlot(0), manaPotion),
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
            spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get()));

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
    static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
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

            var restoredOwner = SpellDispenserBlockEntity.readOwnerProfile(blockEntity.getUpdateTag());
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
            restored.load(original.getUpdateTag());

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

    static void arcanumInAJarComparatorOutputMatchesStoredEssence(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(0, 1, 0), 0, 0, 0);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(1, 1, 0), 3, 0, 3);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(2, 1, 0), ArcanumInAJarBlockEntity.MAX_STORED_PARAMETER, 0, 8);
            assertArcanumInAJarComparatorOutput(helper, new BlockPos(3, 1, 0), 0, 5, 0);
        });
    }

    static void atelierStationComparatorOutputMatchesStoredPotionFluidAmount(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertAtelierStationComparatorOutput(helper, new BlockPos(0, 1, 0), 0, false, 0);
            assertAtelierStationComparatorOutput(helper, new BlockPos(1, 1, 0), AtelierStationBlockEntity.MILLIBUCKETS_PER_USE, false, 1);
            assertAtelierStationComparatorOutput(helper, new BlockPos(2, 1, 0), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT / 2, false, 8);
            assertAtelierStationComparatorOutput(helper, new BlockPos(3, 1, 0), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT, false, 15);
            assertAtelierStationComparatorOutput(helper, new BlockPos(4, 1, 0), 0, true, 0);
        });
    }

    static void spellDispenserComparatorOutputMatchesStoredMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertSpellDispenserComparatorOutput(helper, new BlockPos(0, 1, 0), 0, false, 0);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(1, 1, 0), 1, false, 1);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(2, 1, 0), SpellDispenserManaHelper.MAX_MANA / 2, false, 8);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(3, 1, 0), SpellDispenserManaHelper.MAX_MANA, false, 15);
            assertSpellDispenserComparatorOutput(helper, new BlockPos(4, 1, 0), 0, true, 0);
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
            var modifiers = item.getAttributeModifiers(slotContext, UUID.randomUUID(), stack);
            helper.assertTrue(modifiers.isEmpty(),
                    "Isekai Travel Guidebook should not add spellbook attributes: " + describeModifiers(modifiers));
        });
    }
    static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
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
    static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
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
    static void archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);

            var rejected = inventory.insertItem(0, new ItemStack(Items.DIAMOND), false);
            helper.assertTrue(!rejected.isEmpty() && rejected.is(Items.DIAMOND),
                    "Archivist's Grimoire should reject non-scroll insertion");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty(),
                    "Rejected non-scroll should not remain in Archivist's Grimoire inventory");

            inventory.setStackInSlot(5, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()));
            var restoredInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            assertScrollSpell(
                    helper,
                    restoredInventory.getStackInSlot(5),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Archivist's Grimoire lost persisted scroll data"
            );

            restoredInventory.setStackInSlot(6, new ItemStack(Items.DIAMOND));
            var cleanedInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            helper.assertTrue(cleanedInventory.getStackInSlot(6).isEmpty(),
                    "Archivist's Grimoire should purge non-scroll stacks from saved inventory data");
        });
    }
    static void archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 2 + 4,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 5 + 1,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())
            );

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 4);
            helper.assertTrue(ArchivistsGrimoire.ensureSelectedRowHasScroll(grimoireStack),
                    "Archivist's Grimoire should find a populated row from an empty selected row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 2,
                    "Archivist's Grimoire should normalize empty selected row to the first populated row");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 1),
                    "Archivist's Grimoire should move forward to the next populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire forward navigation skipped the wrong rows");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 1),
                    "Archivist's Grimoire should wrap forward to the first populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 2,
                    "Archivist's Grimoire forward wrap did not land on the populated row");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, -1),
                    "Archivist's Grimoire should wrap backward to the last populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire backward wrap did not land on the populated row");

            helper.assertFalse(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 0),
                    "Archivist's Grimoire should ignore zero row delta");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire zero delta should not change the selected row");
        });
    }
    static void archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT + 8,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 4 + 3,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get())
            );

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 1);
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    1,
                    "Archivist's Grimoire visible slot 0 mismatch"
            );
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 8),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get(),
                    1,
                    "Archivist's Grimoire visible slot 8 mismatch"
            );
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 3) == SpellData.EMPTY,
                    "Archivist's Grimoire should not expose spells from another row");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, -1) == SpellData.EMPTY,
                    "Archivist's Grimoire should return empty for negative visible slots");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, ArchivistsGrimoire.COLUMN_COUNT) == SpellData.EMPTY,
                    "Archivist's Grimoire should return empty for out-of-range visible slots");

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 4);
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 3),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(),
                    1,
                    "Archivist's Grimoire should expose the newly selected row"
            );
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0) == SpellData.EMPTY,
                    "Archivist's Grimoire should hide previous row spells after row change");
        });
    }

    static void archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, true,
                    "Empty Archivist's Grimoire should show the special inscription hint");

            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()));
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, false,
                    "Archivist's Grimoire with a stored spell should hide the special inscription hint");

            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2, ItemStack.EMPTY);
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, true,
                    "Archivist's Grimoire should show the special inscription hint again after becoming empty");
        });
    }
    static void archivistsGrimoireCurioAndUpgradeContractsStayRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var spellbookTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT)
            );
            helper.assertTrue(grimoireStack.is(spellbookTag),
                    "Archivist's Grimoire should be tagged for the Curios spellbook slot");
            assertUpgradeable(helper, grimoireStack,
                    "Archivist's Grimoire should remain upgradeable via explicit whitelist entry");

            var item = (ArchivistsGrimoire) ItemRegistry.ARCHIVISTS_GRIMOIRE.get();
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT,
                    helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    grimoireStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    200.0D,
                    AttributeModifier.Operation.ADDITION,
                    "Archivist's Grimoire spellbook-slot max mana bonus regression"
            );

            var wrongSlotContext = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.CHARM,
                    helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 1)),
                    0,
                    false,
                    true
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    wrongSlotContext,
                    grimoireStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    0.0D,
                    AttributeModifier.Operation.ADDITION,
                    "Archivist's Grimoire should not add spellbook max mana outside the spellbook slot"
            );
        });
    }
    static void archivistsGrimoireSpellSelectionManagerReadsVisibleRow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack);
            inventory.setStackInSlot(0, createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get()));
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 3 + 1,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 3 + 4,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get())
            );
            ArchivistsGrimoire.setSelectedRow(grimoireStack, 3);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_selection_test");
            equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT, grimoireStack);

            var manager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var spellbookOptions = manager.getSpellsForSlot(io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT);
            helper.assertTrue(spellbookOptions.size() == 2,
                    "Archivist's Grimoire should expose exactly the two spells in the selected row but got " + spellbookOptions.size());
            helper.assertTrue(spellbookOptions.stream().anyMatch(option ->
                            option.slotIndex == 1
                                    && option.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                    "Archivist's Grimoire spell selection missing selected-row Magic Missile at visible slot 1");
            helper.assertTrue(spellbookOptions.stream().anyMatch(option ->
                            option.slotIndex == 4
                                    && option.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()),
                    "Archivist's Grimoire spell selection missing selected-row Fire Breath at visible slot 4");
            helper.assertFalse(spellbookOptions.stream().anyMatch(option ->
                            option.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get()),
                    "Archivist's Grimoire spell selection should not expose spells outside the selected row");
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
    static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var gold = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var diamond = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var instantRecastSpell = SpellRegistry.HIGANBANA.get();
            var longRecastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            var continuousSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();

            helper.assertFalse(iron.canImbueSpell(instantRecastSpell, 1),
                    "Iron Spellcaster Gun should continue rejecting recast spells");
            helper.assertTrue(gold.canImbueSpell(instantRecastSpell, 1),
                    "Gold Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(instantRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(longRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow long recast spell imbuing");
            helper.assertFalse(diamond.canImbueSpell(continuousSpell, 1),
                    "Diamond Spellcaster Gun should continue rejecting continuous spells");
        });
    }
    static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            applyRestrictedImbueNormalization(helper, stack, item, spell, 1);

            var player = createArcherMultiplePlayer(helper, new BlockPos(0, 12, 0), "spellgun_recast_ammo_bypass_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertFalse(firstUse.getResult().consumesAction(),
                    "Diamond Spellcaster Gun should reject initial Archer Multiple cast without ammo");

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);

            var recastUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(recastUse.getResult().consumesAction(),
                    "Diamond Spellcaster Gun should allow recast without ammo");

            var ammoStack = new ItemStack(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(), 1);
            player.getInventory().add(ammoStack);
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);
            magicData.setPlayerCastingItem(stack);
            MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SWORD
            ));
            helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                    player,
                    player.getInventory(),
                    ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get()
            ) == 1, "Recast Spellcaster Gun cast should not consume ammo from the cast event");
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

            var restored = roundTripItemStack(stack);
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

            var restored = roundTripItemStack(stack);
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

            var restored = roundTripItemStack(stack);
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

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable after save/load");
        });
    }

    static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = ItemRegistry.PHOTON_SIPHON.get();
            var stack = createInitializedPresetStack(item);
            var spellContainer = ISpellContainer.get(stack);

            helper.assertFalse(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Photon Siphon should not block external imbue as a UniqueItem");
            helper.assertTrue(spellContainer != null, "Photon Siphon default spell container is null");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon should still start with locked Mana Charge");
        });
    }

    static void photonSiphonWorkbenchRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "photon_siphon_workbench_repair_test");
            var item = (PhotonSiphon) ItemRegistry.PHOTON_SIPHON.get();
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            var legacyReplacementStack = createInitializedPresetStack(item);
            applyLegacyLockedReplacement(helper, legacyReplacementStack, replacementSpell, 1);
            var legacyReplacementMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, legacyReplacementStack);
            legacyReplacementMenu.isSpellExtractionBlocked();
            var repairedReplacementContainer = ISpellContainer.get(legacyReplacementStack);
            helper.assertTrue(repairedReplacementContainer != null,
                    "Photon Siphon repaired replacement spell container is null");
            assertSpellData(helper, repairedReplacementContainer, 0, replacementSpell, 1, false,
                    "Photon Siphon Workbench repair should unlock legacy non-default replacement spells");

            var defaultStack = createInitializedPresetStack(item);
            var defaultMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, defaultStack);
            defaultMenu.isSpellExtractionBlocked();
            var defaultContainer = ISpellContainer.get(defaultStack);
            helper.assertTrue(defaultContainer != null, "Photon Siphon default spell container is null after Workbench check");
            assertSpellData(helper, defaultContainer, 0, SpellRegistry.MANA_CHARGE.get(), 1, true,
                    "Photon Siphon Workbench repair should not unlock the default Mana Charge");
        });
    }

    static void spellcasterWorkbenchExtractionWarningsMatchImbueState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_workbench_warning_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);

            var emptyAmuletMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, emptyAmulet);
            helper.assertTrue(emptyAmuletMenu.isBlockedByMissingSpellExtraction(),
                    "Empty Autocast Amulet should report a missing spell, not an unsupported extraction warning");
            helper.assertFalse(emptyAmuletMenu.isWarnedByUnsupportedEmptySpellExtraction(),
                    "Empty Autocast Amulet should remain a potentially extractable Workbench target");
            helper.assertTrue(emptyAmuletMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Empty Autocast Amulet should not create a scroll result");

            var imbuedAmulet = autocastAmulet.createArcaneAnvilImbueResult(
                    new ItemStack(autocastAmulet),
                    new SpellData(SpellRegistry.SENSE_EVIL.get(), 1)
            );
            var imbuedAmuletMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, imbuedAmulet);
            helper.assertFalse(imbuedAmuletMenu.isSpellExtractionBlocked(),
                    "Imbued Autocast Amulet should be extractable in Spellcaster Workbench");
            helper.assertTrue(imbuedAmuletMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem()
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                    "Imbued Autocast Amulet should create a scroll result");

            var manaForceBlade = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var emptyBlade = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(emptyBlade);
            var emptyBladeMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, emptyBlade);
            helper.assertTrue(emptyBladeMenu.isWarnedByUnsupportedEmptySpellExtraction(),
                    "Empty Mana Force Blade should warn that future imbued spells cannot be extracted");
            helper.assertFalse(emptyBladeMenu.isBlockedByMissingSpellExtraction(),
                    "Empty Mana Force Blade should not use the missing-spell message");

            var imbuedBlade = new ItemStack(manaForceBlade);
            manaForceBlade.initializeSpellContainer(imbuedBlade);
            setSingleUnlockedSpell(helper, imbuedBlade,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1);
            var imbuedBladeMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, imbuedBlade);
            helper.assertTrue(imbuedBladeMenu.isBlockedByUnsupportedSpellExtraction(),
                    "Imbued Mana Force Blade should keep the actual extraction-not-allowed error");
            helper.assertTrue(imbuedBladeMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Imbued Mana Force Blade should not create a scroll result");

            var emptyEnchantressRobe = new ItemStack(ItemRegistry.ENCHANTRESS_ROBE.get());
            var emptyEnchantressRobeMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, emptyEnchantressRobe);
            helper.assertTrue(emptyEnchantressRobeMenu.isWarnedByUnsupportedEmptySpellExtraction(),
                    "Empty Enchantress Robe chestplate should warn that future imbued spells cannot be extracted");
            helper.assertFalse(emptyEnchantressRobeMenu.isBlockedByMissingSpellExtraction(),
                    "Empty Enchantress Robe chestplate should not use the missing-spell message");

            var emptyEnchantressHatMenu = createSpellcasterWorkbenchMenuWithSingleInput(
                    player,
                    new ItemStack(ItemRegistry.ENCHANTRESS_HAT.get())
            );
            helper.assertFalse(emptyEnchantressHatMenu.isSpellExtractionBlocked(),
                    "Enchantress Hat should not warn because it does not expose an imbue slot");

            var presetStaffMenu = createSpellcasterWorkbenchMenuWithSingleInput(
                    player,
                    createInitializedPresetStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get())
            );
            helper.assertTrue(presetStaffMenu.isBlockedByDefaultSpellExtraction(),
                    "Copper Swingcast Staff preset spell should keep the default-spell extraction error");

            var spellcastersFlaskMenu = createSpellcasterWorkbenchMenuWithSingleInput(
                    player,
                    new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get())
            );
            var spellcastersFlaskResult = spellcastersFlaskMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
            helper.assertTrue(spellcastersFlaskResult.is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spellcaster's Flask should keep the Workbench particle toggle result");
            helper.assertTrue(SpellcastersFlask.isEffectParticlesSuppressed(spellcastersFlaskResult),
                    "Spellcaster's Flask Workbench result should toggle particles off from the default state");

            var alchemistsFlask = (AlchemistsFlask) ItemRegistry.ALCHEMISTS_FLASK.get();
            var defaultAlchemistsFlask = new ItemStack(alchemistsFlask);
            alchemistsFlask.initializeSpellContainer(defaultAlchemistsFlask);
            var alchemistsFlaskMenu = createSpellcasterWorkbenchMenuWithSingleInput(player, defaultAlchemistsFlask);
            helper.assertTrue(alchemistsFlaskMenu.isBlockedByDefaultSpellExtraction(),
                    "Alchemist's Flask preset Extract should keep the default-spell extraction error");
            helper.assertTrue(alchemistsFlaskMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Alchemist's Flask should not create a Workbench particle toggle result");
            helper.assertTrue(alchemistsFlaskMenu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT).isEmpty(),
                    "Alchemist's Flask result slot should not allow taking a hidden particle toggle result");
        });
    }

    static void spellcasterWorkbenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellcaster_workbench_imbue_test");
            var autocastAmulet = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
            var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
            var senseEvil = SpellRegistry.SENSE_EVIL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var greaterHeal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();

            var emptyAmulet = new ItemStack(autocastAmulet);
            autocastAmulet.initializeSpellContainer(emptyAmulet);
            var emptyAmuletMenu = createSpellcasterWorkbenchMenuWithInputs(player, emptyAmulet, createSpellScroll(heal));
            var emptyAmuletResult = emptyAmuletMenu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
            helper.assertFalse(emptyAmuletResult.isEmpty(),
                    "Empty Autocast Amulet should accept Workbench imbue with an allowlisted scroll");
            assertStackHasSpell(helper, emptyAmuletResult, heal, 1,
                    "Workbench-imbued Autocast Amulet should contain heal");
            helper.assertTrue(emptyAmuletMenu.getSlot(0).getItem().isEmpty(),
                    "Workbench imbue should consume the source Autocast Amulet");
            helper.assertTrue(emptyAmuletMenu.getSlot(1).getItem().isEmpty(),
                    "Workbench imbue should consume the source scroll");

            var twoSlotAmulet = autocastAmulet.createSpellSlotUpgradeResult(new ItemStack(autocastAmulet), upgradeItem);
            twoSlotAmulet = autocastAmulet.createArcaneAnvilImbueResult(twoSlotAmulet, new SpellData(senseEvil, 1));
            var twoSlotAmuletMenu = createSpellcasterWorkbenchMenuWithInputs(player, twoSlotAmulet, createSpellScroll(heal));
            var twoSlotAmuletResult = twoSlotAmuletMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().copy();
            helper.assertFalse(twoSlotAmuletResult.isEmpty(),
                    "Autocast Amulet with an empty extra slot should accept Workbench imbue");
            var twoSlotContainer = ISpellContainer.get(twoSlotAmuletResult);
            helper.assertTrue(twoSlotContainer != null && twoSlotContainer.getActiveSpellCount() == 2,
                    "Workbench imbue should add a second Autocast Amulet spell");
            assertStackHasSpell(helper, twoSlotAmuletResult, senseEvil, 1,
                    "Workbench imbue should keep the existing Autocast Amulet spell");
            assertStackHasSpell(helper, twoSlotAmuletResult, heal, 1,
                    "Workbench imbue should add heal to the empty Autocast Amulet slot");

            var fullAmulet = autocastAmulet.createArcaneAnvilImbueResult(twoSlotAmuletResult, new SpellData(greaterHeal, 1));
            var fullAmuletMenu = createSpellcasterWorkbenchMenuWithInputs(player, fullAmulet, createSpellScroll(heal));
            helper.assertTrue(fullAmuletMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Full Autocast Amulet should not replace an existing spell in Workbench imbue");
            helper.assertFalse(fullAmuletMenu.isBlockedByUnsupportedWorkbenchImbue(),
                    "Full Autocast Amulet should fail because no slot is available, not because the equipment is unsupported");

            var manaForceBlade = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var bladeMenu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    new ItemStack(manaForceBlade),
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            helper.assertTrue(bladeMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Workbench imbue should not produce a result for items whose imbues cannot be extracted later");
            helper.assertTrue(bladeMenu.isBlockedByUnsupportedWorkbenchImbue(),
                    "Workbench imbue should report unsupported equipment for non-extractable imbue targets");

            var externalSpellContainerStack = new ItemStack(Items.DIAMOND_SWORD);
            ISpellContainer.set(externalSpellContainerStack, ISpellContainer.create(1, false, false));
            var externalContainerMenu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    externalSpellContainerStack,
                    createSpellScroll(heal)
            );
            helper.assertTrue(externalContainerMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Workbench imbue should not produce a result for generic ISpellContainer items");
            helper.assertTrue(externalContainerMenu.isBlockedByUnsupportedWorkbenchImbue(),
                    "Generic ISpellContainer items with a scroll should report unsupported equipment instead of no-op");

            var disallowedSpellMenu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    new ItemStack(autocastAmulet),
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            helper.assertTrue(disallowedSpellMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Workbench imbue should not accept a spell rejected by the target item");
            helper.assertFalse(disallowedSpellMenu.isBlockedByUnsupportedWorkbenchImbue(),
                    "Rejected scrolls should not use the unsupported-equipment error");
        });
    }

    static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertTrue(stack.getMaxDamage() == ReflectcastShield.DURABILITY,
                    "Reflectcast Shield durability should be " + ReflectcastShield.DURABILITY + " but got " + stack.getMaxDamage());
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(2.9F, true) == 0,
                    "Reflectcast Shield should keep sub-threshold successful guard durability at zero");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(3.0F, true) == 1,
                    "Reflectcast Shield should clamp successful guard durability to one");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(12.75F, true) == 1,
                    "Reflectcast Shield should keep high-damage successful guard durability at one");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(3.0F, false) == 4,
                    "Reflectcast Shield should keep vanilla shield durability cost when the spell cannot trigger");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(12.75F, false) == 13,
                    "Reflectcast Shield should keep vanilla high-damage durability cost when the spell cannot trigger");

            var tag = stack.getOrCreateTag();
            helper.assertFalse(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 100L),
                    "Reflectcast Shield should not suppress durability before a cost is recorded");
            ReflectcastShield.rememberDurabilityConsumed(stack, 100L);
            helper.assertTrue(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 100L),
                    "Reflectcast Shield should suppress durability on the recorded tick");
            helper.assertTrue(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 110L),
                    "Reflectcast Shield should suppress durability through the ten tick window");
            helper.assertFalse(ReflectcastShield.isDurabilityConsumptionSuppressed(tag, 111L),
                    "Reflectcast Shield should allow durability after the ten tick window");
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
                        EnchantmentRegistry.SHELL,
                        EnchantmentRegistry.SYNCHRONIZATION,
                        EnchantmentRegistry.NEUTRALIZATION
                ),
                "Mana Shield Charm"
        ));
    }

    static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var shell = EnchantmentRegistry.SHELL.get();
            var synchronization = EnchantmentRegistry.SYNCHRONIZATION.get();
            var neutralization = EnchantmentRegistry.NEUTRALIZATION.get();

            helper.assertFalse(shell.isCompatibleWith(synchronization),
                    "Shell and Synchronization should stay mutually exclusive");
            helper.assertFalse(shell.isCompatibleWith(neutralization),
                    "Shell and Neutralization should stay mutually exclusive");
            helper.assertFalse(synchronization.isCompatibleWith(neutralization),
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

        var shellCharm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        shellCharm.enchant(EnchantmentRegistry.SHELL.get(), 1);
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

        armoredMana.setMana(50.0F);
        unarmoredMana.setMana(50.0F);
        bypassMana.setMana(50.0F);
        armored.invulnerableTime = 0;
        unarmored.invulnerableTime = 0;
        bypassArmor.invulnerableTime = 0;
        var armoredInitialHealth = armored.getHealth();
        var unarmoredInitialHealth = unarmored.getHealth();
        var bypassInitialHealth = bypassArmor.getHealth();
        helper.runAtTickTime(1, () -> {
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
                    "Shell should still burn out the unarmored player at 50 mana");
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
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SHELL.get(), 1);
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
            var incomingDamage = findDamageForArmorReducedTarget(armor, toughness, 1.0F);
            var reducedDamage = CombatRules.getDamageAfterAbsorb(incomingDamage, armor, toughness);

            helper.assertTrue(Math.abs(reducedDamage - 1.0F) < 1.0e-3F,
                    "Shell low mana test should configure an armor-reduced hit worth exactly one barrier step"
                            + " reducedDamage=" + reducedDamage
                            + " incomingDamage=" + incomingDamage);

            var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), incomingDamage);
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
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armorStack = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                player.setItemSlot(slot, armorStack);
            }

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization cost test could not resolve player mana data");
            magicData.setMana(120.0F);
            var availableMana = magicData.getMana();
            player.invulnerableTime = 0;

            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
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
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armorStack = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
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
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            var expectedHealthLoss = CombatRules.getDamageAfterAbsorb(
                    CombatRules.getDamageAfterMagicAbsorb(5.0F, protection),
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
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            equipProtectionIvIronArmor(player);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization low mana enchant-stage test could not resolve player mana data");
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var expectedArmor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var expectedToughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(5.0F, protection);
            var synchronizationSteps = countWholeDamageStepsForGameTest(5.0F - reducedDamage);
            helper.assertTrue(synchronizationSteps > 0,
                    "Synchronization low mana enchant-stage test should require at least one enchant mitigation cost step");
            magicData.setMana(synchronizationSteps * 30.0F - 1.0F);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            var expectedHealthLoss = CombatRules.getDamageAfterAbsorb(
                    reducedDamage,
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
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            equipProtectionIvIronArmor(player);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization low mana barrier-stage test could not resolve player mana data");
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
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
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.NEUTRALIZATION.get(), 1);
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

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Spellcaster Gun should stay cleared after save/load");
        });
    }
    static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(stack);
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

            var restored = roundTripItemStack(stack);
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

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff legacy locked replacement should be recovered after save/load");
        });
    }
    static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
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
    static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()),
                    "Ender Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get()),
                    "Archivist's Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "Elemental Bow should remain upgradeable via explicit whitelist entry");
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
    static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
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
    static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
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
                    EquipmentSlot.MAINHAND,
                    item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack)
            );
            MinecraftForge.EVENT_BUS.post(event);

            assertSingleModifierAmount(
                    helper,
                    event.getModifiers().get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Charged Twin Blade Staff melee damage upgrade should be a single display modifier"
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(event.getModifiers())
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
            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade test could not resolve the imbued spell school");
            var attunementAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Mana Force Blade test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            var upgradeKey = findUpgradeKeyForPowerAttribute(attunementAttribute);
            helper.assertTrue(upgradeKey != null,
                    "Mana Force Blade test could not resolve a matching upgrade orb for " + ForgeRegistries.ATTRIBUTES.getKey(attunementAttribute));

            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    upgradeKey,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    EquipmentSlot.MAINHAND,
                    item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack)
            );
            MinecraftForge.EVENT_BUS.post(event);

            assertSingleModifierAmount(
                    helper,
                    event.getModifiers().get(attunementAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.09D,
                    "Mana Force Blade Attunement and matching upgrade should merge into one display modifier"
                            + " spell=" + spell.getSpellResource()
                            + " school=" + imbuedSchool.getId()
                            + " attribute=" + ForgeRegistries.ATTRIBUTES.getKey(attunementAttribute)
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(event.getModifiers())
            );
        });
    }

    static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_attack_mana_once_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "Mana Force Blade attack mana test could not resolve player mana data");
            magicData.setMana(100.0F);

            var firstTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            var secondTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            item.hurtEnemy(stack, firstTarget, player);
            item.hurtEnemy(stack, secondTarget, player);

            var expectedMana = 100.0F
                    - jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(player, stack);
            helper.assertTrue(Math.abs(magicData.getMana() - expectedMana) < 1.0e-4F,
                    "Mana Force Blade should spend attack mana once per tick even when multiple targets are hit"
                            + " expected=" + expectedMana
                            + " actual=" + magicData.getMana());
        });
    }

    static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.ManaForceBlade) ItemRegistry.MANA_FORCE_BLADE.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);
            setSingleUnlockedSpell(helper, stack,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.GUIDING_BOLT_SPELL.get(), 1);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mana_force_blade_config_formula_test");
            var spellPower = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            helper.assertTrue(spellPower != null,
                    "Mana Force Blade config formula test could not resolve spell power attribute");
            if (spellPower != null) {
                spellPower.setBaseValue(1.5D);
            }

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null,
                    "Mana Force Blade config formula test could not resolve imbued school");
            var schoolPowerAttribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(schoolPowerAttribute != null,
                    "Mana Force Blade config formula test could not resolve school power attribute");
            var schoolPower = schoolPowerAttribute == null ? null : player.getAttribute(schoolPowerAttribute);
            helper.assertTrue(schoolPower != null,
                    "Mana Force Blade config formula test could not resolve player school power instance");
            if (schoolPower != null) {
                schoolPower.setBaseValue(1.2D);
            }

            var baseDamage = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackDamage(stack);
            var damageMultiplier = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 1.0F);
            helper.assertTrue(Math.abs(damageMultiplier - 1.8F) < 1.0e-4F,
                    "Mana Force Blade should multiply spell power and school power for imbued damage but got "
                            + damageMultiplier);
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.5F) - 0.9F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale should directly scale the final school multiplier");
            helper.assertTrue(Math.abs(jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveDamageMultiplier(player, stack, 0.0F) - 1.0F) < 1.0e-4F,
                    "Mana Force Blade imbue damage scale 0 should disable imbued damage changes");

            var fullManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 1.0F);
            helper.assertTrue(Math.abs(fullManaCost - baseDamage * 3.0F * 1.8F) < 1.0e-4F,
                    "Mana Force Blade full school mana scale should follow final imbued damage: " + fullManaCost);

            var halfSchoolManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.5F, 1.0F);
            helper.assertTrue(Math.abs(halfSchoolManaCost - baseDamage * 3.0F * 1.4F) < 1.0e-4F,
                    "Mana Force Blade half school mana scale should only halve the school-derived increase: "
                            + halfSchoolManaCost);

            var noSchoolManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 0.0F, 1.0F);
            helper.assertTrue(Math.abs(noSchoolManaCost - baseDamage * 3.0F) < 1.0e-4F,
                    "Mana Force Blade school mana scale 0 should ignore school multiplier for mana cost: "
                            + noSchoolManaCost);

            var disabledManaCost = jp.aquafactory.apprenticecodex.item.ManaForceBlade.resolveBladeAttackManaCost(
                    player, stack, 3.0F, 1.0F, 0.0F);
            helper.assertTrue(disabledManaCost == 0.0F,
                    "Mana Force Blade imbue damage scale 0 should also disable hit mana cost");
        });
    }

    static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
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
    static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
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
    static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
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
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.get();
            var movementSpeedModifier = effect.getAttributeModifiers().get(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            helper.assertTrue(movementSpeedModifier != null, "LongStride is missing the movement speed attribute modifier");

            var actualAmount = effect.getAttributeModifierValue(0, movementSpeedModifier);
            helper.assertTrue(Math.abs(actualAmount - 0.15D) < 1.0e-9D,
                    "LongStride movement speed bonus regression: expected 0.15 but got " + actualAmount);
        });
    }
    static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dynamic_casting_movespeed_rebalance_test");
            var effect = (jp.aquafactory.apprenticecodex.effect.LongStrideMobility) EffectRegistry.LONG_STRIDE_MOBILITY.get();
            var castingMoveSpeed = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.get());
            helper.assertTrue(castingMoveSpeed != null,
                    "Dynamic casting mobility test could not resolve the CASTING_MOVESPEED attribute");

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 200, 0));
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
                    "apprenticecodex.casting_movespeed.dynamic_test",
                    0.5D,
                    AttributeModifier.Operation.ADDITION
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
    static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
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
    static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spell Gun",
                // 1.21.1申し送り事項:
                // enchantable / book / anvil の面は Item 定義と Forge 側フックの移植差で崩れやすい。
                // 1.20.1 の通りに見えても、1.21.1 では spell gun 系をそのまま持ち込める前提にしないこと。
                item -> item instanceof AbstractSpellGunItem,
                ApprenticeCodexGameTestScenarios::expectedSpellGunEnchantments
        ));
    }
    static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
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
    static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
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
    static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
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
    static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
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
    static void elementalBowHeldWisdomAndPlunderWorkInBothHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();

            var mainhandPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "elemental_bow_mainhand_held_enchant_test"));
            var mainhandBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            mainhandBow.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            mainhandBow.enchant(EnchantmentRegistry.PLUNDER.get(), 2);
            mainhandPlayer.setItemInHand(InteractionHand.MAIN_HAND, mainhandBow);

            var mainhandExperience = new BlockEvent.BreakEvent(level, new BlockPos(3, 2, 0), state, mainhandPlayer);
            mainhandExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(mainhandExperience);
            helper.assertTrue(mainhandExperience.getExpToDrop() == 4,
                    "Elemental Bow mainhand Wisdom should increase block experience from 3 to 4 but got " + mainhandExperience.getExpToDrop());

            var mainhandLootingEvent = new net.minecraftforge.event.entity.living.LootingLevelEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1)),
                    mainhandPlayer.damageSources().playerAttack(mainhandPlayer),
                    0
            );
            jp.aquafactory.apprenticecodex.enchantment.PlunderLootingLevelEvent.onLootingLevel(mainhandLootingEvent);
            helper.assertTrue(mainhandLootingEvent.getLootingLevel() == 2,
                    "Elemental Bow mainhand Plunder should set looting level to 2 but got " + mainhandLootingEvent.getLootingLevel());

            var offhandPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "elemental_bow_offhand_held_enchant_test"));
            var offhandBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            offhandBow.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            offhandBow.enchant(EnchantmentRegistry.PLUNDER.get(), 3);
            offhandPlayer.setItemInHand(InteractionHand.OFF_HAND, offhandBow);

            var offhandExperience = new BlockEvent.BreakEvent(level, new BlockPos(4, 2, 0), state, offhandPlayer);
            offhandExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(offhandExperience);
            helper.assertTrue(offhandExperience.getExpToDrop() == 4,
                    "Elemental Bow offhand Wisdom should increase block experience from 3 to 4 but got " + offhandExperience.getExpToDrop());

            var offhandLootingEvent = new net.minecraftforge.event.entity.living.LootingLevelEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1)),
                    offhandPlayer.damageSources().playerAttack(offhandPlayer),
                    0
            );
            jp.aquafactory.apprenticecodex.enchantment.PlunderLootingLevelEvent.onLootingLevel(offhandLootingEvent);
            helper.assertTrue(offhandLootingEvent.getLootingLevel() == 3,
                    "Elemental Bow offhand Plunder should set looting level to 3 but got " + offhandLootingEvent.getLootingLevel());
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

    static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_apprentice_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            assertCraftsmansDelightBasicDiscountOnly(helper, player, SpellRegistry.HARVEST_MOON.get(), 60, "Harvest Moon");
            assertCraftsmansDelightBasicDiscountOnly(helper, player, SpellRegistry.EARTH_FORGE.get(), 20, "Earth Forge");
        });
    }

    private static void assertCraftsmansDelightBasicDiscountOnly(
            GameTestHelper helper,
            FakePlayer player,
            AbstractSpell spell,
            int baseManaCost,
            String spellName
    ) {
        if (!(spell instanceof ICraftsmansDelightAffectedSpell affectedSpell)) {
            helper.fail(spellName + " should opt into CraftsmansDelight support");
            return;
        }

        helper.assertFalse(affectedSpell.isCraftsmansDelightBreakSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight break speed bonuses");
        helper.assertFalse(affectedSpell.isCraftsmansDelightProcessSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight process speed bonuses");
        helper.assertFalse(affectedSpell.isCraftsmansDelightCastingMobilityEnabled(),
                spellName + " should keep CraftsmansDelight casting mobility disabled");
        helper.assertTrue(CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spell.getSpellId()),
                spellName + " should be a CraftsmansDelight mana discount target");
        helper.assertTrue(CraftsmansDelightSpellSupport.isCooldownReductionTarget(spell),
                spellName + " should be a CraftsmansDelight cooldown reduction target");

        var manaEvent = new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                1,
                baseManaCost,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        CraftsmansDelightManaCostDiscountEvent.onSpellCast(manaEvent);
        var expectedManaCost = Math.max(1, Math.round(baseManaCost * 0.5f));
        helper.assertTrue(manaEvent.getManaCost() == expectedManaCost,
                spellName + " mana cost should be reduced to " + expectedManaCost + " but got " + manaEvent.getManaCost());

        var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                spell.getSpellCooldown(),
                spell,
                player,
                CastSource.SPELLBOOK
        );
        CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(cooldownEvent);
        helper.assertTrue(cooldownEvent.getEffectiveCooldown()
                        == CraftsmansDelight.getReducedEffectiveCooldown(spell, player, CastSource.SPELLBOOK),
                spellName + " cooldown should route through the reduced cooldown helper");
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
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped");
            helper.assertTrue(spell.getUniqueInfo(1, player).stream().anyMatch(component -> component.getString().contains("16")),
                    "Touch Dig unique info should display 16 block range while CraftsmansDelight is equipped");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }
    static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_ring_enchant_merge_test");
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(Enchantments.BLOCK_FORTUNE, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.BLOCK_FORTUNE, 3);
            equipRingCurio(player, ringStack);

            var mergedFortuneTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(mergedFortuneTool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) == 3,
                    "Touch Dig should prefer the higher Fortune level from the ring");

            ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var mergedSilkTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertTrue(mergedSilkTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "Touch Dig should inherit Silk Touch from the ring");
            helper.assertTrue(mergedSilkTool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) == 0,
                    "Touch Dig should drop Fortune when Silk Touch is present");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 1));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = getFreshItemDrops(helper.getLevel(), blockPos, 1.5D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig with ring Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
    static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_bare_hand_ring_enchant_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var synthesizedTool = CraftsmansDelight.createTouchDigTool(player);
            helper.assertFalse(synthesizedTool.isEmpty(),
                    "Touch Dig should synthesize a mining tool when the caster is bare-handed but the ring has mining enchantments");
            helper.assertTrue(synthesizedTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "Touch Dig should copy Silk Touch onto the synthesized bare-hand tool");

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = getFreshItemDrops(helper.getLevel(), blockPos, 1.5D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Bare-hand Touch Dig with ring Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
    static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "spectral_hammer_ring_enchant_test");
            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

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

            var drops = getFreshItemDrops(helper.getLevel(), targetPos, 2.0D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should drop stone");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Spectral Hammer with ring Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
    static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var existingItemPos = Vec3.atCenterOf(logPos);
            var existingItem = new ItemEntity(
                    level,
                    existingItemPos.x,
                    existingItemPos.y,
                    existingItemPos.z,
                    new ItemStack(Items.COBBLESTONE)
            );
            level.addFreshEntity(existingItem);

            var job = new TinyLumberjackJob(originPos, 1, player);
            job.tick(level);

            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should move new log drops to the initial chopped block while CraftsmansDelight is equipped");
            helper.assertTrue(!existingItem.isRemoved() && existingItem.position().distanceToSqr(existingItemPos) < 0.01D,
                    "Tiny Lumberjack drop moving should not move ItemEntities that existed before the block break");
            helper.succeed();
        });
    }

    static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_unequip_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var job = new TinyLumberjackJob(originPos, 1, player);
            equipRingCurio(player, ItemStack.EMPTY);
            job.tick(level);

            helper.assertFalse(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should stop moving job drops after CraftsmansDelight is unequipped");
            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(logPos), 1.25D),
                    "Tiny Lumberjack should leave log drops near the broken block when CraftsmansDelight is not currently equipped");
            helper.succeed();
        });
    }

    static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_penetrated_armor_test");
            var armor = player.getAttribute(Attributes.ARMOR);
            var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            helper.assertTrue(armor != null, "Player is missing armor attribute");
            helper.assertTrue(toughness != null, "Player is missing armor toughness attribute");

            armor.setBaseValue(10.0D);
            toughness.setBaseValue(8.0D);
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR.get(), 100, 0));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 8.0D) < 1.0E-6D,
                    "Penetrated Armor I should reduce armor by 20%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor should reduce armor toughness by 100%");

            player.removeEffect(EffectRegistry.PENETRATED_ARMOR.get());
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR.get(), 100, 3));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 2.0D) < 1.0E-6D,
                    "Penetrated Armor IV should reduce armor by 80%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor toughness reduction should not depend on amplifier");

            var source = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(
                    helper.getLevel(),
                    player,
                    DamageTypes.WORLD_FLATTER
            );
            helper.assertTrue(source.is(DamageTypes.WORLD_FLATTER),
                    "World Flatter damage source should use apprenticecodex:world_flatter");
            helper.assertTrue(!source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "World Flatter should no longer use apprenticecodex:bypasses_iframe");
            helper.assertTrue(!source.is(DamageTypeTags.BYPASSES_COOLDOWN),
                    "World Flatter should no longer bypass vanilla cooldown i-frame");
        });
    }

    static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_block_filter_test");
            var pos = helper.absolutePos(new BlockPos(0, 2, 0));

            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should target pickaxe-mineable stone");
            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState()),
                    "World Flatter should target shovel-mineable dirt");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.GLASS.defaultBlockState(), Blocks.GLASS.defaultBlockState()),
                    "World Flatter should reject glass because it has no specific pickaxe/shovel tool tag");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()),
                    "World Flatter should reject axe-mineable logs");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.BEDROCK.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()),
                    "World Flatter should reject unbreakable blocks");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should not splash unrelated ore blocks from a non-ore center");
        });
    }

    static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "world_flatter_single_target_owner");
            owner.setYRot(0.0F);
            owner.setXRot(0.0F);

            var target = helper.spawn(EntityType.SHEEP, new BlockPos(0, 2, 4));
            var bystander = helper.spawn(EntityType.SHEEP, new BlockPos(1, 2, 4));
            target.setNoAi(true);
            bystander.setNoAi(true);
            var targetHealth = target.getHealth();
            var bystanderHealth = bystander.getHealth();

            var weapon = new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), level, owner);
            weapon.setDamage(4.0F);
            weapon.setPenetratedArmorAmplifier(1);
            weapon.setToolSpeed(4.0F);
            weapon.updateOwnerTarget(level, new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY,
                    target.getBoundingBox().getCenter(),
                    target,
                    null
            ));

            for (var i = 0; i < 14; ++i) {
                target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
                weapon.tickOnServer(level);
            }
            helper.assertTrue(Math.abs(target.getHealth() - targetHealth) < 1.0E-6F,
                    "World Flatter should not damage an entity before the 15 tick attach completes");

            target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
            weapon.tickOnServer(level);
            helper.assertTrue(target.getHealth() < targetHealth,
                    "World Flatter should damage the attached moving target after 15 ticks");
            helper.assertTrue(target.hasEffect(EffectRegistry.PENETRATED_ARMOR.get()),
                    "World Flatter should apply Penetrated Armor after successful damage");
            helper.assertTrue(Math.abs(bystander.getHealth() - bystanderHealth) < 1.0E-6F,
                    "World Flatter should not damage nearby non-target entities");
        });
    }

    static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
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

    static void rightClickMagicWeaponTooltipsStartWithShieldHint(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var rightClickMagicWeapons = ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> item instanceof AbstractRightClickMagicWeaponItem)
                    .toList();
            helper.assertTrue(!rightClickMagicWeapons.isEmpty(),
                    "Right Click Magic Weapon tooltip test found no target items");

            for (var item : rightClickMagicWeapons) {
                var stack = new ItemStack(item);
                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(!tooltipLines.isEmpty(),
                        item + " should expose right click magic weapon tooltip");
                assertTranslatableKey(
                        helper,
                        tooltipLines.get(0),
                        "item.apprenticecodex.right_click_magic_weapon.desc",
                        item + " should show shield priority tooltip first"
                );
            }

            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    1,
                    "item.apprenticecodex.crystal_bladed_staff.desc",
                    "Crystal Bladed Staff should show its ability tooltip after shield priority tooltip"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    1,
                    "item.apprenticecodex.swingcast.common.desc",
                    "Swingcast Staff should show swingcast tooltip after shield priority tooltip"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Spell Gun shift hint should stand out"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Swingcast Staff shift hint should stand out"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Reflectcast Shield shift hint should stand out"
            );
        });
    }

    static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            var expectedEnchantments = expectedElementalBowEnchantments();
            var expectedBookEnchantments = expectedElementalBowBookEnchantments();
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantments,
                    expectedBookEnchantments,
                    expectedEnchantments,
                    "Elemental Bow"
            );
        });
    }
    static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_view_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            var healingArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), net.minecraft.world.item.alchemy.Potions.HEALING);
            var regenerationArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var healingId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(healingArrow));
            var regenerationId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(regenerationArrow));
            helper.assertTrue(healingId != null && regenerationId != null,
                    "Elemental Bow selection view test could not resolve tipped arrow potion ids");
            var availablePotionIds = new LinkedHashSet<ResourceLocation>();
            if (healingId != null) {
                availablePotionIds.add(healingId);
            }
            if (regenerationId != null) {
                availablePotionIds.add(regenerationId);
            }
            var expectedPotionOrder = ForgeRegistries.POTIONS.getValues().stream()
                    .map(ForgeRegistries.POTIONS::getKey)
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
            helper.assertTrue("\u221e".equals(views.get(1).badgeText()),
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

            var spectralArrowId = ResourceLocation.tryParse("minecraft:spectral_arrow");
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

            var healingArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), net.minecraft.world.item.alchemy.Potions.HEALING);
            var healingId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(healingArrow));
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
                    helper.assertTrue(PotionUtils.getPotion(tippedOverlay.iconStack()) == net.minecraft.world.item.alchemy.Potions.HEALING,
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
        stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
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
    static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_empty_selection_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));

            var selectedViews = ElementalBow.getAvailableSelectionViews(player, stack);
            var spectralView = selectedViews.stream()
                    .filter(view -> "special".equals(view.selection().shotMode())
                            && ResourceLocation.tryParse("minecraft:spectral_arrow").equals(view.selection().selectionId()))
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
                                    && ResourceLocation.tryParse("minecraft:spectral_arrow").equals(view.selection().selectionId())),
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
    static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_mana_gate_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
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
    static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.getOrCreateTag().putString("ElementalBowMode", "fire");

            item.initializeSpellContainer(stack);

            assertElementalBowSelection(helper, stack, null, null,
                    "Elemental Bow should clear unresolved legacy mode values back to normal mode");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container after falling back to normal mode");
        });
    }
    static void elementalBowSynchronizesSpellContainerToCurrentMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            item.initializeSpellContainer(stack);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Elemental Bow should expose a spell container outside NONE mode");
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Elemental Bow should keep its derived spell out of the spell wheel");
            assertSpellData(
                    helper,
                    spellContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    1,
                    true,
                    "Elemental Bow should sync Fire mode into a locked spell container"
            );
        });
    }
    static void elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.enchant(Enchantments.POWER_ARROWS, 2);
            stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 1);
            stack.enchant(Enchantments.FLAMING_ARROWS, 1);

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            item.initializeSpellContainer(stack);
            helper.assertTrue(stack.getEnchantmentLevel(Enchantments.POWER_ARROWS) == 2,
                    "Elemental Bow spell container test should preserve POWER II on the stack");
            helper.assertTrue(stack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) == 1,
                    "Elemental Bow spell container test should preserve FLAME I on the stack");
            helper.assertTrue(stack.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get()) == 1,
                    "Elemental Bow spell container test should preserve TRANSCENDENCE I on the stack");
            var fireMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireMode != null, "Elemental Bow Fire mode should resolve from the loaded mode definitions");
            var expectedFireLevel = fireMode != null ? fireMode.resolveSpellLevel(stack) : 1;
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow should expose a displayed spell profile in Fire mode");
            helper.assertTrue(fireProfile.spell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    "Elemental Bow Fire mode should resolve Fire Arrow");
            helper.assertTrue(fireProfile.spellLevel() == expectedFireLevel,
                    "Elemental Bow Fire mode display level should stay in sync with the loaded mode resolver but got " + fireProfile.spellLevel());
            var fireContainer = ISpellContainer.get(stack);
            helper.assertTrue(fireContainer != null, "Elemental Bow Fire mode should keep a synced spell container");
            helper.assertTrue(fireContainer != null && !fireContainer.isSpellWheel(),
                    "Elemental Bow Fire mode container should stay hidden from the spell wheel");
            assertSpellData(
                    helper,
                    fireContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    expectedFireLevel,
                    true,
                    "Elemental Bow Fire mode container should stay in sync with the loaded mode resolver"
            );

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.ENDER_RESOURCE);
            item.initializeSpellContainer(stack);
            var enderMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.ENDER_RESOURCE);
            helper.assertTrue(enderMode != null, "Elemental Bow Ender mode should resolve from the loaded mode definitions");
            var expectedEnderLevel = enderMode != null ? enderMode.resolveSpellLevel(stack) : 1;
            var enderProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(enderProfile != null, "Elemental Bow should expose a displayed spell profile in Ender mode");
            helper.assertTrue(enderProfile.spell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get(),
                    "Elemental Bow Ender mode should resolve Magic Arrow");
            helper.assertTrue(enderProfile.spellLevel() == expectedEnderLevel,
                    "Elemental Bow Ender mode display level should stay in sync with the loaded mode resolver but got " + enderProfile.spellLevel());
            var enderContainer = ISpellContainer.get(stack);
            helper.assertTrue(enderContainer != null, "Elemental Bow Ender mode should keep a synced spell container");
            helper.assertTrue(enderContainer != null && !enderContainer.isSpellWheel(),
                    "Elemental Bow Ender mode container should stay hidden from the spell wheel");
            assertSpellData(
                    helper,
                    enderContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get(),
                    expectedEnderLevel,
                    true,
                    "Elemental Bow Ender mode container should stay in sync with the loaded mode resolver"
            );

            stack.getOrCreateTag().remove("ElementalBowMode");
            item.initializeSpellContainer(stack);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container in NONE mode");
        });
    }
    static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_spell_wheel_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            ((ElementalBow) stack.getItem()).initializeSpellContainer(stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var mainhandSelections = selectionManager.getSpellsForSlot(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND);
            helper.assertTrue(mainhandSelections.isEmpty(),
                    "Elemental Bow should not add its derived spell to the mainhand spell wheel: " + mainhandSelections);
            helper.assertTrue(selectionManager.getSelection() == null,
                    "Elemental Bow should not create a selected spell from its derived container");
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
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
            helper.assertTrue(selection != null && io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(selection.slot),
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
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration() - jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player)
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
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration() - (jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player) - 1)
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
    static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_hold_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            helper.assertTrue(spellPowerAttribute != null, "Focus Staffbow continuous multiplier test could not resolve spell power attribute");
            var modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            helper.assertTrue(modifier != null && modifier.getAmount() > 0.0D,
                    "Focus Staffbow continuous multiplier should start rising immediately after cast start");
        });
        helper.runAtTickTime(101, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
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
            var modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.getAmount();
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
            var spellPowerAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
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
            var modifier = spellPowerAttribute == null ? null : spellPowerAttribute.getModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
            var actualAmount = modifier == null ? 0.0D : modifier.getAmount();
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
                        bowStack.getUseDuration() - 251
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
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
    static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_standard_time_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 2));
        MagicData.getPlayerMagicData(player).setMana(300.0F);

        var castTimeReductionAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get());
        helper.assertTrue(castTimeReductionAttribute != null,
                "Focus Staffbow continuous standard time test could not resolve cast time reduction attribute");
        if (castTimeReductionAttribute != null) {
            castTimeReductionAttribute.addPermanentModifier(new AttributeModifier(
                    UUID.fromString("6cc24610-4701-4af1-a197-f1403c48f2fb"),
                    "apprenticecodex.focus_staffbow.continuous_standard_time_test",
                    0.75D,
                    AttributeModifier.Operation.MULTIPLY_BASE
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
                        bowStack.getUseDuration() - 2
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
    static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_continuous_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
    static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "focus_staffbow_instant_base_mana_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration()
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
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration() - spell.getEffectiveCastTime(1, player)
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
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration() - 120
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
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration() - 120
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
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration()
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
        bowStack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
        var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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
                        bowStack.getUseDuration()
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
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
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

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration());
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 1,
                    "Focus Staffbow should consume the equipped Spellcaster Quiver arrow before loose inventory arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Focus Staffbow should leave loose inventory arrows untouched while the quiver still has arrows");
        });
    }
    static void focusStaffbowAcceptsSynthesisEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var item = (FocusStaffbow) stack.getItem();
            helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.SYNTHESIS.get()),
                    "Focus Staffbow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.SYNTHESIS.get())),
                    "Focus Staffbow should accept Synthesis from enchanted books");
            helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, EnchantmentRegistry.SYNTHESIS.get()),
                    "Focus Staffbow should allow Synthesis through anvil merges");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, Enchantments.INFINITY_ARROWS),
                    "Focus Staffbow should reject Infinity at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(Enchantments.INFINITY_ARROWS)),
                    "Focus Staffbow should reject Infinity from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, Enchantments.INFINITY_ARROWS),
                    "Focus Staffbow should reject Infinity through anvil merges");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.TRANSCENDENCE.get()),
                    "Focus Staffbow should reject Transcendence at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.TRANSCENDENCE.get())),
                    "Focus Staffbow should reject Transcendence from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, EnchantmentRegistry.TRANSCENDENCE.get()),
                    "Focus Staffbow should reject Transcendence through anvil merges");

            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var haunted = MalumHauntedCompat.getHauntedEnchantment();
            helper.assertTrue(haunted != null, "malum:haunted is not registered");
            helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, haunted),
                    "Focus Staffbow should allow malum:haunted at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(haunted)),
                    "Focus Staffbow should allow malum:haunted from enchanted books");
            helper.assertTrue(item.isAnvilMergeEnchantmentAllowed(stack, haunted),
                    "Focus Staffbow should allow malum:haunted through anvil merges");

            var animated = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_ANIMATED);
            helper.assertTrue(animated != null, "malum:animated is not registered");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, animated),
                    "Focus Staffbow should keep rejecting malum:animated at the enchanting table");
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, createEnchantedBook(animated)),
                    "Focus Staffbow should keep rejecting malum:animated from enchanted books");
            helper.assertFalse(item.isAnvilMergeEnchantmentAllowed(stack, animated),
                    "Focus Staffbow should keep rejecting malum:animated through anvil merges");
        });
    }
    static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION
            ) - 3.0D) < 1.0e-9D, "Focus Staffbow attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION
            ) - (-3.0D)) < 1.0e-9D, "Focus Staffbow attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            ) - 0.10D) < 1.0e-9D, "Focus Staffbow spell power regression: " + describeModifiers(modifiers));
        });
    }
    static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedChargedTwinBladeStaffEnchantments(stack),
                    "Charged Twin Blade Staff"
            );
        });
    }
    static void manaForceBladeKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedManaForceBladeEnchantments(stack),
                    "Mana Force Blade"
            );
        });
    }
    static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION
            ) - 10.0D) < 1.0e-9D, "Charged Twin Blade Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            ) - 0.10D) < 1.0e-9D, "Charged Twin Blade Staff spell power regression: " + describeModifiers(modifiers));
        });
    }
    static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var baseStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var baseDamage = ChargedTwinBladeStaff.resolveThrownDamage(baseStack, MobType.UNDEFINED);
            helper.assertTrue(Math.abs(baseDamage - 11.0D) < 1.0e-9D,
                    "Charged Twin Blade Staff base thrown damage regression: " + baseDamage);

            var sharpnessStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            sharpnessStack.enchant(Enchantments.SHARPNESS, 3);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    sharpnessStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(sharpnessStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff sharpness thrown damage regression"
            );

            var smiteStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            smiteStack.enchant(Enchantments.SMITE, 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    MobType.UNDEAD,
                    baseDamage + EnchantmentHelper.getDamageBonus(smiteStack, MobType.UNDEAD),
                    "Charged Twin Blade Staff smite thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(smiteStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff smite fallback damage regression"
            );

            var baneStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            baneStack.enchant(Enchantments.BANE_OF_ARTHROPODS, 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    MobType.ARTHROPOD,
                    baseDamage + EnchantmentHelper.getDamageBonus(baneStack, MobType.ARTHROPOD),
                    "Charged Twin Blade Staff bane thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(baneStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff bane fallback damage regression"
            );

            var impalingStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            impalingStack.enchant(Enchantments.IMPALING, 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    MobType.WATER,
                    baseDamage + EnchantmentHelper.getDamageBonus(impalingStack, MobType.WATER),
                    "Charged Twin Blade Staff impaling thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(impalingStack, MobType.UNDEFINED),
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
                stack.getUseDuration() - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
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
        stack.enchant(net.minecraft.world.item.enchantment.Enchantments.LOYALTY, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff loyalty mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration() - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - 100.0F / 3.0F)) < 1.0e-3F,
                    "Charged Twin Blade Staff loyalty mana discount regressed: " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_riptide_test");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff riptide test could not resolve player mana data");
            magicData.setMana(50.0F);

            stack.getItem().releaseUsing(
                    stack,
                    helper.getLevel(),
                    player,
                    stack.getUseDuration() - jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))), new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff self profile failed to cast Oakskin"
            );
            helper.assertTrue(player.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get()),
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
            helper.assertTrue(player.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get()),
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
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ray_of_siphoning"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
                io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
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
    static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Elemental Bow should reject Arcane Anvil spell imbuing regardless of scroll spell"
            );
        });
    }
    static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = ElementalBow.createInsufficientManaMessage(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    null
            );
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.irons_spellbooks.cast_error_mana",
                    "Elemental Bow mana error should use Iron's cast_error_mana key"
            );
        });
    }
    static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_partial_release_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow partial release test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Elemental Bow should start drawing when mana and ammo are available: " + useResult.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 19);
            helper.assertTrue(stack.getDamageValue() == 0, "Elemental Bow should not lose durability before full draw");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should not consume arrows before full draw");
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Elemental Bow should not consume mana before full draw: " + magicData.getMana());
        });
    }
    static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start vanilla draw with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow should enter use state for Infinity vanilla draw");
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
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 20);
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
    static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow arrow-only mode should enter use state for Infinity draw");
        });
    }
    static void elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_special_arrow_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing while the selected arrow exists: " + firstUse.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow special mode should consume the selected arrow even with Infinity");

            var secondUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(secondUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing again with Infinity after the selected arrow runs out: " + secondUse.getResult());
            helper.assertTrue(player.isUsingItem(),
                    "Elemental Bow special mode should enter use state again while keeping its empty selection");
            assertElementalBowSelection(helper, stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"),
                    "Elemental Bow special mode should keep the selected arrow after ammo loss");
        });
    }
    static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow magic mode should fail to start without ammo even with Infinity: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow magic mode should not enter use state without ammo");
        });
    }
    static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.SYNTHESIS.get()),
                    "Elemental Bow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.SYNTHESIS.get())),
                    "Elemental Bow should accept Synthesis from enchanted books");
            helper.assertFalse(EnchantmentRegistry.SYNTHESIS.get().isCompatibleWith(Enchantments.INFINITY_ARROWS),
                    "Synthesis should be incompatible with Infinity");
            helper.assertFalse(EnchantmentRegistry.SYNTHESIS.get().isCompatibleWith(Enchantments.MENDING),
                    "Synthesis should be incompatible with Mending");

            assertTooltipKeyAt(helper, stack, 0, "item.apprenticecodex.elemental_bow.mode",
                    "Elemental Bow should always show the current mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.desc", ChatFormatting.GRAY,
                    "Elemental Bow should always show the description tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment",
                    "Elemental Bow should not show spell ammo tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show Infinity spell tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_synthesis",
                    "Elemental Bow should not show Synthesis spell tooltip while not in magic mode");

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            assertTooltipKeyAt(helper, stack, 1, "item.apprenticecodex.elemental_bow.desc",
                    "Elemental Bow should show the description below the mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment", ChatFormatting.YELLOW,
                    "Elemental Bow should show the no-enchantment spell tooltip in magic mode");

            var infinityStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(infinityStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            infinityStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            assertTooltipKeyUsesColor(helper, infinityStack, "item.apprenticecodex.elemental_bow.spell.with_infinity", ChatFormatting.YELLOW,
                    "Elemental Bow should show the Infinity spell tooltip in magic mode");

            var synthesisStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(synthesisStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            synthesisStack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            assertTooltipKeyUsesColor(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", ChatFormatting.AQUA,
                    "Elemental Bow should show the Synthesis spell tooltip in magic mode");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.with_synthesis",
                    "Elemental Bow should no longer show the legacy Synthesis tooltip key");

            synthesisStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            assertTooltipKeyUsesColor(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", ChatFormatting.AQUA,
                    "Elemental Bow should prefer the Synthesis spell tooltip when Synthesis and Infinity are both present");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show the Infinity spell tooltip when Synthesis is also present");
        });
    }
    static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_empty_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start without arrows when Synthesis is enchanted: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(stack.getDamageValue() == 1,
                    "Elemental Bow Synthesis magic shot should still damage the bow after a successful cast");
            helper.assertTrue(magicData.getMana() < initialMana,
                    "Elemental Bow Synthesis magic shot should still consume spell mana");
        });
    }
    static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_ammo_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis ammo test could not resolve player mana data");
            magicData.setMana(250.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start with Synthesis while arrows are present: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
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
            SpellcasterQuiverPickupEvent.onEntityItemPickup(new EntityItemPickupEvent(player, itemEntity));

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

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
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

            var view = findElementalBowSelectionView(player, bowStack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));
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

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
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

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
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

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Vanilla Bow should consume the lone normal arrow before more numerous Spellcaster Quiver special arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 8,
                    "Vanilla Bow should not consume Spellcaster Quiver special arrows while a normal arrow existed");
        });
    }
    static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "vanilla_bow_infinity_quiver_test");
            var bowStack = new ItemStack(Items.BOW);
            bowStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Vanilla Bow should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
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

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Elemental Bow vanilla mode should consume the held special arrow before Spellcaster Quiver normal arrows");
            helper.assertTrue(SpellcasterQuiver.getStoredItemCount(quiverStack) == 5,
                    "Elemental Bow vanilla mode should leave Spellcaster Quiver normal arrows untouched when a held special arrow was chosen");
        });
    }
    static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_quiver_test");
            var bowStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            bowStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

            var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
            SpellcasterQuiver.store(quiverStack, new ItemStack(Items.SPECTRAL_ARROW, 8));
            equipCurio(player, CuriosSlotConstants.BACK, quiverStack);

            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with Infinity and only Spellcaster Quiver special arrows: " + result.getResult());

            bowStack.getItem().releaseUsing(bowStack, helper.getLevel(), player, bowStack.getUseDuration() - 20);
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
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));

            item.initializeSpellContainer(stack);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should not expose a spell container outside magic mode");
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack) == null,
                    "Elemental Bow should not expose a displayed spell profile outside magic mode");
        });
    }
    static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_helper_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var cooldownAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get());
            helper.assertTrue(cooldownAttribute != null, "Elemental Bow cooldown helper test could not resolve cooldown attribute");
            cooldownAttribute.addPermanentModifier(new AttributeModifier(
                    UUID.fromString("24565bf4-5900-4a8f-8e05-a9f4a0db3dd7"),
                    "apprenticecodex.elemental_bow.cooldown_helper_test",
                    0.35D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));

            var expectedCooldown = (int) (fireArrow.getSpellCooldown() * (2 - Utils.softCapFormula(
                    player.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get())
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
    static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
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
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
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
            fireStack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());

            var natureStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            natureStack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.NATURE_RESOURCE.toString());

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
    static void reflectcastShieldKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
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
    static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Spellcasters Flask",
                item -> item.getClass() == SpellcastersFlask.class,
                expectedFlaskEnchantments()
        ));
    }
    static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Alchemists Flask",
                item -> item.getClass() == AlchemistsFlask.class,
                expectedAlchemistsFlaskEnchantments()
        ));
    }
    static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // 1.21.1申し送り事項:
            // treasure / tradeable / discoverable は定義形式の変更で見落としやすい。
            // フラグだけ移したつもりでも司書取引や戦利品生成がズレるので、移植時は個別に再検証すること。
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
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SYNTHESIS, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SHELL, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.SYNCHRONIZATION, false, false, true);
            assertApprenticeEnchantmentFlags(helper, EnchantmentRegistry.NEUTRALIZATION, false, false, true);
        });
    }
    static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // 1.21.1申し送り事項:
            // 本棚由来の抽選可否は enchanting table 可否だけでは追えず、book/anvil 側の定義差分でも崩れる。
            // Flask 系除外は「今も本から引けないか」を seed 探索込みで見直し、そのまま移植前提にしない。
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

                    helper.assertFalse(ApprenticeEnchantmentAvailability.isExcludedFromRandomBookLoot(enchantment),
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
                    ApprenticeCodexGameTestScenarios::expectedEnchantressRobeEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    ApprenticeCodexGameTestScenarios::expectedStealthRuneArmorEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Chromatic Magia Dress",
                    item -> item instanceof ChromaticMagiaDressItem,
                    ApprenticeCodexGameTestScenarios::expectedChromaticMagiaDressEnchantments
            );
        });
    }
    static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.apprenticeMageRobeSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                    ArmorItem.Type.CHESTPLATE, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                    ArmorItem.Type.LEGGINGS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ApprenticeMageRobeItem) ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Apprentice Mage Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == (armorType == ArmorItem.Type.CHESTPLATE),
                        "Apprentice Mage Robe " + armorType + " imbue surface regression");
            }
        });
    }

    static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var lightningSpellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
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
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Enchantress Robe " + armorType + " spell power config regression: " + describeModifiers(modifiers));

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
    static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
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
            var expectedGlobalSpellPower = ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece();
            helper.assertTrue(Math.abs(globalSpellPowerBonus - expectedGlobalSpellPower) < 1.0e-9D,
                    "Enchantress Robe chestplate should keep configured spell power after imbue: " + describeModifiers(modifiers));

            var imbuedSchoolSpellPowerBonus = sumModifierAmount(
                    modifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(imbuedSchoolSpellPowerBonus - 0.05D) < 1.0e-9D,
                    "Enchantress Robe chestplate should add +0.05 imbued school spell power: " + describeModifiers(modifiers));
        });
    }
    static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.chromaticMagiaDressSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                    ArmorItem.Type.CHESTPLATE, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                    ArmorItem.Type.LEGGINGS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                    ArmorItem.Type.BOOTS, (ChromaticMagiaDressItem) ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                helper.assertTrue(item.getMaterial().getDefenseForType(armorType) == ArmorMaterials.IRON.getDefenseForType(armorType),
                        "Chromatic Magia Dress " + armorType + " defense should match iron");
                helper.assertTrue(Math.abs(item.getMaterial().getToughness() - 1.0F) < 1.0e-6F,
                        "Chromatic Magia Dress " + armorType + " toughness should be 1");
                helper.assertTrue(item.getEnchantmentValue(stack) == ChromaticMagiaDressStats.enchantmentValue(),
                        "Chromatic Magia Dress " + armorType + " enchantment value changed");
                helper.assertTrue(item.isValidRepairItem(
                                stack,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                        ),
                        "Chromatic Magia Dress " + armorType + " should repair with mithril scrap");

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 125.0D) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Chromatic Magia Dress " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Chromatic Magia Dress " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && (item.getDescriptionId() + ".desc").equals(contents.getKey())),
                        "Chromatic Magia Dress " + armorType + " should show its lang desc key");
            }
        });
    }
    static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var spellPowerAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();
            var expectedSpellPower = ApprenticeCodexServerConfig.stealthRuneArmorSpellPowerBonusPerPiece();
            var pieces = Map.of(
                    ArmorItem.Type.HELMET, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                    ArmorItem.Type.CHESTPLATE, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                    ArmorItem.Type.LEGGINGS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                    ArmorItem.Type.BOOTS, (StealthRuneArmorItem) ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
            );

            for (var entry : pieces.entrySet()) {
                var armorType = entry.getKey();
                var item = entry.getValue();
                var stack = new ItemStack(item);
                item.initializeSpellContainer(stack);

                var modifiers = item.getAttributeModifiers(armorType.getSlot(), stack);
                var maxManaBonus = sumModifierAmount(modifiers.get(maxManaAttribute), AttributeModifier.Operation.ADDITION);
                helper.assertTrue(Math.abs(maxManaBonus - 50.0D) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " max mana regression: " + describeModifiers(modifiers));

                var spellPowerBonus = sumModifierAmount(modifiers.get(spellPowerAttribute), AttributeModifier.Operation.MULTIPLY_BASE);
                helper.assertTrue(Math.abs(spellPowerBonus - expectedSpellPower) < 1.0e-9D,
                        "Stealth Rune Armor " + armorType + " spell power config regression: " + describeModifiers(modifiers));

                helper.assertTrue(ISpellContainer.isSpellContainer(stack) == item.hasImbueSlot(),
                        "Stealth Rune Armor " + armorType + " imbue surface regression");

                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltipLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && ("item." + ApprenticeCodex.MODID + ".stealth_rune_armor.desc").equals(contents.getKey())),
                        "Stealth Rune Armor " + armorType + " should show its lang desc key");
            }
        });
    }

    static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "chromatic_magia_dress_history_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Chromatic Magia Dress test could not resolve player mana data");

            var helmet = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get());
            var chestplate = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
            var leggings = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
            var boots = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            player.setItemSlot(EquipmentSlot.LEGS, leggings);
            player.setItemSlot(EquipmentSlot.FEET, boots);
            var schoolSpellPowerBonusPerHistory =
                    ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();

            var longSpell = SpellRegistry.COMPOUND_PHIAL.get();
            for (int i = 0; i < 21; ++i) {
                postSpellOnCast(player, longSpell, 1);
            }
            assertSchoolSpellPowerBonus(helper, helmet, EquipmentSlot.HEAD, longSpell,
                    20.0D * schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress helmet should keep only the latest 20 LONG histories");
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, longSpell, 0.0D,
                    "Chromatic Magia Dress chestplate should ignore non-recast LONG spells");

            var continuousSpell = SpellRegistry.FORCE_FIELD.get();
            postSpellOnCast(player, continuousSpell, 1);
            assertSchoolSpellPowerBonus(helper, leggings, EquipmentSlot.LEGS, continuousSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress leggings should record CONTINUOUS spells");

            var instantSpell = SpellRegistry.MANA_SLASH.get();
            postSpellOnCast(player, instantSpell, 1);
            assertSchoolSpellPowerBonus(helper, boots, EquipmentSlot.FEET, instantSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress boots should record INSTANT spells");

            var recastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should record initial recast-capable casts");

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    recastSpell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            postSpellOnCast(player, recastSpell, 1);
            assertSchoolSpellPowerBonus(helper, chestplate, EquipmentSlot.CHEST, recastSpell, schoolSpellPowerBonusPerHistory,
                    "Chromatic Magia Dress chestplate should ignore casts while the same spell is in Recast");
        });
    }
    static void pastelStaffKeepsItsLocalEnchantingRules(GameTestHelper helper) {
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
    static void circuitHeatStaffKeepsExpectedStatsAndEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var item = (CircuitHeatStaff) stack.getItem();
            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION,
                    3.0D,
                    "Circuit Heat Staff attack damage modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION,
                    -3.0D,
                    "Circuit Heat Staff attack speed modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.10D,
                    "Circuit Heat Staff spell power modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Circuit Heat Staff fire spell power modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Circuit Heat Staff lightning spell power modifier changed"
            );
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Circuit Heat Staff should not expose an imbue spell container");

            CircuitHeatStaff.startStaffOverheat(stack, helper.getLevel(), 20 * 45);
            var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(stack, helper.getLevel());
            helper.assertTrue(remainingOverheatTicks == 20 * 45,
                    "Circuit Heat Staff item overheat should keep the requested duration: "
                            + remainingOverheatTicks);

            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedCircuitHeatStaffEnchantments(stack),
                    "Circuit Heat Staff"
            );
        });
    }

    static void circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var baseManaCost = 100;
            var step = 1;

            var referenceAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 10);
            helper.assertTrue(referenceAdditionalMana == 20,
                    "Circuit Heat Staff skipped 10 seconds should keep the old step-1 extra mana: "
                            + referenceAdditionalMana);

            var shortAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 5);
            helper.assertTrue(shortAdditionalMana == 10,
                    "Circuit Heat Staff skipped 5 seconds should halve the step-1 extra mana: "
                            + shortAdditionalMana);

            var longAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 20 * 40);
            helper.assertTrue(longAdditionalMana == 80,
                    "Circuit Heat Staff skipped 40 seconds should quadruple the step-1 extra mana: "
                            + longAdditionalMana);

            var noSkippedCooldownAdditionalMana = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, step, 0);
            helper.assertTrue(noSkippedCooldownAdditionalMana == 0,
                    "Circuit Heat Staff should not add mana when no cooldown is skipped: "
                            + noSkippedCooldownAdditionalMana);
        });
    }

    static void circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_overheat_duration_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGIC_SPEAR.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff overheat duration test could not resolve player mana data");

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            magicData.setPlayerCastingItem(staffStack);

            var castCooldownTicks = 20 * 120;
            var skippedCooldownTicks = 20 * 40;
            var expectedOverheatTicks = castCooldownTicks + skippedCooldownTicks;
            var plannedManaCost = Math.max(1, spell.getManaCost(1));
            CircuitHeatStaffCastEvent.reserveOverheatCast(
                    player,
                    spell.getSpellId(),
                    plannedManaCost,
                    plannedManaCost,
                    expectedOverheatTicks
            );

            magicData.setMana(plannedManaCost);
            var event = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

            var remainingOverheatTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel());
            helper.assertTrue(remainingOverheatTicks == expectedOverheatTicks,
                    "Circuit Heat Staff item overheat should use cast cooldown plus skipped cooldown: "
                            + remainingOverheatTicks + " / expected " + expectedOverheatTicks);

            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
        });
    }

    static void circuitHeatStaffBypassKeepsBaseManaGate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_base_mana_gate_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff mana gate test could not resolve player mana data");
            var baseManaCost = spell.getManaCost(1);
            magicData.setMana(baseManaCost - 1.0F);

            var selection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == spell,
                    "Circuit Heat Staff mana gate test could not resolve the selected spell: " + selection);
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());

            var result = staffStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Circuit Heat Staff should fail forced casts when base mana is insufficient but got " + result.getResult());
            helper.assertTrue(Math.abs(magicData.getMana() - (baseManaCost - 1.0F)) < 1.0e-4F,
                    "Circuit Heat Staff base mana failure should not mutate mana: " + magicData.getMana());
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff should restore the original cooldown after base mana failure");
            helper.assertFalse(jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                            .getState(player, spell.getSpellId()).active(),
                    "Circuit Heat Staff should not store bypass overheat state after base mana failure");
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff item should not enter overheat cooldown after base mana failure");
        });
    }

    static void circuitHeatStaffContinuousBypassKeepsOverheatManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_continuous_mana_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff continuous mana test could not resolve player mana data");

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            magicData.getSyncedData();
            magicData.initiateCast(
                    spell,
                    1,
                    spell.getCastTime(1),
                    CastSource.SPELLBOOK,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            magicData.setPlayerCastingItem(staffStack);

            var baseManaCost = spell.getManaCost(1);
            var plannedManaCost = baseManaCost
                    + jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, 1, 20 * 10);
            CircuitHeatStaffCastEvent.reserveOverheatCast(
                    player,
                    spell.getSpellId(),
                    plannedManaCost,
                    plannedManaCost * 3.0F,
                    60,
                    true
            );

            magicData.setMana(plannedManaCost * 3.0F);
            var firstEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(firstEvent);
            helper.assertTrue(firstEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous first tick should use overheated mana cost: " + firstEvent.getManaCost());
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should not enter item overheat while overheated continuous mana can still be paid");

            magicData.setMana(plannedManaCost + 5.0F);
            var secondEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(secondEvent);
            helper.assertTrue(secondEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous later tick should keep overheated mana cost: " + secondEvent.getManaCost());
            helper.assertFalse(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should still avoid item overheat while continuous mana remains above the overheated cost");

            magicData.setMana(plannedManaCost);
            var depletionEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(depletionEvent);
            helper.assertTrue(depletionEvent.getManaCost() == plannedManaCost,
                    "Circuit Heat Staff continuous depletion tick should keep overheated mana cost: " + depletionEvent.getManaCost());
            helper.assertTrue(CircuitHeatStaff.isStaffOverheated(staffStack, helper.getLevel()),
                    "Circuit Heat Staff should enter item overheat when the overheated continuous cost depletes mana");
            helper.assertTrue(
                    CircuitHeatStaff.formatOverheatManaCostForDisplay(spell, plannedManaCost).equals(plannedManaCost * 2 + "/s"),
                    "Circuit Heat Staff continuous warning should display per-second mana"
            );

            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
            magicData.resetCastingState();
        });
    }

    static void circuitHeatStaffRecastDoesNotTouchBypassOverheatState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_recast_neutral_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff recast test could not resolve player mana data");
            magicData.setMana(0.0F);

            jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager.applyAfterBypass(
                    player,
                    spell.getSpellId(),
                    200
            );
            jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager.applyAfterBypass(
                    player,
                    spell.getSpellId(),
                    200
            );
            var stateBefore = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateBefore.active() && stateBefore.chainDepth() == 2,
                    "Circuit Heat Staff recast setup should start from bypass chain depth 2 but got " + stateBefore);

            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SPELLBOOK,
                    null
            ), magicData);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "Circuit Heat Staff recast setup should create an active recast");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff recast setup should not leave a normal cooldown");
            CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 100);
            var staffOverheatBefore = CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel());
            helper.assertTrue(staffOverheatBefore > 0,
                    "Circuit Heat Staff recast setup should start from item overheat cooldown");

            var result = staffStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.CONSUME,
                    "Circuit Heat Staff recast should start through the recast-neutral path during item overheat but got "
                            + result.getResult());
            var stateAfterUse = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateAfterUse.active()
                            && stateAfterUse.chainDepth() == stateBefore.chainDepth()
                            && stateAfterUse.expireGameTime() == stateBefore.expireGameTime(),
                    "Circuit Heat Staff recast use should not mutate bypass state: " + stateAfterUse
                            + " / before " + stateBefore);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Circuit Heat Staff recast use should not consume mana before cast resolution: " + magicData.getMana());

            spell.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Circuit Heat Staff recast resolution should keep Iron's no-mana recast behavior: " + magicData.getMana());
            helper.assertTrue(CircuitHeatStaff.getStaffOverheatRemainingTicks(staffStack, helper.getLevel()) == staffOverheatBefore,
                    "Circuit Heat Staff recast should ignore existing item overheat without clearing or refreshing it");
            var stateAfterCast = jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager
                    .getState(player, spell.getSpellId());
            helper.assertTrue(stateAfterCast.active()
                            && stateAfterCast.chainDepth() == stateBefore.chainDepth()
                            && stateAfterCast.expireGameTime() == stateBefore.expireGameTime(),
                    "Circuit Heat Staff recast resolution should not mutate bypass state: " + stateAfterCast
                            + " / before " + stateBefore);

            magicData.resetCastingState();
        });
    }

    static void circuitHeatStaffDropCoolingConsumesWaterSource(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER);

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnItem(helper, waterPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime while dropped: " + itemEntity.getAge());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff water-source cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(helper.getBlockState(waterPos).isAir(),
                    "Circuit Heat Staff water-source cooling should consume the source after three cycles");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresFlowingWater(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnItem(helper, waterPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks > 20 * 55,
                    "Circuit Heat Staff should not use flowing water for cooling: " + remainingTicks);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingConsumesCauldronLevel(GameTestHelper helper) {
        var cauldronPos = new BlockPos(0, 2, 0);
        helper.setBlock(
                cauldronPos,
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var state = helper.getBlockState(cauldronPos);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff cauldron cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 2,
                    "Circuit Heat Staff cauldron cooling should consume one water level after three cycles: " + state);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsPowderSnowBlock(GameTestHelper helper) {
        var powderSnowPos = new BlockPos(0, 2, 0);
        helper.setBlock(powderSnowPos, Blocks.POWDER_SNOW);

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, powderSnowPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff powder snow cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(helper.getBlockState(powderSnowPos).is(Blocks.POWDER_SNOW),
                    "Circuit Heat Staff powder snow cooling should not consume powder snow block");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(GameTestHelper helper) {
        var cauldronPos = new BlockPos(0, 2, 0);
        helper.setBlock(
                cauldronPos,
                Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
        );

        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        CircuitHeatStaff.startStaffOverheat(staffStack, helper.getLevel(), 20 * 60);
        var itemEntity = spawnNoGravityItem(helper, cauldronPos, staffStack);

        helper.runAtTickTime(40, () -> {
            var state = helper.getBlockState(cauldronPos);
            var remainingTicks = CircuitHeatStaff.getStaffOverheatRemainingTicks(itemEntity.getItem(), helper.getLevel());
            helper.assertTrue(remainingTicks <= 20 * 30,
                    "Circuit Heat Staff powder snow cauldron cooling should reduce at least 30 seconds after three cycles: "
                            + remainingTicks);
            helper.assertTrue(state.is(Blocks.POWDER_SNOW_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    "Circuit Heat Staff powder snow cauldron cooling should not consume cauldron level: " + state);
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(GameTestHelper helper) {
        var waterPos = new BlockPos(0, 2, 0);
        placeWaterTestBasin(helper, waterPos);
        helper.setBlock(waterPos, Blocks.WATER);

        var itemEntity = spawnItem(helper, waterPos, new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()));

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime even when it is not overheated: "
                            + itemEntity.getAge());
            helper.assertTrue(helper.getBlockState(waterPos).is(Blocks.WATER),
                    "Circuit Heat Staff should not consume water when it is not overheated");
            helper.succeed();
        });
    }

    static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(GameTestHelper helper) {
        var powderSnowPos = new BlockPos(0, 2, 0);
        helper.setBlock(powderSnowPos, Blocks.POWDER_SNOW);

        var itemEntity = spawnNoGravityItem(helper, powderSnowPos, new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get()));

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(itemEntity.getAge() == Short.MIN_VALUE,
                    "Circuit Heat Staff drop should use unlimited lifetime in powder snow even when it is not overheated: "
                            + itemEntity.getAge());
            helper.assertTrue(helper.getBlockState(powderSnowPos).is(Blocks.POWDER_SNOW),
                    "Circuit Heat Staff should not change powder snow when it is not overheated");
            helper.succeed();
        });
    }

    static void malumHauntedBonusResolvesFromSupportedMainhandWeapons(GameTestHelper helper) {
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

            var focusStaffbow = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
            focusStaffbow.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(focusStaffbow),
                    "Focus Staffbow should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(focusStaffbow) > 0.0D,
                    "Focus Staffbow should resolve a positive Haunted magic damage bonus");

            var chargedTwinBladeStaff = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            chargedTwinBladeStaff.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(chargedTwinBladeStaff),
                    "Charged Twin Blade Staff should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(chargedTwinBladeStaff) > 0.0D,
                    "Charged Twin Blade Staff should resolve a positive Haunted magic damage bonus");

            var manaForceBlade = new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get());
            manaForceBlade.enchant(haunted, 1);
            helper.assertTrue(MalumHauntedCompat.isSupportedHauntedMainhandItem(manaForceBlade),
                    "Mana Force Blade should be a supported Haunted main hand item");
            helper.assertTrue(MalumHauntedCompat.resolveHauntedMagicDamageBonus(manaForceBlade) > 0.0D,
                    "Mana Force Blade should resolve a positive Haunted magic damage bonus");

            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get())),
                    "Spellgun should stay outside Haunted support");
            helper.assertFalse(MalumHauntedCompat.isSupportedHauntedMainhandItem(new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get())),
                    "Reflectcast Shield should stay outside Haunted support");
        });
    }
    static void malumHauntedBonusUsesDedicatedDamageType(GameTestHelper helper) {
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
    static void magicDamageTagActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
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

            var spellData = Capabilities.getSpellDataOrNull(owner);
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
    static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_vanilla_menu_test");
        var shelfPos = new BlockPos(0, 1, 0);
        placeAndAssertBlockEntity(helper, shelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
        var absoluteShelfPos = helper.absolutePos(shelfPos);
        var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
        shelf.setShelfData(player, false, Direction.NORTH);
        shelf.setLifeData(20 * 60, 10.0);

        helper.runAtTickTime(1, () -> {
            var personalInventory = player.getCapability(Capabilities.PERSONAL_INVENTORY)
                    .orElseThrow(() -> new IllegalStateException("Missing personal inventory for Personal Shelf GameTest"));

            personalInventory.getHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            for (var slot = 1; slot < 54; ++slot) {
                personalInventory.getHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            player.getInventory().setItem(0, new ItemStack(Items.DIRT));

            helper.assertTrue(player.openMenu(shelf).isPresent(), "Personal Shelf should open a menu for its owner");
            helper.assertTrue(player.containerMenu instanceof ChestMenu chestMenu && chestMenu.getRowCount() == 6,
                    "Personal Shelf should expose a vanilla six-row chest menu");
            var chestMenu = (ChestMenu) player.containerMenu;
            helper.assertTrue(chestMenu.getSlot(0).getItem().is(Items.DIAMOND),
                    "Personal Shelf chest menu should read from the opener's personal inventory");

            var quickMoved = chestMenu.quickMoveStack(player, 81);
            helper.assertTrue(quickMoved.isEmpty(),
                    "Full Personal Shelf quick move should fail cleanly instead of looping");
            helper.assertTrue(player.getInventory().getItem(0).is(Items.DIRT),
                    "Failed Personal Shelf quick move should leave the player's stack in place");
            helper.succeed();
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

    static void companionTrunkClimbsOneBlockStepWhenFollowingOwner(GameTestHelper helper) {
        var trunkPos = new BlockPos(-2, 12, 0);
        var ownerPos = new BlockPos(2, 13, 0);
        prepareSummonedEntityIsolationArea(helper, trunkPos);
        for (var x = 0; x <= 2; ++x) {
            helper.setBlock(new BlockPos(x, 12, 0), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 13, 0), Blocks.AIR);
            helper.setBlock(new BlockPos(x, 14, 0), Blocks.AIR);
        }

        var owner = createCompanionTrunkPlayer(helper, ownerPos);
        var trunk = createCompanionTrunk(helper, owner, trunkPos);
        var absoluteOwnerPos = helper.absolutePos(ownerPos);

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.onGround(),
                    "Companion Trunk should land on the raised step after following its owner");
            helper.assertTrue(trunk.blockPosition().getY() >= absoluteOwnerPos.getY(),
                    "Companion Trunk should climb onto the one block step while following its owner");
        });
    }

    static void companionTrunkLandingDoesNotTrampleFarmland(GameTestHelper helper) {
        var farmlandPos = new BlockPos(0, 11, 0);
        var owner = createCompanionTrunkPlayer(helper, new BlockPos(0, 12, 1));
        helper.setBlock(farmlandPos, Blocks.FARMLAND);
        helper.setBlock(farmlandPos.above(), Blocks.AIR);
        helper.setBlock(farmlandPos.above(2), Blocks.AIR);
        helper.setBlock(farmlandPos.above(3), Blocks.AIR);
        var trunk = createCompanionTrunk(helper, owner, farmlandPos.above(4));

        helper.succeedWhen(() -> {
            helper.assertTrue(trunk.onGround(), "Companion Trunk should land on the farmland test block");
            helper.assertBlockPresent(Blocks.FARMLAND, farmlandPos);
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

    static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_magnet_owner_test");
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 4.0, 0.0);
        var item = new ItemEntity(level, owner.getX() + 2.0, owner.getY(), owner.getZ(), new ItemStack(Items.IRON_INGOT));
        level.addFreshEntity(owner);
        level.addFreshEntity(item);

        helper.runAtTickTime(1, () -> {
            familiar.tickOnServer(level);
            helper.assertFalse(item.isRemoved(), "AutoMagnet test item should remain as an entity before player pickup");
            helper.assertTrue(item.position().distanceToSqr(owner.position()) <= 0.001,
                    "AutoMagnet should collect items when no Solegnolia blocks it");
            helper.succeed();
        });
    }

    static void earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(GameTestHelper helper) {
        var centerPos = new BlockPos(2, 3, 2);
        var sourceWaterPos = centerPos;
        var flowingWaterPos = centerPos.east();
        var seagrassPos = centerPos.west();
        var waterloggedStairPos = centerPos.north();
        var lavaPos = centerPos.offset(2, 0, 2);
        var changedToLavaPos = centerPos.offset(2, 0, -2);

        for (var x = -1; x <= 5; x++) {
            for (var z = -1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, centerPos.getY() - 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, centerPos.getY(), z), Blocks.STONE);
            }
        }
        helper.setBlock(seagrassPos.below(), Blocks.DIRT);
        helper.setBlock(sourceWaterPos, Blocks.WATER);
        helper.setBlock(flowingWaterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        helper.setBlock(seagrassPos, Blocks.SEAGRASS);
        helper.setBlock(changedToLavaPos, Blocks.WATER);
        helper.setBlock(
                waterloggedStairPos,
                Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.WATERLOGGED, true)
        );
        helper.setBlock(lavaPos, Blocks.LAVA);

        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 4, 1), "earth_forge_water_replace_test");
        helper.runAtTickTime(1, () -> castEarthForge(helper, player, centerPos, Direction.UP, 3));
        helper.runAtTickTime(2, () -> helper.setBlock(changedToLavaPos, Blocks.LAVA));

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.DIRT, sourceWaterPos);
            helper.assertBlockPresent(Blocks.DIRT, flowingWaterPos);
            helper.assertBlockPresent(Blocks.DIRT, seagrassPos);
            helper.assertBlockPresent(Blocks.OAK_STAIRS, waterloggedStairPos);
            helper.assertBlockProperty(waterloggedStairPos, StairBlock.WATERLOGGED, true);
            helper.assertBlockPresent(Blocks.LAVA, lavaPos);
            helper.assertBlockPresent(Blocks.LAVA, changedToLavaPos);
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

    private static ItemEntity spawnItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var absolutePos = helper.absoluteVec(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D));
        var itemEntity = new ItemEntity(helper.getLevel(), absolutePos.x, absolutePos.y, absolutePos.z, stack);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
    }

    private static void placeWaterTestBasin(GameTestHelper helper, BlockPos waterPos) {
        helper.setBlock(waterPos.below(), Blocks.STONE);
        for (var direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(waterPos.relative(direction), Blocks.STONE);
        }
    }

    private static ItemEntity spawnNoGravityItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var absolutePos = helper.absoluteVec(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D));
        var itemEntity = new ItemEntity(helper.getLevel(), absolutePos.x, absolutePos.y, absolutePos.z, stack);
        itemEntity.setNoGravity(true);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
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

    private static net.minecraftforge.event.entity.living.LivingAttackEvent postLivingAttackEventForGameTest(
            ServerPlayer player,
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
        var event = new net.minecraftforge.event.entity.living.LivingAttackEvent(player, source, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    private static void equipRingCurio(FakePlayer player, ItemStack ringStack) {
        equipCurio(player, io.redspace.ironsspellbooks.compat.Curios.RING_SLOT, ringStack);
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
        return player.getCapability(Capabilities.SPELL_DATA)
                .map(data -> data.get(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE))
                .orElseThrow(() -> new IllegalStateException("Missing spell data for Mana Shield Charm GameTest"));
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

    private static boolean hasItemEntityWithin(ServerLevel level, Item item, Vec3 pos, double radius) {
        return !level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos, pos).inflate(radius),
                itemEntity -> !itemEntity.isRemoved() && itemEntity.getItem().is(item)
        ).isEmpty();
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

    private static void castEarthForge(GameTestHelper helper, FakePlayer player, BlockPos centerPos, Direction effectDirection, int radius) {
        var spell = (EarthForge) SpellRegistry.EARTH_FORGE.get();
        var absoluteCenterPos = helper.absolutePos(centerPos);
        var tag = new CompoundTag();
        tag.putInt("CenterX", absoluteCenterPos.getX());
        tag.putInt("CenterY", absoluteCenterPos.getY());
        tag.putInt("CenterZ", absoluteCenterPos.getZ());
        tag.putInt("EffectDirection", effectDirection.get3DDataValue());
        tag.putInt("Radius", radius);

        var castData = new EarthForge.EarthForgeCastData();
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
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
        // SenseEvil の到達距離検証では移動や落下で座標がぶれると誤検知しやすいため固定する。
        zombie.setNoAi(true);
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

    private static void prepareWideSearchIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    private static void prepareMiningSpellIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    private static void prepareSummonedEntityIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    private static void prepareHighIsolationPlatform(GameTestHelper helper, BlockPos centerPos) {
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
        castData.deserializeNBT(tag);
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

    private static Set<ResourceLocation> expectedChargedTwinBladeStaffEnchantments(ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                new ItemStack(Items.DIAMOND_SWORD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(collectAllowedEnchantments(
                new ItemStack(Items.TRIDENT),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.TRIDENT))
                        && !isDurabilityTargetEnchantment(enchantment)
        ));
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.WISDOM
        ));
        addExpectedMalumHauntedIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedManaForceBladeEnchantments(ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                new ItemStack(Items.DIAMOND_SWORD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.SURGE,
                EnchantmentRegistry.ATTUNEMENT,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.TRANSCENDENCE
        ));
        addExpectedMalumHauntedIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedCircuitHeatStaffEnchantments(ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                new ItemStack(Items.DIAMOND_SWORD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER
        ));
        addExpectedMalumHauntedIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static void assertChargedTwinBladeStaffThrownDamage(
            GameTestHelper helper,
            ItemStack stack,
            MobType mobType,
            double expectedDamage,
            String failureMessage
    ) {
        var actualDamage = ChargedTwinBladeStaff.resolveThrownDamage(stack, mobType);
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-9D,
                failureMessage + ": mobType=" + mobType + ", expected=" + expectedDamage + ", actual=" + actualDamage);
    }

    private static Set<ResourceLocation> expectedReflectcastShieldEnchantments(ItemStack stack) {
        var expectedEnchantments = collectAllowedEnchantments(
                new ItemStack(Items.SHIELD),
                enchantment -> enchantment.canApplyAtEnchantingTable(new ItemStack(Items.SHIELD))
        );
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedElementalBowEnchantments() {
        var bowStack = new ItemStack(Items.BOW);
        var expectedEnchantments = collectAllowedEnchantments(
                bowStack,
                enchantment -> Items.BOW.canApplyAtEnchantingTable(bowStack, enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER,
                EnchantmentRegistry.SYNTHESIS
        ));
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedElementalBowBookEnchantments() {
        var bowStack = new ItemStack(Items.BOW);
        var expectedEnchantments = collectAllowedEnchantments(
                bowStack,
                enchantment -> Items.BOW.isBookEnchantable(bowStack, createEnchantedBook(enchantment))
        );
        expectedEnchantments.addAll(registryIdSet(
                EnchantmentRegistry.TRANSCENDENCE,
                EnchantmentRegistry.WISDOM,
                EnchantmentRegistry.PLUNDER,
                EnchantmentRegistry.SYNTHESIS
        ));
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

    private static Set<ResourceLocation> expectedChromaticMagiaDressEnchantments(ItemStack stack) {
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

    private static float getEquippedAttributeTotal(Player player, Attribute attribute) {
        var total = 0.0F;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            total += (float) stack.getAttributeModifiers(slot).get(attribute).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();
        }
        return total;
    }

    private static void equipProtectionIvIronArmor(ServerPlayer player) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
            armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            player.setItemSlot(slot, armorStack);
        }
    }

    private static float findDamageForArmorReducedTarget(float armor, float toughness, float targetReducedDamage) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterAbsorb(high, armor, toughness) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterAbsorb(mid, armor, toughness);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static float findDamageForMagicReducedTarget(int protection, float targetReducedDamage) {
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
            int protection
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

    private static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = stack.getTag();
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
    private static ElementalBow.ModeSelectionView findElementalBowSelectionView(ServerPlayer player, ItemStack stack,
                                                                                String shotMode, @Nullable ResourceLocation selectionId) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals(selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    private static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        var tag = stack.getOrCreateTag();
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
    }

    private static void assertTranslatableKey(GameTestHelper helper, Component component, String expectedKey, String message) {
        var contents = component.getContents();
        helper.assertTrue(contents instanceof TranslatableContents,
                message + " (component was not translatable: " + component + ")");
        if (contents instanceof TranslatableContents translatableContents) {
            helper.assertTrue(expectedKey.equals(translatableContents.getKey()),
                    message + " (expected=" + expectedKey + ", actual=" + translatableContents.getKey() + ")");
        }
    }

    private static void assertTooltipKeyAt(
            GameTestHelper helper,
            ItemStack stack,
            int index,
            String expectedKey,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        helper.assertTrue(tooltipLines.size() > index,
                message + " (tooltip line count=" + tooltipLines.size() + ")");
        assertTranslatableKey(helper, tooltipLines.get(index), expectedKey, message);
    }

    private static void assertTooltipKeyUsesColor(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            ChatFormatting expectedColor,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var matchingLine = tooltipLines.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()))
                .findFirst();
        helper.assertTrue(matchingLine.isPresent(),
                message + " (missing tooltip key=" + expectedKey + ")");
        if (matchingLine.isPresent()) {
            var expectedTextColor = TextColor.fromLegacyFormat(expectedColor);
            helper.assertTrue(Objects.equals(expectedTextColor, matchingLine.get().getStyle().getColor()),
                    message + " (expected=" + expectedTextColor + ", actual="
                            + matchingLine.get().getStyle().getColor() + ")");
        }
    }

    private static void assertTooltipKeyAbsent(GameTestHelper helper, ItemStack stack, String key, String message) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
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

    private static void assertCastingMoveSpeedModifierAmount(
            GameTestHelper helper,
            net.minecraft.world.entity.ai.attributes.AttributeInstance attributeInstance,
            @org.jetbrains.annotations.Nullable UUID excludedModifierId,
            double expectedAmount,
            String message
    ) {
        var actualAmount = attributeInstance.getModifiers().stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .filter(modifier -> excludedModifierId == null || !excludedModifierId.equals(modifier.getId()))
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + attributeInstance.getModifiers());
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

    @Nullable
    private static net.minecraft.resources.ResourceKey<io.redspace.ironsspellbooks.item.armor.UpgradeOrbType> findUpgradeKeyForPowerAttribute(
            Attribute spellPowerAttribute
    ) {
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.FIRE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ICE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.LIGHTNING_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.HOLY_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ENDER_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ENDER_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.BLOOD_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.EVOCATION_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.EVOCATION_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, io.redspace.ironsspellbooks.api.registry.AttributeRegistry.NATURE_SPELL_POWER.get())) {
            return io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.NATURE_SPELL_POWER;
        }
        return null;
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

    private static void assertSingleModifierAmount(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifiers = modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .toList();
        helper.assertTrue(matchingModifiers.size() == 1,
                message + ": expected exactly one " + operation + " modifier but got " + matchingModifiers);
        var actualAmount = matchingModifiers.get(0).getAmount();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    private static void postSpellOnCast(ServerPlayer player, AbstractSpell spell, int spellLevel) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                spellLevel,
                spell.getManaCost(spellLevel),
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        ));
    }

    private static void assertSchoolSpellPowerBonus(
            GameTestHelper helper,
            ItemStack stack,
            EquipmentSlot slot,
            AbstractSpell spell,
            double expectedAmount,
            String message
    ) {
        var attribute = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(spell.getSchoolType());
        helper.assertTrue(attribute != null, "Could not resolve school spell power attribute for " + spell.getSpellId());
        var actualAmount = sumModifierAmount(
                stack.getAttributeModifiers(slot).get(attribute),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected=" + expectedAmount
                        + ", actual=" + actualAmount
                        + ", modifiers=" + describeModifiers(stack.getAttributeModifiers(slot)));
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

    private static void assertArcanumInAJarComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int storedParameterCount,
            int remainingOperationCount,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.ARCANUM_IN_A_JAR.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof ArcanumInAJarBlockEntity,
                "Arcanum in a Jar block entity was not created");

        var tag = new CompoundTag();
        tag.putInt("StoredParameterCount", storedParameterCount);
        tag.putInt("RemainingOperationCount", remainingOperationCount);
        blockEntity.load(tag);

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Arcanum in a Jar should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Arcanum in a Jar comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }

    private static void assertAtelierStationComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int storedFluidAmount,
            boolean insertInventoryFlask,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.ATELIER_STATION.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof AtelierStationBlockEntity,
                "Atelier Station block entity was not created");

        if (blockEntity instanceof AtelierStationBlockEntity atelierStation) {
            if (storedFluidAmount > 0) {
                var tag = new CompoundTag();
                var storedFluidList = new ListTag();
                var storedFluidTag = new CompoundTag();
                var storedPotion = PotionUtils.setPotion(new ItemStack(Items.POTION),
                        net.minecraft.world.item.alchemy.Potions.REGENERATION);
                storedFluidTag.put("Item", storedPotion.save(new CompoundTag()));
                storedFluidTag.putInt("Amount", storedFluidAmount);
                storedFluidList.add(storedFluidTag);
                tag.put("StoredFluids", storedFluidList);
                blockEntity.load(tag);
            }

            if (insertInventoryFlask) {
                var flask = createFilledSpellcastersFlask(
                        PotionUtils.setPotion(new ItemStack(Items.POTION),
                                net.minecraft.world.item.alchemy.Potions.REGENERATION),
                        1,
                        0
                );
                atelierStation.getFlaskInventory().setStackInSlot(0, flask);
            }
        }

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Atelier Station should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Atelier Station comparator output mismatch: expected " + expectedOutput + " but got " + output);
    }

    private static void assertSpellDispenserComparatorOutput(
            GameTestHelper helper,
            BlockPos pos,
            int currentMana,
            boolean insertInventoryScroll,
            int expectedOutput
    ) {
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity,
                "Spell Dispenser block entity was not created");

        if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
            spellDispenser.setCurrentMana(currentMana);
            if (insertInventoryScroll) {
                spellDispenser.getInventory().setStackInSlot(
                        SpellDispenserBlockEntity.SPELL_SLOT_INDEX,
                        createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())
                );
            }
        }

        var absolutePos = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.getBlock().hasAnalogOutputSignal(state),
                "Spell Dispenser should advertise comparator output");

        var output = state.getAnalogOutputSignal(helper.getLevel(), absolutePos);
        helper.assertTrue(output == expectedOutput,
                "Spell Dispenser comparator output mismatch: expected " + expectedOutput + " but got " + output);
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

    private static void assertArchivistsGrimoireInscribeHintTooltip(
            GameTestHelper helper,
            ItemStack stack,
            boolean expected,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var hasHint = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents translatableContents
                        && "item.apprenticecodex.special_spellbook.inscribe_hint".equals(translatableContents.getKey()));
        helper.assertTrue(hasHint == expected, message);
    }

    private static void assertScrollSpell(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            String message
    ) {
        helper.assertTrue(stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                message + " (stack is not a scroll: " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");

        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + " (scroll spell container is null)");
        assertSpellData(helper, spellContainer.getSpellAtIndex(0), expectedSpell, 1, message);
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
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }

    private static FluidStack createIronsManaPotionFluid(net.minecraft.world.item.alchemy.Potion potion, int amountMb) {
        var fluid = io.redspace.ironsspellbooks.fluids.PotionFluid.from(PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
        fluid.setAmount(amountMb);
        return fluid;
    }

    private static FluidStack createCreateManaPotionFluid(net.minecraft.world.item.alchemy.Potion potion, int amountMb) {
        var createPotion = ForgeRegistries.FLUIDS.getValue(ResourceLocation.fromNamespaceAndPath("create", "potion"));
        if (createPotion == null) {
            return FluidStack.EMPTY;
        }

        var fluid = new FluidStack(createPotion, amountMb);
        var tag = fluid.getOrCreateTag();
        tag.putString("Potion", BuiltInRegistries.POTION.getKey(potion).toString());
        tag.putString("Bottle", "REGULAR");
        return fluid;
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

    private static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithSingleInput(Player player, ItemStack stack) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        return menu;
    }

    private static SpellcasterWorkbenchMenu createSpellcasterWorkbenchMenuWithInputs(Player player, ItemStack first, ItemStack second) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        return menu;
    }

    private static void assertStackHasSpell(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + ": missing spell container");
        if (spellContainer == null) {
            return;
        }

        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            var spellData = spellContainer.getSpellAtIndex(index);
            if (spellData != SpellData.EMPTY
                    && spellData.getSpell() == expectedSpell
                    && spellData.getLevel() == expectedLevel) {
                return;
            }
        }

        helper.assertTrue(false, message + ": expected spell was not found");
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

    private static ItemStack roundTripItemStack(ItemStack stack) {
        return ItemStack.of(stack.save(new CompoundTag()));
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
        AutocastAmuletAutoCastEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent.onPlayerTick(
                new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player)
        );
    }

    private static void assertSpellData(
            GameTestHelper helper,
            SpellData spellData,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                message + " (spell data is empty)");
        helper.assertTrue(spellData.getSpell() == expectedSpell,
                message + " (spell mismatch: " + spellData.getSpell().getSpellResource() + ")");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
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

    private static Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>> createEmptyVillagerTrades() {
        var trades = new Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>>();
        for (var level = 1; level <= 5; level++) {
            trades.put(level, new ArrayList<>());
        }
        return trades;
    }

    private static List<MerchantOffer> createOffers(List<VillagerTrades.ItemListing> listings, long randomSeed) {
        var random = RandomSource.create(randomSeed);
        var offers = new ArrayList<MerchantOffer>();
        for (var listing : listings) {
            var offer = listing.getOffer(null, random);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return offers;
    }

    private static void assertContainsOffer(GameTestHelper helper, List<MerchantOffer> offers,
                                            ItemStack costA, ItemStack costB, ItemStack result,
                                            int maxUses, String message) {
        helper.assertTrue(offers.stream().anyMatch(offer -> offerMatches(offer, costA, costB, result, maxUses)), message);
    }

    private static boolean offerMatches(MerchantOffer offer, ItemStack costA, ItemStack costB, ItemStack result, int maxUses) {
        return stackMatches(offer.getBaseCostA(), costA)
                && stackMatches(offer.getCostB(), costB)
                && stackMatches(offer.getResult(), result)
                && offer.getMaxUses() == maxUses;
    }

    private static boolean stackMatches(ItemStack actual, ItemStack expected) {
        if (expected.isEmpty()) {
            return actual.isEmpty();
        }

        return actual.is(expected.getItem()) && actual.getCount() == expected.getCount();
    }

    private static boolean hasBaseCostItem(List<MerchantOffer> offers, Item item) {
        return offers.stream().anyMatch(offer -> offer.getBaseCostA().is(item));
    }

    private static int countBaseCostItems(List<MerchantOffer> offers, Item... items) {
        var count = 0;
        for (var offer : offers) {
            for (var item : items) {
                if (offer.getBaseCostA().is(item)) {
                    count++;
                    break;
                }
            }
        }
        return count;
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
