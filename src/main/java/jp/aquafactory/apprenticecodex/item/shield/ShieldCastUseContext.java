package jp.aquafactory.apprenticecodex.item.shield;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ShieldCastUseContext {
    private static final ThreadLocal<Deque<MagicData>> PRESERVED_MAGIC_DATA = new ThreadLocal<>();

    private ShieldCastUseContext() {
    }

    public static void runPreservingShieldUse(MagicData magicData, Runnable action) {
        var stack = PRESERVED_MAGIC_DATA.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            PRESERVED_MAGIC_DATA.set(stack);
        }
        stack.push(magicData);
        try {
            action.run();
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                PRESERVED_MAGIC_DATA.remove();
            }
        }
    }

    public static boolean shouldPreserveShieldUse(MagicData magicData) {
        var stack = PRESERVED_MAGIC_DATA.get();
        return stack != null && stack.contains(magicData);
    }

    public static boolean shouldPreserveCurrentShieldUse(Player player) {
        var item = player.getUseItem().getItem();
        return item instanceof ReflectcastShield || item instanceof BulwarkGreatshield;
    }
}
