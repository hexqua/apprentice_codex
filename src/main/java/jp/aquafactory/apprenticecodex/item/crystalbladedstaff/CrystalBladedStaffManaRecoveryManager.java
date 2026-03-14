package jp.aquafactory.apprenticecodex.item.crystalbladedstaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CrystalBladedStaffManaRecoveryManager {
    private static final Map<ServerLevel, List<PendingManaRecovery>> PENDING_RECOVERIES = new WeakHashMap<>();
    private static final Map<ServerLevel, List<PendingLaunchSound>> PENDING_LAUNCH_SOUNDS = new WeakHashMap<>();

    private CrystalBladedStaffManaRecoveryManager() {
    }

    public static void submit(ServerLevel level, PendingManaRecovery recovery) {
        PENDING_RECOVERIES.computeIfAbsent(level, ignored -> new ArrayList<>()).add(recovery);
    }

    public static void submitLaunchSound(ServerLevel level, PendingLaunchSound launchSound) {
        PENDING_LAUNCH_SOUNDS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(launchSound);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        var recoveries = PENDING_RECOVERIES.get(serverLevel);
        var gameTime = serverLevel.getGameTime();
        if (recoveries != null && !recoveries.isEmpty()) {
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

        var launchSounds = PENDING_LAUNCH_SOUNDS.get(serverLevel);
        if (launchSounds != null && !launchSounds.isEmpty()) {
            var iterator = launchSounds.iterator();
            while (iterator.hasNext()) {
                var launchSound = iterator.next();
                if (gameTime < launchSound.executeAtGameTime()) {
                    continue;
                }

                AudioTools.playSoundFromPosition(serverLevel, launchSound.position(),
                        SoundRegistry.SIPHON_ORB_LAUNCH.get(), SoundSource.PLAYERS);
                iterator.remove();
            }

            if (launchSounds.isEmpty()) {
                PENDING_LAUNCH_SOUNDS.remove(serverLevel);
            }
        }
    }

    public record PendingManaRecovery(UUID playerUuid, long executeAtGameTime, float manaAmount) {
    }

    public record PendingLaunchSound(Vec3 position, long executeAtGameTime) {
    }
}
