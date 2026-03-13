package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PlunderLootingEvent {
    private static final Map<UUID, PendingLootingState> PENDING_LOOTING = new HashMap<>();

    private PlunderLootingEvent() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        var attackingPlayer = resolveAttackingPlayer(event.getSource(), event.getEntity().getKillCredit());
        if (attackingPlayer == null) {
            return;
        }

        var plunderLevel = getApplicablePlunderLevel(attackingPlayer);
        if (plunderLevel <= 0) {
            return;
        }

        var looting = attackingPlayer.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        var temporaryLootingTarget = resolveTemporaryLootingTarget(attackingPlayer);
        if (temporaryLootingTarget == null || temporaryLootingTarget.stack().isEmpty()) {
            return;
        }

        var targetStack = temporaryLootingTarget.stack();
        var originalEnchantments = targetStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var originalLootingLevel = originalEnchantments.getLevel(looting);
        if (originalLootingLevel >= plunderLevel) {
            temporaryLootingTarget.restoreHands();
            return;
        }

        var updatedEnchantments = new ItemEnchantments.Mutable(originalEnchantments);
        updatedEnchantments.set(looting, plunderLevel);
        EnchantmentHelper.setEnchantments(targetStack, updatedEnchantments.toImmutable());
        PENDING_LOOTING.put(
                event.getEntity().getUUID(),
                new PendingLootingState(attackingPlayer, targetStack, originalEnchantments, temporaryLootingTarget.movedOffhandToMainhand())
        );
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var pendingState = PENDING_LOOTING.remove(event.getEntity().getUUID());
        if (pendingState == null) {
            return;
        }

        EnchantmentHelper.setEnchantments(pendingState.stack(), pendingState.originalEnchantments());
        if (pendingState.movedOffhandToMainhand()) {
            pendingState.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            pendingState.player().setItemInHand(InteractionHand.OFF_HAND, pendingState.stack());
        }
    }

    private static Player resolveAttackingPlayer(net.minecraft.world.damagesource.DamageSource damageSource, net.minecraft.world.entity.LivingEntity killCredit) {
        if (damageSource != null && damageSource.getEntity() instanceof Player player) {
            return player;
        }

        return killCredit instanceof Player player ? player : null;
    }

    private static TemporaryLootingTarget resolveTemporaryLootingTarget(Player player) {
        if (!player.getMainHandItem().isEmpty()) {
            return new TemporaryLootingTarget(player, player.getMainHandItem(), false);
        }

        if (player.getOffhandItem().isEmpty()) {
            return null;
        }

        var offhandStack = player.getOffhandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, offhandStack);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        return new TemporaryLootingTarget(player, offhandStack, true);
    }

    private static int getApplicablePlunderLevel(Player player) {
        // 遅延着弾する spell まで厳密追跡するのは重いため、撃破時の手持ち spell gun を見る緩め判定に寄せる.
        return Math.max(
                getPlunderLevel(player.getMainHandItem()),
                getPlunderLevel(player.getOffhandItem())
        );
    }

    private static int getPlunderLevel(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractSpellGunItem)) {
            return 0;
        }

        return Enchantments.getLevel(stack, Enchantments.PLUNDER);
    }

    private record PendingLootingState(
            Player player,
            ItemStack stack,
            ItemEnchantments originalEnchantments,
            boolean movedOffhandToMainhand
    ) {
    }

    private record TemporaryLootingTarget(
            Player player,
            ItemStack stack,
            boolean movedOffhandToMainhand
    ) {
        private void restoreHands() {
            if (movedOffhandToMainhand) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, stack);
            }
        }
    }
}
