package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class RightClickSpellItemHelper {
    private RightClickSpellItemHelper() {
    }

    public static boolean hasMainHandRightClickBehavior(Player player, ItemStack stack) {
        // 素手は処理の割り込みがうまくいかないため、オフハンドキャスト対象外にする.
        if (stack.isEmpty()) {
            return true;
        }

        var item = stack.getItem();
        // クールダウン中でもメインハンド側の操作を優先し、オフハンド魔法は割り込ませない。
        if (player.getCooldowns().isOnCooldown(item)) {
            return true;
        }

        if (isRightClickSpellItem(stack)) {
            return true;
        }

        if (stack.getFoodProperties(player) != null || item.getUseDuration(stack, player) > 0) {
            return true;
        }

        // ブロック設置のような `useOn` 系アイテムは、視線先に対象がなくても
        // メインハンド操作を優先してオフハンド魔法の誤発動を防ぐ。
        return hasUseOverride(item) || hasUseOnOverride(item);
    }

    public static boolean isRightClickSpellItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof CastingItem || stack.getItem() instanceof Scroll) {
            return true;
        }

        return stack.has(ComponentRegistry.CASTING_IMPLEMENT);
    }

    private static boolean hasUseOverride(Item item) {
        return ITEM_USE_OVERRIDE_CACHE.get(item.getClass());
    }

    private static boolean hasUseOnOverride(Item item) {
        return ITEM_USE_ON_OVERRIDE_CACHE.get(item.getClass());
    }

    private static final ClassValue<Boolean> ITEM_USE_OVERRIDE_CACHE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> itemClass) {
            try {
                var useMethod = itemClass.getMethod("use", Level.class, Player.class, InteractionHand.class);
                return useMethod.getDeclaringClass() != Item.class;
            } catch (NoSuchMethodException ignored) {
                return false;
            }
        }
    };

    private static final ClassValue<Boolean> ITEM_USE_ON_OVERRIDE_CACHE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> itemClass) {
            try {
                var useOnMethod = itemClass.getMethod("useOn", UseOnContext.class);
                return useOnMethod.getDeclaringClass() != Item.class;
            } catch (NoSuchMethodException ignored) {
                return false;
            }
        }
    };
}
