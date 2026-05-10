package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public final class EpicFightCompat {
    public static final String MOD_ID = "epicfight";
    private static final String CHARGED_TWIN_BLADE_STAFF_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightChargedTwinBladeStaffCompat";
    private static final String MULTIPURPOSE_STAFFRIFLE_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightMultipurposeStaffrifleCompat";

    private EpicFightCompat() {
    }

    public static void register(IEventBus modEventBus) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            registerCompat(CHARGED_TWIN_BLADE_STAFF_COMPAT_CLASS, modEventBus);
            registerCompat(MULTIPURPOSE_STAFFRIFLE_COMPAT_CLASS, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight 互換の初期化に失敗しました", exception);
        }

        ApprenticeCodex.LOGGER.info("Epic Fight compat enabled");
    }

    private static void registerCompat(String className, IEventBus modEventBus) throws ReflectiveOperationException {
        var compatClass = Class.forName(className);
        compatClass.getMethod("register", IEventBus.class).invoke(null, modEventBus);
    }
}
