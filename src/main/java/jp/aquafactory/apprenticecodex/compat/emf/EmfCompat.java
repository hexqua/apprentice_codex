package jp.aquafactory.apprenticecodex.compat.emf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.fml.ModList;

public final class EmfCompat {
    public static final String MOD_ID = "entity_model_features";
    private static final String CLIENT_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.emf.EmfClientCompat";

    private EmfCompat() {
    }

    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            var compatClass = Class.forName(CLIENT_COMPAT_CLASS);
            compatClass.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize EMF compatibility", exception);
        }

        ApprenticeCodex.LOGGER.info("EMF compat enabled");
    }
}
