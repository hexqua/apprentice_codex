package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityPotionBrewing;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class PotionRegistry {
    private static final int BASE_DURATION_TICKS = 20 * 60 * 3;
    private static final int LONG_DURATION_TICKS = 20 * 60 * 8;
    private static final int STRONG_DURATION_TICKS = 20 * 90;

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
        // 1.21.1 の Ingredient は ItemStack のポーション内容を見ず item 種別だけで一致するため、
        // 大釜と JEI も potion mix を参照するため、親和派生も addMix へ統一する。
        builder.addMix(Potions.STRENGTH, ItemRegistry.ARCANE_CINDER.get(), INTELLIGENCE);
        builder.addMix(Potions.LONG_STRENGTH, ItemRegistry.ARCANE_CINDER.get(), LONG_INTELLIGENCE);
        builder.addMix(Potions.STRONG_STRENGTH, ItemRegistry.ARCANE_CINDER.get(), STRONG_INTELLIGENCE);
        builder.addMix(INTELLIGENCE, Items.REDSTONE, LONG_INTELLIGENCE);
        builder.addMix(INTELLIGENCE, Items.GLOWSTONE_DUST, STRONG_INTELLIGENCE);

        for (var transition : SchoolAffinityPotionBrewing.getTransitions()) {
            builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(transition.inputPotion()),
                    transition.catalyst(),
                    BuiltInRegistries.POTION.wrapAsHolder(transition.outputPotion())
            );
        }
    }
}
