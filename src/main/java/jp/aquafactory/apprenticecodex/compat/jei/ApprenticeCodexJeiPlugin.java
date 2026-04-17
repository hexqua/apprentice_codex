package jp.aquafactory.apprenticecodex.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
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
                new SpellcasterWorkbenchRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(
                jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe.class,
                new SpellbookCarryoverSmithingJeiExtension()
        );
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        registerAffinityPotionJeiRecipes(registration);
        registerCustomRecipes(registration);
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
                recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).stream()
                        .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                        .toList()
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
