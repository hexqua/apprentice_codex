package jp.aquafactory.apprenticecodex.gametest;

import com.electronwill.nightconfig.core.file.FileWatcher;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class GameTestConfigIsolation {
    private GameTestConfigIsolation() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!(event.getServer() instanceof GameTestServer)) {
            return;
        }
        // Forge 1.20.1のConfigValue#setはautosaveを発火する。監視スレッドのload/clearCacheが
        // 同期テストの一時上書きに割り込むため、専用GameTestプロセスでは対象設定の監視を外す。
        // 通常サーバー・runClientの再読込には介入せず、このクラス自体も配布jarから除外される。
        for (var config : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER)) {
            if (config.getModId().equals(ApprenticeCodex.MODID)
                    || config.getModId().equals("irons_spellbooks")) {
                FileWatcher.defaultInstance().removeWatch(config.getFullPath());
                ApprenticeCodex.LOGGER.info("Disabled config file watching for GameTest overrides: {}", config.getFileName());
            }
        }
    }
}
