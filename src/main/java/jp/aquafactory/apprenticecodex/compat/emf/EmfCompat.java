package jp.aquafactory.apprenticecodex.compat.emf;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.fml.ModList;

public final class EmfCompat {
    public static final String MOD_ID = "entity_model_features";
    private static final String CLIENT_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.emf.EmfClientCompat";
    private static boolean registered;
    @Nullable
    private static Player firstPersonHandPlayer;

    private EmfCompat() {
    }

    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        if (registered) {
            return;
        }

        try {
            var compatClass = Class.forName(CLIENT_COMPAT_CLASS);
            compatClass.getMethod("register").invoke(null);
            registered = true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize EMF compatibility", exception);
        }

        ApprenticeCodex.LOGGER.info("EMF compat enabled");
    }

    public static void renderSpellrifleHand(Player player, Runnable render) {
        if (!registered) {
            render.run();
            return;
        }
        // 描画スレッド内の呼び出し区間だけ停止条件を有効にする。入れ子・例外時にも元へ戻す。
        var previous = firstPersonHandPlayer;
        firstPersonHandPlayer = player;
        try {
            render.run();
        } finally {
            firstPersonHandPlayer = previous;
        }
    }

    static boolean isRenderingSpellrifleHand(Player player) {
        return firstPersonHandPlayer == player;
    }
}
