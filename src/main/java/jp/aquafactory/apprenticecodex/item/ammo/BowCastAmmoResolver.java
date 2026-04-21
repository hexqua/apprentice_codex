package jp.aquafactory.apprenticecodex.item.ammo;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverBowAmmoResolver;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Predicate;

public final class BowCastAmmoResolver {
    private static final ItemStack BOW_AMMO_PROBE_STACK = new ItemStack(Items.BOW);

    private BowCastAmmoResolver() {
    }

    public enum FocusStaffbowAmmoRoute {
        NONE,
        BYPASS,
        NORMAL_ARROW,
        BOW_MODE
    }

    public static boolean canStartFocusStaffbowUse(Player player, ItemStack weaponStack) {
        return resolveFocusStaffbowAmmoRoute(player, weaponStack) != FocusStaffbowAmmoRoute.NONE;
    }

    public static FocusStaffbowAmmoRoute resolveFocusStaffbowAmmoRoute(Player player, ItemStack weaponStack) {
        if (player.getAbilities().instabuild || hasSynthesis(weaponStack)) {
            // creative と synthesis は触媒矢を消費しないため、探索せず即通す。
            return FocusStaffbowAmmoRoute.BYPASS;
        }

        if (resolveElementalNormalArrowAmmo(player) != null) {
            return FocusStaffbowAmmoRoute.NORMAL_ARROW;
        }

        return resolveBowModeAmmo(player) != null
                ? FocusStaffbowAmmoRoute.BOW_MODE
                : FocusStaffbowAmmoRoute.NONE;
    }

    public static boolean consumeFocusStaffbowAmmo(Player player, FocusStaffbowAmmoRoute route) {
        return switch (route) {
            case BYPASS -> true;
            case NORMAL_ARROW -> consume(resolveElementalNormalArrowAmmo(player));
            case BOW_MODE -> consume(resolveBowModeAmmo(player));
            case NONE -> false;
        };
    }

    @Nullable
    public static SpellcasterQuiverBowAmmoResolver.AmmoSource resolveElementalNormalArrowAmmo(Player player) {
        Predicate<ItemStack> normalArrowPredicate = stack -> stack.is(Items.ARROW);

        var quiverAmmo = SpellcasterQuiver.findAccessibleArrow(player, normalArrowPredicate);
        if (quiverAmmo != null) {
            return new SpellcasterQuiverBowAmmoResolver.StoredAmmoSource(
                    quiverAmmo,
                    () -> SpellcasterQuiver.consumeAccessibleArrow(player, normalArrowPredicate)
            );
        }

        for (var ammoStack : collectElementalArrowCandidates(player)) {
            if (normalArrowPredicate.test(ammoStack)) {
                return createLooseAmmoSource(player, ammoStack);
            }
        }
        return null;
    }

    @Nullable
    public static SpellcasterQuiverBowAmmoResolver.AmmoSource resolveBowModeAmmo(Player player) {
        return SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, BOW_AMMO_PROBE_STACK);
    }

    private static boolean consume(@Nullable SpellcasterQuiverBowAmmoResolver.AmmoSource ammoSource) {
        return ammoSource != null && ammoSource.consume();
    }

    private static boolean hasSynthesis(ItemStack stack) {
        return EnchantmentRegistry.SYNTHESIS.isPresent()
                && stack.getEnchantmentLevel(EnchantmentRegistry.SYNTHESIS.get()) > 0;
    }

    private static SpellcasterQuiverBowAmmoResolver.LooseAmmoSource createLooseAmmoSource(Player player, ItemStack ammoStack) {
        return new SpellcasterQuiverBowAmmoResolver.LooseAmmoSource(ammoStack, () -> {
            ammoStack.shrink(1);
            if (ammoStack.isEmpty()) {
                player.getInventory().removeItem(ammoStack);
            }
            return true;
        });
    }

    private static Iterable<ItemStack> collectElementalArrowCandidates(Player player) {
        var stacks = new ArrayList<ItemStack>(1 + player.getInventory().items.size());
        stacks.add(player.getOffhandItem());
        stacks.addAll(player.getInventory().items);
        return stacks;
    }
}
