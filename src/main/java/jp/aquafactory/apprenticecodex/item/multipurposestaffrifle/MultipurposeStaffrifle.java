package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastContext;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleRateLimiter;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncMultipurposeStaffrifleFireEffectPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.List;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunSpellListManager;

public final class MultipurposeStaffrifle extends Item
        implements GeoItem, NonDamageableAnvilMergeItem, IJeiInfoItem, CastAnimationOverrideItem,
        AttributeEnchantmentPolicy, WisdomPolicy, PlunderTarget {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.multipurpose_staffrifle.desc_";
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
    private static final float BASE_EMPTY_CASING_RETURN_CHANCE = 0.0F;
    private static final float EQUIPPED_AMMO_POUCH_EMPTY_CASING_RETURN_CHANCE = 0.2F;
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;
    private static final ResourceLocation SPELL_POWER_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "multipurpose_staffrifle.mainhand.spell_power");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers baseMainhandModifiers = buildBaseMainhandModifiers();

    public MultipurposeStaffrifle() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
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
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment) || isSupportedStaffrifleEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
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

        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null
                || selection.spellData == SpellData.EMPTY
                || selection.spellData.getSpell() == SpellRegistry.none()) {
            sendActionBarError(player, Component.translatable("ui.apprenticecodex.multipurpose_staffrifle.not_selected"));
            return false;
        }

        var spellData = selection.spellData;
        var spell = spellData.getSpell();
        if (spell.getCastType() == CastType.CONTINUOUS) {
            sendActionBarError(player, Component.translatable(
                    "ui.apprenticecodex.multipurpose_staffrifle.cannot_cast",
                    spell.getDisplayName(player),
                    stack.getHoverName()
            ));
            return false;
        }

        if (isSpecialCastSpellDenied(spell)) {
            sendActionBarError(player, Component.translatable(
                    "ui.apprenticecodex.multipurpose_staffrifle.deny_list",
                    spell.getDisplayName(player)
            ));
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
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
            try (var ignored = MultipurposeStaffrifleCastContext.open(player.getUUID(), stack, spell, recast)) {
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
            throw new IllegalStateException("Multipurpose Staffrifle special cast context failed to close.", exception);
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(player);
        }

        if (!casted) {
            return false;
        }

        MultipurposeStaffrifleCastContext.rememberPending(player.getUUID(), stack, spell, recast, player.level().getGameTime());
        playSuccessfulFireEffects(player, spell, adsFullAuto);
        triggerFiredAnimation(player, stack);
        return true;
    }

    public Item getAmmoItem(ItemStack stack) {
        return ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get();
    }

    public static boolean isSpecialCastSpellDenied(@Nullable AbstractSpell spell) {
        return SpellGunSpellListManager.isDenylisted(spell)
                || (spell != null
                && ApprenticeCodexServerConfig.isMultipurposeStaffrifleSpellDenied(spell.getSpellResource()));
    }

    public Item getDisplayedAmmoItem(ItemStack stack) {
        return getAmmoItem(stack);
    }

    public float resolveEmptyCasingReturnChance(Player player) {
        return SpellcasterAmmoPouch.isEquippedBy(player)
                ? EQUIPPED_AMMO_POUCH_EMPTY_CASING_RETURN_CHANCE
                : BASE_EMPTY_CASING_RETURN_CHANCE;
    }

    public boolean shouldReturnEmptyCasing(Player player) {
        var emptyCasingReturnChance = resolveEmptyCasingReturnChance(player);
        return emptyCasingReturnChance > 0.0F
                && player.getRandom().nextFloat() < emptyCasingReturnChance;
    }

    public int resolveSpecialCooldownTicks(int originalCooldownTicks) {
        var cooldown = Math.max(0, originalCooldownTicks);
        if (cooldown <= ApprenticeCodexServerConfig.multipurposeStaffrifleCooldownBypassThresholdTicks()) {
            return 0;
        }

        return Math.max(
                ApprenticeCodexServerConfig.multipurposeStaffrifleReducedCooldownMinimumTicks(),
                cooldown - ApprenticeCodexServerConfig.multipurposeStaffrifleCooldownReductionTicks()
        );
    }

    public static boolean isAdsUse(@Nullable LivingEntity entity) {
        if (entity == null || !entity.isUsingItem() || entity.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        var useItem = entity.getUseItem();
        return !useItem.isEmpty() && useItem.getItem() instanceof MultipurposeStaffrifle;
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
        appendMultipurposeStaffrifleHelpTooltip(stack, lines);
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
        Networks.sendToTrackingEntityAndSelf(player, new SyncMultipurposeStaffrifleFireEffectPacket(player.getId()));

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var color = MagicTools.resolveSchoolTintColor(spell.getSchoolType());
        var red = ((color >> 16) & 0xFF) / 255.0F;
        var green = ((color >> 8) & 0xFF) / 255.0F;
        var blue = (color & 0xFF) / 255.0F;
        var muzzlePosition = resolveMuzzlePosition(player, adsFullAuto);
        var look = player.getLookAngle().normalize();
        spawnMuzzleFlashParticles(serverLevel, muzzlePosition, look, red, green, blue);
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

    private static void spawnMuzzleFlashParticles(ServerLevel level, Vec3 center, Vec3 look,
                                                  float red, float green, float blue) {
        var random = level.getRandom();
        for (var i = 0; i < MUZZLE_RHOMBUS_COUNT; ++i) {
            var size = Mth.lerp(random.nextFloat(), 0.16F, 0.28F);
            var position = center.add(createMuzzleParticleOffset(random, look, 0.08D));
            var velocity = look.scale(Mth.lerp(random.nextFloat(), 0.03D, 0.08D));
            level.sendParticles(
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
            level.sendParticles(
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

    private static Vec3 createMuzzleParticleOffset(net.minecraft.util.RandomSource random, Vec3 look, double radius) {
        return look.scale(random.nextDouble() * 0.08D).add(createRandomSpread(random, radius));
    }

    private static Vec3 createRandomSpread(net.minecraft.util.RandomSource random, double radius) {
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
        return MultipurposeStaffrifleRateLimiter.canAttemptSpecialCast(player);
    }

    private static void sendActionBarError(ServerPlayer player, Component component) {
        player.connection.send(new ClientboundSetActionBarTextPacket(component.copy().withStyle(ChatFormatting.RED)));
    }

    private void appendMultipurposeStaffrifleHelpTooltip(ItemStack stack, List<Component> lines) {
        appendMultipurposeStaffrifleDescription(lines);
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectMultipurposeStaffrifleAbilityTooltipSection(),
                "item.apprenticecodex.spellgun.tooltip.ability_multipurpose_title",
                "item.apprenticecodex.spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(ImbueTooltipHelper.translatableGray(
                        "item.apprenticecodex.spellgun.tooltip.restrict_restrict_not_continuous"
                )),
                "item.apprenticecodex.spellgun.tooltip.restrict_multipurpose_title",
                "item.apprenticecodex.spellgun.tooltip.restrict_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(ImbueTooltipHelper.createAmmoTooltipLine(getDisplayedAmmoItem(stack), null)),
                "item.apprenticecodex.spellgun.tooltip.ammo_title",
                "item.apprenticecodex.spellgun.tooltip.ammo_none"
        );
    }

    private static List<Component> collectMultipurposeStaffrifleAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item.apprenticecodex.spellgun.tooltip.ability_skip_cooldown",
                ImbueTooltipHelper.formatTooltipSeconds(ApprenticeCodexServerConfig.multipurposeStaffrifleCooldownBypassThresholdTicks())
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item.apprenticecodex.spellgun.tooltip.ability_subtract_cooldown",
                ImbueTooltipHelper.formatTooltipSeconds(ApprenticeCodexServerConfig.multipurposeStaffrifleCooldownReductionTicks()),
                ImbueTooltipHelper.formatTooltipSeconds(ApprenticeCodexServerConfig.multipurposeStaffrifleReducedCooldownMinimumTicks())
        ));
        return translatedLines;
    }

    private static void appendMultipurposeStaffrifleDescription(List<Component> lines) {
        lines.add(Component.translatable(
                "item.apprenticecodex.multipurpose_staffrifle.desc_1",
                ImbueTooltipHelper.getAttackKeyName()
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "item.apprenticecodex.multipurpose_staffrifle.desc_2",
                ImbueTooltipHelper.getUseKeyName()
        ).withStyle(ChatFormatting.GRAY));
    }

    private ItemAttributeModifiers buildMainhandModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return baseMainhandModifiers;
        }

        var builder = ItemAttributeModifiers.builder();
        addEnchantmentModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                SPELL_POWER_BONUS + getEnchantmentLevel(stack, Enchantments.SURGE) * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.spell_power"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                getEnchantmentLevel(stack, Enchantments.ALACRITY) * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.alacrity.cooldown_reduction"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MANA_REGEN,
                getEnchantmentLevel(stack, Enchantments.REFLUX) * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.reflux.mana_regen"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                getEnchantmentLevel(stack, Enchantments.RESERVOIR) * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADD_VALUE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.reservoir.max_mana"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                getEnchantmentLevel(stack, Enchantments.TENSE) * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.tense.cast_time_reduction"
        );
        return builder.build();
    }

    private static ItemAttributeModifiers buildBaseMainhandModifiers() {
        return ItemAttributeModifiers.builder().add(
                AttributeRegistry.SPELL_POWER,
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        ).build();
    }

    private static void addEnchantmentModifier(ItemAttributeModifiers.Builder builder,
                                               Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                               double amount,
                                               AttributeModifier.Operation operation,
                                               String modifierIdSeed) {
        if (amount == 0.0D) {
            return;
        }

        builder.add(
                attribute,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, modifierIdSeed),
                        amount,
                        operation
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return Enchantments.getLevel(stack, enchantment);
    }

    private static boolean isSupportedStaffrifleEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }

        return enchantment.is(Enchantments.ALACRITY)
                || enchantment.is(Enchantments.REFLUX)
                || enchantment.is(Enchantments.RESERVOIR)
                || enchantment.is(Enchantments.SURGE)
                || enchantment.is(Enchantments.TENSE)
                || enchantment.is(Enchantments.WISDOM)
                || enchantment.is(Enchantments.PLUNDER);
    }

}
