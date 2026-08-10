package jp.aquafactory.apprenticecodex.item.flask;

import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class SpellcastersFlask extends AbstractPotionFlaskItem {
    private static final float BASE_DRINK_DURATION_TICKS = 32.0F;
    private static final float GUZZLE_REDUCTION_PER_LEVEL = 0.1F;
    private static final int GUZZLE_LEVEL_CAP = 9;

    public SpellcastersFlask() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context) {
        var result = super.onItemUseFirst(stack, context);
        if (result.consumesAction()) {
            return result;
        }

        // 錬金醸造台は飲用より汲み取り優先.
        if (context.getLevel().getBlockState(context.getClickedPos()).is(BlockRegistry.ALCHEMY_BREWER.get())) {
            return InteractionResult.PASS;
        }

        var player = context.getPlayer();
        if (player == null || !canConsumeStoredItem(stack)) {
            return InteractionResult.PASS;
        }

        sendForcedNormalDrinkWarning(context.getLevel(), player, stack);
        player.startUsingItem(context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        normalizeStoredDosesToCapacity(stack);
        if (!canConsumeStoredItem(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        sendForcedNormalDrinkWarning(level, player, stack);
        player.startUsingItem(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return Math.max(1, Math.round(BASE_DRINK_DURATION_TICKS * getDrinkDurationMultiplier(stack)));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity livingEntity) {
        normalizeStoredDosesToCapacity(stack);
        var extractedEffects = extractStoredEffects(stack);
        if (extractedEffects.isEmpty()) {
            return stack;
        }

        if (livingEntity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!level.isClientSide) {
            for (var effect : extractedEffects) {
                applyScaledEffect(stack, livingEntity, effect);
            }
            decrementStoredDoseCount(stack, getStoredDoseConsumptionCount(stack));
        }

        if (livingEntity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        livingEntity.gameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    protected boolean isSupportedFlaskEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.GUZZLE)
                || super.isSupportedFlaskEnchantment(enchantment);
    }

    private int getGuzzleLevel(ItemStack stack) {
        return Enchantments.getLevel(stack, Enchantments.GUZZLE);
    }

    private float getDrinkDurationMultiplier(ItemStack stack) {
        var guzzleReduction = Math.min(getGuzzleLevel(stack), GUZZLE_LEVEL_CAP) * GUZZLE_REDUCTION_PER_LEVEL;
        return Math.max(0.1F, 1.0F - guzzleReduction);
    }

    private boolean canConsumeStoredItem(ItemStack stack) {
        return getStoredDoseCount(stack) > 0 && !extractStoredEffects(stack).isEmpty();
    }

    private static void sendForcedNormalDrinkWarning(Level level, Player player, ItemStack stack) {
        if (level.isClientSide || !isStoredVanillaPotionTypeMismatched(stack)) {
            return;
        }

        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.flask_system.drink_with_force_normal",
                getStoredItem(stack).getHoverName()
        ).withStyle(ChatFormatting.YELLOW), true);
    }

    private java.util.List<MobEffectInstance> extractStoredEffects(ItemStack flaskStack) {
        return extractEffectsFromItem(getStoredItem(flaskStack));
    }

    private void applyScaledEffect(ItemStack flaskStack, LivingEntity livingEntity, MobEffectInstance originalEffect) {
        var scaledEffect = scaleEffect(flaskStack, originalEffect);
        if (scaledEffect.getEffect().value().isInstantenous()) {
            // 即時効果は addEffect では発火しないため、PotionItem 相当の経路で適用する。
            scaledEffect.getEffect().value().applyInstantenousEffect(
                    livingEntity,
                    livingEntity,
                    livingEntity,
                    scaledEffect.getAmplifier(),
                    1.0D
            );
            return;
        }

        livingEntity.addEffect(scaledEffect);
    }
}
