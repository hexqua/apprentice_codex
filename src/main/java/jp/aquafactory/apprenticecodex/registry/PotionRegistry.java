package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityBrewingRecipe;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

public final class PotionRegistry {
    private static final int BASE_DURATION_TICKS = 20 * 60 * 3;
    private static final int LONG_DURATION_TICKS = 20 * 60 * 8;
    private static final int STRONG_DURATION_TICKS = 20 * 90;
    private static final List<Item> BREWING_CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, ApprenticeCodex.MODID);

    public static final DeferredHolder<Potion, Potion> INTELLIGENCE =
            POTIONS.register("intelligence", () -> new Potion(
                    "intelligence",
                    new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.INTELLIGENCE.get()), BASE_DURATION_TICKS)
            ));
    public static final DeferredHolder<Potion, Potion> LONG_INTELLIGENCE =
            POTIONS.register("long_intelligence", () -> new Potion(
                    "intelligence",
                    new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.INTELLIGENCE.get()), LONG_DURATION_TICKS)
            ));
    public static final DeferredHolder<Potion, Potion> STRONG_INTELLIGENCE =
            POTIONS.register("strong_intelligence", () -> new Potion(
                    "intelligence",
                    new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.INTELLIGENCE.get()), STRONG_DURATION_TICKS, 1)
            ));

    private PotionRegistry() {
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
        eventBus.addListener(PotionRegistry::registerDynamicPotions);
        NeoForge.EVENT_BUS.addListener(PotionRegistry::addRecipes);
    }

    private static void registerDynamicPotions(RegisterEvent event) {
        event.register(Registries.POTION, helper -> {
            for (var definition : SchoolAffinityRegistry.getDefinitions()) {
                helper.register(definition.basePotionId(), definition.basePotion());
                helper.register(definition.longPotionId(), definition.longPotion());
                helper.register(definition.strongPotionId(), definition.strongPotion());
            }
        });
    }

    public static void addRecipes(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        for (var container : BREWING_CONTAINERS) {
            builder.addRecipe(new BrewingRecipe(
                    Ingredient.of(PotionContentsHelper.createPotionStack(container, Potions.STRENGTH.value())),
                    Ingredient.of(ItemRegistry.ARCANE_CINDER.get()),
                    PotionContentsHelper.createPotionStack(container, INTELLIGENCE.get())
            ));
            builder.addRecipe(new BrewingRecipe(
                    Ingredient.of(PotionContentsHelper.createPotionStack(container, Potions.LONG_STRENGTH.value())),
                    Ingredient.of(ItemRegistry.ARCANE_CINDER.get()),
                    PotionContentsHelper.createPotionStack(container, LONG_INTELLIGENCE.get())
            ));
            builder.addRecipe(new BrewingRecipe(
                    Ingredient.of(PotionContentsHelper.createPotionStack(container, Potions.STRONG_STRENGTH.value())),
                    Ingredient.of(ItemRegistry.ARCANE_CINDER.get()),
                    PotionContentsHelper.createPotionStack(container, STRONG_INTELLIGENCE.get())
            ));
            builder.addRecipe(new BrewingRecipe(
                    Ingredient.of(PotionContentsHelper.createPotionStack(container, INTELLIGENCE.get())),
                    Ingredient.of(Items.REDSTONE),
                    PotionContentsHelper.createPotionStack(container, LONG_INTELLIGENCE.get())
            ));
            builder.addRecipe(new BrewingRecipe(
                    Ingredient.of(PotionContentsHelper.createPotionStack(container, INTELLIGENCE.get())),
                    Ingredient.of(Items.GLOWSTONE_DUST),
                    PotionContentsHelper.createPotionStack(container, STRONG_INTELLIGENCE.get())
            ));
        }
        builder.addRecipe(new SchoolAffinityBrewingRecipe());
    }
}
