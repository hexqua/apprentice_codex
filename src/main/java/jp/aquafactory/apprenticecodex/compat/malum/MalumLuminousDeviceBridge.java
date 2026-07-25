package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class MalumLuminousDeviceBridge {
    private static final String MALUM_MOD_ID = "malum";
    private static boolean hasLoggedInitializationFailure;

    private MalumLuminousDeviceBridge() {
    }

    public static boolean isSupportedEther(ItemStack stack) {
        if (!isMalumAvailable()) {
            return false;
        }
        try {
            return MalumLuminousDeviceBridgeImpl.isSupportedEther(stack);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
            return false;
        }
    }

    public static void normalizeForStorage(ItemStack stack) {
        if (!isMalumAvailable()) {
            return;
        }
        try {
            MalumLuminousDeviceBridgeImpl.normalizeForStorage(stack);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
        }
    }

    public static boolean isSameEtherIgnoringColor(ItemStack first, ItemStack second) {
        if (!isMalumAvailable()) {
            return false;
        }
        try {
            return MalumLuminousDeviceBridgeImpl.isSameEtherIgnoringColor(first, second);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
            return false;
        }
    }

    private static boolean isMalumAvailable() {
        return ModList.get().isLoaded(MALUM_MOD_ID);
    }

    private static void logInitializationFailureOnce(LinkageError error) {
        if (hasLoggedInitializationFailure) {
            return;
        }
        hasLoggedInitializationFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Malum bridge for Luminous Device could not be initialized. Ether support will be skipped.",
                error
        );
    }
}
