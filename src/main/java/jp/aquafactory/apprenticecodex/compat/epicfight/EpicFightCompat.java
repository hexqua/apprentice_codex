package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.List;

public final class EpicFightCompat {
    public static final String MOD_ID = "epicfight";
    private static final String CHARGED_TWIN_BLADE_STAFF_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightChargedTwinBladeStaffCompat";
    private static final String MULTIPURPOSE_STAFFRIFLE_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightMultipurposeStaffrifleCompat";
    private static final String SMASHCAST_SCEPTER_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSmashcastScepterCompat";
    private static final String SCROLLCASTER_GAUNTLET_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightScrollcasterGauntletCompat";
    private static final String SPELLCHARGED_GREATSWORD_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat";
    private static final String SPELLGUN_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellgunCompat";
    private static final String SWING_MAGIC_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat";

    private EpicFightCompat() {
    }

    public static void register(IEventBus modEventBus) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            registerCompat(CHARGED_TWIN_BLADE_STAFF_COMPAT_CLASS, modEventBus);
            registerCompat(MULTIPURPOSE_STAFFRIFLE_COMPAT_CLASS, modEventBus);
            registerCompat(SMASHCAST_SCEPTER_COMPAT_CLASS, modEventBus);
            registerCompat(SCROLLCASTER_GAUNTLET_COMPAT_CLASS, modEventBus);
            registerCompat(SPELLCHARGED_GREATSWORD_COMPAT_CLASS, modEventBus);
            registerCompat(SPELLGUN_COMPAT_CLASS, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight 互換の初期化に失敗しました", exception);
        }

        ApprenticeCodex.LOGGER.info("Epic Fight compat enabled");
    }

    private static void registerCompat(String className, IEventBus modEventBus) throws ReflectiveOperationException {
        var compatClass = Class.forName(className);
        compatClass.getMethod("register", IEventBus.class).invoke(null, modEventBus);
    }

    public static boolean canUseOffhandSpellgun(ServerPlayer player) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return true;
        }

        try {
            var compatClass = Class.forName(SPELLGUN_COMPAT_CLASS);
            return (boolean) compatClass.getMethod("canUseOffhandSpellgun", ServerPlayer.class).invoke(null, player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight のSpellgunオフハンド判定に失敗しました", exception);
        }
    }

    public static boolean queueMainhandSpellgunCast(ServerPlayer player, BlockTargetData targetData) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }

        try {
            var compatClass = Class.forName(SPELLGUN_COMPAT_CLASS);
            return (boolean) compatClass
                    .getMethod("queueMainhandCast", ServerPlayer.class, BlockTargetData.class)
                    .invoke(null, player, targetData);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight のSpellgunメインハンド発動保留に失敗しました", exception);
        }
    }

    public static boolean queueAttackcastRingTargets(ServerPlayer player, List<BlockTargetData> ringTargets) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }

        try {
            var compatClass = Class.forName(SWING_MAGIC_COMPAT_CLASS);
            return (boolean) compatClass
                    .getMethod("queueAttackcastRingTargets", ServerPlayer.class, List.class)
                    .invoke(null, player, ringTargets);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Epic Fight のAttackcast Ring対象同期に失敗しました", exception);
        }
    }
}
