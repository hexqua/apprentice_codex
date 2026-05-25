package jp.aquafactory.apprenticecodex.remoteownercast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public record RemoteOwnerCastContext(
        UUID ownerId,
        Vec3 eyePosition,
        Vec3 forward,
        RemoteOwnerCastOrigin origin
) {
    private static final ThreadLocal<Deque<RemoteOwnerCastContext>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static Scope push(ServerPlayer owner, Vec3 eyePosition, Vec3 forward, RemoteOwnerCastOrigin origin) {
        var normalizedForward = forward.lengthSqr() > 1.0E-6D ? forward.normalize() : owner.getLookAngle();
        var context = new RemoteOwnerCastContext(owner.getUUID(), eyePosition, normalizedForward, origin);
        STACK.get().push(context);
        return new Scope(context);
    }

    public static @Nullable RemoteOwnerCastContext get(ServerPlayer owner) {
        var stack = STACK.get();
        if (stack.isEmpty()) {
            return null;
        }
        var context = stack.peek();
        return context.ownerId().equals(owner.getUUID()) ? context : null;
    }

    public static final class Scope implements AutoCloseable {
        private final RemoteOwnerCastContext context;
        private boolean closed;

        private Scope(RemoteOwnerCastContext context) {
            this.context = context;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            var stack = STACK.get();
            if (!stack.isEmpty() && stack.peek() == context) {
                stack.pop();
            } else {
                stack.remove(context);
            }
            if (stack.isEmpty()) {
                STACK.remove();
            }
        }
    }
}
