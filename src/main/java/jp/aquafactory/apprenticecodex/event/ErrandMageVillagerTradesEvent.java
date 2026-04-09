package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.registries.PotionRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ErrandMageVillagerTradesEvent {
    private static final int BUY_XP = 2;
    private static final int SELL_XP = 5;
    private static final float PRICE_MULTIPLIER = 0.05f;

    private ErrandMageVillagerTradesEvent() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfessionRegistry.ERRAND_MAGE.get()) {
            return;
        }

        var trades = event.getTrades();
        addBuyTrade(trades, 1, ItemRegistry.COMFORT_BERRIES.get(), 22, 1, 16);
        addBuyTrade(trades, 1, io.redspace.ironsspellbooks.registries.ItemRegistry.FROZEN_BONE_SHARD.get(), 32, 1, 16);

        addBuyTrade(trades, 2, io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get(), 4, 1, 16);
        addBuyTrade(trades, 2, io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_PEARL.get(), 3, 1, 16);

        addSellTrade(trades, 3, 7, new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get()), 12);
        addSellTrade(trades, 3, 3, createManaPotion(), 12);

        // MerchantOffer 側は cost stack が無タグなら追加 NBT を無視する。
        // ただし取引画面の自動投入は別経路なので、そちらは MerchantMenuMixin 側で同じ item 判定に寄せる。
        addBuyTrade(trades, 4, io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get(), 1, 1, 16);
        addDualCostSellTrade(trades, 5,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                new ItemStack(Items.EMERALD, 16),
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_COMMON.get()),
                3);
    }

    private static void addBuyTrade(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level, Item inputItem,
                                    int inputCount, int emeraldCount, int maxUses) {
        addTrade(trades, level, (trader, random) -> new MerchantOffer(
                ErrandMageTradeHelper.createPaymentStack(inputItem, inputCount),
                new ItemStack(Items.EMERALD, emeraldCount),
                maxUses,
                BUY_XP,
                PRICE_MULTIPLIER
        ));
    }

    private static void addSellTrade(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level, int emeraldCount,
                                     ItemStack result, int maxUses) {
        addTrade(trades, level, (trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCount),
                result.copy(),
                maxUses,
                SELL_XP,
                PRICE_MULTIPLIER
        ));
    }

    private static void addDualCostSellTrade(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level,
                                             ItemStack costA, ItemStack costB, ItemStack result, int maxUses) {
        addTrade(trades, level, (trader, random) -> new MerchantOffer(
                sanitizePaymentStack(costA),
                java.util.Optional.of(new ItemCost(costB.getItem(), costB.getCount())),
                result.copy(),
                maxUses,
                SELL_XP,
                PRICE_MULTIPLIER
        ));
    }

    private static void addTrade(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level, VillagerTrades.ItemListing listing) {
        trades.get(level).add(listing);
    }

    private static ItemStack createManaPotion() {
        return PotionContentsHelper.createPotionStack(Items.POTION, PotionRegistry.INSTANT_MANA_ONE.get());
    }

    private static ItemCost sanitizePaymentStack(ItemStack stack) {
        return ErrandMageTradeHelper.createPaymentStack(stack);
    }
}
