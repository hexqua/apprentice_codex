package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverBowAmmoResolver;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowClientTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BoundBowItem extends BowItem {
    public static final int DURABILITY = 1561;
    public static final String INSTANCE_ID_TAG = "apprenticecodex:bound_bow_instance_id";

    public BoundBowItem() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE));
    }

    public static ItemStack create(UUID instanceId, int powerLevel) {
        var stack = new ItemStack(ItemRegistry.BOUND_BOW.get());
        stack.getOrCreateTag().putUUID(INSTANCE_ID_TAG, instanceId);
        if (powerLevel > 0) {
            stack.enchant(Enchantments.POWER_ARROWS, powerLevel);
        }
        return stack;
    }

    public static boolean isBoundBow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BoundBowItem;
    }

    public static boolean isGeneratedBoundBow(ItemStack stack) {
        return isBoundBow(stack) && getInstanceId(stack).isPresent();
    }

    public static Optional<UUID> getInstanceId(ItemStack stack) {
        if (!isBoundBow(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(INSTANCE_ID_TAG)
                ? Optional.of(tag.getUUID(INSTANCE_ID_TAG))
                : Optional.empty();
    }

    public static boolean hasInstanceId(ItemStack stack, @Nullable UUID instanceId) {
        if (instanceId == null) {
            return false;
        }
        return getInstanceId(stack).map(instanceId::equals).orElse(false);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
                             @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        var canFireWithoutAmmo = player.getAbilities().instabuild
                || stack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
        var ammoSource = SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, stack);
        var hasAmmo = ammoSource != null;
        var shouldForgeArrow = !hasAmmo && !canFireWithoutAmmo && canForgeArrow(player);
        var drawDuration = getUseDuration(stack) - timeLeft;
        drawDuration = ForgeEventFactory.onArrowLoose(
                stack,
                level,
                player,
                drawDuration,
                hasAmmo || canFireWithoutAmmo || shouldForgeArrow
        );
        if (drawDuration < 0 || (!hasAmmo && !canFireWithoutAmmo && !shouldForgeArrow)) {
            return;
        }

        var ammoStack = hasAmmo ? ammoSource.stack() : new ItemStack(Items.ARROW);

        var power = getPowerForTime(drawDuration);
        if (power < 0.1F) {
            return;
        }

        var infiniteAmmo = ammoSource != null && ammoSource.isInfinite(stack, player)
                || player.getAbilities().instabuild
                || ammoStack.getItem() instanceof ArrowItem infiniteArrowItem
                && infiniteArrowItem.isInfinite(ammoStack, stack, player);
        if (!level.isClientSide) {
            var arrowItem = ammoStack.getItem() instanceof ArrowItem resolvedArrowItem
                    ? resolvedArrowItem
                    : (ArrowItem) Items.ARROW;
            var arrow = arrowItem.createArrow(level, ammoStack, player);
            arrow = customArrow(arrow);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            if (power == 1.0F) {
                arrow.setCritArrow(true);
            }

            var powerLevel = stack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
            if (powerLevel > 0) {
                arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5D + 0.5D);
            }

            var punchLevel = stack.getEnchantmentLevel(Enchantments.PUNCH_ARROWS);
            if (punchLevel > 0) {
                arrow.setKnockback(punchLevel);
            }

            if (stack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0) {
                arrow.setSecondsOnFire(100);
            }

            stack.hurtAndBreak(1, player, bowUser -> bowUser.broadcastBreakEvent(player.getUsedItemHand()));
            if (infiniteAmmo || shouldForgeArrow
                    || player.getAbilities().instabuild && (ammoStack.is(Items.SPECTRAL_ARROW) || ammoStack.is(Items.TIPPED_ARROW))) {
                // マナから錬成した矢を拾えるとアイテム生成になってしまうため、弾道だけ通常矢として扱う。
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            if (shouldForgeArrow) {
                consumeForgeArrowMana(player);
            }
            level.addFreshEntity(arrow);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (!infiniteAmmo && !player.getAbilities().instabuild && ammoSource != null) {
            ammoSource.consume();
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var hasAmmo = SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, stack) != null;
        var canFireWithoutAmmo = player.getAbilities().instabuild
                || stack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
        var canStart = hasAmmo || canFireWithoutAmmo || canForgeArrow(player);
        var nockResult = ForgeEventFactory.onArrowNock(stack, level, player, hand, canStart);
        if (nockResult != null) {
            return nockResult;
        }

        if (!canStart) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return Items.BOW.canApplyAtEnchantingTable(new ItemStack(Items.BOW), enchantment);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return Items.BOW.getEnchantmentValue(new ItemStack(Items.BOW));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

        BoundBowClientTooltip.getStoredItemName(stack).ifPresent(storedItemName -> {
            lines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".bound_weapon.contain_item.item",
                    storedItemName
            ).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(
                    "item." + ApprenticeCodex.MODID + ".bound_weapon.contain_item.hint"
            ).withStyle(ChatFormatting.DARK_GRAY));
        });
    }

    private static boolean canForgeArrow(Player player) {
        var manaCost = ApprenticeCodexServerConfig.boundBowForgeArrowManaCost();
        if (manaCost <= 0.0F) {
            return true;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        return magicData != null && magicData.getMana() + 0.0001F >= manaCost;
    }

    private static void consumeForgeArrowMana(Player player) {
        var manaCost = ApprenticeCodexServerConfig.boundBowForgeArrowManaCost();
        if (manaCost <= 0.0F) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }
}
