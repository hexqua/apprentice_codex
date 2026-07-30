package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverBowAmmoResolver;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public abstract class BowItemSpellcasterQuiverMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$useSpellcasterQuiverAmmo(Level level, Player player, InteractionHand hand,
                                                          CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var bowStack = player.getItemInHand(hand);
        if (!SpellcasterQuiverBowAmmoResolver.hasSupportedQuiverAmmo(player, bowStack)) {
            return;
        }

        var ammoSource = SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, bowStack);
        var hasAmmo = ammoSource != null;
        var nockResult = EventHooks.onArrowNock(bowStack, level, player, hand, hasAmmo);
        if (nockResult != null) {
            cir.setReturnValue(nockResult);
            return;
        }

        if (!player.getAbilities().instabuild && !hasAmmo) {
            cir.setReturnValue(InteractionResultHolder.fail(bowStack));
            return;
        }

        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.consume(bowStack));
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$releaseSpellcasterQuiverAmmo(ItemStack bowStack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        if (!(entity instanceof Player player) || !SpellcasterQuiverBowAmmoResolver.hasSupportedQuiverAmmo(player, bowStack)) {
            return;
        }

        var ammoSource = SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, bowStack);
        var canFireWithoutAmmo = player.getAbilities().instabuild
                || apprentice_codex$getEnchantmentLevel(bowStack, Enchantments.INFINITY) > 0;
        var drawDuration = bowStack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(bowStack, level, player, drawDuration, ammoSource != null || canFireWithoutAmmo);
        if (drawDuration < 0) {
            ci.cancel();
            return;
        }

        if (ammoSource == null && !canFireWithoutAmmo) {
            ci.cancel();
            return;
        }

        var ammoStack = ammoSource != null ? ammoSource.stack() : new ItemStack(Items.ARROW);
        var infiniteAmmo = ammoSource == null
                || player.getAbilities().instabuild
                || ammoSource.isInfinite(bowStack, player);
        var power = BowItem.getPowerForTime(drawDuration);
        if (power < 0.1F) {
            ci.cancel();
            return;
        }

        var bowItem = (BowItem) (Object) this;
        if (!level.isClientSide) {
            var arrowItem = (ArrowItem) (ammoStack.getItem() instanceof ArrowItem ? ammoStack.getItem() : Items.ARROW);
            var arrow = arrowItem.createArrow(level, ammoStack, player, bowStack);
            arrow = bowItem.customArrow(arrow, ammoStack, bowStack);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            if (power == 1.0F) {
                arrow.setCritArrow(true);
            }

            bowStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (infiniteAmmo || player.getAbilities().instabuild && (ammoStack.is(Items.SPECTRAL_ARROW) || ammoStack.is(Items.TIPPED_ARROW))) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            level.addFreshEntity(arrow);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (!infiniteAmmo && !player.getAbilities().instabuild && ammoSource != null) {
            ammoSource.consume();
        }

        player.awardStat(Stats.ITEM_USED.get(bowItem));
        ci.cancel();
    }

    @Unique
    private static int apprentice_codex$getEnchantmentLevel(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return 0;
        }

        for (var holder : enchantments.keySet()) {
            if (holder.is(enchantmentKey)) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }
}
