package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityBrewingRecipe;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public final class PotionRegistry {
    private static final int BASE_DURATION_TICKS = 20 * 60 * 3;
    private static final int LONG_DURATION_TICKS = 20 * 60 * 8;
    private static final int STRONG_DURATION_TICKS = 20 * 90;

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, ApprenticeCodex.MODID);

    public static final RegistryObject<Potion> INTELLIGENCE =
            POTIONS.register("intelligence", () -> new Potion(
                    "intelligence",
                    new MobEffectInstance(EffectRegistry.INTELLIGENCE.get(), BASE_DURATION_TICKS)
            ));
    public static final RegistryObject<Potion> LONG_INTELLIGENCE =
            POTIONS.register("long_intelligence", () -> new Potion(
                    "intelligence",
                    new MobEffectInstance(EffectRegistry.INTELLIGENCE.get(), LONG_DURATION_TICKS)
            ));
    public static final RegistryObject<Potion> STRONG_INTELLIGENCE =
            POTIONS.register("strong_intelligence", () -> new Potion(
                    "intelligence",
                    new MobEffectInstance(EffectRegistry.INTELLIGENCE.get(), STRONG_DURATION_TICKS, 1)
            ));

    private PotionRegistry() {
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
        eventBus.addListener(PotionRegistry::registerDynamicPotions);
        eventBus.addListener(PotionRegistry::addRecipes);
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

    public static void addRecipes(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PotionBrewing.addMix(Potions.STRENGTH, ItemRegistry.ARCANE_CINDER.get(), INTELLIGENCE.get());
            PotionBrewing.addMix(Potions.LONG_STRENGTH, ItemRegistry.ARCANE_CINDER.get(), LONG_INTELLIGENCE.get());
            PotionBrewing.addMix(Potions.STRONG_STRENGTH, ItemRegistry.ARCANE_CINDER.get(), STRONG_INTELLIGENCE.get());
            PotionBrewing.addMix(INTELLIGENCE.get(), Items.REDSTONE, LONG_INTELLIGENCE.get());
            PotionBrewing.addMix(INTELLIGENCE.get(), Items.GLOWSTONE_DUST, STRONG_INTELLIGENCE.get());
            BrewingRecipeRegistry.addRecipe(new SchoolAffinityBrewingRecipe());
        });
    }
}
