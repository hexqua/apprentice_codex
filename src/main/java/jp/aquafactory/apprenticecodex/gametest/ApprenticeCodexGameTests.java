package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
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
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final String LODESTONE_MOD_ID = "lodestone";
    private static final String MALUM_MOD_ID = "malum";
    private static final ResourceLocation LODESTONE_MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath(LODESTONE_MOD_ID, "magic_proficiency");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );
    private static final ResourceLocation MALUM_HAUNTED = MalumHauntedCompat.hauntedEnchantmentId();
    private static final ResourceLocation MALUM_ANIMATED = MalumHauntedCompat.animatedEnchantmentId();
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");

    private ApprenticeCodexGameTests() {
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

            assertBuiltinRegistryEntries(helper, "potion", BuiltInRegistries.POTION, PotionRegistry.POTIONS.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe type", BuiltInRegistries.RECIPE_TYPE, RecipeRegistry.RECIPE_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "creative tab", BuiltInRegistries.CREATIVE_MODE_TAB, CreativeTabRegistry.TABS.getEntries());

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
    public static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 0), BlockRegistry.MAGE_LIGHT_TORCH.get(), BlockEntityRegistry.MAGE_LIGHT_TORCH.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 0), BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 0), BlockRegistry.RIFT_HOLE.get(), BlockEntityRegistry.RIFT_HOLE.get());
            placeAndAssertBlockEntity(helper, new BlockPos(3, 1, 0), BlockRegistry.ARCANUM_IN_A_JAR.get(), BlockEntityRegistry.ARCANUM_IN_A_JAR.get());
            placeAndAssertBlockEntity(helper, new BlockPos(0, 1, 1), BlockRegistry.ESSENCE_SMOKER.get(), BlockEntityRegistry.ESSENCE_SMOKER.get());
            placeAndAssertBlockEntity(helper, new BlockPos(1, 1, 1), BlockRegistry.ATELIER_STATION.get(), BlockEntityRegistry.ATELIER_STATION.get());

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
                    .filter(ApprenticeCodexGameTests::isApprenticeSpell)
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
            helper.assertTrue(spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(),
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
                ApprenticeCodexGameTests::expectedSpellGunEnchantments
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
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Right Click Magic Weapon",
                // 1.21.1申し送り事項:
                // 1.20.1 では StaffItem にしていない武器でも、1.21.1 側では StaffItem 化する場合がある。
                // ここは 1.20.1 の AbstractRightClickMagicWeaponItem 系の付与面を固定し、
                // port 時に StaffItem へ寄せた結果の差分を意図的に見えるようにしておく。
                item -> item instanceof AbstractRightClickMagicWeaponItem,
                ApprenticeCodexGameTests::expectedRightClickMagicWeaponEnchantments
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
                item -> item instanceof SpellcastersFlask,
                expectedFlaskEnchantments()
        ));
    }

    @GameTest(template = TEMPLATE)
    public static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertCategoryEnchantments(
                    helper,
                    "Enchantress Robe",
                    item -> item instanceof EnchantressRobeItem,
                    ApprenticeCodexGameTests::expectedEnchantressRobeEnchantments
            );
            assertCategoryEnchantments(
                    helper,
                    "Stealth Rune Armor",
                    item -> item instanceof StealthRuneArmorItem,
                    ApprenticeCodexGameTests::expectedStealthRuneArmorEnchantments
            );
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
