package jp.aquafactory.apprenticecodex.item.manaforcereleasedevice;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaForceReleaseDeviceServerConfig.Values;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ManaForceReleaseDeviceRenderer;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public final class ManaForceReleaseDevice extends Item implements GeoItem, WisdomPolicy {
    public static final int CHARGE_TICKS = 40;
    public static final int COOLDOWN_TICKS = 40;
    public static final int REQUIRED_MANA_PERCENT = 20;
    private static final String CONTROLLER = "device";
    private final ManaForceReleaseTier tier;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ManaForceReleaseDevice(ManaForceReleaseTier tier) {
        super(new Properties().durability(tier.durability).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, tier.attackDamage - 1,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
        this.tier = tier;
        GeoItem.registerSyncedAnimatable(this);
    }

    public ManaForceReleaseTier tier() { return tier; }

    public static double requiredMana(Player player) {
        return player.getAttributeValue(AttributeRegistry.MAX_MANA) * REQUIRED_MANA_PERCENT / 100.0;
    }

    public static boolean hasRequiredMana(Player player, double mana) {
        return Double.isFinite(mana) && mana >= requiredMana(player) && mana > 0;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                         @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            error(player, "cannot_use_offhand", stack.getHoverName());
            return InteractionResultHolder.fail(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)
                || (!level.isClientSide && MagicData.getPlayerMagicData(player).isCasting())) {
            return InteractionResultHolder.fail(stack);
        }
        double mana = level.isClientSide ? ClientMagicData.getPlayerMana() : MagicData.getPlayerMagicData(player).getMana();
        if (!hasRequiredMana(player, mana)) {
            error(player, "insufficient_mana", Utils.stringTruncation(requiredMana(player), 2));
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel serverLevel) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), CONTROLLER, "charge");
            level.playSound(null, player, SoundRegistry.FORCE_RELEASE_CHARGE.get(), SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void error(Player player, String suffix, Object... args) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("ui.apprenticecodex.mana_force_release_device." + suffix,
                    args).withStyle(ChatFormatting.RED), true);
        }
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player)) return;
        var data = MagicData.getPlayerMagicData(player);
        if (data.isCasting()) {
            player.stopUsingItem();
        } else if (!hasRequiredMana(player, data.getMana())) {
            error(player, "cancel_by_insufficient_mana");
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            player.stopUsingItem();
        }
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                             @NotNull LivingEntity entity) {
        if (entity instanceof ServerPlayer player && player.getMainHandItem() == stack
                && !MagicData.getPlayerMagicData(player).isCasting()
                && hasRequiredMana(player, MagicData.getPlayerMagicData(player).getMana())) {
            fire(player, stack);
        }
        return stack;
    }

    private void fire(ServerPlayer player, ItemStack stack) {
        var level = player.serverLevel();
        var data = MagicData.getPlayerMagicData(player);
        float mana = data.getMana();
        // 発射時の全量を確定してから命中処理へ進む。命中時の外部MODによる回復は妨げない。
        data.setMana(0);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        stopTriggeredAnim(player, GeoItem.getOrAssignId(stack, level), CONTROLLER, "charge");
        triggerAnim(player, GeoItem.getId(stack), CONTROLLER, "release");

        var hit = RaycastTools.raycastFromEyeWithAimAssist(player, tier.range, 1,
                target -> CombatTools.isValidCombatTarget(target, player));
        if (hit.hitEntity() == null) {
            hit = RaycastTools.raycastFromEyeWithAimAssist(player, tier.range, 4,
                    target -> CombatTools.isValidCombatTarget(target, player));
        }
        var end = hit.hitPosition();
        if (hit.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(hit.hitEntity());
            float damage = damage(mana, player.getAttributeValue(AttributeRegistry.SPELL_POWER),
                    ApprenticeCodexServerConfig.manaForceReleaseDeviceConfig().values());
            if (target instanceof LivingEntity living) {
                damage *= resistanceMultiplier(living.getAttributeValue(AttributeRegistry.SPELL_RESIST));
            }
            // 装置攻撃はschoolなし。攻撃エンチャントは使わず、手持ち効果とReplenishingは許容する。
            CombatTools.applyUnscaledDamage(target, damage,
                    CombatTools.getDamageSource(level, player, DamageTypes.MANA_FORCE_RELEASE_DEVICE),
                    CombatTools.KnockbackTypes.DEFAULT);
        }
        var bolt = EntityRegistry.MANA_FORCE_RELEASE_BOLT.get().create(level);
        if (bolt != null) {
            bolt.setPos(player.getEyePosition().add(player.getLookAngle().scale(0.55)).add(0, -0.22, 0));
            bolt.setup(end, 10, level.random.nextInt(), hit.hitEntity() != null);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, player, SoundRegistry.FORCE_RELEASE_FIRE.get(), SoundSource.PLAYERS, 1, 1);
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    public float damage(double mana, double spellPower, Values values) {
        return (float) (tier.baseDamage(mana, values.softCap(tier)) * spellPower * values.multiplier(tier));
    }

    public float resistanceMultiplier(double resist) {
        return 2 - (float) Utils.softCapFormula(tier == ManaForceReleaseTier.MASTER ? Math.min(resist, 1) : resist);
    }

    @Override
    public void onStopUsing(@NotNull ItemStack stack, @NotNull LivingEntity entity, int remaining) {
        if (entity.level() instanceof ServerLevel level) {
            stopTriggeredAnim(entity, GeoItem.getOrAssignId(stack, level), CONTROLLER, "charge");
        }
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) { return CHARGE_TICKS; }
    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.BOW; }
    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }
    @Override
    public void postHurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }
    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility ability) {
        return ability == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, ability);
    }
    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack ingredient) {
        return ingredient.is(ItemRegistry.ARCANE_ESSENCE.get());
    }
    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) { return tier.enchantability; }
    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        var id = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return new ItemStack(Items.DIAMOND_SWORD).supportsEnchantment(enchantment)
                || enchantment.is(Enchantments.WISDOM)
                || MalumCompatibility.isSpiritPlunderSupported(stack, id)
                || MalumCompatibility.isMagicCapableWeaponEnchantment(stack, id)
                || MalumCompatibility.isReplenishingEnchantment(id);
    }
    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return super.isBookEnchantable(stack, book) && EnchantmentHelper.getEnchantmentsForCrafting(book).keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }
    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        if (FMLEnvironment.dist == Dist.CLIENT) ManaForceReleaseDeviceTooltip.append(this, lines);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, CONTROLLER, 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle")))
                .triggerableAnim("charge", RawAnimation.begin().thenPlayAndHold("charge"))
                .triggerableAnim("release", RawAnimation.begin().thenPlay("release")));
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override
    public double getBoneResetTime() {
        // idleに含まれない顎なども、中断時にはGeckoLib既定の5tick補間を挟まず元へ戻す。
        return 0;
    }
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ManaForceReleaseDeviceRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new ManaForceReleaseDeviceRenderer();
                return renderer;
            }
        });
    }
}
