package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeEvents;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ImbueShieldBlockCastEvent {
    private ImbueShieldBlockCastEvent() {
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide || event.getBlockedDamage() <= 0.0f || !player.isUsingItem()) {
            return;
        }

        var shieldStack = player.getUseItem();
        if (shieldStack.getItem() instanceof BulwarkGreatshield) {
            return;
        }
        if (shieldStack.getItem() instanceof ReflectcastShield) {
            return;
        }
        if (shieldStack.getItem() instanceof ParrycastBuckler) {
            return;
        }

        if (!(shieldStack.getItem() instanceof AbstractImbueShieldItem imbueShieldItem)
                || !imbueShieldItem.supportsBlockTriggeredImbuedSpell()) {
            return;
        }

        imbueShieldItem.tryTriggerImbuedSpellOnBlock(player, shieldStack, player.getUsedItemHand());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onParrycastBucklerBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide
                || event.getBlockedDamage() <= 0.0F || !player.isUsingItem()) {
            return;
        }
        var stack = player.getUseItem();
        if (!(stack.getItem() instanceof ParrycastBuckler buckler)) {
            return;
        }

        var hand = player.getUsedItemHand();
        var perfectGuard = buckler.handlePerfectGuard(player, stack, hand);
        event.setShieldTakesDamage(false);
        applyParrycastBucklerDurability(event.getOriginalBlockedDamage(), player, stack, hand, perfectGuard);
        if (!perfectGuard && player.isUsingItem() && player.getUseItem() == stack) {
            player.stopUsingItem();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBulwarkGreatshieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide
                || event.getBlockedDamage() <= 0.0F || !player.isUsingItem()) {
            return;
        }
        var shieldStack = player.getUseItem();
        if (!(shieldStack.getItem() instanceof BulwarkGreatshield)) {
            return;
        }

        event.setShieldTakesDamage(false);
        applyBulwarkDurability(event.getOriginalBlockedDamage(), player, shieldStack, player.getUsedItemHand());
        BulwarkGreatshieldRuntime.tryRecoverMana(player);
        jp.aquafactory.apprenticecodex.event.KnockbackControlEvent.markIgnoreKnockbackThisTick(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onReflectcastShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide || event.getBlockedDamage() <= 0.0f || !player.isUsingItem()) {
            return;
        }

        var shieldStack = player.getUseItem();
        if (!(shieldStack.getItem() instanceof ReflectcastShield reflectcastShield)) {
            return;
        }

        var usedHand = player.getUsedItemHand();
        var spellTriggered = reflectcastShield.tryTriggerImbuedSpellOnBlock(player, shieldStack, usedHand);

        // 1.20.1 Forge は ShieldBlockEvent 後にバニラ耐久を削るため、ReflectcastShield だけ手動消費に差し替える。
        event.setShieldTakesDamage(false);
        applyReflectcastShieldDurability(event, player, shieldStack, usedHand, spellTriggered);
        if (spellTriggered) {
            ManaForceBladeEvents.playBlueGuardEffect(player, resolveReflectcastEffectPosition(player, event), 16);
        }
    }

    private static void applyReflectcastShieldDurability(
            ShieldBlockEvent event,
            ServerPlayer player,
            ItemStack shieldStack,
            InteractionHand usedHand,
            boolean spellTriggered
    ) {
        var now = player.level().getGameTime();
        var durabilityCost = ReflectcastShield.resolveBlockedDurabilityCost(
                event.getOriginalBlockedDamage(),
                spellTriggered
        );
        if (durabilityCost <= 0 || ReflectcastShield.isDurabilityConsumptionSuppressed(shieldStack, now)) {
            return;
        }

        var beforeDamage = shieldStack.getDamageValue();
        var beforeCount = shieldStack.getCount();
        shieldStack.hurtAndBreak(durabilityCost, player, brokenPlayer -> {
            brokenPlayer.broadcastBreakEvent(usedHand);
            ForgeEventFactory.onPlayerDestroyItem(player, shieldStack, usedHand);
            if (player.getUseItem() == shieldStack) {
                player.stopUsingItem();
            }
        });

        if (shieldStack.isEmpty()) {
            player.setItemSlot(resolveEquipmentSlot(usedHand), ItemStack.EMPTY);
            player.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + player.level().random.nextFloat() * 0.4F);
        }

        if (shieldStack.isEmpty() || shieldStack.getDamageValue() > beforeDamage || shieldStack.getCount() < beforeCount) {
            ReflectcastShield.rememberDurabilityConsumed(shieldStack, now);
        }
    }

    private static EquipmentSlot resolveEquipmentSlot(InteractionHand usedHand) {
        return usedHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    private static void applyBulwarkDurability(
            float originalBlockedDamage,
            ServerPlayer player,
            ItemStack shieldStack,
            InteractionHand usedHand
    ) {
        var now = player.level().getGameTime();
        if (originalBlockedDamage < 3.0F || BulwarkGreatshield.isDurabilityConsumptionSuppressed(shieldStack, now)) {
            return;
        }

        var beforeDamage = shieldStack.getDamageValue();
        var beforeCount = shieldStack.getCount();
        shieldStack.hurtAndBreak(1, player, brokenPlayer -> {
            brokenPlayer.broadcastBreakEvent(usedHand);
            ForgeEventFactory.onPlayerDestroyItem(player, shieldStack, usedHand);
            if (player.getUseItem() == shieldStack) {
                player.stopUsingItem();
            }
        });
        if (shieldStack.isEmpty()) {
            player.setItemSlot(resolveEquipmentSlot(usedHand), ItemStack.EMPTY);
            player.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + player.level().random.nextFloat() * 0.4F);
        }
        // hurtAndBreak 内の Unbreaking が消費を打ち消した場合は抑止時間を開始しない。
        if (shieldStack.isEmpty() || shieldStack.getDamageValue() > beforeDamage || shieldStack.getCount() < beforeCount) {
            BulwarkGreatshield.rememberDurabilityConsumed(shieldStack, now);
        }
    }

    private static void applyParrycastBucklerDurability(
            float originalBlockedDamage,
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            boolean perfectGuard
    ) {
        var now = player.level().getGameTime();
        var cost = ParrycastBuckler.resolveDurabilityCost(originalBlockedDamage, perfectGuard);
        if (cost <= 0 || ParrycastBuckler.isDurabilitySuppressed(stack, now)) {
            return;
        }
        var beforeDamage = stack.getDamageValue();
        var beforeCount = stack.getCount();
        stack.hurtAndBreak(cost, player, brokenPlayer -> {
            brokenPlayer.broadcastBreakEvent(hand);
            ForgeEventFactory.onPlayerDestroyItem(player, stack, hand);
            if (player.getUseItem() == stack) player.stopUsingItem();
        });
        if (stack.isEmpty()) {
            player.setItemSlot(resolveEquipmentSlot(hand), ItemStack.EMPTY);
            player.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + player.level().random.nextFloat() * 0.4F);
        } else if (stack.getDamageValue() > beforeDamage || stack.getCount() < beforeCount) {
            ParrycastBuckler.rememberDurabilityConsumed(stack, now);
        }
    }

    private static Vec3 resolveReflectcastEffectPosition(ServerPlayer player, ShieldBlockEvent event) {
        var source = event.getDamageSource();
        var sourceEntity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        if (sourceEntity == null) {
            return player.getBoundingBox().getCenter();
        }

        return midpoint(player, sourceEntity);
    }

    private static Vec3 midpoint(ServerPlayer player, Entity sourceEntity) {
        return player.getBoundingBox().getCenter()
                .add(sourceEntity.getBoundingBox().getCenter())
                .scale(0.5D);
    }
}
