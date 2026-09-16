package jp.aquafactory.apprenticecodex.datagen.gems;

import io.redspace.ironsjewelry.core.actions.ApplyEffectAction;
import io.redspace.ironsjewelry.core.actions.CreateItemsAction;
import io.redspace.ironsjewelry.core.data.QualityScalar;
import io.redspace.ironsjewelry.utils.JewelryModTags;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.gems.AdvanceSpellCooldownsAction;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/** 1.21.1のJewelry素材定義。効果の追加はここで行い、generatedのJSONは編集しない。 */
public final class GemsMaterials {
    private GemsMaterials() {
    }

    public static List<GemsMaterial> definitions() {
        var operation = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        return List.of(
                material("crystalline_arcane", 0.5)
                        .ingredient(Ingredient.of(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()))
                        .attribute(AttributeRegistry.SPELL_POWER, 0.03, operation)
                        .action(new CreateItemsAction(
                                Optional.of(new QualityScalar(0.05, 0.025, 0.01, Optional.of(0.2))),
                                ItemRegistry.CRYSTALLINE_ARCANE_SHARD,
                                Optional.of(SoundRegistry.VANILLA_CRYSTALLIZE_MANA),
                                new QualityScalar(1),
                                new QualityScalar(1)),
                                false
                        )
                        .negativeEffect(MobEffects.WEAKNESS)
                        .tag(JewelryModTags.GEM).build(),
                material("emberstained_netherite", 2.75)
                        .ingredient(Ingredient.of(ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get()))
                        .attribute(AttributeRegistry.SPELL_RESIST, 0.1, operation)
                        .action(new AdvanceSpellCooldownsAction(new QualityScalar(10, 5, 0, Optional.of(20d))), true)
                        .negativeEffect(EffectRegistry.THERMAL_SUNDERED)
                        .tag(JewelryModTags.METAL).build(),
                material("mana_enveloped_silver", 3)
                        .ingredient(Ingredient.of(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get()))
                        .attribute(AttributeRegistry.MAX_MANA, 0.1, operation)
                        .action(new ApplyEffectAction(new QualityScalar(100),
                                new QualityScalar(0, 0.5, 0, Optional.of(2d)), EffectRegistry.ARCANE_CHARGE), true)
                        .negativeEffect(MobEffectRegistry.BLIGHT)
                        .tag(JewelryModTags.METAL).build(),
                material("spellstained_arcane", 1.75)
                        .ingredient(Ingredient.of(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()))
                        .attribute(AttributeRegistry.MANA_REGEN, 0.05, operation)
                        .action(new ApplyEffectAction(new QualityScalar(100),
                                new QualityScalar(0, 1, 0, Optional.of(2d)), EffectRegistry.MANA_REGENERATION), true)
                        .negativeEffect(MobEffectRegistry.REND)
                        .tag(JewelryModTags.METAL).build(),
                material("spellstained_diamond", 2.25)
                        .ingredient(Ingredient.of(ItemRegistry.SPELLSTAINED_DIAMOND.get()))
                        .attribute(AttributeRegistry.MANA_REGEN, 0.15, operation)
                        .action(new ApplyEffectAction(new QualityScalar(100),
                                new QualityScalar(0, 1, 0, Optional.of(2d)), EffectRegistry.MANA_REGENERATION), true)
                        .negativeEffect(MobEffectRegistry.SOUL_BURN)
                        .tag(JewelryModTags.GEM).build(),
                material("wisdom", 2.5)
                        .ingredient(Ingredient.of(ItemRegistry.WISDOM_SHARD.get()))
                        .attribute(AttributeRegistry.MAX_MANA, 0.1, operation)
                        .action(new ApplyEffectAction(new QualityScalar(100),
                                new QualityScalar(0, 1, 0, Optional.of(2d)), EffectRegistry.INTELLIGENCE), true)
                        .negativeEffect(MobEffectRegistry.BLIGHT)
                        .tag(JewelryModTags.GEM).build()
        );
    }

    private static GemsMaterial.Builder material(String name, double quality) {
        return GemsMaterial.builder(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name),
                "material." + ApprenticeCodex.MODID + "." + name,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "palettes/" + name), quality);
    }
}
