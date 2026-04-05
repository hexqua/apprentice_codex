package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.armor.StealthRuneArmorItem;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertBuiltinRegistryEntries(helper, "item", BuiltInRegistries.ITEM, ItemRegistry.ITEMS.getEntries());
            assertBuiltinRegistryEntries(helper, "block", BuiltInRegistries.BLOCK, BlockRegistry.BLOCKS.getEntries());
            assertBuiltinRegistryEntries(helper, "block entity", BuiltInRegistries.BLOCK_ENTITY_TYPE, BlockEntityRegistry.BLOCK_ENTITY_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "entity", BuiltInRegistries.ENTITY_TYPE, EntityRegistry.ENTITIES.getEntries());
            assertBuiltinRegistryEntries(helper, "mob effect", BuiltInRegistries.MOB_EFFECT, EffectRegistry.EFFECTS.getEntries());
            assertBuiltinRegistryEntries(helper, "attribute", BuiltInRegistries.ATTRIBUTE, ApprenticeAttributeRegistry.ATTRIBUTES.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe serializer", BuiltInRegistries.RECIPE_SERIALIZER, RecipeRegistry.RECIPE_SERIALIZERS.getEntries());
            assertBuiltinRegistryEntries(helper, "potion", BuiltInRegistries.POTION, PotionRegistry.POTIONS.getEntries());
            assertBuiltinRegistryEntries(helper, "recipe type", BuiltInRegistries.RECIPE_TYPE, RecipeRegistry.RECIPE_TYPES.getEntries());
            assertBuiltinRegistryEntries(helper, "creative tab", BuiltInRegistries.CREATIVE_MODE_TAB, CreativeTabRegistry.TABS.getEntries());
            assertEnchantmentsRegistered(helper);

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
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellstained_runic_tablet"),
                    RecipeRegistry.SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_cane_lodestone_bind"),
                    RecipeRegistry.EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER.get(), null);

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker/infuse_coal_to_arcane_cinder"),
                    RecipeRegistry.ESSENCE_SMOKER_SERIALIZER.get(), RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "grind_runner/bone_meal_from_bone"),
                    RecipeRegistry.GRIND_RUNNER_SERIALIZER.get(), RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/basic_spellcaster_round"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());

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
            placeAndAssertBlockEntity(helper, new BlockPos(2, 1, 0), BlockRegistry.ARCANUM_IN_A_JAR.get(), BlockEntityRegistry.ARCANUM_IN_A_JAR.get());
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

    @GameTest(template = TEMPLATE)
    public static void swingMagicWeaponsUseBaseAttackModifierIds(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
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
                    "Crystal Bladed Staff should remain upgradeable after the 1.21.1 StaffItem migration");
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
    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedEnchantments = expectedSpellGunEnchantments();
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractSpellGunItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Spell Gun");

            for (var stack : stacks) {
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

    @GameTest(template = TEMPLATE)
    public static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractOffhandMagicItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Offhand Magic Item");

            for (var stack : stacks) {
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

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertCategoryEnchantments(
                helper,
                "Right Click Magic Weapon",
                // 1.21.1 では Crystal Bladed Staff が StaffItem 化され、このカテゴリから外れている。
                item -> item instanceof AbstractRightClickMagicWeaponItem,
                stack -> expectedRightClickMagicWeaponEnchantments(helper.getLevel().registryAccess())
        ));
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

    @GameTest(template = TEMPLATE)
    public static void pastelStaffKeepsItsExtraMiningEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var stack = new ItemStack(ItemRegistry.PASTEL_STAFF.get());

            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),
                    true, true, true, true, null, "Pastel Staff fortune rule");
            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),
                    true, true, true, true, null, "Pastel Staff silk touch rule");
            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(Enchantments.TRANSCENDENCE),
                    false, false, false, false, null, "Pastel Staff should keep rejecting transcendence");
            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(Enchantments.WISDOM),
                    false, false, false, false, null, "Pastel Staff should keep rejecting wisdom");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var stack = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            var item = (CrystalBladedStaff) stack.getItem();

            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),
                    false, false, false, false, false, "Crystal Bladed Staff should keep rejecting fortune");
            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),
                    false, false, false, false, false, "Crystal Bladed Staff should keep rejecting silk touch");
            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(Enchantments.TRANSCENDENCE),
                    true, true, true, true, true, "Crystal Bladed Staff transcendence rule");
            assertSingleEnchantmentSurfaces(helper, stack, enchantmentLookup.getOrThrow(Enchantments.WISDOM),
                    true, true, true, true, true, "Crystal Bladed Staff wisdom rule");

            helper.assertTrue(item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Crystal Bladed Staff should keep accepting diamonds as its repair material");
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

    @GameTest(template = TEMPLATE)
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
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

    @GameTest(template = TEMPLATE)
    public static void mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(GameTestHelper helper) {
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
            var movementSpeedModifiers = new java.util.ArrayList<AttributeModifier>();
            effect.createModifiers(0, (attribute, modifier) -> {
                if (attribute.equals(Attributes.MOVEMENT_SPEED)) {
                    movementSpeedModifiers.add(modifier);
                }
            });

            helper.assertTrue(!movementSpeedModifiers.isEmpty(), "LongStride is missing the movement speed attribute modifier");

            var actualAmount = movementSpeedModifiers.stream()
                    .filter(modifier -> modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .mapToDouble(AttributeModifier::amount)
                    .sum();
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

    private static Set<ResourceLocation> expectedSpellGunEnchantments() {
        return registryIdSet(
                Enchantments.REFLUX,
                Enchantments.RESERVOIR,
                Enchantments.SURGE,
                Enchantments.ATTUNEMENT,
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM,
                Enchantments.PLUNDER
        );
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

    private static Set<ResourceLocation> expectedRightClickMagicWeaponEnchantments(RegistryAccess registryAccess) {
        var expectedEnchantments = collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
                        && !isDurabilityTargetEnchantment(enchantment)
        );
        expectedEnchantments.addAll(registryIdSet(
                Enchantments.TRANSCENDENCE,
                Enchantments.WISDOM
        ));
        return expectedEnchantments;
    }

    private static Set<ResourceLocation> expectedFlaskEnchantments() {
        return registryIdSet(
                Enchantments.GUZZLE,
                Enchantments.LARGE_MUG,
                Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY
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
                Enchantments.GLOW_ENERGY
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
