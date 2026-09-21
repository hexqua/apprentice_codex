package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceHelper;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceTarget;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.event.client.FullautoRapidcastSpellrifleClientLookup;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.ammo.EmptyCasingReturnPolicy;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.ImmediateSneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.SneakSelectionView;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.spellgun.RifleSpellTooltipClientHelper;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunSpellListManager;
import jp.aquafactory.apprenticecodex.item.StoredSpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellrifleMuzzleParticles;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class FullautoRapidcastSpellrifle extends Item
        implements GeoItem, NonDamageableAnvilMergeItem, IJeiInfoItem, CastAnimationOverrideItem,
        AttributeEnchantmentPolicy, WisdomPolicy, PlunderTarget, TranscendenceTarget,
        StoredSpellCalibrationImbueTarget, SpellCalibrationAdjustmentTarget, ImmediateSneakSelectionUiItem {
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final HolderLookup.Provider FALLBACK_LOOKUP = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final CalibrationAdjustmentProfile CALIBRATION_PROFILE = CalibrationAdjustmentProfile.of(
            CalibrationAdjustmentRule.unique("silver_ring", FullautoRapidcastSpellrifle::isSilverRing,
                    CalibrationAdjustmentHints.silverRing()).withEffectLines(CalibrationAdjustmentEffects.addLongSupport()),
            CalibrationAdjustmentRule.unique("recovery_rune", FullautoRapidcastSpellrifle::isRecoveryRune,
                            CalibrationAdjustmentHint.specificItem(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE))
                    .withEffectLines(CalibrationAdjustmentEffects.removeRecoil()),
            CalibrationAdjustmentRule.repeatable("slot_upgrade", FullautoRapidcastSpellrifle::isSlotUpgrade,
                            CalibrationAdjustmentHint.specificItem(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE))
                    .withEffectLines(CalibrationAdjustmentEffects.addScrollSlot(1))
    );
    public static final String ECHO_ADJUSTMENT_ID = "echo_cast";
    private static final CalibrationAdjustmentRule ECHO_RULE = CalibrationAdjustmentRule.unique(
            ECHO_ADJUSTMENT_ID, candidate -> candidate.is(ItemRegistry.MULTICAST_ECHO_STAFF.get()),
            CalibrationAdjustmentHint.specificItem(ItemRegistry.MULTICAST_ECHO_STAFF))
            .withEffectLines(() -> CalibrationAdjustmentEffects.gainEchoCast(FullautoEchoCasting.manaMultiplierForCurrentThread()));
    private static final CalibrationAdjustmentProfile ECHO_PROFILE = CalibrationAdjustmentProfile.of(
            Stream.concat(CALIBRATION_PROFILE.rules().stream(), Stream.of(ECHO_RULE))
                    .toArray(CalibrationAdjustmentRule[]::new));
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.fullauto_rapidcast_spellrifle.desc_";
    private static final String MAIN_CONTROLLER = "main";
    private static final String FIRED_ANIMATION = "fired";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_FIRED = RawAnimation.begin().thenPlay("fired");
    private static final int MAX_USE_DURATION = 72000;
    private static final float ADS_FOV_MODIFIER = 0.85F;
    private static final int MUZZLE_RHOMBUS_COUNT = 4;
    private static final int MUZZLE_SPARK_COUNT = 7;
    private static final int MUZZLE_RHOMBUS_WHITEN_TICKS = 2;
    private static final int MUZZLE_SPARK_WHITEN_TICKS = 3;
    private static final int MUZZLE_RHOMBUS_LIFETIME = 8;
    private static final int MUZZLE_SPARK_LIFETIME = 10;
    private static final int ENCHANTMENT_VALUE = 15;
    private static final Set<AttributeEnchantmentType> DIRECT_ATTRIBUTE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.TENSE
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public FullautoRapidcastSpellrifle() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return DIRECT_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            if (player instanceof ServerPlayer serverPlayer) {
                sendActionBarError(serverPlayer, Component.translatable("ui.apprenticecodex.common.cannot_use_offhand", stack.getHoverName()));
            }
            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return buildMainhandModifiers(stack);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return isSupportedStaffrifleEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }).triggerableAnim(FIRED_ANIMATION, ANIM_FIRED));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    public boolean tryTriggerSelectedSpell(ServerPlayer player, boolean adsFullAuto) {
        return tryTriggerSelectedSpell(player, adsFullAuto, null);
    }

    public boolean tryTriggerSelectedSpell(ServerPlayer player, boolean adsFullAuto, @Nullable BlockTargetData targetData) {
        var stack = player.getMainHandItem();
        if (stack.isEmpty() || stack.getItem() != this) {
            return false;
        }

        var spellData = getSelectedSpellData(stack, player.level().registryAccess());
        // 使用不可の魔法も注入済みとして扱い、未注入とは異なるエラーを表示する。
        if (spellData == SpellData.EMPTY || spellData.getSpell() == SpellRegistry.none()) {
            var lookup = player.level().registryAccess();
            for (var slot = 0; slot < getEnabledCalibrationScrollSlotCount(stack, lookup); slot++) {
                var stored = FullautoRapidcastSpellrifleScrollStorage.spell(stack, slot, lookup);
                if (stored != SpellData.EMPTY && stored.getSpell() != SpellRegistry.none()) {
                    spellData = stored;
                    break;
                }
            }
        }
        if (spellData == SpellData.EMPTY || spellData.getSpell() == SpellRegistry.none()) {
            sendActionBarError(player, Component.translatable("ui.apprenticecodex.spellgun.not_imbued", stack.getHoverName()));
            return false;
        }

        var spell = spellData.getSpell();
        if (!canCastSpell(stack, spell, player.level().registryAccess())) {
            sendActionBarError(player, Component.translatable(
                    "ui.apprenticecodex.fullauto_rapidcast_spellrifle.cannot_cast",
                    spell.getDisplayName(player)
            ));
            return false;
        }

        if (isSpecialCastSpellDenied(spell)) {
            sendActionBarError(player, Component.translatable(
                    "ui.apprenticecodex.fullauto_rapidcast_spellrifle.deny_list",
                    spell.getDisplayName(player)
            ));
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var spellLevel = spell.getLevelFor(resolveImbuedSpellLevel(stack, spellData), player);
        var recast = magicData != null && magicData.getPlayerRecasts().hasRecastForSpell(spell);
        if (!player.isCreative() && !recast && !SpellGunCastEvent.hasAmmo(player, player.getInventory(), getAmmoItem(stack))) {
            sendActionBarError(player, Component.translatable(
                    "ui.apprenticecodex.spellgun.missing_ammo",
                    getAmmoItem(stack).getDescription()
            ));
            return false;
        }

        if (!canAttemptSpecialCast(player)) {
            return false;
        }

        if (targetData != null) {
            BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);
        }

        boolean casted;
        try {
            try (var ignored = FullautoRapidcastSpellrifleCastContext.open(player.getUUID(), stack, spell, recast);
                 var attackScope = FullautoEchoCasting.openAttackScope(player, stack, spell)) {
                casted = spell.attemptInitiateCast(
                        stack,
                        spellLevel,
                        player.level(),
                        player,
                        CastSource.SWORD,
                        true,
                        SpellSelectionManager.MAINHAND
                );
                if (casted) {
                    TriggeredSpellCastHelper.applyLongCastDurationOverride(
                            player,
                            spellLevel,
                            spell,
                            magicData,
                            SpellSelectionManager.MAINHAND,
                            0
                    );
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Fullauto Rapidcast Spellrifle special cast context failed to close.", exception);
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(player);
        }

        if (!casted) {
            return false;
        }

        FullautoRapidcastSpellrifleCastContext.rememberPending(player.getUUID(), stack, spell, recast, player.level().getGameTime());
        playSuccessfulFireEffects(player, spell, adsFullAuto);
        triggerFiredAnimation(player, stack);
        return true;
    }

    public Item getAmmoItem(ItemStack stack) {
        return ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get();
    }

    public static boolean isSpecialCastSpellDenied(@Nullable AbstractSpell spell) {
        return SpellGunSpellListManager.isDenylisted(spell)
                || (spell != null
                && ApprenticeCodexServerConfig.isFullautoRapidcastSpellrifleSpellDenied(spell.getSpellResource()));
    }

    public Item getDisplayedAmmoItem(ItemStack stack) {
        return getAmmoItem(stack);
    }

    public float resolveEmptyCasingReturnChance(Player player) {
        return EmptyCasingReturnPolicy.resolveReturnChance(player);
    }

    public boolean shouldReturnEmptyCasing(Player player) {
        return EmptyCasingReturnPolicy.shouldReturnEmptyCasing(player);
    }

    public int resolveSpecialCooldownTicks(int baseCooldownTicks, int effectiveCooldownTicks, int castTimeTicks) {
        return FullautoCooldownPolicy.resolve(baseCooldownTicks, effectiveCooldownTicks, castTimeTicks);
    }

    public static boolean isAdsUse(@Nullable LivingEntity entity) {
        if (entity == null || !entity.isUsingItem() || entity.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        var useItem = entity.getUseItem();
        return !useItem.isEmpty() && useItem.getItem() instanceof FullautoRapidcastSpellrifle;
    }

    public static float getAdsFovModifier() {
        return ADS_FOV_MODIFIER;
    }

    public void triggerFiredAnimation(ServerPlayer serverPlayer, ItemStack stack) {
        var instanceId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        triggerAnim(serverPlayer, instanceId, MAIN_CONTROLLER, FIRED_ANIMATION);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        appendFullautoRapidcastSpellrifleHelpTooltip(stack, lines);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RifleSpellTooltipClientHelper.append(stack, lines);
        }
    }

    private static void playSuccessfulFireEffects(ServerPlayer player, AbstractSpell spell, boolean adsFullAuto) {
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundRegistry.STAFFRIFLE.get(),
                SoundSource.PLAYERS,
                0.9F,
                0.96F + player.getRandom().nextFloat() * 0.08F
        );

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var color = MagicTools.resolveSchoolTintColor(spell.getSchoolType());
        var red = ((color >> 16) & 0xFF) / 255.0F;
        var green = ((color >> 8) & 0xFF) / 255.0F;
        var blue = (color & 0xFF) / 255.0F;
        var muzzlePosition = resolveMuzzlePosition(player, adsFullAuto);
        var look = player.getLookAngle().normalize();
        spawnMuzzleFlashParticles(player, muzzlePosition, look, red, green, blue);
    }

    private static Vec3 resolveMuzzlePosition(ServerPlayer player, boolean adsFullAuto) {
        var look = player.getLookAngle().normalize();
        var right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F).normalize();
        var side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0D : -1.0D;
        var sideOffset = adsFullAuto ? 0.0D : 0.22D * side;
        var downOffset = adsFullAuto ? -0.37D : -0.43D;
        return player.getEyePosition()
                .add(look.scale(0.95D))
                .add(right.scale(sideOffset))
                .add(0.0D, downOffset, 0.0D);
    }

    private static void spawnMuzzleFlashParticles(ServerPlayer player, Vec3 center, Vec3 look,
                                                  float red, float green, float blue) {
        var random = player.serverLevel().getRandom();
        for (var i = 0; i < MUZZLE_RHOMBUS_COUNT; ++i) {
            var size = Mth.lerp(random.nextFloat(), 0.16F, 0.28F);
            var position = center.add(createMuzzleParticleOffset(random, look, 0.08D));
            var velocity = look.scale(Mth.lerp(random.nextFloat(), 0.03D, 0.08D));
            SpellrifleMuzzleParticles.send(player,
                    createMuzzleRhombusOptions(size, red, green, blue),
                    position.x,
                    position.y,
                    position.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    0.0D
            );
        }

        for (var i = 0; i < MUZZLE_SPARK_COUNT; ++i) {
            var size = Mth.lerp(random.nextFloat(), 0.06F, 0.12F);
            var position = center.add(createMuzzleParticleOffset(random, look, 0.14D));
            var velocity = look.scale(Mth.lerp(random.nextFloat(), 0.05D, 0.13D))
                    .add(createRandomSpread(random, 0.035D));
            SpellrifleMuzzleParticles.send(player,
                    createMuzzleSparkOptions(size, red, green, blue),
                    position.x,
                    position.y,
                    position.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    0.0D
            );
        }
    }

    private static Vec3 createMuzzleParticleOffset(RandomSource random, Vec3 look, double radius) {
        return look.scale(random.nextDouble() * 0.08D).add(createRandomSpread(random, radius));
    }

    private static Vec3 createRandomSpread(RandomSource random, double radius) {
        return new Vec3(
                (random.nextDouble() - 0.5D) * radius,
                (random.nextDouble() - 0.5D) * radius,
                (random.nextDouble() - 0.5D) * radius
        );
    }

    private static AdditiveGlowParticleOptions createMuzzleRhombusOptions(float size, float red, float green, float blue) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                size,
                red,
                green,
                blue,
                MUZZLE_RHOMBUS_WHITEN_TICKS,
                MUZZLE_RHOMBUS_LIFETIME,
                2,
                0.78F,
                1.16F,
                0.82F,
                1.0F,
                0.02F,
                0.62F,
                0.55F,
                true
        );
    }

    private static AdditiveGlowParticleOptions createMuzzleSparkOptions(float size, float red, float green, float blue) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                size,
                red,
                green,
                blue,
                MUZZLE_SPARK_WHITEN_TICKS,
                MUZZLE_SPARK_LIFETIME,
                3,
                0.9F,
                1.35F,
                0.86F,
                1.0F,
                0.04F,
                0.68F,
                0.62F,
                true
        );
    }

    private static boolean canAttemptSpecialCast(ServerPlayer player) {
        return FullautoRapidcastSpellrifleRateLimiter.canAttemptSpecialCast(player);
    }

    private static void sendActionBarError(ServerPlayer player, Component component) {
        player.connection.send(new ClientboundSetActionBarTextPacket(component.copy().withStyle(ChatFormatting.RED)));
    }

    private void appendFullautoRapidcastSpellrifleHelpTooltip(ItemStack stack, List<Component> lines) {
        appendFullautoRapidcastSpellrifleDescription(stack, lines);
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectFullautoRapidcastSpellrifleAbilityTooltipSection(stack),
                "item.apprenticecodex.spellgun.tooltip.ability_title",
                "item.apprenticecodex.spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                getImbueRestrictionTooltipLines(stack),
                "item.apprenticecodex.spellgun.tooltip.restrict_title",
                "item.apprenticecodex.spellgun.tooltip.restrict_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(ImbueTooltipHelper.createAmmoTooltipLine(getDisplayedAmmoItem(stack), null)),
                "item.apprenticecodex.spellgun.tooltip.ammo_title",
                "item.apprenticecodex.spellgun.tooltip.ammo_none"
        );
    }

    private static List<Component> collectFullautoRapidcastSpellrifleAbilityTooltipSection(ItemStack stack) {
        var translatedLines = new ArrayList<Component>();
        if (FullautoEchoCasting.enabledForCurrentThread() && FullautoEchoCasting.hasStaff(stack, serializationLookup())) {
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item.apprenticecodex.spellgun.tooltip.ability_mana_penalty",
                    Math.round(FullautoEchoCasting.manaMultiplierForCurrentThread() * 100) + "%"));
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item.apprenticecodex.spellgun.tooltip.ability_adapt_attack_echo"));
        }
        if (hasSilverRing(stack, serializationLookup())) {
            translatedLines.add(ImbueTooltipHelper.translatableGray("item.apprenticecodex.spellgun.tooltip.ability_long_to_instant"));
            translatedLines.add(ImbueTooltipHelper.translatableGray("item.apprenticecodex.spellgun.tooltip.ability_extend_cooldown"));
        }
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item.apprenticecodex.spellgun.tooltip.ability_skip_cooldown",
                ImbueTooltipHelper.formatTooltipSeconds(ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleCooldownBypassThresholdTicks())
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item.apprenticecodex.spellgun.tooltip.ability_subtract_cooldown",
                ImbueTooltipHelper.formatTooltipSeconds(ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleCooldownReductionTicks()),
                ImbueTooltipHelper.formatTooltipSeconds(ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleReducedCooldownMinimumTicks())
        ));
        return translatedLines;
    }

    private static void appendFullautoRapidcastSpellrifleDescription(ItemStack stack, List<Component> lines) {
        lines.add(Component.translatable(
                "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_1",
                ImbueTooltipHelper.getAttackKeyName()
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                hasRecoveryRune(stack, serializationLookup())
                        ? "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_2.no_recoil"
                        : "item.apprenticecodex.fullauto_rapidcast_spellrifle.desc_2",
                ImbueTooltipHelper.getUseKeyName()
        ).withStyle(ChatFormatting.GRAY));
    }

    private ItemAttributeModifiers buildMainhandModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return ItemAttributeModifiers.EMPTY;
        }

        var base = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        var merged = AttributeEnchantmentResolver.resolveMergedModifiers(
                base.build(),
                stack,
                "fullauto_rapidcast_spellrifle_mainhand"
        );
        var result = ItemAttributeModifiers.builder();
        for (var entry : merged.entries()) {
            result.add(entry.getKey(), entry.getValue(), EquipmentSlotGroup.MAINHAND);
        }
        return result.build();
    }

    private static boolean isSupportedStaffrifleEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }

        return enchantment.is(Enchantments.ALACRITY)
                || enchantment.is(Enchantments.REFLUX)
                || enchantment.is(Enchantments.RESERVOIR)
                || enchantment.is(Enchantments.TRANSCENDENCE)
                || enchantment.is(Enchantments.TENSE)
                || enchantment.is(Enchantments.WISDOM)
                || enchantment.is(Enchantments.PLUNDER);
    }

    public static int resolveImbuedSpellLevel(ItemStack stack, SpellData spellData) {
        return TranscendenceHelper.resolveScrollSpellLevel(stack, spellData.getLevel());
    }

    public static boolean isSilverRing(ItemStack stack) {
        return stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get());
    }

    private static boolean isRecoveryRune(ItemStack stack) {
        return stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get());
    }

    public static boolean hasRecoveryRune(ItemStack stack, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof FullautoRapidcastSpellrifle)) return false;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; slot++) {
            if (isRecoveryRune(CalibrationAdjustmentStorage.get(stack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, lookup))) {
                return true;
            }
        }
        return false;
    }

    private static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess()
                : FMLEnvironment.dist == Dist.CLIENT ? FullautoRapidcastSpellrifleClientLookup.lookup() : FALLBACK_LOOKUP;
    }

    public static boolean isSlotUpgrade(ItemStack stack) {
        return stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
    }

    public static boolean hasSilverRing(ItemStack stack, HolderLookup.Provider lookup) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; slot++) {
            if (isSilverRing(CalibrationAdjustmentStorage.get(stack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, lookup)))
                return true;
        }
        return false;
    }

    public static int getEnabledCalibrationScrollSlotCount(ItemStack stack, HolderLookup.Provider lookup) {
        var count = 1;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; slot++) {
            if (isSlotUpgrade(CalibrationAdjustmentStorage.get(stack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, lookup)))
                count++;
        }
        return count;
    }

    public static boolean canCastSpell(ItemStack stack, AbstractSpell spell, HolderLookup.Provider lookup) {
        return spell != null && spell != SpellRegistry.none() && spell.isEnabled()
                && !isSpecialCastSpellDenied(spell)
                && (spell.getCastType() == CastType.INSTANT
                || spell.getCastType() == CastType.LONG && hasSilverRing(stack, lookup));
    }

    @Override
    public boolean acceptsCalibrationSpell(@NotNull SpellData data) {
        return SpellCalibrationImbueTarget.acceptsInstantOrLong(data);
    }

    @Override
    public boolean isCalibrationSlotAvailable(@NotNull ItemStack stack, int slot) {
        return isCalibrationSlotAvailable(stack, slot, serializationLookup());
    }

    @Override
    public boolean isCalibrationSlotAvailable(@NotNull ItemStack stack, int slot, HolderLookup.@NotNull Provider lookup) {
        return stack.getItem() == this && slot >= 0 && slot < getEnabledCalibrationScrollSlotCount(stack, lookup);
    }

    @Override
    public boolean isCalibrationSpellUsable(@NotNull ItemStack stack, @NotNull SpellData data) {
        return isCalibrationSpellUsable(stack, data, serializationLookup());
    }

    @Override
    public boolean isCalibrationSpellUsable(@NotNull ItemStack stack, @NotNull SpellData data,
                                            HolderLookup.@NotNull Provider lookup) {
        return canCastSpell(stack, data.getSpell(), lookup);
    }

    @Override
    public boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack stack) {
        for (var slot = 0; slot < FullautoRapidcastSpellrifleScrollStorage.MAX_SCROLL_SLOTS; slot++) {
            if (!FullautoRapidcastSpellrifleScrollStorage.get(stack, slot, serializationLookup()).isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack stack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack stack) {
        return FullautoEchoCasting.enabledForCurrentThread() ? ECHO_PROFILE : CALIBRATION_PROFILE;
    }

    @Override
    public void onCalibrationAdjustmentsChanged(@NotNull ItemStack stack, HolderLookup.@NotNull Provider lookup) {
        normalizeSelectedScrollIndex(stack, lookup);
    }

    public int normalizeSelectedScrollIndex(ItemStack stack, HolderLookup.Provider lookup) {
        var selected = FullautoRapidcastSpellrifleScrollStorage.selected(stack);
        if (isSelectable(stack, selected, lookup)) return selected;
        selected = -1;
        for (var slot = 0; slot < getEnabledCalibrationScrollSlotCount(stack, lookup); slot++) {
            if (isSelectable(stack, slot, lookup)) {
                selected = slot;
                break;
            }
        }
        FullautoRapidcastSpellrifleScrollStorage.select(stack, selected);
        return selected;
    }

    private boolean isSelectable(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        return evaluateCalibrationImbue(stack, slot,
                FullautoRapidcastSpellrifleScrollStorage.spell(stack, slot, lookup), lookup).isUsable();
    }

    public static SpellData getSelectedSpellData(ItemStack stack, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof FullautoRapidcastSpellrifle rifle)) return SpellData.EMPTY;
        return FullautoRapidcastSpellrifleScrollStorage.spell(stack, rifle.normalizeSelectedScrollIndex(stack, lookup), lookup);
    }

    @Override
    public boolean isSneakSelectionUiEnabled(ItemStack stack) {
        return getEnabledCalibrationScrollSlotCount(stack, serializationLookup()) >= 2;
    }

    @Override
    public ItemStack resolveSneakSelectionStack(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : ItemStack.EMPTY;
    }

    @Override
    public List<SneakSelectionView> getSneakSelectionViews(ItemStack stack) {
        var lookup = serializationLookup();
        var views = new ArrayList<SneakSelectionView>();
        for (var slot = 0; slot < getEnabledCalibrationScrollSlotCount(stack, lookup); slot++) {
            var data = FullautoRapidcastSpellrifleScrollStorage.spell(stack, slot, lookup);
            if (data != SpellData.EMPTY) data = new SpellData(data.getSpell(), resolveImbuedSpellLevel(stack, data));
            views.add(SneakSelectionView.forSpell(slot, data, isSelectable(stack, slot, lookup)));
        }
        return views;
    }

    @Override
    public int getSneakSelectionIndex(ItemStack stack) {
        return normalizeSelectedScrollIndex(stack, serializationLookup());
    }

    @Override
    public boolean isSneakSelectionIndexSelectable(ItemStack stack, int index) {
        return isSelectable(stack, index, serializationLookup());
    }

    @Override
    public void setSneakSelectionIndex(ItemStack stack, int index) {
        if (isSneakSelectionIndexSelectable(stack, index))
            FullautoRapidcastSpellrifleScrollStorage.select(stack, index);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) normalizeSelectedScrollIndex(stack, level.registryAccess());
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    public List<Component> getImbueRestrictionTooltipLines(ItemStack stack) {
        return List.of(ImbueTooltipHelper.translatableGray(hasSilverRing(stack, serializationLookup())
                ? "item.apprenticecodex.spellgun.tooltip.restrict_restrict_not_continuous"
                : "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only"));
    }
}
