package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

public final class EpicFightCompat {
    public static final String MOD_ID = "epicfight";
    private static final String CHARGED_TWIN_BLADE_STAFF_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightChargedTwinBladeStaffCompat";

    private EpicFightCompat() {
    }

    public static void register(IEventBus modEventBus) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            var compatClass = Class.forName(CHARGED_TWIN_BLADE_STAFF_COMPAT_CLASS);
            compatClass.getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight 互換の初期化に失敗しました", exception);
        }

        ApprenticeCodex.LOGGER.info("Epic Fight compat enabled");
    }
}
