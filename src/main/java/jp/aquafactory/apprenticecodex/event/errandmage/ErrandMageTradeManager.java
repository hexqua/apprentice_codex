package jp.aquafactory.apprenticecodex.event.errandmage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ErrandMageTradeManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "errand_mage_trades";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ErrandMageTradeManager INSTANCE = new ErrandMageTradeManager();
    private static volatile List<ErrandMageTradeDefinition> definitions = List.of();
    private static volatile Set<Item> ignoreNbtPaymentItems = Set.of();

    private ErrandMageTradeManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static void addTrades(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        for (var definition : definitions) {
            var listings = trades.get(definition.level());
            if (listings == null) {
                ApprenticeCodex.LOGGER.warn("Errand Mage trade level is missing from event map: {}", definition.level());
                continue;
            }
            listings.add(createListing(definition));
        }
    }

    public static boolean shouldIgnorePaymentTags(Item item) {
        return ignoreNbtPaymentItems.contains(item);
    }

    private static VillagerTrades.ItemListing createListing(ErrandMageTradeDefinition definition) {
        return (trader, random) -> {
            var primaryCostDefinition = definition.costs().get(random.nextInt(definition.costs().size()));
            var primaryCost = primaryCostDefinition.createStack();
            var result = definition.result().createStack();
            if (primaryCost.isEmpty() || result.isEmpty()) {
                return null;
            }

            if (definition.type() == ErrandMageTradeDefinition.Type.BUY) {
                return new MerchantOffer(
                        ErrandMageTradeHelper.createPaymentStack(primaryCostDefinition, primaryCost),
                        result,
                        definition.maxUses(),
                        definition.xp(),
                        definition.priceMultiplier()
                );
            }

            var secondaryCost = Optional.<ItemCost>empty();
            if (definition.costB().isPresent()) {
                var secondaryCostDefinition = definition.costB().get();
                var secondaryCostStack = secondaryCostDefinition.createStack();
                if (secondaryCostStack.isEmpty()) {
                    return null;
                }
                secondaryCost = java.util.Optional.of(ErrandMageTradeHelper.createPaymentStack(
                        secondaryCostDefinition,
                        secondaryCostStack
                ));
            }
            return new MerchantOffer(
                    ErrandMageTradeHelper.createPaymentStack(primaryCostDefinition, primaryCost),
                    secondaryCost,
                    result,
                    definition.maxUses(),
                    definition.xp(),
                    definition.priceMultiplier()
            );
        };
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resourceMap,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler
    ) {
        var resolvedDefinitions = new ArrayList<ErrandMageTradeDefinition>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeDefinitions(entry.getKey(), entry.getValue(), resolvedDefinitions));

        definitions = List.copyOf(resolvedDefinitions);
        ignoreNbtPaymentItems = resolveIgnoreNbtPaymentItems(resolvedDefinitions);
    }

    private static void mergeDefinitions(
            ResourceLocation resourceId,
            JsonElement element,
            List<ErrandMageTradeDefinition> resolvedDefinitions
    ) {
        try {
            var json = GsonHelper.convertToJsonObject(element, resourceId.toString());
            var values = GsonHelper.getAsJsonArray(json, "values");
            for (var index = 0; index < values.size(); index++) {
                var definition = ErrandMageTradeDefinition.parse(
                        resourceId,
                        index,
                        GsonHelper.convertToJsonObject(values.get(index), resourceId + ".values[" + index + "]")
                );
                if (definition != null) {
                    resolvedDefinitions.add(definition);
                }
            }
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.error("Failed to parse Errand Mage trade definitions {}", resourceId, exception);
        }
    }

    private static Set<Item> resolveIgnoreNbtPaymentItems(List<ErrandMageTradeDefinition> definitions) {
        var items = new HashSet<Item>();
        for (var definition : definitions) {
            addIgnoreNbtPaymentItems(definition.costs(), items);
            definition.costB().ifPresent(stack -> addIgnoreNbtPaymentItem(stack, items));
        }
        return Set.copyOf(items);
    }

    private static void addIgnoreNbtPaymentItems(List<ErrandMageTradeStack> stacks, Set<Item> items) {
        for (var stack : stacks) {
            addIgnoreNbtPaymentItem(stack, items);
        }
    }

    private static void addIgnoreNbtPaymentItem(ErrandMageTradeStack stack, Set<Item> items) {
        if (!stack.ignoreNbt()) {
            return;
        }

        var item = BuiltInRegistries.ITEM.getOptional(stack.item()).orElse(null);
        if (item == null) {
            ApprenticeCodex.LOGGER.error("Errand Mage ignore_nbt item is missing: {}", stack.item());
            return;
        }
        items.add(item);
    }
}
