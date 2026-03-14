package jp.aquafactory.apprenticecodex.item.crystalbladedstaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CrystalBladedStaffManaRecoveryManager {
    private static final Map<ServerLevel, List<PendingManaRecovery>> PENDING_RECOVERIES = new WeakHashMap<>();

    private CrystalBladedStaffManaRecoveryManager() {
    }

    public static void submit(ServerLevel level, PendingManaRecovery recovery) {
        PENDING_RECOVERIES.computeIfAbsent(level, ignored -> new ArrayList<>()).add(recovery);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        var recoveries = PENDING_RECOVERIES.get(serverLevel);
        if (recoveries == null || recoveries.isEmpty()) {
            return;
        }

        var gameTime = serverLevel.getGameTime();
        var iterator = recoveries.iterator();
        while (iterator.hasNext()) {
            var recovery = iterator.next();
            if (gameTime < recovery.executeAtGameTime()) {
                continue;
            }

            var player = serverLevel.getServer().getPlayerList().getPlayer(recovery.playerUuid());
            if (player != null && player.serverLevel() == serverLevel) {
                var magicData = MagicData.getPlayerMagicData(player);
                if (magicData != null) {
                    // 演出の着弾と効果の体感を揃えるため、オーブ単位で遅延回復する。
                    MagicTools.recoverManaSafely(player, magicData, recovery.manaAmount());
                }
            }
            iterator.remove();
        }

        if (recoveries.isEmpty()) {
            PENDING_RECOVERIES.remove(serverLevel);
        }
    }

    public record PendingManaRecovery(UUID playerUuid, long executeAtGameTime, float manaAmount) {
    }
}
