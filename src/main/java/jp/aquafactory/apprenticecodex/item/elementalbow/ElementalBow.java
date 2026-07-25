package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.SneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.item.ammo.BowAmmoConsumptionNotification;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiverBowAmmoResolver;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.ResolvedDefinition;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;

public class ElementalBow extends BowItem implements GeoItem, IPresetSpellContainer, ArcaneAnvilImbueBlockItem,
        IJeiInfoItem, SneakSelectionUiItem, TranscendencePolicy, WisdomPolicy, PlunderTarget {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.elemental_bow.desc_";

    public static final int READY_DRAW_TICKS = 20;
    private static final String MAIN_CONTROLLER = "main";
    private static final String RELEASE_ANIMATION = "release";
    private static final String MODE_TAG = "ElementalBowMode";
    private static final String SHOT_MODE_TAG = "ElementalBowShotMode";
    private static final String AMMO_SELECTION_TAG = "ElementalBowAmmoSelection";
    private static final ItemStack ENCHANTMENT_PROBE_STACK = new ItemStack(Items.BOW);
    private static final float MANA_SAFE_MARGIN = 0.001F;
    private static final int OVERHEAT_WARNING_INTERVAL_TICKS = 10;
    private static final float DRAW_ANIMATION_SOURCE_SECONDS = 0.32F;
    private static final ResourceLocation SPECTRAL_ARROW_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow");
    private static final Set<Item> VANILLA_ARROW_ITEMS = Set.of(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_DRAW = RawAnimation.begin().thenPlayAndHold("draw");
    private static final RawAnimation ANIM_RELEASE = RawAnimation.begin().thenPlay("release");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ElementalBow() {
        super(new Properties().durability(1561).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public Handling transcendenceHandling() {
        return Handling.INTERNAL;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    var stack = state.getData(DataTickets.ITEMSTACK);
                    var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
                    if (ElementalBowClientRenderState.shouldPlayDrawAnimation(stack, perspective)) {
                        state.setAnimation(ANIM_DRAW);
                        state.getController().setAnimationSpeed(resolveDrawAnimationSpeed(stack));
                    } else {
                        state.setAnimation(ANIM_IDLE);
                        state.getController().setAnimationSpeed(1.0D);
                    }
                    return PlayState.CONTINUE;
                }).triggerableAnim(RELEASE_ANIMATION, ANIM_RELEASE)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static boolean isElementalSpell(@Nullable AbstractSpell spell) {
        return ElementalBowModeManager.isElementalSpell(spell);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        initializeSpellContainer(stack);
        var selection = normalizeModeState(stack);
        return switch (selection.kind()) {
            case NORMAL -> useNormalArrowMode(level, player, usedHand, stack);
            case ARROW, SPECIAL, MOD -> useTrackedArrowMode(level, player, usedHand, stack, selection);
            case MAGIC -> useMagicMode(level, player, usedHand, stack, selection);
        };
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var profile = resolveSpellProfile(stack, resolveConfiguredMagicMode(stack));
        if (profile == null) {
            ISpellContainer.remove(stack);
            return;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer != null) {
            var spellData = spellContainer.getSpellAtIndex(0);
            if (spellData != SpellData.EMPTY
                    && spellData.getSpell() == profile.spell()
                    && spellData.getLevel() == profile.spellLevel()
                    && spellData.isLocked()
                    && spellContainer.getMaxSpellCount() == 1
                    && !spellContainer.isSpellWheel()) {
                return;
            }
        }

        // モード由来の spell 情報は tooltip と外部参照先で共有したいが、
        // 通常の spell wheel へは流さない。
        var mutable = ISpellContainer.create(1, false, false).mutableCopy();
        mutable.addSpellAtIndex(profile.spell(), profile.spellLevel(), 0, true);
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        initializeSpellContainer(stack);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (!isHeldElementalBow(player.getMainHandItem()) && !isHeldElementalBow(player.getOffhandItem())) {
            ElementalBowOverheatManager.clearObservedSchools(player);
            return;
        }

        // 1プレイヤーにつき1回だけ走らせるという意図でガードはつける.
        var trackingStack = isHeldElementalBow(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
        if (stack != trackingStack) {
            return;
        }

        ElementalBowOverheatManager.refreshObservedSchoolsWhileHolding(player);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        var mode = resolveConfiguredMagicMode(stack);
        if (mode == null) {
            return;
        }

        var drawDuration = stack.getUseDuration(entity) - remainingUseDuration;
        if (drawDuration <= 0 || drawDuration >= mode.resolveRequiredDrawTicks()) {
            return;
        }

        if (!player.getAbilities().instabuild && (drawDuration == 1 || drawDuration % OVERHEAT_WARNING_INTERVAL_TICKS == 0)) {
            displayOverheatManaWarning(player, stack, mode);
        }
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        initializeSpellContainer(stack);

        var selection = normalizeModeState(stack);
        switch (selection.kind()) {
            case NORMAL -> releaseVanillaShot(stack, level, player, timeLeft);
            case ARROW, SPECIAL, MOD -> releaseTrackedArrowShot(stack, level, player, timeLeft, selection);
            case MAGIC -> {
                var mode = resolveConfiguredMagicMode(stack);
                if (mode != null) {
                    releaseElementalShot(stack, level, player, timeLeft, mode);
                }
            }
        }
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return Items.BOW.supportsEnchantment(ENCHANTMENT_PROBE_STACK, enchantment)
                || isSupportedAdditionalElementalBowEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return Items.BOW.isPrimaryItemFor(ENCHANTMENT_PROBE_STACK, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        if (Items.BOW.isBookEnchantable(ENCHANTMENT_PROBE_STACK, book)) {
            return true;
        }

        return bookContainsOnlySupportedAdditionalElementalBowEnchantments(book);
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 10;
    }

    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        initializeSpellContainer(stack);
        super.appendHoverText(stack, context, lines, flag);
        lines.add(
                Component.translatable("item.apprenticecodex.elemental_bow.mode", getModeDisplayName(stack))
                        .withStyle(ChatFormatting.GRAY)
        );
        lines.add(Component.translatable("item.apprenticecodex.elemental_bow.desc")
                .withStyle(ChatFormatting.GRAY));
        if (normalizeModeState(stack).kind() != ShotModeKind.MAGIC) {
            return;
        }

        if (hasSynthesis(stack)) {
            lines.add(Component.translatable("item.apprenticecodex.elemental_bow.spell.with_synthesis")
                    .withStyle(ChatFormatting.AQUA));
        } else if (hasInfinity(stack)) {
            lines.add(Component.translatable("item.apprenticecodex.elemental_bow.spell.with_infinity")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            lines.add(Component.translatable("item.apprenticecodex.elemental_bow.spell.no_enchantment")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    @Nullable
    @SuppressWarnings("DataFlowIssue")
    private static InteractionResultHolder<ItemStack> fireArrowNockHook(
            ItemStack stack,
            Level level,
            Player player,
            InteractionHand usedHand
    ) {
        // NeoForge 21.1.219 の EventHooks#onArrowNock は戻り値を non-null 扱いしているが、
        // 実装は ArrowNockEvent#getAction() をそのまま返しており、未設定時は null になる。
        return EventHooks.onArrowNock(stack, level, player, usedHand, true);
    }

    private InteractionResultHolder<ItemStack> useNormalArrowMode(Level level, Player player, InteractionHand usedHand, ItemStack stack) {
        var ammoSource = resolveBowModeAmmoSource(player, stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
        if (ammoSource == null && !canFireWithoutAmmo) {
            return InteractionResultHolder.fail(stack);
        }

        var nockResult = fireArrowNockHook(stack, level, player, usedHand);
        if (nockResult != null) {
            return nockResult;
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> useTrackedArrowMode(
            Level level,
            Player player,
            InteractionHand usedHand,
            ItemStack stack,
            ModeSelection selection
    ) {
        var ammoSource = resolveAmmoSource(player, stack, selection);
        var canFireWithoutAmmo = canFireTrackedArrowWithoutAmmo(player, stack, selection);
        if (ammoSource == null && !canFireWithoutAmmo) {
            return InteractionResultHolder.fail(stack);
        }

        var nockResult = fireArrowNockHook(stack, level, player, usedHand);
        if (nockResult != null) {
            return nockResult;
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> useMagicMode(
            Level level,
            Player player,
            InteractionHand usedHand,
            ItemStack stack,
            ModeSelection selection
    ) {
        var mode = ElementalBowModeManager.getResolvedDefinition(selection.id());
        if (mode == null) {
            clearAllModeTags(stack);
            return InteractionResultHolder.fail(stack);
        }

        var ammoSource = resolveAmmoSource(player, stack, selection);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasSynthesis(stack);
        if (ammoSource == null && !canFireWithoutAmmo) {
            return InteractionResultHolder.fail(stack);
        }

        var profile = createSpellCastProfile(stack, mode);

        var requiredMana = profile.spell().getManaCost(profile.spellLevel());
        if (!player.getAbilities().instabuild) {
            var magicData = MagicData.getPlayerMagicData(player);
            var extraMana = level.isClientSide
                    ? 0.0F
                    : getAdditionalManaCost(player, mode, profile);
            var totalRequiredMana = requiredMana + extraMana;
            if (magicData == null || magicData.getMana() + MANA_SAFE_MARGIN < totalRequiredMana) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            extraMana > MANA_SAFE_MARGIN
                                    ? createOverheatInsufficientManaMessage(totalRequiredMana)
                                    : createInsufficientManaMessage(profile.spell(), player),
                            true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }
        }

        var nockResult = fireArrowNockHook(stack, level, player, usedHand);
        if (nockResult != null) {
            return nockResult;
        }

        if (!level.isClientSide && !player.getAbilities().instabuild) {
            displayOverheatManaWarning(player, stack, mode);
        }
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    private void releaseVanillaShot(ItemStack stack, Level level, Player player, int timeLeft) {
        var ammoSource = resolveBowModeAmmoSource(player, stack);
        var hasAmmo = ammoSource != null && !ammoSource.stack().isEmpty();
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(stack, level, player, drawDuration, hasAmmo || canFireWithoutAmmo);
        if (drawDuration < 0) {
            return;
        }

        if (!hasAmmo && !canFireWithoutAmmo) {
            return;
        }

        var ammoStack = hasAmmo ? ammoSource.stack() : new ItemStack(Items.ARROW);
        var infiniteAmmo = player.getAbilities().instabuild
                || isInfinityNormalArrowShot(stack, ammoStack)
                || hasAmmo && ammoSource.isInfinite(stack, player);
        var power = getPowerForTime(drawDuration);
        if (power < 0.1F) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        fireVanillaArrow(level, player, stack, ammoStack, power, infiniteAmmo);
        if (!player.getAbilities().instabuild && hasAmmo && !infiniteAmmo) {
            consumeAmmoAndNotify(player, ammoSource);
        }
        triggerReleaseAnimation(player, stack);
    }

    @Nullable
    private AmmoSource resolveBowModeAmmoSource(Player player, ItemStack bowStack) {
        if (!SpellcasterQuiverBowAmmoResolver.hasSupportedQuiverAmmo(player, bowStack)) {
            return resolveVanillaAmmoSource(player, bowStack);
        }

        var ammoSource = SpellcasterQuiverBowAmmoResolver.resolveBowAmmo(player, bowStack);
        return ammoSource == null ? null : new AmmoSource() {
            @Override
            public ItemStack stack() {
                return ammoSource.stack();
            }

            @Override
            public boolean consume() {
                return ammoSource.consume();
            }

            @Override
            public boolean isInfinite(ItemStack bowStack, Player player) {
                return ammoSource.isInfinite(bowStack, player);
            }
        };
    }

    private void releaseTrackedArrowShot(ItemStack stack, Level level, Player player, int timeLeft, ModeSelection selection) {
        var ammoSource = resolveAmmoSource(player, stack, selection);
        var hasAmmo = ammoSource != null && !ammoSource.stack().isEmpty();
        var canFireWithoutAmmo = canFireTrackedArrowWithoutAmmo(player, stack, selection);
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(stack, level, player, drawDuration, hasAmmo || canFireWithoutAmmo);
        if (drawDuration < 0) {
            return;
        }

        if (!hasAmmo && !canFireWithoutAmmo) {
            return;
        }

        var ammoStack = hasAmmo ? ammoSource.stack() : createRepresentativeAmmo(selection);
        var infiniteAmmo = player.getAbilities().instabuild
                || isInfinityNormalArrowShot(stack, ammoStack)
                || hasAmmo && ammoSource.isInfinite(stack, player);
        var power = getPowerForTime(drawDuration);
        if (power < 0.1F) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        fireVanillaArrow(level, player, stack, ammoStack, power, infiniteAmmo);
        if (!player.getAbilities().instabuild && hasAmmo && !infiniteAmmo) {
            consumeAmmoAndNotify(player, ammoSource);
        }
        triggerReleaseAnimation(player, stack);
    }

    private void releaseElementalShot(ItemStack stack, Level level, Player player, int timeLeft, ResolvedDefinition mode) {
        var ammoSource = resolveMagicArrowCatalystAmmoSource(player);
        var hasSynthesisEnchantment = hasSynthesis(stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasSynthesisEnchantment;
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(stack, level, player, drawDuration, ammoSource != null || canFireWithoutAmmo);
        if (drawDuration < mode.resolveRequiredDrawTicks()) {
            return;
        }

        // 属性ショットは server 側でのみ最終判定と詠唱を行う。
        // client 側でも同じ再判定を通すと、server が先にマナを消費した直後の同期値を見て
        // 「現在マナ 0」の不足表示だけが二重に出ることがある。
        if (level.isClientSide) {
            return;
        }

        var profile = createSpellCastProfile(stack, mode);

        if (!player.getAbilities().instabuild) {
            if (ammoSource == null && !hasSynthesisEnchantment) {
                return;
            }

            var magicData = MagicData.getPlayerMagicData(player);
            var requiredMana = profile.spell().getManaCost(profile.spellLevel());
            var extraMana = getAdditionalManaCost(player, mode, profile);
            var totalRequiredMana = requiredMana + extraMana;
            if (magicData == null || magicData.getMana() + MANA_SAFE_MARGIN < totalRequiredMana) {
                player.displayClientMessage(
                        extraMana > MANA_SAFE_MARGIN
                                ? createOverheatInsufficientManaMessage(totalRequiredMana)
                                : createInsufficientManaMessage(profile.spell(), player),
                        true
                );
                return;
            }
        }

        ElementalBowOverheatManager.clearPendingCooldown(player, mode.schoolId());
        var overheatMana = player.getAbilities().instabuild
                ? 0.0F
                : getAdditionalManaCost(player, mode, profile);
        if (!castElementalSpell(player, stack, profile)) {
            return;
        }

        if (overheatMana > 0.0F) {
            consumeAdditionalMana(player, overheatMana);
        }
        ElementalBowOverheatManager.applyOverheatAfterCast(
                player,
                mode.schoolId(),
                ElementalBowOverheatManager.consumePendingCooldown(player, mode.schoolId(), profile.spell().getSpellCooldown())
        );

        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (ammoSource != null && !hasSynthesisEnchantment) {
                consumeAmmoAndNotify(player, ammoSource);
            }
        }

        triggerReleaseAnimation(player, stack);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (player.getRandom().nextFloat() * 0.4F + 1.2F) + 0.35F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private boolean castElementalSpell(Player player, ItemStack stack, SpellCastProfile profile) {
        var slotId = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;
        var magicData = MagicData.getPlayerMagicData(player);
        var casted = profile.spell().attemptInitiateCast(
                stack,
                profile.spellLevel(),
                player.level(),
                player,
                CastSource.SWORD,
                true,
                slotId
        );
        if (!casted) {
            return false;
        }

        TriggeredSpellCastHelper.applyLongCastDurationOverride(
                player,
                profile.spellLevel(),
                profile.spell(),
                magicData,
                slotId,
                0
        );
        return true;
    }

    private void fireVanillaArrow(Level level, Player player, ItemStack bowStack, ItemStack ammoStack, float power, boolean infiniteAmmo) {
        if (!level.isClientSide) {
            var arrowItem = ammoStack.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem) Items.ARROW;
            var arrow = arrowItem.createArrow(level, ammoStack, player, bowStack);
            arrow = customArrow(arrow, ammoStack, bowStack);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            if (power == 1.0F) {
                arrow.setCritArrow(true);
            }

            bowStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (infiniteAmmo) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            level.addFreshEntity(arrow);
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (player.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private void consumeAmmoAndNotify(Player player, AmmoSource ammoSource) {
        var consumedStack = ammoSource.stack().copyWithCount(1);
        if (!ammoSource.consume() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BowAmmoConsumptionNotification.send(
                serverPlayer,
                ItemRegistry.ELEMENTAL_BOW.getId(),
                consumedStack
        );
    }

    @Nullable
    private SpellCastProfile resolveSpellProfile(ItemStack stack, @Nullable ResolvedDefinition mode) {
        return mode == null ? null : createSpellCastProfile(stack, mode);
    }

    private SpellCastProfile createSpellCastProfile(ItemStack stack, ResolvedDefinition mode) {
        return new SpellCastProfile(mode.spell(), mode.resolveSpellLevel(stack));
    }

    @Nullable
    public static DisplayedSpellProfile getDisplayedSpellProfile(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow elementalBow)) {
            return null;
        }

        var mode = elementalBow.resolveConfiguredMagicMode(stack);
        if (mode == null) {
            return null;
        }
        var profile = elementalBow.createSpellCastProfile(stack, mode);
        return new DisplayedSpellProfile(profile.spell(), profile.spellLevel());
    }

    @Nullable
    public static ResourceLocation getConfiguredSchoolId(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow elementalBow)) {
            return null;
        }

        var mode = elementalBow.resolveConfiguredMagicMode(stack);
        return mode != null ? mode.schoolId() : null;
    }

    @Nullable
    public static InventoryOverlayView getInventoryOverlayView(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow)) {
            return null;
        }

        var selection = normalizeModeState(stack);
        if (selection.kind() == ShotModeKind.NORMAL) {
            return null;
        }

        if (selection.kind() == ShotModeKind.MAGIC) {
            var resolvedMode = ElementalBowModeManager.getResolvedDefinition(selection.id());
            if (resolvedMode == null) {
                return null;
            }

            return new InventoryOverlayView(
                    SelectionIconKind.ITEM,
                    SchoolAffinityRegistry.createIconStack(resolvedMode.schoolType()),
                    resolvedMode.spell().getSpellIconResource()
            );
        }

        return new InventoryOverlayView(
                SelectionIconKind.ITEM,
                createRepresentativeAmmo(selection),
                null
        );
    }

    public static Component createInsufficientManaMessage(AbstractSpell spell, @Nullable Player caster) {
        return Component.translatable("ui.irons_spellbooks.cast_error_mana", spell.getDisplayName(caster))
                .withStyle(ChatFormatting.RED);
    }

    public static Component createOverheatInsufficientManaMessage(float manaCost) {
        return Component.translatable(
                        "ui.apprenticecodex.elemental_bow.overheat_insufficient_mana",
                        formatDisplayedOverheatManaCost(manaCost)
                )
                .withStyle(ChatFormatting.RED);
    }

    public static List<ModeSelectionView> getAvailableSelectionViews(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow)) {
            return List.of();
        }

        var currentSelection = normalizeModeState(stack);
        var ammoSummary = summarizeAmmoInventory(player);
        var selections = buildAvailableSelections(currentSelection, ammoSummary);
        var views = new ArrayList<ModeSelectionView>(selections.size());
        for (var selection : selections) {
            views.add(createSelectionView(player, stack, selection, currentSelection, ammoSummary));
        }
        return List.copyOf(views);
    }

    public static void applyClientSelection(
            ServerPlayer player,
            InteractionHand hand,
            String shotModeName,
            @Nullable ResourceLocation selectionId,
            boolean continueUse
    ) {
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ElementalBow)) {
            return;
        }

        var requestedSelection = resolveSelectionFromKey(shotModeName, selectionId);
        if (requestedSelection == null || !isSelectableMode(player, stack, requestedSelection)) {
            return;
        }

        var currentSelection = normalizeModeState(stack);
        if (sameSelection(currentSelection, requestedSelection)) {
            if (continueUse) {
                stack.getItem().use(player.level(), player, hand);
            }
            return;
        }

        setModeSelection(stack, requestedSelection);
        player.displayClientMessage(
                Component.translatable(
                                "ui.apprenticecodex.elemental_bow.mode_switched",
                                resolveSelectionDisplayName(stack, requestedSelection)
                        )
                        .withStyle(ChatFormatting.GOLD),
                true
        );
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 1.1F);

        if (continueUse) {
            stack.getItem().use(player.level(), player, hand);
        }
    }

    private static boolean isSelectableMode(Player player, ItemStack stack, ModeSelection requestedSelection) {
        var currentSelection = normalizeModeState(stack);
        for (var selectable : buildAvailableSelections(currentSelection, summarizeAmmoInventory(player))) {
            if (sameSelection(selectable, requestedSelection)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static ModeSelection resolveSelectionFromKey(String shotModeName, @Nullable ResourceLocation selectionId) {
        var shotMode = ShotModeKind.fromSerializedName(shotModeName);
        if (shotMode == null) {
            return null;
        }
        if (shotMode == ShotModeKind.NORMAL) {
            return ModeSelection.normal();
        }

        if (shotMode == ShotModeKind.ARROW) {
            return ModeSelection.arrow();
        }
        return selectionId != null ? new ModeSelection(shotMode, selectionId) : null;
    }

    @Nullable
    private AmmoSource resolveAmmoSource(Player player, ItemStack bowStack, ModeSelection selection) {
        return switch (selection.kind()) {
            case NORMAL -> resolveVanillaAmmoSource(player, bowStack);
            case MAGIC -> resolveMagicArrowCatalystAmmoSource(player);
            case ARROW -> resolveNormalArrowAmmoSource(player);
            case SPECIAL -> resolveSpecialAmmoSource(player, selection.id());
            case MOD -> resolveModAmmoSource(player, selection.id());
        };
    }

    @Nullable
    private AmmoSource resolveVanillaAmmoSource(Player player, ItemStack bowStack) {
        var quiverSource = resolveQuiverAmmoSource(player, ammoStack -> ammoStack.getItem() instanceof ArrowItem);
        if (quiverSource != null) {
            return quiverSource;
        }

        var projectile = player.getProjectile(bowStack);
        return projectile.isEmpty() ? null : new LooseAmmoSource(projectile);
    }

    @Nullable
    private AmmoSource resolveNormalArrowAmmoSource(Player player) {
        return adaptAmmoSource(BowCastAmmoResolver.resolveElementalNormalArrowAmmo(player));
    }

    @Nullable
    private AmmoSource resolveMagicArrowCatalystAmmoSource(Player player) {
        return adaptAmmoSource(BowCastAmmoResolver.resolveElementalMagicArrowCatalystAmmo(
                player,
                ApprenticeCodexServerConfig.elementalBowMagicArrowCatalystItemIds()
        ));
    }

    @Nullable
    private AmmoSource resolveSpecialAmmoSource(Player player, @Nullable ResourceLocation selectionId) {
        if (selectionId == null) {
            return null;
        }

        var quiverSource = resolveQuiverAmmoSource(player, ammoStack -> matchesSpecialAmmo(ammoStack, selectionId));
        if (quiverSource != null) {
            return quiverSource;
        }

        for (var ammoStack : collectCandidateAmmoStacks(player)) {
            if (ammoStack.isEmpty()) {
                continue;
            }

            if (matchesSpecialAmmo(ammoStack, selectionId)) {
                return new LooseAmmoSource(ammoStack);
            }
        }
        return null;
    }

    @Nullable
    private AmmoSource resolveModAmmoSource(Player player, @Nullable ResourceLocation selectionId) {
        if (selectionId == null) {
            return null;
        }

        var quiverSource = resolveQuiverAmmoSource(player, ammoStack -> matchesModAmmo(ammoStack, selectionId));
        if (quiverSource != null) {
            return quiverSource;
        }

        for (var ammoStack : collectCandidateAmmoStacks(player)) {
            if (ammoStack.isEmpty()) {
                continue;
            }

            if (matchesModAmmo(ammoStack, selectionId)) {
                return new LooseAmmoSource(ammoStack);
            }
        }
        return null;
    }

    @Nullable
    private AmmoSource resolveQuiverAmmoSource(Player player, Predicate<ItemStack> predicate) {
        var quiverAmmo = SpellcasterQuiver.findAccessibleArrow(player, predicate);
        return quiverAmmo == null ? null : new StoredAmmoSource(quiverAmmo, () -> SpellcasterQuiver.consumeAccessibleArrow(player, predicate));
    }

    private static boolean matchesSpecialAmmo(ItemStack ammoStack, ResourceLocation selectionId) {
        if (SPECTRAL_ARROW_ID.equals(selectionId)) {
            return ammoStack.is(Items.SPECTRAL_ARROW);
        }

        if (!ammoStack.is(Items.TIPPED_ARROW)) {
            return false;
        }

        var potion = PotionContentsHelper.getPotion(ammoStack);
        return potion != null && selectionId.equals(BuiltInRegistries.POTION.getKey(potion));
    }

    private static boolean matchesModAmmo(ItemStack ammoStack, ResourceLocation selectionId) {
        var item = ammoStack.getItem();
        var itemId = BuiltInRegistries.ITEM.getKey(item);
        return item instanceof ArrowItem && !VANILLA_ARROW_ITEMS.contains(item) && selectionId.equals(itemId);
    }

    @Nullable
    private AmmoSource adaptAmmoSource(@Nullable SpellcasterQuiverBowAmmoResolver.AmmoSource ammoSource) {
        if (ammoSource == null) {
            return null;
        }

        return new AmmoSource() {
            @Override
            public ItemStack stack() {
                return ammoSource.stack();
            }

            @Override
            public boolean consume() {
                return ammoSource.consume();
            }

            @Override
            public boolean isInfinite(ItemStack bowStack, Player player) {
                return ammoSource.isInfinite(bowStack, player);
            }
        };
    }

    private float getAdditionalManaCost(Player player, ResolvedDefinition mode, SpellCastProfile profile) {
        return ElementalBowOverheatManager.getAdditionalManaCost(player, mode.schoolId(), profile.spell().getManaCost(profile.spellLevel()));
    }

    private void displayOverheatManaWarning(Player player, ItemStack stack, ResolvedDefinition mode) {
        var profile = createSpellCastProfile(stack, mode);

        var extraMana = getAdditionalManaCost(player, mode, profile);
        if (extraMana <= MANA_SAFE_MARGIN) {
            return;
        }

        player.displayClientMessage(
                Component.translatable(
                                "ui.apprenticecodex.elemental_bow.overheat_mana_warning",
                                formatDisplayedOverheatManaCost(profile.spell().getManaCost(profile.spellLevel()) + extraMana)
                        )
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }

    private void consumeAdditionalMana(Player player, float manaCost) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || manaCost <= 0.0F) {
            return;
        }

        magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
        if (player instanceof ServerPlayer serverPlayer) {
            // overheat 分は通常の詠唱消費から外して後段で引いているため、
            // client のマナ HUD もここで即座に同期して違和感を残さない。
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    private static boolean isHeldElementalBow(ItemStack stack) {
        return stack.getItem() instanceof ElementalBow;
    }

    private static int formatDisplayedOverheatManaCost(float manaCost) {
        return (int) Math.ceil(manaCost - MANA_SAFE_MARGIN);
    }

    private static boolean hasInfinity(ItemStack stack) {
        return getEnchantmentLevel(stack, Enchantments.INFINITY.location()) > 0;
    }

    private static boolean hasSynthesis(ItemStack stack) {
        return getEnchantmentLevel(stack, jp.aquafactory.apprenticecodex.enchantment.Enchantments.SYNTHESIS.location()) > 0;
    }

    private static boolean canFireTrackedArrowWithoutAmmo(Player player, ItemStack stack, ModeSelection selection) {
        return player.getAbilities().instabuild || selection.kind() == ShotModeKind.ARROW && hasInfinity(stack);
    }

    private static boolean isInfinityNormalArrowShot(ItemStack bowStack, ItemStack ammoStack) {
        return hasInfinity(bowStack) && ammoStack.is(Items.ARROW);
    }

    public static double resolveDrawAnimationSpeed(ItemStack stack) {
        return DRAW_ANIMATION_SOURCE_SECONDS / resolveDrawDurationSeconds(stack);
    }

    private static float resolveDrawDurationSeconds(ItemStack stack) {
        return Math.max(1, resolveMagicRequiredDrawTicks(stack)) / 20.0F;
    }

    public static int resolveMagicRequiredDrawTicks(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow elementalBow)) {
            return READY_DRAW_TICKS;
        }

        var mode = elementalBow.resolveConfiguredMagicMode(stack);
        return mode == null ? READY_DRAW_TICKS : mode.resolveRequiredDrawTicks();
    }

    private void triggerReleaseAnimation(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var instanceId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        triggerAnim(serverPlayer, instanceId, MAIN_CONTROLLER, RELEASE_ANIMATION);
    }

    @Nullable
    private ResolvedDefinition resolveConfiguredMagicMode(ItemStack stack) {
        var selection = normalizeModeState(stack);
        return selection.kind() == ShotModeKind.MAGIC
                ? ElementalBowModeManager.getResolvedDefinition(selection.id())
                : null;
    }

    private static ModeSelection normalizeModeState(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null) {
            return ModeSelection.normal();
        }

        var storedShotMode = tag.contains(SHOT_MODE_TAG) ? ShotModeKind.fromSerializedName(tag.getString(SHOT_MODE_TAG)) : null;
        if (storedShotMode == null) {
            if (!tag.contains(MODE_TAG)) {
                return ModeSelection.normal();
            }

            var legacyModeId = ResourceLocation.tryParse(tag.getString(MODE_TAG));
            var resolvedLegacyMode = ElementalBowModeManager.getResolvedDefinition(legacyModeId);
            if (resolvedLegacyMode != null) {
                return new ModeSelection(ShotModeKind.MAGIC, resolvedLegacyMode.schoolId());
            }

            clearLegacyMagicMode(stack);
            return ModeSelection.normal();
        }

        if (storedShotMode == ShotModeKind.NORMAL) {
            clearAllModeTags(stack);
            return ModeSelection.normal();
        }

        if (storedShotMode == ShotModeKind.MAGIC) {
            var storedModeId = ResourceLocation.tryParse(tag.getString(MODE_TAG));
            var resolvedMode = ElementalBowModeManager.getResolvedDefinition(storedModeId);
            if (resolvedMode != null) {
                return new ModeSelection(ShotModeKind.MAGIC, resolvedMode.schoolId());
            }

            clearAllModeTags(stack);
            return ModeSelection.normal();
        }

        if (storedShotMode == ShotModeKind.ARROW) {
            clearStoredValue(stack, MODE_TAG);
            clearStoredValue(stack, AMMO_SELECTION_TAG);
            return ModeSelection.arrow();
        }

        if (!tag.contains(AMMO_SELECTION_TAG)) {
            clearAllModeTags(stack);
            return ModeSelection.normal();
        }

        var selectionId = ResourceLocation.tryParse(tag.getString(AMMO_SELECTION_TAG));
        if (selectionId == null) {
            clearAllModeTags(stack);
            return ModeSelection.normal();
        }

        return new ModeSelection(storedShotMode, selectionId);
    }

    private static List<ModeSelection> buildAvailableSelections(ModeSelection currentSelection, AmmoInventorySummary ammoSummary) {
        var selections = new ArrayList<ModeSelection>();
        selections.add(ModeSelection.normal());
        selections.add(ModeSelection.arrow());
        selections.addAll(collectSpecialArrowSelections(ammoSummary, currentSelection));
        selections.addAll(collectModArrowSelections(ammoSummary, currentSelection));
        for (var resolvedDefinition : ElementalBowModeManager.getResolvedDefinitions()) {
            selections.add(new ModeSelection(ShotModeKind.MAGIC, resolvedDefinition.schoolId()));
        }
        return selections;
    }

    private static List<ModeSelection> collectSpecialArrowSelections(AmmoInventorySummary ammoSummary, ModeSelection currentSelection) {
        var selections = new ArrayList<ModeSelection>();
        if (ammoSummary.specialArrowCounts().containsKey(SPECTRAL_ARROW_ID)
                || shouldPreserveUnavailableCurrentSelection(currentSelection, ShotModeKind.SPECIAL, SPECTRAL_ARROW_ID)) {
            selections.add(new ModeSelection(ShotModeKind.SPECIAL, SPECTRAL_ARROW_ID));
        }

        for (var potion : BuiltInRegistries.POTION) {
            var potionId = BuiltInRegistries.POTION.getKey(potion);
            if (potionId != null && (ammoSummary.specialArrowCounts().containsKey(potionId)
                    || shouldPreserveUnavailableCurrentSelection(currentSelection, ShotModeKind.SPECIAL, potionId))) {
                selections.add(new ModeSelection(ShotModeKind.SPECIAL, potionId));
            }
        }

        return selections;
    }

    private static List<ModeSelection> collectModArrowSelections(AmmoInventorySummary ammoSummary, ModeSelection currentSelection) {
        var selections = new ArrayList<ModeSelection>();

        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ArrowItem) || VANILLA_ARROW_ITEMS.contains(item)) {
                continue;
            }

            var itemId = BuiltInRegistries.ITEM.getKey(item);
            if (ammoSummary.modArrowCounts().containsKey(itemId)
                    || shouldPreserveUnavailableCurrentSelection(currentSelection, ShotModeKind.MOD, itemId)) {
                selections.add(new ModeSelection(ShotModeKind.MOD, itemId));
            }
        }

        return selections;
    }

    private static boolean shouldPreserveUnavailableCurrentSelection(
            ModeSelection currentSelection,
            ShotModeKind kind,
            ResourceLocation selectionId
    ) {
        return currentSelection.kind() == kind
                && selectionId.equals(currentSelection.id());
    }

    private static AmmoInventorySummary summarizeAmmoInventory(Player player) {
        int normalArrowCount = 0;
        var specialArrowCounts = new LinkedHashMap<ResourceLocation, Integer>();
        var modArrowCounts = new LinkedHashMap<ResourceLocation, Integer>();

        var quiverNormalArrowCount = new int[1];
        SpellcasterQuiver.forEachAccessibleArrow(player, (ammoStack, count) ->
                quiverNormalArrowCount[0] += accumulateAmmoCount(ammoStack, count, specialArrowCounts, modArrowCounts));
        normalArrowCount += quiverNormalArrowCount[0];
        for (var ammoStack : collectCandidateAmmoStacks(player)) {
            normalArrowCount += accumulateAmmoCount(ammoStack, ammoStack.getCount(), specialArrowCounts, modArrowCounts);
        }

        return new AmmoInventorySummary(normalArrowCount, Map.copyOf(specialArrowCounts), Map.copyOf(modArrowCounts));
    }

    private static int accumulateAmmoCount(
            ItemStack ammoStack,
            int count,
            Map<ResourceLocation, Integer> specialArrowCounts,
            Map<ResourceLocation, Integer> modArrowCounts
    ) {
        if (ammoStack.isEmpty() || count <= 0) {
            return 0;
        }

        if (ammoStack.is(Items.ARROW)) {
            return count;
        }

        if (ammoStack.is(Items.SPECTRAL_ARROW)) {
            specialArrowCounts.merge(SPECTRAL_ARROW_ID, count, Integer::sum);
            return 0;
        }

        if (ammoStack.is(Items.TIPPED_ARROW)) {
            var potion = PotionContentsHelper.getPotion(ammoStack);
            if (potion != null) {
                specialArrowCounts.merge(BuiltInRegistries.POTION.getKey(potion), count, Integer::sum);
            }
            return 0;
        }

        if (ammoStack.getItem() instanceof ArrowItem arrowItem && !VANILLA_ARROW_ITEMS.contains(arrowItem)) {
            var itemId = BuiltInRegistries.ITEM.getKey(arrowItem);
            modArrowCounts.merge(itemId, count, Integer::sum);
        }
        return 0;
    }

    private static List<ItemStack> collectCandidateAmmoStacks(Player player) {
        var stacks = new ArrayList<ItemStack>(1 + player.getInventory().items.size());
        stacks.add(player.getOffhandItem());
        stacks.addAll(player.getInventory().items);
        return stacks;
    }

    private static boolean sameSelection(ModeSelection left, ModeSelection right) {
        return left.kind() == right.kind() && Objects.equals(left.id(), right.id());
    }

    private static ModeSelectionView createSelectionView(
            Player player,
            ItemStack stack,
            ModeSelection selection,
            ModeSelection currentSelection,
            AmmoInventorySummary ammoSummary
    ) {
        var isCurrentSelection = sameSelection(selection, currentSelection);
        if (selection.kind() == ShotModeKind.MAGIC) {
            var resolvedMode = ElementalBowModeManager.getResolvedDefinition(selection.id());
            var spellIcon = resolvedMode != null ? resolvedMode.spell().getSpellIconResource() : null;
            var levelText = resolvedMode != null ? Integer.toString(resolvedMode.resolveSpellLevel(stack)) : "?";
            var badgeColor = resolvedMode != null ? resolvedMode.color() : 0xFFFFFF;
            var overheatActive = resolvedMode != null && ElementalBowOverheatManager.getState(player, resolvedMode.schoolId()).active();
            var overheatFillRatio = resolvedMode != null
                    ? ElementalBowOverheatManager.getCooldownOverlayRatio(player, resolvedMode.schoolId())
                    : 0.0F;
            return new ModeSelectionView(
                    selection.toKey(),
                    resolveSelectionDisplayName(stack, selection),
                    SelectionIconKind.SPELL,
                    ItemStack.EMPTY,
                    spellIcon,
                    levelText,
                    badgeColor,
                    isCurrentSelection,
                    overheatActive,
                    overheatFillRatio
            );
        }

        if (selection.kind() == ShotModeKind.NORMAL) {
            return new ModeSelectionView(
                    selection.toKey(),
                    resolveSelectionDisplayName(stack, selection),
                    SelectionIconKind.ITEM,
                    createRepresentativeAmmo(selection),
                    null,
                    null,
                    0xFFFFFF,
                    isCurrentSelection,
                    false,
                    0.0F
            );
        }

        int count;
        if (selection.kind() == ShotModeKind.ARROW) {
            count = ammoSummary.normalArrowCount();
        } else if (selection.kind() == ShotModeKind.SPECIAL) {
            count = selection.id() == null ? 0 : ammoSummary.specialArrowCounts().getOrDefault(selection.id(), 0);
        } else {
            count = selection.id() == null ? 0 : ammoSummary.modArrowCounts().getOrDefault(selection.id(), 0);
        }
        var representativeAmmo = createRepresentativeAmmo(selection);
        var badgeText = isInfiniteSelectionAmmo(player, stack, selection, representativeAmmo)
                ? "∞"
                : formatSelectionCount(count);
        int badgeColor = "∞".equals(badgeText) || count > 0 ? 0xFFFFFF : 0xFF5555;
        return new ModeSelectionView(
                selection.toKey(),
                resolveSelectionDisplayName(stack, selection),
                SelectionIconKind.ITEM,
                representativeAmmo,
                null,
                badgeText,
                badgeColor,
                isCurrentSelection,
                false,
                0.0F
        );
    }

    private static boolean isInfiniteSelectionAmmo(Player player, ItemStack bowStack, ModeSelection selection, ItemStack representativeAmmo) {
        if (selection.kind() == ShotModeKind.NORMAL || selection.kind() == ShotModeKind.MAGIC) {
            return false;
        }

        if (selection.kind() == ShotModeKind.ARROW) {
            return player.getAbilities().instabuild
                    || hasInfinity(bowStack)
                    || representativeAmmo.getItem() instanceof ArrowItem arrowItem
                    && arrowItem.isInfinite(representativeAmmo, bowStack, player);
        }

        return representativeAmmo.getItem() instanceof ArrowItem arrowItem
                && arrowItem.isInfinite(representativeAmmo, bowStack, player);
    }

    private static String formatSelectionCount(int count) {
        return count >= 1000 ? (count / 1000) + "k" : Integer.toString(Math.max(count, 0));
    }

    private static void setModeSelection(ItemStack stack, ModeSelection selection) {
        switch (selection.kind()) {
            case NORMAL -> clearAllModeTags(stack);
            case ARROW -> {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(SHOT_MODE_TAG, selection.kind().serializedName()));
                clearStoredValue(stack, MODE_TAG);
                clearStoredValue(stack, AMMO_SELECTION_TAG);
            }
            case SPECIAL, MOD -> {
                var selectionId = requireSelectionId(selection);
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putString(SHOT_MODE_TAG, selection.kind().serializedName());
                    tag.putString(AMMO_SELECTION_TAG, selectionId.toString());
                });
                clearStoredValue(stack, MODE_TAG);
            }
            case MAGIC -> {
                var selectionId = requireSelectionId(selection);
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putString(SHOT_MODE_TAG, selection.kind().serializedName());
                    tag.putString(MODE_TAG, selectionId.toString());
                });
                clearStoredValue(stack, AMMO_SELECTION_TAG);
            }
        }
        if (stack.getItem() instanceof ElementalBow elementalBow) {
            elementalBow.initializeSpellContainer(stack);
        }
    }

    private static void clearLegacyMagicMode(ItemStack stack) {
        clearStoredValue(stack, MODE_TAG);
    }

    private static void clearAllModeTags(ItemStack stack) {
        clearStoredValue(stack, SHOT_MODE_TAG);
        clearStoredValue(stack, MODE_TAG);
        clearStoredValue(stack, AMMO_SELECTION_TAG);
    }

    private static void clearStoredValue(ItemStack stack, String key) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(key));
    }

    private Component getModeDisplayName(ItemStack stack) {
        return resolveSelectionDisplayName(stack, normalizeModeState(stack));
    }

    private static Component resolveSelectionDisplayName(ItemStack stack, ModeSelection selection) {
        return switch (selection.kind()) {
            case NORMAL, ARROW, SPECIAL, MOD -> resolveAmmoDisplayName(selection).copy();
            case MAGIC -> resolveMagicDisplayName(stack, selection.id());
        };
    }

    private static Component resolveMagicDisplayName(ItemStack stack, @Nullable ResourceLocation selectionId) {
        if (selectionId == null) {
            return Component.literal("?");
        }

        var resolvedMode = ElementalBowModeManager.getResolvedDefinition(selectionId);
        if (resolvedMode == null) {
            return Component.literal(selectionId.toString());
        }

        var textColor = resolvedMode.color();
        var spellLevel = resolvedMode.resolveSpellLevel(stack);
        return resolvedMode.spell().getDisplayName(null).copy()
                .withStyle(style -> style.withColor(textColor))
                .append(Component.literal(" " + spellLevel).withStyle(style -> style.withColor(textColor)));
    }

    private static Component resolveAmmoDisplayName(ModeSelection selection) {
        if (selection.kind() == ShotModeKind.NORMAL) {
            return new ItemStack(Items.BOW).getHoverName();
        }
        if (selection.kind() == ShotModeKind.ARROW) {
            return new ItemStack(Items.ARROW).getHoverName();
        }
        if (selection.kind() == ShotModeKind.SPECIAL) {
            return selection.id() == null ? Component.literal("?") : resolveSpecialAmmoDisplayName(selection.id());
        }
        if (selection.kind() == ShotModeKind.MOD) {
            return selection.id() == null ? Component.literal("?") : resolveModArrowDisplayName(selection.id());
        }
        throw new IllegalStateException("Magic selection should be rendered by resolveMagicDisplayName");
    }

    private static Component resolveSpecialAmmoDisplayName(ResourceLocation selectionId) {
        if (SPECTRAL_ARROW_ID.equals(selectionId)) {
            return new ItemStack(Items.SPECTRAL_ARROW).getHoverName();
        }

        var potion = BuiltInRegistries.POTION.getOptional(selectionId).orElse(null);
        if (potion == null) {
            return Component.literal(selectionId.toString());
        }

        return PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, potion).getHoverName();
    }

    private static Component resolveModArrowDisplayName(ResourceLocation selectionId) {
        var item = BuiltInRegistries.ITEM.getOptional(selectionId).orElse(null);
        return item != null ? new ItemStack(item).getHoverName() : Component.literal(selectionId.toString());
    }

    private static ItemStack createRepresentativeAmmo(ModeSelection selection) {
        if (selection.kind() == ShotModeKind.NORMAL) {
            return new ItemStack(Items.BOW);
        }

        if (selection.kind() == ShotModeKind.ARROW) {
            return new ItemStack(Items.ARROW);
        }

        if (selection.kind() == ShotModeKind.SPECIAL && selection.id() != null) {
            if (SPECTRAL_ARROW_ID.equals(selection.id())) {
                return new ItemStack(Items.SPECTRAL_ARROW);
            }

            var potion = BuiltInRegistries.POTION.getOptional(selection.id()).orElse(null);
            return potion != null
                    ? PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, potion)
                    : new ItemStack(Items.TIPPED_ARROW);
        }

        if (selection.kind() == ShotModeKind.MOD && selection.id() != null) {
            var item = BuiltInRegistries.ITEM.getOptional(selection.id()).orElse(null);
            if (item instanceof ArrowItem) {
                return new ItemStack(item);
            }
        }

        return new ItemStack(Items.ARROW);
    }

    private static ResourceLocation requireSelectionId(ModeSelection selection) {
        if (selection.id() == null) {
            throw new IllegalStateException("Selection id is required for " + selection.kind().serializedName() + " mode");
        }
        return selection.id();
    }

    private static boolean isSupportedAdditionalElementalBowEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.PLUNDER)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.SYNTHESIS);
    }

    private static boolean bookContainsOnlySupportedAdditionalElementalBowEnchantments(ItemStack book) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return !enchantments.isEmpty()
                && enchantments.keySet().stream().allMatch(ElementalBow::isSupportedAdditionalElementalBowEnchantment);
    }

    private record SpellCastProfile(AbstractSpell spell, int spellLevel) {
    }

    public record DisplayedSpellProfile(AbstractSpell spell, int spellLevel) {
    }

    public enum SelectionIconKind {
        ITEM,
        SPELL
    }

    public record InventoryOverlayView(
            SelectionIconKind iconKind,
            ItemStack iconStack,
            @Nullable ResourceLocation spellIcon
    ) {
        public InventoryOverlayView {
            iconStack = iconStack.copy();
        }
    }

    public record ModeSelectionKey(String shotMode, @Nullable ResourceLocation selectionId) {
    }

    public record ModeSelectionView(
            ModeSelectionKey selection,
            Component displayName,
            SelectionIconKind iconKind,
            ItemStack iconStack,
            @Nullable ResourceLocation spellIcon,
            @Nullable String badgeText,
            int badgeColor,
            boolean currentSelection,
            boolean overheatActive,
            float overheatFillRatio
    ) {
        public ModeSelectionView {
            iconStack = iconStack.copy();
            overheatFillRatio = Mth.clamp(overheatFillRatio, 0.0F, 1.0F);
        }
    }

    private record AmmoInventorySummary(
            int normalArrowCount,
            Map<ResourceLocation, Integer> specialArrowCounts,
            Map<ResourceLocation, Integer> modArrowCounts
    ) {
    }

    @Nullable
    private static CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static int getEnchantmentLevel(ItemStack stack, ResourceLocation enchantmentId) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return 0;
        }

        for (var enchantment : enchantments.keySet()) {
            var enchantmentKey = enchantment.unwrapKey().orElse(null);
            if (enchantmentKey != null && enchantmentId.equals(enchantmentKey.location())) {
                return enchantments.getLevel(enchantment);
            }
        }

        return 0;
    }

    private interface AmmoSource {
        ItemStack stack();

        boolean consume();

        default boolean isInfinite(ItemStack bowStack, Player player) {
            return stack().getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(stack(), bowStack, player);
        }
    }

    private record LooseAmmoSource(ItemStack stack) implements AmmoSource {
        @Override
        public boolean consume() {
            stack.shrink(1);
            return true;
        }
    }

    private record StoredAmmoSource(ItemStack stack, BooleanSupplier consumer) implements AmmoSource {
        @Override
        public boolean consume() {
            return consumer.getAsBoolean();
        }
    }

    private enum ShotModeKind {
        NORMAL("normal"),
        ARROW("arrow"),
        SPECIAL("special"),
        MOD("mod"),
        MAGIC("magic");

        private final String serializedName;

        ShotModeKind(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        @Nullable
        public static ShotModeKind fromSerializedName(String serializedName) {
            for (var value : values()) {
                if (value.serializedName.equals(serializedName)) {
                    return value;
                }
            }
            return null;
        }
    }

    private record ModeSelection(ShotModeKind kind, @Nullable ResourceLocation id) {
        private static ModeSelection normal() {
            return new ModeSelection(ShotModeKind.NORMAL, null);
        }

        private static ModeSelection arrow() {
            return new ModeSelection(ShotModeKind.ARROW, null);
        }

        private ModeSelectionKey toKey() {
            return new ModeSelectionKey(kind.serializedName(), id);
        }
    }
}
