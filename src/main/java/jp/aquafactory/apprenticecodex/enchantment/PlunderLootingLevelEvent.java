package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PlunderLootingLevelEvent {
    private PlunderLootingLevelEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLootingLevel(LootingLevelEvent event) {
        if (!EnchantmentRegistry.PLUNDER.isPresent()) {
            return;
        }

        var attackingPlayer = resolveAttackingPlayer(event);
        if (attackingPlayer == null) {
            return;
        }

        // 旧 Looting 付き spell gun は新方式では無効にし、独自エンチャント側へ寄せる。
        var sanitizedLootingLevel = sanitizeVanillaLootingLevel(event.getLootingLevel(), attackingPlayer);
        var plunderLevel = getApplicablePlunderLevel(attackingPlayer);
        event.setLootingLevel(Math.max(sanitizedLootingLevel, plunderLevel));
    }

    private static Player resolveAttackingPlayer(LootingLevelEvent event) {
        var damageSource = event.getDamageSource();
        if (damageSource != null && damageSource.getEntity() instanceof Player player) {
            return player;
        }

        return event.getEntity().getKillCredit() instanceof Player player ? player : null;
    }

    private static int sanitizeVanillaLootingLevel(int lootingLevel, Player player) {
        var mainHandItem = player.getMainHandItem();
        if (mainHandItem.isEmpty()
                || !(mainHandItem.getItem() instanceof PlunderTarget)) {
            return lootingLevel;
        }

        return Math.max(0, lootingLevel - mainHandItem.getEnchantmentLevel(Enchantments.MOB_LOOTING));
    }

    private static int getApplicablePlunderLevel(Player player) {
        // SpellOnCastEvent だけでは遅延着弾する spell まで追跡できないため、
        // 撃破時の手持ち magic weapon を見る緩め判定を採用する。
        return Math.max(
                getPlunderLevel(player.getMainHandItem()),
                getPlunderLevel(player.getOffhandItem())
        );
    }

    private static int getPlunderLevel(ItemStack stack) {
        if (stack.isEmpty()
                || !(stack.getItem() instanceof PlunderTarget)) {
            return 0;
        }

        return stack.getEnchantmentLevel(EnchantmentRegistry.PLUNDER.get());
    }
}
