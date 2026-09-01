package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class ArcanumInAJarConfigState {
    private static final Set<Runnable> CHANGE_LISTENERS = new LinkedHashSet<>();
    private static @Nullable Values values;

    private ArcanumInAJarConfigState() {
    }

    public static Optional<Values> values() {
        return Optional.ofNullable(values);
    }

    public static void set(
            @Nullable ResourceLocation materialItemId,
            @Nullable ResourceLocation productItemId,
            int processingTimeTicks
    ) {
        values = materialItemId == null || productItemId == null
                ? null
                : new Values(materialItemId, productItemId, Math.max(1, processingTimeTicks));
        notifyChangeListeners();
    }

    public static void reset() {
        values = null;
        notifyChangeListeners();
    }

    public static void addChangeListener(Runnable listener) {
        // JEI は任意依存のため、共通の同期状態はJEI型を参照せず汎用listenerで更新を通知する。
        CHANGE_LISTENERS.add(listener);
    }

    public static void removeChangeListener(Runnable listener) {
        CHANGE_LISTENERS.remove(listener);
    }

    private static void notifyChangeListeners() {
        for (var listener : CHANGE_LISTENERS.toArray(Runnable[]::new)) {
            listener.run();
        }
    }

    public record Values(
            ResourceLocation materialItemId,
            ResourceLocation productItemId,
            int processingTimeTicks
    ) {
    }
}
