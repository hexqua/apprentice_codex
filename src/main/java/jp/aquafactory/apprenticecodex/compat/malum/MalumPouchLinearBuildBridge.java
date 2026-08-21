package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSource;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.List;

public final class MalumPouchLinearBuildBridge {
    private static boolean hasLoggedInitializationFailure;

    private MalumPouchLinearBuildBridge() {
    }

    public static List<LinearBuildItemSource> collectSources(ServerPlayer player) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            return List.of();
        }

        try {
            return MalumPouchLinearBuildBridgeImpl.collectSources(player);
        } catch (LinkageError error) {
            logInitializationFailureOnce(error);
            return List.of();
        }
    }

    private static void logInitializationFailureOnce(LinkageError error) {
        if (hasLoggedInitializationFailure) {
            return;
        }
        hasLoggedInitializationFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Malum Pouch bridge for Linear Build could not be initialized. Pouch retrieval will be skipped.",
                error
        );
    }
}
