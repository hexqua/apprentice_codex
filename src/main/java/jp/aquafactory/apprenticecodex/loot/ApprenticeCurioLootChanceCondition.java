package jp.aquafactory.apprenticecodex.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.LootConditionRegistry;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

public final class ApprenticeCurioLootChanceCondition implements LootItemCondition {
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

    public static final class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ApprenticeCurioLootChanceCondition> {
        @Override
        public void serialize(
                JsonObject json,
                ApprenticeCurioLootChanceCondition condition,
                JsonSerializationContext context
        ) {
            json.addProperty("base_chance", condition.baseChance);
        }

        @Override
        public @NotNull ApprenticeCurioLootChanceCondition deserialize(
                JsonObject json,
                JsonDeserializationContext context
        ) {
            return new ApprenticeCurioLootChanceCondition(GsonHelper.getAsDouble(json, "base_chance"));
        }
    }
}
