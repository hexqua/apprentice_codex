package jp.aquafactory.apprenticecodex.compat.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class MagiCompressorGadgetAirBridge {
    private static boolean forceUnavailableForGameTest;
    private static boolean hasLoggedInitializationFailure;

    private MagiCompressorGadgetAirBridge() {
    }

    public static float getMaxAir(ItemStack stack) {
        if (!isCreateAvailable()) {
            return 0.0F;
        }

        try {
            return MagiCompressorGadgetAirBridgeImpl.getMaxAir(stack);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
            return 0.0F;
        }
    }

    public static float getStoredAir(ItemStack stack) {
        if (!isCreateAvailable()) {
            return 0.0F;
        }

        try {
            return MagiCompressorGadgetAirBridgeImpl.getStoredAir(stack);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
            return 0.0F;
        }
    }

    public static void setStoredAir(ItemStack stack, float air) {
        if (!isCreateAvailable()) {
            return;
        }

        try {
            MagiCompressorGadgetAirBridgeImpl.setStoredAir(stack, air);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
        }
    }

    static void registerBacktankSupplier() {
        if (!isCreateAvailable()) {
            return;
        }

        try {
            MagiCompressorGadgetAirBridgeImpl.registerBacktankSupplier();
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
        }
    }

    public static void setForceUnavailableForGameTest(boolean unavailable) {
        forceUnavailableForGameTest = unavailable;
    }

    private static boolean isCreateAvailable() {
        return !forceUnavailableForGameTest && ModList.get().isLoaded(CreateCompat.MOD_ID);
    }

    private static void logInitializationFailureOnce(LinkageError error) {
        if (hasLoggedInitializationFailure) {
            return;
        }

        hasLoggedInitializationFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Magi-Compressor Gadget Create air bridge could not be initialized. Its compressed-air features will be disabled.",
                error
        );
    }
}
