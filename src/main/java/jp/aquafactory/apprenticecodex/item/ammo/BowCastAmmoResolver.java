package jp.aquafactory.apprenticecodex.item.ammo;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverBowAmmoResolver;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class BowCastAmmoResolver {
    private static final List<ResourceLocation> DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS =
            List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "arrow"));

    private BowCastAmmoResolver() {
    }

    public enum FocusStaffbowAmmoRoute {
        NONE,
        BYPASS,
        ARROW_CATALYST
    }

    public static boolean canStartFocusStaffbowUse(Player player, ItemStack weaponStack) {
        return canStartFocusStaffbowUse(player, weaponStack, true, DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS);
    }

    public static boolean canStartFocusStaffbowUse(Player player, ItemStack weaponStack, boolean requireArrowCatalyst) {
        return canStartFocusStaffbowUse(player, weaponStack, requireArrowCatalyst, DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS);
    }

    public static boolean canStartFocusStaffbowUse(
            Player player,
            ItemStack weaponStack,
            boolean requireArrowCatalyst,
            List<ResourceLocation> arrowCatalystItemIds
    ) {
        return resolveFocusStaffbowAmmoRoute(player, weaponStack, requireArrowCatalyst, arrowCatalystItemIds)
                != FocusStaffbowAmmoRoute.NONE;
    }

    public static FocusStaffbowAmmoRoute resolveFocusStaffbowAmmoRoute(Player player, ItemStack weaponStack) {
        return resolveFocusStaffbowAmmoRoute(player, weaponStack, true, DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS);
    }

    public static FocusStaffbowAmmoRoute resolveFocusStaffbowAmmoRoute(Player player, ItemStack weaponStack, boolean requireArrowCatalyst) {
        return resolveFocusStaffbowAmmoRoute(player, weaponStack, requireArrowCatalyst, DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS);
    }

    public static FocusStaffbowAmmoRoute resolveFocusStaffbowAmmoRoute(
            Player player,
            ItemStack weaponStack,
            boolean requireArrowCatalyst,
            List<ResourceLocation> arrowCatalystItemIds
    ) {
        if (!requireArrowCatalyst) {
            return FocusStaffbowAmmoRoute.BYPASS;
        }

        if (player.getAbilities().instabuild || hasSynthesis(weaponStack)) {
            // creative と synthesis は触媒矢を消費しないため、探索せず即通す。
            return FocusStaffbowAmmoRoute.BYPASS;
        }

        return resolveFocusStaffbowArrowCatalystAmmo(player, arrowCatalystItemIds) != null
                ? FocusStaffbowAmmoRoute.ARROW_CATALYST
                : FocusStaffbowAmmoRoute.NONE;
    }

    public static boolean consumeFocusStaffbowAmmo(Player player, FocusStaffbowAmmoRoute route) {
        return consumeFocusStaffbowAmmo(player, route, DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS);
    }

    public static boolean consumeFocusStaffbowAmmo(
            Player player,
            FocusStaffbowAmmoRoute route,
            List<ResourceLocation> arrowCatalystItemIds
    ) {
        return switch (route) {
            case BYPASS -> true;
            case ARROW_CATALYST -> consume(resolveFocusStaffbowArrowCatalystAmmo(player, arrowCatalystItemIds));
            case NONE -> false;
        };
    }

    @Nullable
    public static SpellcasterQuiverBowAmmoResolver.AmmoSource resolveFocusStaffbowArrowCatalystAmmo(
            Player player,
            List<ResourceLocation> arrowCatalystItemIds
    ) {
        if (arrowCatalystItemIds.isEmpty()) {
            return null;
        }

        Predicate<ItemStack> catalystPredicate = stack -> isFocusStaffbowArrowCatalyst(stack, arrowCatalystItemIds);

        var quiverAmmo = SpellcasterQuiver.findAccessibleArrow(player, catalystPredicate);
        if (quiverAmmo != null) {
            return new SpellcasterQuiverBowAmmoResolver.StoredAmmoSource(
                    quiverAmmo,
                    () -> SpellcasterQuiver.consumeAccessibleArrow(player, catalystPredicate)
            );
        }

        for (var ammoStack : collectElementalArrowCandidates(player)) {
            if (catalystPredicate.test(ammoStack)) {
                return createLooseAmmoSource(player, ammoStack);
            }
        }
        return null;
    }

    @Nullable
    public static SpellcasterQuiverBowAmmoResolver.AmmoSource resolveElementalNormalArrowAmmo(Player player) {
        return resolveFocusStaffbowArrowCatalystAmmo(player, DEFAULT_FOCUS_STAFFBOW_ARROW_CATALYST_ITEMS);
    }

    private static boolean consume(@Nullable SpellcasterQuiverBowAmmoResolver.AmmoSource ammoSource) {
        return ammoSource != null && ammoSource.consume();
    }

    private static boolean hasSynthesis(ItemStack stack) {
        return EnchantmentRegistry.SYNTHESIS.isPresent()
                && stack.getEnchantmentLevel(EnchantmentRegistry.SYNTHESIS.get()) > 0;
    }

    private static boolean isFocusStaffbowArrowCatalyst(ItemStack stack, List<ResourceLocation> arrowCatalystItemIds) {
        var itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && arrowCatalystItemIds.contains(itemId);
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
