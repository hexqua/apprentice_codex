package jp.aquafactory.apprenticecodex.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsjewelry.core.actions.ApplyDamageAction;
import io.redspace.ironsjewelry.core.actions.ApplyEffectAction;
import io.redspace.ironsjewelry.core.actions.ApplyFreezeAction;
import io.redspace.ironsjewelry.core.actions.CreateItemsAction;
import io.redspace.ironsjewelry.core.actions.ExplodeAction;
import io.redspace.ironsjewelry.core.actions.HealAction;
import io.redspace.ironsjewelry.core.actions.IAction;
import io.redspace.ironsjewelry.core.actions.IgniteAction;
import io.redspace.ironsjewelry.core.actions.KnockbackAction;
import io.redspace.ironsjewelry.core.data.MaterialDefinition;
import io.redspace.ironsjewelry.core.data.QualityScalar;
import io.redspace.ironsjewelry.registry.IronsJewelryRegistries;
import io.redspace.ironsjewelry.registry.ParameterTypeRegistry;
import io.redspace.ironsjewelry.utils.JewelryModTags;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.datagen.gems.GemsMaterial;
import jp.aquafactory.apprenticecodex.datagen.gems.GemsMaterialDataProvider;
import jp.aquafactory.apprenticecodex.datagen.gems.GemsMaterials;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class GemsMaterialGameTestScenarios {
    private GemsMaterialGameTestScenarios() {
    }

    static void verifyLoadedMaterials(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var registry = IronsJewelryRegistries.materialRegistry(access);
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        var expected = GemsMaterials.definitions();
        var count = registry.keySet().stream().filter(id -> id.getNamespace().equals(ApprenticeCodex.MODID)).count();
        helper.assertTrue(count == expected.size(), "All generated materials should load without extra fixture materials");
        for (var material : expected) {
            var actual = registry.getHolder(material.id()).orElseThrow();
            var actualJson = MaterialDefinition.CODEC.encodeStart(ops, actual.value()).getOrThrow();
            helper.assertTrue(actualJson.equals(GemsMaterialDataProvider.encode(material, ops)),
                    "Loaded material should match its generated definition: " + material.id());
            for (var tag : List.of(JewelryModTags.METAL, JewelryModTags.GEM)) {
                helper.assertTrue(actual.is(tag) == material.tags().contains(tag),
                        "Loaded material tag membership should match: " + material.id() + " / " + tag.location());
            }
        }
    }

    static void verifyCodecCoverage(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        var scaled = new QualityScalar(3, 0.5, 1, Optional.of(5d));
        var magic = access.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC);
        var blocks = access.lookupOrThrow(Registries.BLOCK);
        List<IAction> actions = List.of(
                new KnockbackAction(new QualityScalar(-2, -1)),
                new IgniteAction(new QualityScalar(40)),
                new ApplyEffectAction(new QualityScalar(100), scaled, MobEffects.REGENERATION),
                new ApplyDamageAction(magic, scaled, Optional.empty(), Optional.empty()),
                new ApplyDamageAction(magic, scaled, Optional.of(8d), Optional.of(SoundEvents.GENERIC_EXPLODE)),
                new ApplyFreezeAction(scaled, false),
                new ApplyFreezeAction(scaled, true),
                new HealAction(scaled),
                new ExplodeAction(true, false, "action.irons_jewelry.explode.wind_burst", Optional.empty(),
                        Optional.empty(), Optional.empty(), Vec3.ZERO, scaled, false, Level.ExplosionInteraction.NONE,
                        ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE),
                new ExplodeAction(false, true, "action.irons_jewelry.explode.wind_burst", Optional.of(magic),
                        Optional.of(scaled), Optional.of(blocks.getOrThrow(BlockTags.LOGS)), new Vec3(0, 1, 0), scaled,
                        true, Level.ExplosionInteraction.BLOCK, ParticleTypes.FLAME, ParticleTypes.EXPLOSION_EMITTER,
                        SoundEvents.GENERIC_EXPLODE),
                new ExplodeAction(false, true, "action.irons_jewelry.explode.wind_burst", Optional.of(magic),
                        Optional.of(scaled), Optional.of(HolderSet.direct(Blocks.STONE.builtInRegistryHolder())), Vec3.ZERO,
                        scaled, false, Level.ExplosionInteraction.NONE, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER,
                        SoundEvents.GENERIC_EXPLODE),
                new CreateItemsAction(Optional.empty(), Items.DIAMOND.builtInRegistryHolder(), Optional.empty(), scaled, scaled),
                new CreateItemsAction(Optional.of(new QualityScalar(0.5)), Items.DIAMOND.builtInRegistryHolder(),
                        Optional.of(SoundEvents.GENERIC_EXPLODE), new QualityScalar(1), scaled)
        );
        Set<ResourceLocation> coveredActions = new HashSet<>();
        for (var action : actions) {
            coveredActions.add(IronsJewelryRegistries.ACTION_REGISTRY.getKey(action.codec()));
            for (boolean targetSelf : List.of(false, true)) {
                var material = fixture().ingredient(Ingredient.of(Items.DIAMOND))
                        .attribute(Attributes.MAX_HEALTH, 2, AttributeModifier.Operation.ADD_VALUE)
                        .positiveEffect(MobEffects.REGENERATION).negativeEffect(MobEffects.POISON)
                        .action(action, targetSelf).build();
                roundTrip(helper, material, ops);
            }
        }
        helper.assertTrue(coveredActions.equals(IronsJewelryRegistries.ACTION_REGISTRY.keySet()),
                "Codec fixtures should cover all registered jewelry actions");
        // 上流で種類が追加された際にも、未対応のまま通過させない。
        var parameters = Set.of(ParameterTypeRegistry.EMPTY.get(), ParameterTypeRegistry.ATTRIBUTE_PARAMETER.get(),
                ParameterTypeRegistry.POSITIVE_EFFECT_PARAMETER.get(), ParameterTypeRegistry.NEGATIVE_EFFECT_PARAMETER.get(),
                ParameterTypeRegistry.ACTION_PARAMETER.get());
        helper.assertTrue(new HashSet<>(IronsJewelryRegistries.PARAMETER_TYPE_REGISTRY.stream().toList()).equals(parameters),
                "Codec fixtures should cover all registered jewelry parameters");
        roundTrip(helper, fixture().build(), ops);
        roundTrip(helper, fixture().ingredient(Ingredient.of(ItemTags.LOGS)).build(), ops);
        roundTrip(helper, fixture().ingredient(Ingredient.of(Items.DIAMOND, Items.EMERALD)).build(), ops);
        var emptyMaterial = fixture().parameter(ParameterTypeRegistry.EMPTY.get(), null).build();
        helper.assertTrue(emptyMaterial.definition().bonusParameters().isEmpty(),
                "Void parameters should be omitted because the upstream map codec cannot decode null values");
        roundTrip(helper, emptyMaterial, ops);
        for (var operation : AttributeModifier.Operation.values()) {
            roundTrip(helper, fixture().attribute(Attributes.MAX_HEALTH, -0.5, operation).build(), ops);
        }
        var constant = QualityScalar.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createDouble(4)).getOrThrow();
        helper.assertTrue(constant.sample(3) == 4, "Numeric quality scalars should remain constant");
        var negative = new QualityScalar(-3, -1, -1, Optional.of(-5d));
        var decodedScalar = QualityScalar.CODEC.parse(JsonOps.INSTANCE,
                QualityScalar.CODEC.encodeStart(JsonOps.INSTANCE, negative).getOrThrow()).getOrThrow();
        helper.assertTrue(decodedScalar.equals(negative), "Negative scalar bounds should not be reinterpreted");

        expectFailure(helper, () -> fixture().positiveEffect(MobEffects.REGENERATION).positiveEffect(MobEffects.POISON),
                "Duplicate bonus parameter");
        expectFailure(helper, () -> fixture().parameter(ParameterTypeRegistry.EMPTY.get(), null)
                .parameter(ParameterTypeRegistry.EMPTY.get(), null), "Duplicate bonus parameter");
        expectFailure(helper, () -> GemsMaterialDataProvider.validateUniqueIds(List.of(fixture().build(), fixture().build())),
                "Duplicate jewelry material ID");
        expectFailure(helper, () -> GemsMaterialDataProvider.encode(fixture().ingredient(Ingredient.EMPTY).build(), ops),
                "apprenticecodex:codec_fixture");
    }

    private static GemsMaterial.Builder fixture() {
        return GemsMaterial.builder(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "codec_fixture"),
                "material.apprenticecodex.codec_fixture",
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "palettes/wisdom"), 2);
    }

    private static void roundTrip(GameTestHelper helper, GemsMaterial material, RegistryOps<JsonElement> ops) {
        var encoded = GemsMaterialDataProvider.encode(material, ops);
        var decoded = MaterialDefinition.CODEC.parse(ops, encoded).getOrThrow();
        var reencoded = MaterialDefinition.CODEC.encodeStart(ops, decoded).getOrThrow();
        helper.assertTrue(encoded.equals(reencoded), "Material data should survive the upstream codec round trip: " + encoded);
    }

    private static void expectFailure(GameTestHelper helper, Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            helper.assertTrue(expected.getMessage().contains(message), "Failure should identify the invalid material: " + expected);
            return;
        }
        helper.fail("Expected material validation failure: " + message);
    }
}
