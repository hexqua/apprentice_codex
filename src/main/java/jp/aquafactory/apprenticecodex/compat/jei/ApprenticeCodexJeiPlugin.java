package jp.aquafactory.apprenticecodex.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.jei.AlchemistCauldronJeiRecipe;
import io.redspace.ironsspellbooks.jei.AlchemistCauldronRecipeCategory;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerRecipe;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerModifierRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
@JeiPlugin
public class ApprenticeCodexJeiPlugin implements IModPlugin {
    private static final String EN_US_RESOURCE_PATH = "assets/" + ApprenticeCodex.MODID + "/lang/en_us.json";
    private static final int MAX_INFO_LINES = 32;
    private static final List<Item> BREWING_CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
    // todo: この表示を頼りにはみ出ないlangを設定する.
    private static final List<Component> CALIBRATION_EFFECT_PLACEHOLDER = List.of(
            Component.literal("Effect placeholder line 1"),
            Component.literal("Effect placeholder line 2"),
            Component.literal("Effect placeholder line 3")
    );

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "jei_plugin");
    private static final Set<String> EN_US_TRANSLATION_KEYS = loadEnUsTranslationKeys();

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new GrindRunnerRecipeCategory(guiHelper, buildGrindRunnerCatalyst()),
                new EssenceSmokerRecipeCategory(guiHelper),
                new SpellcasterWorkbenchRecipeCategory(guiHelper),
                new AlchemyBrewerRecipeCategory(guiHelper),
                new SpellCalibrationAdjustmentRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(@NotNull IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(
                jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe.class,
                new SpellbookCarryoverSmithingJeiExtension()
        );
        registration.getSmithingCategory().addExtension(
                jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe.class,
                new AlchemistsFlaskSmithingJeiExtension()
        );
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registerAffinityPotionJeiRecipes(registration);
        registerCustomRecipes(registration);
        registration.addRecipes(
                ApprenticeCodexJeiRecipeTypes.SPELL_CALIBRATION_ADJUSTMENT,
                collectSpellCalibrationAdjustmentJeiRecipes()
        );
        Map<String, GroupedJeiInfo> groupedInfos = new LinkedHashMap<>();

        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null || !ApprenticeCodex.MODID.equals(itemId.getNamespace())) {
                continue;
            }
            if (!(item instanceof IJeiInfoItem jeiInfoItem)) {
                continue;
            }

            var keyPrefix = jeiInfoItem.getJeiInfoTranslationKeyPrefix();
            if (keyPrefix == null || keyPrefix.isBlank()) {
                ApprenticeCodex.LOGGER.warn("JEI info skipped: empty key prefix for {}.", itemId);
                continue;
            }

            var groupId = resolveGroupId(itemId, jeiInfoItem.getJeiInfoGroupId());
            var groupedInfo = groupedInfos.get(groupId);
            if (groupedInfo == null) {
                groupedInfo = new GroupedJeiInfo(keyPrefix);
                groupedInfos.put(groupId, groupedInfo);
            } else if (!groupedInfo.keyPrefix().equals(keyPrefix)) {
                ApprenticeCodex.LOGGER.warn(
                        "JEI info skipped: group {} has inconsistent key prefix ({} != {}) for {}.",
                        groupId,
                        groupedInfo.keyPrefix(),
                        keyPrefix,
                        itemId
                );
                continue;
            }

            groupedInfo.itemStacks().add(new ItemStack(item));
        }

        for (var entry : groupedInfos.entrySet()) {
            var groupId = entry.getKey();
            var groupedInfo = entry.getValue();
            var infoComponents = collectInfoComponents(groupedInfo.keyPrefix());
            if (infoComponents.isEmpty()) {
                ApprenticeCodex.LOGGER.warn(
                        "JEI info skipped: no translation key found for prefix {} (group {}).",
                        groupedInfo.keyPrefix(),
                        groupId
                );
                continue;
            }

            registration.addItemStackInfo(groupedInfo.itemStacks(), infoComponents.toArray(Component[]::new));
        }
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(buildGrindRunnerCatalyst(), ApprenticeCodexJeiRecipeTypes.GRIND_RUNNER);
        registration.addRecipeCatalyst(new ItemStack(ItemRegistry.ESSENCE_SMOKER.get()), ApprenticeCodexJeiRecipeTypes.ESSENCE_SMOKER);
        registration.addRecipeCatalyst(
                new ItemStack(ItemRegistry.SPELLCASTER_WORKBENCH.get()),
                ApprenticeCodexJeiRecipeTypes.SPELLCASTER_WORKBENCH
        );
        registration.addRecipeCatalyst(
                new ItemStack(ItemRegistry.ALCHEMY_BREWER.get()),
                ApprenticeCodexJeiRecipeTypes.ALCHEMY_BREWER
        );
        registration.addRecipeCatalyst(
                new ItemStack(ItemRegistry.SPELL_CALIBRATION_BENCH.get()),
                ApprenticeCodexJeiRecipeTypes.SPELL_CALIBRATION_ADJUSTMENT
        );
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        var hiddenStacks = collectHiddenAffinityPotionStacks();
        if (hiddenStacks.isEmpty()) {
            return;
        }

        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenStacks);
    }

    private static String resolveGroupId(ResourceLocation itemId, String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return itemId.toString();
        }
        return groupId;
    }

    private static List<Component> collectInfoComponents(String keyPrefix) {
        List<Component> components = new ArrayList<>();

        for (int line = 1; line <= MAX_INFO_LINES; line++) {
            var key = keyPrefix + line;
            if (!EN_US_TRANSLATION_KEYS.contains(key)) {
                break;
            }

            components.add(Component.translatable(key));
        }
        return components;
    }

    private static void registerCustomRecipes(IRecipeRegistration registration) {
        var recipeManager = getClientRecipeManager();
        if (recipeManager == null) {
            ApprenticeCodex.LOGGER.warn("JEI recipe registration skipped: client recipe manager is not available.");
            return;
        }

        registration.addRecipes(
                ApprenticeCodexJeiRecipeTypes.GRIND_RUNNER,
                recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get())
        );
        registration.addRecipes(
                ApprenticeCodexJeiRecipeTypes.ESSENCE_SMOKER,
                recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get())
        );
        registration.addRecipes(
                ApprenticeCodexJeiRecipeTypes.SPELLCASTER_WORKBENCH,
                collectSpellcasterWorkbenchJeiRecipes(recipeManager)
        );
        registration.addRecipes(
                ApprenticeCodexJeiRecipeTypes.ALCHEMY_BREWER,
                collectAlchemyBrewerJeiRecipes(recipeManager)
        );
        registration.addRecipes(
                RecipeTypes.SMITHING,
                recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMITHING).stream()
                        .filter(recipe -> recipe instanceof jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe
                                || recipe instanceof jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe)
                        .map(SmithingRecipe.class::cast)
                        .toList()
        );
    }

    private static List<AlchemyBrewerJeiRecipe> collectAlchemyBrewerJeiRecipes(RecipeManager recipeManager) {
        var baseRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .toList();
        var modifierRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .toList();
        var recipes = new ArrayList<AlchemyBrewerJeiRecipe>();

        for (var base : baseRecipes) {
            addAlchemyBrewerJeiRecipe(recipes, base.getId(), base, null);
            for (var modifier : modifierRecipes) {
                if (modifier.input().equals(base.result())) {
                    addAlchemyBrewerJeiRecipe(recipes, base.getId(), base, modifier);
                }
            }
        }
        return recipes;
    }

    private static List<SpellCalibrationAdjustmentJeiRecipe> collectSpellCalibrationAdjustmentJeiRecipes() {
        var recipes = new ArrayList<SpellCalibrationAdjustmentJeiRecipe>();
        BuiltInRegistries.ITEM.stream()
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .forEach(item -> {
                    if (!(item instanceof SpellCalibrationAdjustmentTarget target)) {
                        return;
                    }

                    var targetStack = item.getDefaultInstance().copyWithCount(1);
                    for (var rule : target.getCalibrationAdjustmentProfile(targetStack).rules()) {
                        var candidates = rule.collectDisplayCandidates();
                        var targetId = BuiltInRegistries.ITEM.getKey(item);
                        if (candidates.isEmpty()) {
                            ApprenticeCodex.LOGGER.warn(
                                    "Spell Calibration Bench JEI rule skipped because it has no display candidates: {}/{}.",
                                    targetId,
                                    rule.displayId()
                            );
                            continue;
                        }

                        recipes.add(new SpellCalibrationAdjustmentJeiRecipe(
                                ResourceLocation.fromNamespaceAndPath(
                                        ApprenticeCodex.MODID,
                                        String.join(
                                                "/",
                                                "jei",
                                                "spell_calibration_bench",
                                                targetId.getNamespace(),
                                                targetId.getPath(),
                                                rule.displayId()
                                        )
                                ),
                                targetStack,
                                candidates,
                                CALIBRATION_EFFECT_PLACEHOLDER,
                                rule.constraintDisplay()
                        ));
                    }
                });
        return recipes;
    }

    private static void addAlchemyBrewerJeiRecipe(
            List<AlchemyBrewerJeiRecipe> recipes,
            ResourceLocation baseRecipeId,
            AlchemyBrewerRecipe base,
            @Nullable AlchemyBrewerModifierRecipe modifier
    ) {
        var resultId = modifier == null ? base.result() : modifier.result();
        if (!ForgeRegistries.POTIONS.containsKey(resultId)) {
            ApprenticeCodex.LOGGER.warn("Alchemy Brewer JEI recipe skipped: potion {} is not registered.", resultId);
            return;
        }
        var potion = ForgeRegistries.POTIONS.getValue(resultId);
        if (potion == null) {
            return;
        }

        var modifierId = modifier == null ? null : modifier.getId();
        recipes.add(new AlchemyBrewerJeiRecipe(
                createAlchemyBrewerJeiRecipeId(baseRecipeId, modifierId),
                base.base(),
                base.ingredient(),
                modifier == null ? null : modifier.ingredient(),
                PotionContentsHelper.createPotionStack(Items.POTION, potion),
                base.fluidAmountMb(),
                base.processingTimeTicks()
        ));
    }

    private static ResourceLocation createAlchemyBrewerJeiRecipeId(
            ResourceLocation baseRecipeId,
            @Nullable ResourceLocation modifierRecipeId
    ) {
        var path = "jei/alchemy_brewer/"
                + baseRecipeId.getNamespace() + "/" + baseRecipeId.getPath()
                + (modifierRecipeId == null
                ? "/without_additive"
                : "/with_additive/" + modifierRecipeId.getNamespace() + "/" + modifierRecipeId.getPath());
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    private static List<SpellcasterWorkbenchRecipe> collectSpellcasterWorkbenchJeiRecipes(RecipeManager recipeManager) {
        var recipes = new ArrayList<>(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()));
        if (ApprenticeCodexServerConfig.archivistsGrimoireInitialRows()
                < ApprenticeCodexServerConfig.archivistsGrimoireEffectiveMaxRows()) {
            recipes.add(createArchivistsGrimoireUpgradeJeiRecipe());
        }
        recipes.add(createSpellExtractionJeiRecipe());
        recipes.addAll(createSpellThrowableCardJeiRecipes());
        return recipes;
    }

    private static SpellcasterWorkbenchRecipe createArchivistsGrimoireUpgradeJeiRecipe() {
        return new SpellcasterWorkbenchRecipe(
                SpellcasterWorkbenchRecipeCategory.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_RECIPE_ID,
                List.of(
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(ItemRegistry.ARCHIVISTS_GRIMOIRE.get()), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS), 1)
                ),
                List.of(new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get())),
                -10
        );
    }

    private static SpellcasterWorkbenchRecipe createSpellExtractionJeiRecipe() {
        var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var imbuedSword = new ItemStack(Items.IRON_SWORD);
        var swordSpells = ISpellContainer.create(1, true, false).mutableCopy();
        swordSpells.addSpellAtIndex(magicMissile, 1, 0, true);
        ISpellContainer.set(imbuedSword, swordSpells.toImmutable());

        var extractedScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(magicMissile, 1, extractedScroll);

        // 実処理は2入力と空スロットを要求するため、JEI専用レシピの3枠目も空Ingredientとして表示する。
        return new SpellcasterWorkbenchRecipe(
                SpellcasterWorkbenchRecipeCategory.SPELL_EXTRACTION_RECIPE_ID,
                List.of(
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(imbuedSword), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(ItemRegistry.SPELL_EXTRACT_SHARD.get()), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.EMPTY, 1)
                ),
                List.of(extractedScroll),
                -20
        );
    }

    private static List<SpellcasterWorkbenchRecipe> createSpellThrowableCardJeiRecipes() {
        var invokeCount = ApprenticeCodexServerConfig.spellInvokeCardCraftCount();
        var autonomyCount = ApprenticeCodexServerConfig.spellAutonomyCardCraftCount();
        return List.of(
                createSpellThrowableCardJeiRecipe(
                        SpellcasterWorkbenchRecipeCategory.SPELL_INVOKE_CARD_RECIPE_ID,
                        Ingredient.of(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                        invokeCount,
                        Ingredient.of(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), invokeCount)
                ),
                createSpellThrowableCardJeiRecipe(
                        SpellcasterWorkbenchRecipeCategory.SPELL_INVOKE_CARD_REWRITE_RECIPE_ID,
                        Ingredient.of(ItemRegistry.SPELL_INVOKE_CARD.get()),
                        invokeCount,
                        Ingredient.of(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), invokeCount)
                ),
                createSpellThrowableCardJeiRecipe(
                        SpellcasterWorkbenchRecipeCategory.SPELL_AUTONOMY_CARD_RECIPE_ID,
                        Ingredient.of(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                        autonomyCount,
                        Ingredient.of(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), autonomyCount)
                ),
                createSpellThrowableCardJeiRecipe(
                        SpellcasterWorkbenchRecipeCategory.SPELL_AUTONOMY_CARD_REWRITE_RECIPE_ID,
                        Ingredient.of(ItemRegistry.SPELL_AUTONOMY_CARD.get()),
                        autonomyCount,
                        Ingredient.of(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), autonomyCount)
                )
        );
    }

    private static SpellcasterWorkbenchRecipe createSpellThrowableCardJeiRecipe(
            ResourceLocation recipeId,
            Ingredient baseIngredient,
            int baseCount,
            Ingredient catalystIngredient,
            ItemStack result
    ) {
        return new SpellcasterWorkbenchRecipe(
                recipeId,
                List.of(
                        new SpellcasterWorkbenchRecipe.SizedIngredient(baseIngredient, baseCount),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(catalystIngredient, 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(
                                Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                                1
                        )
                ),
                List.of(result),
                -20
        );
    }

    private static void registerAffinityPotionJeiRecipes(IRecipeRegistration registration) {
        var brewingRecipes = createAffinityBrewingRecipes(registration);
        if (!brewingRecipes.isEmpty()) {
            registration.addRecipes(RecipeTypes.BREWING, brewingRecipes);
        }

        var cauldronRecipes = createAffinityCauldronRecipes();
        if (!cauldronRecipes.isEmpty()) {
            registration.addRecipes(AlchemistCauldronRecipeCategory.ALCHEMIST_CAULDRON_RECIPE_TYPE, cauldronRecipes);
        }
    }

    private static List<IJeiBrewingRecipe> createAffinityBrewingRecipes(IRecipeRegistration registration) {
        var vanillaRecipeFactory = registration.getVanillaRecipeFactory();
        var recipes = new ArrayList<IJeiBrewingRecipe>();

        for (var entry : SchoolAffinityRegistry.getBrewingDefinitionsByCatalyst().entrySet()) {
            var catalyst = entry.getKey();
            var definition = entry.getValue();

            for (var container : BREWING_CONTAINERS) {
                addBrewingRecipe(
                        recipes,
                        vanillaRecipeFactory,
                        catalyst,
                        createPotionStack(container, PotionRegistry.INTELLIGENCE.get()),
                        createPotionStack(container, definition.basePotion()),
                        "affinity_from_intelligence",
                        definition.basePotionId(),
                        ForgeRegistries.ITEMS.getKey(catalyst),
                        ForgeRegistries.ITEMS.getKey(container)
                );
                addBrewingRecipe(
                        recipes,
                        vanillaRecipeFactory,
                        catalyst,
                        createPotionStack(container, PotionRegistry.LONG_INTELLIGENCE.get()),
                        createPotionStack(container, definition.longPotion()),
                        "affinity_from_long_intelligence",
                        definition.longPotionId(),
                        ForgeRegistries.ITEMS.getKey(catalyst),
                        ForgeRegistries.ITEMS.getKey(container)
                );
                addBrewingRecipe(
                        recipes,
                        vanillaRecipeFactory,
                        catalyst,
                        createPotionStack(container, PotionRegistry.STRONG_INTELLIGENCE.get()),
                        createPotionStack(container, definition.strongPotion()),
                        "affinity_from_strong_intelligence",
                        definition.strongPotionId(),
                        ForgeRegistries.ITEMS.getKey(catalyst),
                        ForgeRegistries.ITEMS.getKey(container)
                );
                addBrewingRecipe(
                        recipes,
                        vanillaRecipeFactory,
                        Items.REDSTONE,
                        createPotionStack(container, definition.basePotion()),
                        createPotionStack(container, definition.longPotion()),
                        "affinity_extend",
                        definition.longPotionId(),
                        ForgeRegistries.ITEMS.getKey(Items.REDSTONE),
                        ForgeRegistries.ITEMS.getKey(container)
                );
                addBrewingRecipe(
                        recipes,
                        vanillaRecipeFactory,
                        Items.GLOWSTONE_DUST,
                        createPotionStack(container, definition.basePotion()),
                        createPotionStack(container, definition.strongPotion()),
                        "affinity_amplify",
                        definition.strongPotionId(),
                        ForgeRegistries.ITEMS.getKey(Items.GLOWSTONE_DUST),
                        ForgeRegistries.ITEMS.getKey(container)
                );
            }
        }

        return recipes;
    }

    private static List<AlchemistCauldronJeiRecipe> createAffinityCauldronRecipes() {
        if (!ServerConfigs.ALLOW_CAULDRON_BREWING.get()) {
            return List.of();
        }

        var recipes = new ArrayList<AlchemistCauldronJeiRecipe>();
        for (var entry : SchoolAffinityRegistry.getBrewingDefinitionsByCatalyst().entrySet()) {
            var catalyst = entry.getKey();
            var definition = entry.getValue();

            for (var container : BREWING_CONTAINERS) {
                addCauldronRecipe(
                        recipes,
                        catalyst,
                        createPotionStack(container, PotionRegistry.INTELLIGENCE.get()),
                        createPotionStack(container, definition.basePotion())
                );
                addCauldronRecipe(
                        recipes,
                        catalyst,
                        createPotionStack(container, PotionRegistry.LONG_INTELLIGENCE.get()),
                        createPotionStack(container, definition.longPotion())
                );
                addCauldronRecipe(
                        recipes,
                        catalyst,
                        createPotionStack(container, PotionRegistry.STRONG_INTELLIGENCE.get()),
                        createPotionStack(container, definition.strongPotion())
                );
                addCauldronRecipe(
                        recipes,
                        Items.REDSTONE,
                        createPotionStack(container, definition.basePotion()),
                        createPotionStack(container, definition.longPotion())
                );
                addCauldronRecipe(
                        recipes,
                        Items.GLOWSTONE_DUST,
                        createPotionStack(container, definition.basePotion()),
                        createPotionStack(container, definition.strongPotion())
                );
            }
        }
        return recipes;
    }

    private static void addBrewingRecipe(
            List<IJeiBrewingRecipe> recipes,
            mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory vanillaRecipeFactory,
            Item catalyst,
            ItemStack input,
            ItemStack output,
            String transitionKey,
            ResourceLocation outputPotionId,
            ResourceLocation catalystId,
            ResourceLocation containerId
    ) {
        recipes.add(vanillaRecipeFactory.createBrewingRecipe(
                List.of(new ItemStack(catalyst)),
                input,
                output,
                createAffinityPotionRecipeId(transitionKey, outputPotionId, catalystId, containerId)
        ));
    }

    private static void addCauldronRecipe(
            List<AlchemistCauldronJeiRecipe> recipes,
            Item catalyst,
            ItemStack input,
            ItemStack output
    ) {
        recipes.add(new AlchemistCauldronJeiRecipe(
                Ingredient.of(catalyst),
                PotionFluid.from(input),
                List.of(PotionFluid.from(output)),
                ItemStack.EMPTY
        ));
    }

    private static ResourceLocation createAffinityPotionRecipeId(
            String transitionKey,
            ResourceLocation outputPotionId,
            ResourceLocation catalystId,
            ResourceLocation containerId
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                String.join(
                        "/",
                        "jei",
                        "potion",
                        transitionKey,
                        toUidSegment(outputPotionId),
                        toUidSegment(catalystId),
                        toUidSegment(containerId)
                )
        );
    }

    private static String toUidSegment(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath().replace('/', '_');
    }

    private static RecipeManager getClientRecipeManager() {
        var minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        return connection == null ? null : connection.getRecipeManager();
    }

    private static List<ItemStack> collectHiddenAffinityPotionStacks() {
        var hiddenStacks = new ArrayList<ItemStack>();

        for (var definition : SchoolAffinityRegistry.getDefinitions()) {
            if (SchoolAffinityRegistry.getAssignedSchool(definition.slotIndex()).isPresent()) {
                continue;
            }

            hiddenStacks.addAll(createPotionStacks(definition.basePotion()));
            hiddenStacks.addAll(createPotionStacks(definition.longPotion()));
            hiddenStacks.addAll(createPotionStacks(definition.strongPotion()));
        }

        return hiddenStacks;
    }

    private static List<ItemStack> createPotionStacks(Potion potion) {
        var stacks = new ArrayList<ItemStack>(4);
        stacks.add(createPotionStack(Items.POTION, potion));
        stacks.add(createPotionStack(Items.SPLASH_POTION, potion));
        stacks.add(createPotionStack(Items.LINGERING_POTION, potion));
        stacks.add(createPotionStack(Items.TIPPED_ARROW, potion));
        return stacks;
    }

    private static ItemStack createPotionStack(Item item, Potion potion) {
        var stack = new ItemStack(item);
        PotionUtils.setPotion(stack, potion);
        return stack;
    }

    private static ItemStack buildGrindRunnerCatalyst() {
        var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.GRIND_RUNNER.get(), SpellRegistry.GRIND_RUNNER.get().getMinLevel(), scrollStack);
        return scrollStack;
    }

    private static Set<String> loadEnUsTranslationKeys() {
        try (var stream = ApprenticeCodexJeiPlugin.class.getClassLoader().getResourceAsStream(EN_US_RESOURCE_PATH)) {
            if (stream == null) {
                ApprenticeCodex.LOGGER.warn("JEI info disabled: {} was not found.", EN_US_RESOURCE_PATH);
                return Collections.emptySet();
            }

            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return new HashSet<>(json.keySet());
            }
        } catch (Exception e) {
            ApprenticeCodex.LOGGER.warn("JEI info disabled: failed to read {}.", EN_US_RESOURCE_PATH, e);
            return Collections.emptySet();
        }
    }

    private record GroupedJeiInfo(
            String keyPrefix,
            List<ItemStack> itemStacks
    ) {
        private GroupedJeiInfo(String keyPrefix) {
            this(keyPrefix, new ArrayList<>());
        }
    }
}
