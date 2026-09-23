package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemainingCountNotificationPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

import java.util.function.IntFunction;

/** 外套飛行の入力と、プレイヤーが所有する花火の使用をserver側で管理する。 */
public final class MantleFireworkBoost {
    private boolean wasFlying;
    private boolean held;
    private long lastSequence = -1;
    private long lastInputTime = Long.MIN_VALUE;
    private long lastActivation = Long.MIN_VALUE;

    public void reset() {
        wasFlying = false;
        held = false;
        lastInputTime = Long.MIN_VALUE;
    }

    public void tick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (lastInputTime != Long.MIN_VALUE && now - lastInputTime > 10) held = false;
        boolean flying = player.isFallFlying() && ShootingStarMantleRuntime.canFly(player);
        boolean started = flying && !wasFlying;
        wasFlying = flying;
        if (started) activate(player);
    }

    public void input(ServerPlayer player, long sequence, boolean jump) {
        if (sequence < 0 || sequence <= lastSequence) return;
        lastSequence = sequence;
        lastInputTime = player.level().getGameTime();
        boolean pressed = jump && !held;
        held = jump;
        if (pressed) activate(player);
    }

    private void activate(ServerPlayer player) {
        long now = player.level().getGameTime();
        var mantle = ShootingStarMantleRuntime.findEquipped(player);
        if (lastActivation == now || !MantleCalibration.usesFirework(mantle)
                || !player.isFallFlying() || !ShootingStarMantleRuntime.canFly(player)) return;

        var source = findSource(player);
        if (source == null) return;
        var rocket = source.stack().copyWithCount(1);
        // 手持ちを差し替えると使用中アイテムや他MODの装備判定に影響するため、vanillaと同じ飛行用entityを直接生成する。
        if (!player.level().addFreshEntity(new FireworkRocketEntity(player.level(), rocket, player))) return;
        lastActivation = now;
        if (!player.getAbilities().instabuild) source.extract().run();
        player.awardStat(Stats.ITEM_USED.get(Items.FIREWORK_ROCKET));
        if (!player.getAbilities().instabuild && player.connection != null) {
            Networks.sendToPlayer(player, new SyncRemainingCountNotificationPacket(
                    ItemRegistry.SHOOTING_STAR_MANTLE.getId().toString(),
                    new ItemStack(Items.FIREWORK_ROCKET), countRockets(player),
                    SyncRemainingCountNotificationPacket.DisplayType.ITEM_REMAINING));
        }
    }

    private static Source findSource(ServerPlayer player) {
        var shelf = Capabilities.getPersonalInventory(player).orElseThrow().getHandler();
        var source = findBest(shelf.getSlots(), shelf::getStackInSlot,
                slot -> () -> shelf.extractItem(slot, 1, false));
        if (source != null) return source;
        var ender = player.getEnderChestInventory();
        source = findBest(ender.getContainerSize(), ender::getItem,
                slot -> () -> { ender.removeItem(slot, 1); ender.setChanged(); });
        if (source != null) return source;
        var inventory = player.getInventory();
        source = findBest(inventory.items.size() - 9, slot -> inventory.getItem(slot + 9),
                slot -> () -> inventory.removeItem(slot + 9, 1));
        if (source != null) return source;
        source = findBest(9, inventory::getItem,
                slot -> () -> inventory.removeItem(slot, 1));
        if (source != null) return source;
        return findBest(inventory.offhand.size(), inventory.offhand::get,
                slot -> () -> inventory.removeItem(inventory.items.size() + inventory.armor.size() + slot, 1));
    }

    private static Source findBest(int slots, IntFunction<ItemStack> get, IntFunction<Runnable> extract) {
        Source best = null;
        int duration = Integer.MIN_VALUE;
        for (int slot = 0; slot < slots; slot++) {
            var stack = get.apply(slot);
            if (!valid(stack)) continue;
            int candidateDuration = duration(stack);
            if (candidateDuration > duration) {
                best = new Source(stack, extract.apply(slot));
                duration = candidateDuration;
            }
        }
        return best;
    }

    public static long countRockets(ServerPlayer player) {
        long total = 0;
        var shelf = Capabilities.getPersonalInventory(player).orElseThrow().getHandler();
        total += count(shelf.getSlots(), shelf::getStackInSlot);
        var ender = player.getEnderChestInventory();
        total += count(ender.getContainerSize(), ender::getItem);
        var inventory = player.getInventory();
        total += count(inventory.items.size(), inventory::getItem);
        total += count(inventory.offhand.size(), inventory.offhand::get);
        return total;
    }

    private static long count(int slots, IntFunction<ItemStack> get) {
        long total = 0;
        for (int slot = 0; slot < slots; slot++) {
            var stack = get.apply(slot);
            if (valid(stack)) total += stack.getCount();
        }
        return total;
    }

    private static boolean valid(ItemStack stack) {
        if (!stack.is(Items.FIREWORK_ROCKET) || stack.isEmpty()) return false;
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        return fireworks == null || fireworks.explosions().isEmpty();
    }

    private static int duration(ItemStack stack) {
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        return fireworks == null ? 0 : fireworks.flightDuration();
    }

    private record Source(ItemStack stack, Runnable extract) { }
}
