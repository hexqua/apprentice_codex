package jp.aquafactory.apprenticecodex.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.LootConditionRegistry;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

public final class ApprenticeCurioLootChanceCondition implements LootItemCondition {
    public static final MapCodec<ApprenticeCurioLootChanceCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.DOUBLE.fieldOf("base_chance").forGetter(ApprenticeCurioLootChanceCondition::baseChance)
            ).apply(instance, ApprenticeCurioLootChanceCondition::new));

    private final double baseChance;

    public ApprenticeCurioLootChanceCondition(double baseChance) {
        this.baseChance = baseChance;
    }

    @Override
    public @NotNull LootItemConditionType getType() {
        return LootConditionRegistry.APPRENTICE_CURIO_LOOT_CHANCE.get();
    }

    @Override
    public boolean test(LootContext context) {
        if (!ApprenticeCodexServerConfig.enableApprenticeCurioLoot()) {
            return false;
        }

        var multiplier = ApprenticeCodexServerConfig.apprenticeCurioLootChanceMultiplier();
        if (multiplier <= 0.0D) {
            return false;
        }

        var effectiveChance = Math.min(1.0D, Math.max(0.0D, baseChance * multiplier));
        return effectiveChance >= 1.0D || context.getRandom().nextDouble() < effectiveChance;
    }

    private double baseChance() {
        return baseChance;
    }
}
