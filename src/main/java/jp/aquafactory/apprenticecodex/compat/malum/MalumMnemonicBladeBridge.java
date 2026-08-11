package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class MalumMnemonicBladeBridge {
    private static final String MALUM_MOD_ID = "malum";
    private static boolean hasLoggedFailure;

    private MalumMnemonicBladeBridge() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MALUM_MOD_ID);
    }

    public static boolean fire(ServerPlayer player, ItemStack stack, int projectileCount) {
        if (!isAvailable() || projectileCount <= 0) {
            return false;
        }

        try {
            MalumMnemonicBladeBridgeImpl.fire(player, stack, projectileCount);
            return true;
        } catch (LinkageError error) {
            logFailureOnce(error);
            return false;
        }
    }

    public static void playMeleeHitEffect(ServerPlayer attacker, LivingEntity target) {
        if (!isAvailable()) {
            return;
        }

        try {
            MalumMnemonicBladeBridgeImpl.playMeleeHitEffect(attacker, target);
        } catch (LinkageError error) {
            logFailureOnce(error);
        }
    }

    private static void logFailureOnce(LinkageError error) {
        if (hasLoggedFailure) {
            return;
        }
        hasLoggedFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Malum bridge for Soulstained Steel Swingcast Staff could not be initialized.",
                error
        );
    }
}
