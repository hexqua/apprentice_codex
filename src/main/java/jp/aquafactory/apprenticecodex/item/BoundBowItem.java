package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverBowAmmoResolver;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowClientTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BoundBowItem extends BowItem {
    public static final int DURABILITY = 1561;
    public static final String INSTANCE_ID_TAG = "apprenticecodex:bound_bow_instance_id";
    public static final String SUMMON_DAMAGE_MULTIPLIER_TAG = "apprenticecodex:bound_bow_summon_damage_multiplier";

    public BoundBowItem() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE));
    }

    public static ItemStack create(UUID instanceId, int powerLevel, HolderLookup.Provider registries) {
        return create(instanceId, powerLevel, registries, 1.0F);
    }

    public static ItemStack create(UUID instanceId, int powerLevel, HolderLookup.Provider registries,
                                   float summonDamageMultiplier) {
        var stack = new ItemStack(ItemRegistry.BOUND_BOW.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(INSTANCE_ID_TAG, instanceId);
            tag.putFloat(SUMMON_DAMAGE_MULTIPLIER_TAG, summonDamageMultiplier);
        });
        if (powerLevel > 0) {
            registries.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.POWER)
                    .ifPresent(power -> stack.enchant(power, powerLevel));
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
        CompoundTag tag = getCustomDataTag(stack);
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

    public static float getSummonDamageMultiplier(ItemStack stack) {
        if (!isBoundBow(stack)) {
            return 1.0F;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(SUMMON_DAMAGE_MULTIPLIER_TAG)
                ? tag.getFloat(SUMMON_DAMAGE_MULTIPLIER_TAG)
                : 1.0F;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
                             @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        var canFireWithoutAmmo = player.getAbilities().instabuild
                || hasEnchantment(stack, Enchantments.INFINITY);
        var ammoSource = SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, stack);
        var hasAmmo = ammoSource != null;
        var shouldForgeArrow = !hasAmmo && !canFireWithoutAmmo && canForgeArrow(player);
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(
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
            var arrow = arrowItem.createArrow(level, ammoStack, player, stack);
            arrow = customArrow(arrow, ammoStack, stack);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            if (power == 1.0F) {
                arrow.setCritArrow(true);
            }

            EnchantmentHelper.onProjectileSpawned((ServerLevel) level, stack, arrow, ignored -> {
            });
            arrow.setBaseDamage(arrow.getBaseDamage() * getSummonDamageMultiplier(stack));

            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
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
                || hasEnchantment(stack, Enchantments.INFINITY);
        var canStart = hasAmmo || canFireWithoutAmmo || canForgeArrow(player);
        var nockResult = EventHooks.onArrowNock(stack, level, player, hand, canStart);
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
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull net.minecraft.core.Holder<Enchantment> enchantment) {
        return Items.BOW.supportsEnchantment(new ItemStack(Items.BOW), enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull net.minecraft.core.Holder<Enchantment> enchantment) {
        return Items.BOW.isPrimaryItemFor(new ItemStack(Items.BOW), enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return Items.BOW.isBookEnchantable(new ItemStack(Items.BOW), book);
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
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        if (FMLEnvironment.dist == Dist.CLIENT) {
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

    private static boolean hasEnchantment(ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey) {
        return getEnchantmentLevel(stack, enchantmentKey) > 0;
    }

    private static int getEnchantmentLevel(ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey) {
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

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
