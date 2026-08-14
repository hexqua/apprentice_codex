package jp.aquafactory.apprenticecodex.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerRecipe;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerModifierRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityPotionBrewing;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
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

        for (var item : BuiltInRegistries.ITEM) {
            var itemId = BuiltInRegistries.ITEM.getKey(item);
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
                recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).stream()
                        .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                        .toList()
        );
        registration.addRecipes(
                ApprenticeCodexJeiRecipeTypes.ESSENCE_SMOKER,
                recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).stream()
                        .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                        .toList()
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
                        .filter(recipe -> recipe.value() instanceof jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe
                                || recipe.value() instanceof jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe)
                        .toList()
        );
    }

    private static List<AlchemyBrewerJeiRecipe> collectAlchemyBrewerJeiRecipes(RecipeManager recipeManager) {
        var baseRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
        var modifierRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
        var recipes = new ArrayList<AlchemyBrewerJeiRecipe>();

        for (var baseHolder : baseRecipes) {
            var base = baseHolder.value();
            addAlchemyBrewerJeiRecipe(recipes, baseHolder.id(), base, null);
            for (var modifierHolder : modifierRecipes) {
                var modifier = modifierHolder.value();
                if (modifier.input().equals(base.result())) {
                    addAlchemyBrewerJeiRecipe(recipes, baseHolder.id(), base, modifierHolder);
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
                        var clientLevel = Minecraft.getInstance().level;
                        var candidates = clientLevel == null
                                ? rule.collectDisplayCandidates()
                                : rule.collectDisplayCandidates(targetStack, clientLevel.registryAccess());
                        var targetId = BuiltInRegistries.ITEM.getKey(item);
                        if (candidates.isEmpty()) {
                            ApprenticeCodex.LOGGER.warn(
                                    "Spell Calibration Bench JEI rule skipped because it has no display candidates: {}/{}.",
                                    targetId,
                                    rule.displayId()
                            );
                            continue;
                        }

                        var displayCandidates = new ArrayList<ItemStack>();
                        var displayResults = new ArrayList<ItemStack>();
                        for (var candidate : candidates) {
                            var result = targetStack.copy();
                            boolean applied;
                            try {
                                // component 付き候補は生成元と同じ動的レジストリで保存しないと復元不能になる。
                                applied = clientLevel == null
                                        ? target.trySetCalibrationAdjustment(result, 0, candidate)
                                        : target.trySetCalibrationAdjustment(
                                                result,
                                                0,
                                                candidate,
                                                clientLevel.registryAccess()
                                        );
                            } catch (RuntimeException exception) {
                                ApprenticeCodex.LOGGER.warn(
                                        "Spell Calibration Bench JEI candidate skipped because its sample failed to build: {}/{}/{}.",
                                        targetId,
                                        rule.displayId(),
                                        BuiltInRegistries.ITEM.getKey(candidate.getItem()),
                                        exception
                                );
                                continue;
                            }
                            if (!applied) {
                                ApprenticeCodex.LOGGER.warn(
                                        "Spell Calibration Bench JEI candidate skipped because it could not be applied: {}/{}/{}.",
                                        targetId,
                                        rule.displayId(),
                                        BuiltInRegistries.ITEM.getKey(candidate.getItem())
                                );
                                continue;
                            }
                            displayCandidates.add(candidate);
                            displayResults.add(result);
                        }
                        if (displayCandidates.isEmpty()) {
                            ApprenticeCodex.LOGGER.warn(
                                    "Spell Calibration Bench JEI rule skipped because none of its display candidates could be applied: {}/{}.",
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
                                displayCandidates,
                                displayResults,
                                rule.effectLines(),
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
            @Nullable RecipeHolder<AlchemyBrewerModifierRecipe> modifierHolder
    ) {
        var resultId = modifierHolder == null ? base.result() : modifierHolder.value().result();
        if (!BuiltInRegistries.POTION.containsKey(resultId)) {
            ApprenticeCodex.LOGGER.warn("Alchemy Brewer JEI recipe skipped: potion {} is not registered.", resultId);
            return;
        }
        var potion = BuiltInRegistries.POTION.get(resultId);
        if (potion == null) {
            return;
        }

        var modifierId = modifierHolder == null ? null : modifierHolder.id();
        recipes.add(new AlchemyBrewerJeiRecipe(
                createAlchemyBrewerJeiRecipeId(baseRecipeId, modifierId),
                base.base(),
                base.ingredient(),
                modifierHolder == null ? null : modifierHolder.value().ingredient(),
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
        var recipes = new ArrayList<SpellcasterWorkbenchRecipe>();
        for (var recipe : recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get())) {
            recipes.add(recipe.value());
        }
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
                        Ingredient.of(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                        invokeCount,
                        Ingredient.of(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), invokeCount)
                ),
                createSpellThrowableCardJeiRecipe(
                        Ingredient.of(ItemRegistry.SPELL_INVOKE_CARD.get()),
                        invokeCount,
                        Ingredient.of(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), invokeCount)
                ),
                createSpellThrowableCardJeiRecipe(
                        Ingredient.of(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                        autonomyCount,
                        Ingredient.of(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), autonomyCount)
                ),
                createSpellThrowableCardJeiRecipe(
                        Ingredient.of(ItemRegistry.SPELL_AUTONOMY_CARD.get()),
                        autonomyCount,
                        Ingredient.of(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                        new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), autonomyCount)
                )
        );
    }

    private static SpellcasterWorkbenchRecipe createSpellThrowableCardJeiRecipe(
            Ingredient baseIngredient,
            int baseCount,
            Ingredient catalystIngredient,
            ItemStack result
    ) {
        return new SpellcasterWorkbenchRecipe(
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
    }

    private static List<IJeiBrewingRecipe> createAffinityBrewingRecipes(IRecipeRegistration registration) {
        var vanillaRecipeFactory = registration.getVanillaRecipeFactory();
        var recipes = new ArrayList<IJeiBrewingRecipe>();

        for (var transition : SchoolAffinityPotionBrewing.getTransitions()) {
            var catalystId = BuiltInRegistries.ITEM.getKey(transition.catalyst());
            var outputPotionId = BuiltInRegistries.POTION.getKey(transition.outputPotion());

            for (var container : BREWING_CONTAINERS) {
                addBrewingRecipe(
                        recipes,
                        vanillaRecipeFactory,
                        transition.catalyst(),
                        createPotionStack(container, transition.inputPotion()),
                        createPotionStack(container, transition.outputPotion()),
                        transition.transitionKey(),
                        outputPotionId,
                        catalystId,
                        BuiltInRegistries.ITEM.getKey(container)
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
        return PotionContentsHelper.createPotionStack(item, potion);
    }

    private static ItemStack buildGrindRunnerCatalyst() {
        var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(
                SpellRegistry.GRIND_RUNNER.get(),
                SpellRegistry.GRIND_RUNNER.get().getMinLevel(),
                scrollStack
        );
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
