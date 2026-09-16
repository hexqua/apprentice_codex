package jp.aquafactory.apprenticecodex.datagen.gems;

import io.redspace.ironsjewelry.core.actions.IAction;
import io.redspace.ironsjewelry.core.data.AttributeInstance;
import io.redspace.ironsjewelry.core.data.MaterialDefinition;
import io.redspace.ironsjewelry.core.parameters.ActionParameter;
import io.redspace.ironsjewelry.core.parameters.IBonusParameterType;
import io.redspace.ironsjewelry.registry.ParameterTypeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** 素材と所属タグを一緒に宣言し、生成時の追加漏れを防ぐ。 */
public record GemsMaterial(ResourceLocation id, MaterialDefinition definition, Set<TagKey<MaterialDefinition>> tags) {
    public GemsMaterial {
        Objects.requireNonNull(id);
        Objects.requireNonNull(definition);
        tags = Set.copyOf(tags);
    }

    public static Builder builder(ResourceLocation id, String descriptionId, ResourceLocation palette, double quality) {
        return new Builder(id, descriptionId, palette, quality);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final String descriptionId;
        private final ResourceLocation palette;
        private final double quality;
        private Optional<Ingredient> ingredient = Optional.empty();
        private final Map<IBonusParameterType<?>, Object> parameters = new LinkedHashMap<>();
        private final Set<TagKey<MaterialDefinition>> tags = new LinkedHashSet<>();

        private Builder(ResourceLocation id, String descriptionId, ResourceLocation palette, double quality) {
            this.id = Objects.requireNonNull(id);
            this.descriptionId = Objects.requireNonNull(descriptionId);
            this.palette = Objects.requireNonNull(palette);
            this.quality = quality;
        }

        public Builder ingredient(Ingredient value) {
            ingredient = Optional.of(value);
            return this;
        }

        public Builder tag(TagKey<MaterialDefinition> tag) {
            tags.add(Objects.requireNonNull(tag));
            return this;
        }

        public Builder attribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
            return parameter(ParameterTypeRegistry.ATTRIBUTE_PARAMETER.get(), new AttributeInstance(attribute, amount, operation));
        }

        public Builder positiveEffect(Holder<MobEffect> effect) {
            return parameter(ParameterTypeRegistry.POSITIVE_EFFECT_PARAMETER.get(), effect);
        }

        public Builder negativeEffect(Holder<MobEffect> effect) {
            return parameter(ParameterTypeRegistry.NEGATIVE_EFFECT_PARAMETER.get(), effect);
        }

        public Builder action(IAction action, boolean targetSelf) {
            return parameter(ParameterTypeRegistry.ACTION_PARAMETER.get(), new ActionParameter.ActionRunnable(action, targetSelf));
        }

        public <T> Builder parameter(IBonusParameterType<T> type, T value) {
            Objects.requireNonNull(type);
            if (parameters.containsKey(type)) {
                throw new IllegalArgumentException("Duplicate bonus parameter for material " + id);
            }
            // 上流のemptyパラメーターだけはVoidのためnullが正規の値になる。
            if (type == ParameterTypeRegistry.EMPTY.get()) {
                if (value != null) {
                    throw new IllegalArgumentException("Empty bonus parameter must be null for material " + id);
                }
            } else {
                Objects.requireNonNull(value);
            }
            parameters.put(type, value);
            return this;
        }

        public GemsMaterial build() {
            var bonuses = new LinkedHashMap<>(parameters);
            // Jewelry 2.0.2のemptyはVoidだが、上流のdispatchedMapデコードがnullを拒否する。
            // 効果のない指定は省略へ正規化し、読み込めないJSONを生成しない。
            bonuses.remove(ParameterTypeRegistry.EMPTY.get());
            return new GemsMaterial(id, new MaterialDefinition(descriptionId, ingredient, palette, Map.copyOf(bonuses), quality), tags);
        }
    }
}
