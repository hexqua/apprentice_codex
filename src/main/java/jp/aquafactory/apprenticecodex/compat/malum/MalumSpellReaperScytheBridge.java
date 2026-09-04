package jp.aquafactory.apprenticecodex.compat.malum;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class MalumSpellReaperScytheBridge {
    private MalumSpellReaperScytheBridge() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MalumSpellReaperScytheBridge::onCommonSetup);
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MalumCompatibility.MOD_ID);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!isAvailable()) {
            return;
        }

        event.enqueueWork(() -> {
            try {
                MalumSpellReaperScytheBridgeImpl.register();
            } catch (LinkageError error) {
                // バニラスイープだけを止めた半端な状態にせず、対応Malum APIとの差異を起動時に明示する。
                throw new IllegalStateException("Failed to initialize Malum compatibility for Spell Reaper Scythe", error);
            }
        });
    }
}
