package jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class SpellcasterQuiverBowAmmoResolver {
    private SpellcasterQuiverBowAmmoResolver() {
    }

    public static boolean hasSupportedQuiverAmmo(Player player, ItemStack bowStack) {
        if (!(bowStack.getItem() instanceof ProjectileWeaponItem projectileWeaponItem)) {
            return false;
        }

        return SpellcasterQuiver.findAccessibleArrow(player, projectileWeaponItem.getAllSupportedProjectiles()) != null;
    }

    @Nullable
    public static AmmoSource resolveBowAmmo(Player player, ItemStack bowStack) {
        if (!(bowStack.getItem() instanceof ProjectileWeaponItem projectileWeaponItem)) {
            return null;
        }

        var heldProjectile = ProjectileWeaponItem.getHeldProjectile(player, projectileWeaponItem.getSupportedHeldProjectiles());
        if (!heldProjectile.isEmpty()) {
            return createLooseAmmoSource(player, heldProjectile);
        }

        var supportedProjectiles = projectileWeaponItem.getAllSupportedProjectiles();
        var normalArrowSource = resolveNormalArrowSource(player, supportedProjectiles);
        if (normalArrowSource != null) {
            return normalArrowSource;
        }

        // 矢筒経由で特殊矢だけ見えていても、Infinity 付きの弓は通常矢扱いで止める。
        if (hasInfinity(bowStack) && supportedProjectiles.test(new ItemStack(Items.ARROW))) {
            return new VirtualAmmoSource(new ItemStack(Items.ARROW));
        }

        var bestAmmoType = resolveBestCountAmmoType(player, supportedProjectiles);
        if (bestAmmoType == null) {
            return null;
        }

        return resolveSpecificAmmoSource(player, supportedProjectiles, bestAmmoType::matches);
    }

    @Nullable
    private static AmmoSource resolveNormalArrowSource(Player player, Predicate<ItemStack> supportedProjectiles) {
        return resolveSpecificAmmoSource(player, supportedProjectiles, stack -> stack.is(Items.ARROW));
    }

    @Nullable
    private static AmmoSource resolveSpecificAmmoSource(
            Player player,
            Predicate<ItemStack> supportedProjectiles,
            Predicate<ItemStack> ammoPredicate
    ) {
        Predicate<ItemStack> predicate = stack -> supportedProjectiles.test(stack) && ammoPredicate.test(stack);

        var quiverAmmo = SpellcasterQuiver.findAccessibleArrow(player, predicate);
        if (quiverAmmo != null) {
            return new StoredAmmoSource(quiverAmmo, () -> SpellcasterQuiver.consumeAccessibleArrow(player, predicate));
        }

        for (var ammoStack : collectInventoryAmmoStacks(player)) {
            if (predicate.test(ammoStack)) {
                return createLooseAmmoSource(player, ammoStack);
            }
        }
        return null;
    }

    @Nullable
    private static AmmoTypeKey resolveBestCountAmmoType(Player player, Predicate<ItemStack> supportedProjectiles) {
        var aggregatedCounts = new LinkedHashMap<AmmoTypeKey, AggregatedAmmo>();
        for (var ammoStack : collectInventoryAmmoStacks(player)) {
            accumulateAmmo(aggregatedCounts, ammoStack, ammoStack.getCount(), supportedProjectiles);
        }
        SpellcasterQuiver.forEachAccessibleArrow(player,
                (ammoStack, count) -> accumulateAmmo(aggregatedCounts, ammoStack, count, supportedProjectiles));

        AggregatedAmmo bestAmmo = null;
        for (var aggregatedAmmo : aggregatedCounts.values()) {
            if (aggregatedAmmo.normalArrow()) {
                continue;
            }

            if (bestAmmo == null || aggregatedAmmo.count() > bestAmmo.count()) {
                bestAmmo = aggregatedAmmo;
            }
        }
        return bestAmmo == null ? null : bestAmmo.key();
    }

    private static void accumulateAmmo(
            LinkedHashMap<AmmoTypeKey, AggregatedAmmo> aggregatedCounts,
            ItemStack ammoStack,
            int count,
            Predicate<ItemStack> supportedProjectiles
    ) {
        if (ammoStack.isEmpty() || count <= 0 || !supportedProjectiles.test(ammoStack)) {
            return;
        }

        var key = AmmoTypeKey.of(ammoStack);
        var aggregatedAmmo = aggregatedCounts.get(key);
        if (aggregatedAmmo == null) {
            aggregatedCounts.put(key, new AggregatedAmmo(key, ammoStack.is(Items.ARROW), count));
            return;
        }

        aggregatedAmmo.addCount(count);
    }

    private static List<ItemStack> collectInventoryAmmoStacks(Player player) {
        var stacks = new ArrayList<ItemStack>(1 + player.getInventory().items.size());
        stacks.addAll(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        return stacks;
    }

    private static AmmoSource createLooseAmmoSource(Player player, ItemStack ammoStack) {
        BooleanSupplier consumer = () -> {
            ammoStack.shrink(1);
            if (ammoStack.isEmpty()) {
                player.getInventory().removeItem(ammoStack);
            }
            return true;
        };
        return new LooseAmmoSource(ammoStack, consumer);
    }

    private static boolean hasInfinity(ItemStack bowStack) {
        return bowStack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
    }

    public interface AmmoSource {
        ItemStack stack();

        boolean consume();

        default boolean isInfinite(ItemStack bowStack, Player player) {
            return stack().getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(stack(), bowStack, player);
        }
    }

    public record LooseAmmoSource(ItemStack stack, BooleanSupplier consumer) implements AmmoSource {
        @Override
        public boolean consume() {
            return consumer.getAsBoolean();
        }
    }

    public record StoredAmmoSource(ItemStack stack, BooleanSupplier consumer) implements AmmoSource {
        @Override
        public boolean consume() {
            return consumer.getAsBoolean();
        }
    }

    public record VirtualAmmoSource(ItemStack stack) implements AmmoSource {
        @Override
        public boolean consume() {
            return false;
        }

        @Override
        public boolean isInfinite(ItemStack bowStack, Player player) {
            return true;
        }
    }

    private record AmmoTypeKey(Item item, @Nullable CompoundTag tag) {
        private static AmmoTypeKey of(ItemStack stack) {
            return new AmmoTypeKey(stack.getItem(), stack.getTag() == null ? null : stack.getTag().copy());
        }

        private boolean matches(ItemStack stack) {
            return item == stack.getItem()
                    && (tag == null ? stack.getTag() == null : tag.equals(stack.getTag()));
        }
    }

    private static final class AggregatedAmmo {
        private final AmmoTypeKey key;
        private final boolean normalArrow;
        private int count;

        private AggregatedAmmo(AmmoTypeKey key, boolean normalArrow, int count) {
            this.key = key;
            this.normalArrow = normalArrow;
            this.count = count;
        }

        private AmmoTypeKey key() {
            return key;
        }

        private boolean normalArrow() {
            return normalArrow;
        }

        private int count() {
            return count;
        }

        private void addCount(int additionalCount) {
            count += additionalCount;
        }
    }
}
