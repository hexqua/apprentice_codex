package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class IsekaiTravelGuidebookBonusChestLootEvent {
    private static final String BONUS_CHEST_POOL_NAME = ApprenticeCodex.MODID + ":isekai_travel_guidebook_bonus_chest";

    private IsekaiTravelGuidebookBonusChestLootEvent() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!isBonusChestLootEnabled()) {
            return;
        }
        if (!BuiltInLootTables.SPAWN_BONUS_CHEST.equals(event.getName())) {
            return;
        }

        // ボーナスチェスト専用品なので、抽選ではなく常に1冊だけ追加する。
        event.getTable().addPool(LootPool.lootPool()
                .name(BONUS_CHEST_POOL_NAME)
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get()))
                .build());
    }

    private static boolean isBonusChestLootEnabled() {
        try {
            return ApprenticeCodexServerConfig.enableIsekaiTravelGuidebookBonusChestLoot();
        } catch (IllegalStateException ignored) {
            // Loot table 初期読込は config より先に走ることがあるため、その間は既定値 true を使う。
            return true;
        }
    }
}
