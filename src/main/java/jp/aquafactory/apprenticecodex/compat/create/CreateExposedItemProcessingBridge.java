package jp.aquafactory.apprenticecodex.compat.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.ItemStackProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;

import java.util.Set;

public final class CreateExposedItemProcessingBridge {
    private static boolean hasLoggedInitializationFailure;

    private CreateExposedItemProcessingBridge() {
    }

    public static int processBlocks(
            ServerLevel level,
            Iterable<BlockPos> positions,
            int maxProcessCount,
            Set<Object> skipTransportedItems,
            ItemStackProcessor processor
    ) {
        if (maxProcessCount <= 0 || !ModList.get().isLoaded(CreateCompat.MOD_ID)) {
            return 0;
        }

        try {
            return CreateExposedItemProcessingBridgeImpl.processBlocks(
                    level,
                    positions,
                    maxProcessCount,
                    skipTransportedItems,
                    processor
            );
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
            return 0;
        }
    }

    private static void logInitializationFailureOnce(LinkageError error) {
        if (hasLoggedInitializationFailure) {
            return;
        }

        hasLoggedInitializationFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Create exposed item processing bridge could not be initialized. Create-carried items will be skipped.",
                error
        );
    }
}
