package jp.aquafactory.apprenticecodex.event.errandmage;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ErrandMageTradeStack(
        ResourceLocation item,
        int count,
        Optional<ResourceLocation> potion,
        boolean ignoreNbt
) {
    public static @Nullable ErrandMageTradeStack parse(JsonObject json, String context) {
        var itemId = parseResourceLocation(GsonHelper.getAsString(json, "item"), context + ".item");
        if (itemId == null) {
            return null;
        }

        var count = GsonHelper.getAsInt(json, "count", 1);
        if (count < 1 || count > 64) {
            ApprenticeCodex.LOGGER.error("Errand Mage trade stack count out of range at {}: {}", context, count);
            return null;
        }

        var potionId = Optional.<ResourceLocation>empty();
        if (json.has("potion")) {
            var parsedPotionId = parseResourceLocation(GsonHelper.getAsString(json, "potion"), context + ".potion");
            if (parsedPotionId == null) {
                return null;
            }
            potionId = Optional.of(parsedPotionId);
        }

        return new ErrandMageTradeStack(
                itemId,
                count,
                potionId,
                GsonHelper.getAsBoolean(json, "ignore_nbt", false)
        );
    }

    private static @Nullable ResourceLocation parseResourceLocation(String raw, String context) {
        var id = ResourceLocation.tryParse(raw);
        if (id == null) {
            ApprenticeCodex.LOGGER.error("Invalid Errand Mage trade resource location at {}: {}", context, raw);
            return null;
        }
        return id;
    }

    public ItemStack createStack() {
        var resolvedItem = BuiltInRegistries.ITEM.getOptional(item).orElse(null);
        if (resolvedItem == null) {
            ApprenticeCodex.LOGGER.error("Errand Mage trade item is missing: {}", item);
            return ItemStack.EMPTY;
        }

        var stack = new ItemStack(resolvedItem, count);
        if (potion.isPresent()) {
            var potionId = potion.get();
            var resolvedPotion = BuiltInRegistries.POTION.getOptional(potionId).orElse(null);
            if (resolvedPotion == null) {
                ApprenticeCodex.LOGGER.error("Errand Mage trade potion is missing: {}", potionId);
                return ItemStack.EMPTY;
            }
            PotionContentsHelper.setPotion(stack, resolvedPotion);
        }
        return stack;
    }
}
