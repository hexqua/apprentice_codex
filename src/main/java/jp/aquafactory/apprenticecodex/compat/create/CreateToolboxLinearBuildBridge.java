package jp.aquafactory.apprenticecodex.compat.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.List;

public final class CreateToolboxLinearBuildBridge {
    private static boolean hasLoggedInitializationFailure;

    private CreateToolboxLinearBuildBridge() {
    }

    public static List<LinearBuildItemSource> collectSources(ServerPlayer player) {
        if (!ModList.get().isLoaded(CreateCompat.MOD_ID)) {
            return List.of();
        }

        try {
            return CreateToolboxLinearBuildBridgeImpl.collectSources(player);
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
                "Create Toolbox bridge for Linear Build could not be initialized. Toolbox retrieval will be skipped.",
                error
        );
    }
}
