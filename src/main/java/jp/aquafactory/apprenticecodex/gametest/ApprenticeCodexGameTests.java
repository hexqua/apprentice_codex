package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.CastingMoveSpeedAdjustment;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.LinkedHashMap;
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

    private static boolean isApprenticeSpell(AbstractSpell spell) {
        var spellId = spell.getSpellResource();
        return spellId != null && ApprenticeCodex.MODID.equals(spellId.getNamespace());
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
