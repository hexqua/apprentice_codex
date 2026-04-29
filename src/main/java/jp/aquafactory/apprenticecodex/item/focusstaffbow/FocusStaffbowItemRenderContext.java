package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class FocusStaffbowItemRenderContext {
    private static final ThreadLocal<Deque<Optional<LivingEntity>>> RENDERING_ENTITIES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private FocusStaffbowItemRenderContext() {
    }

    public static void push(@Nullable LivingEntity entity) {
        RENDERING_ENTITIES.get().push(Optional.ofNullable(entity));
    }

    public static void pop() {
        var entities = RENDERING_ENTITIES.get();
        if (!entities.isEmpty()) {
            entities.pop();
        }
        if (entities.isEmpty()) {
            RENDERING_ENTITIES.remove();
        }
    }

    @Nullable
    public static LivingEntity getRenderingEntity() {
        var entities = RENDERING_ENTITIES.get();
        return entities.isEmpty() ? null : entities.peek().orElse(null);
    }
}
