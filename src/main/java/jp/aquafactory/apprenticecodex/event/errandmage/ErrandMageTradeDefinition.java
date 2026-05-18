package jp.aquafactory.apprenticecodex.event.errandmage;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record ErrandMageTradeDefinition(
        Type type,
        int level,
        List<ErrandMageTradeStack> costs,
        Optional<ErrandMageTradeStack> costB,
        ErrandMageTradeStack result,
        int maxUses,
        int xp,
        float priceMultiplier
) {
    public ErrandMageTradeDefinition {
        costs = List.copyOf(costs);
    }

    public static @Nullable ErrandMageTradeDefinition parse(ResourceLocation resourceId, int index, JsonObject json) {
        var context = resourceId + ".values[" + index + "]";
        var type = Type.parse(GsonHelper.getAsString(json, "type"), context + ".type");
        if (type == null) {
            return null;
        }

        var level = GsonHelper.getAsInt(json, "level");
        if (level < 1 || level > 5) {
            ApprenticeCodex.LOGGER.error("Errand Mage trade level out of range at {}: {}", context, level);
            return null;
        }

        var costs = new java.util.ArrayList<ErrandMageTradeStack>();
        var costArray = GsonHelper.getAsJsonArray(json, "costs");
        for (var costIndex = 0; costIndex < costArray.size(); costIndex++) {
            var cost = ErrandMageTradeStack.parse(
                    GsonHelper.convertToJsonObject(costArray.get(costIndex), context + ".costs[" + costIndex + "]"),
                    context + ".costs[" + costIndex + "]"
            );
            if (cost == null) {
                return null;
            }
            costs.add(cost);
        }
        if (costs.isEmpty()) {
            ApprenticeCodex.LOGGER.error("Errand Mage trade costs must not be empty at {}", context);
            return null;
        }

        if (type == Type.SELL && costs.size() != 1) {
            ApprenticeCodex.LOGGER.error("Errand Mage sell trade must have exactly one primary cost at {}", context);
            return null;
        }

        var costB = Optional.<ErrandMageTradeStack>empty();
        if (json.has("cost_b")) {
            var parsedCostB = ErrandMageTradeStack.parse(
                    GsonHelper.getAsJsonObject(json, "cost_b"),
                    context + ".cost_b"
            );
            if (parsedCostB == null) {
                return null;
            }
            costB = Optional.of(parsedCostB);
        }

        var result = ErrandMageTradeStack.parse(GsonHelper.getAsJsonObject(json, "result"), context + ".result");
        if (result == null) {
            return null;
        }

        var maxUses = GsonHelper.getAsInt(json, "max_uses");
        if (maxUses < 1) {
            ApprenticeCodex.LOGGER.error("Errand Mage trade max_uses must be positive at {}: {}", context, maxUses);
            return null;
        }

        var xp = GsonHelper.getAsInt(json, "xp");
        if (xp < 0) {
            ApprenticeCodex.LOGGER.error("Errand Mage trade xp must not be negative at {}: {}", context, xp);
            return null;
        }

        return new ErrandMageTradeDefinition(
                type,
                level,
                costs,
                costB,
                result,
                maxUses,
                xp,
                GsonHelper.getAsFloat(json, "price_multiplier")
        );
    }

    public enum Type {
        BUY("buy"),
        SELL("sell");

        private final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        private static @Nullable Type parse(String raw, String context) {
            for (var value : values()) {
                if (value.serializedName.equals(raw.toLowerCase(Locale.ROOT))) {
                    return value;
                }
            }
            ApprenticeCodex.LOGGER.error("Unknown Errand Mage trade type at {}: {}", context, raw);
            return null;
        }
    }
}
