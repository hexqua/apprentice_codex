package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.ResolvedDefinition;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.renderer.item.ElementalBowRenderer;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ElementalBow extends BowItem implements GeoItem, IPresetSpellContainer, ArcaneAnvilImbueBlockItem {
    public static final int READY_DRAW_TICKS = 22;
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
        super(new Properties().durability(384));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ElementalBowRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ElementalBowRenderer();
                }

                return renderer;
            }
        });
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
            case SPECIAL, MOD -> useTrackedArrowMode(level, player, usedHand, stack, selection);
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

        var notificationStack = isHeldElementalBow(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
        if (stack != notificationStack) {
            return;
        }

        for (var cooledSchoolId : ElementalBowOverheatManager.collectCooledSchoolsWhileHolding(player)) {
            player.displayClientMessage(
                    Component.translatable(
                                    "ui.apprenticecodex.elemental_bow.cooling_complete",
                                    resolveSchoolDisplayName(cooledSchoolId)
                            )
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        }
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel)) {
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
        if (drawDuration <= 0 || drawDuration >= READY_DRAW_TICKS) {
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
        if (selection.kind() == ShotModeKind.NORMAL) {
            releaseVanillaShot(stack, level, player, timeLeft);
            return;
        }

        if (selection.kind() == ShotModeKind.MAGIC) {
            var mode = resolveConfiguredMagicMode(stack);
            if (mode != null) {
                releaseElementalShot(stack, level, player, timeLeft, mode);
            }
            return;
        }

        releaseTrackedArrowShot(stack, level, player, timeLeft, selection);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return Items.BOW.supportsEnchantment(ENCHANTMENT_PROBE_STACK, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return Items.BOW.isPrimaryItemFor(ENCHANTMENT_PROBE_STACK, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return Items.BOW.isBookEnchantable(ENCHANTMENT_PROBE_STACK, book);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        initializeSpellContainer(stack);
        super.appendHoverText(stack, context, lines, flag);
        lines.add(
                Component.translatable("item.apprenticecodex.elemental_bow.mode", getModeDisplayName(stack))
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private InteractionResultHolder<ItemStack> useNormalArrowMode(Level level, Player player, InteractionHand usedHand, ItemStack stack) {
        var projectile = player.getProjectile(stack);
        if (!projectile.isEmpty() || !hasInfinity(stack) || player.getAbilities().instabuild) {
            return super.use(level, player, usedHand);
        }

        var nockResult = EventHooks.onArrowNock(stack, level, player, usedHand, true);
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
        var canFireWithoutAmmo = player.getAbilities().instabuild;
        if (ammoSource == null && !canFireWithoutAmmo) {
            return InteractionResultHolder.fail(stack);
        }

        var nockResult = EventHooks.onArrowNock(stack, level, player, usedHand, ammoSource != null || canFireWithoutAmmo);
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
        var canFireWithoutAmmo = player.getAbilities().instabuild;
        if (ammoSource == null && !canFireWithoutAmmo) {
            return InteractionResultHolder.fail(stack);
        }

        var profile = resolveSpellProfile(stack, mode);
        if (profile == null) {
            return InteractionResultHolder.fail(stack);
        }

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

        var nockResult = EventHooks.onArrowNock(stack, level, player, usedHand, ammoSource != null || canFireWithoutAmmo);
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
        var ammoSource = resolveVanillaAmmoSource(player, stack);
        var hasAmmo = ammoSource != null && !ammoSource.isEmpty();
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(stack, level, player, drawDuration, hasAmmo || canFireWithoutAmmo);
        if (drawDuration < 0) {
            return;
        }

        if (!hasAmmo && !canFireWithoutAmmo) {
            return;
        }

        var ammoStack = hasAmmo ? ammoSource : new ItemStack(Items.ARROW);
        var infiniteAmmo = player.getAbilities().instabuild
                || ammoStack.getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(ammoStack, stack, player)
                || (!hasAmmo && hasInfinity(stack));
        var power = getPowerForTime(drawDuration);
        if (power < 0.1F) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        fireVanillaArrow(level, player, stack, ammoStack, power, infiniteAmmo);
        if (!player.getAbilities().instabuild && hasAmmo && !infiniteAmmo) {
            consumeAmmo(player, ammoSource);
        }
        triggerReleaseAnimation(player, stack);
    }

    private void releaseTrackedArrowShot(ItemStack stack, Level level, Player player, int timeLeft, ModeSelection selection) {
        var ammoSource = resolveAmmoSource(player, stack, selection);
        var hasAmmo = ammoSource != null && !ammoSource.isEmpty();
        var canFireWithoutAmmo = player.getAbilities().instabuild;
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(stack, level, player, drawDuration, hasAmmo || canFireWithoutAmmo);
        if (drawDuration < 0) {
            return;
        }

        if (!hasAmmo && !canFireWithoutAmmo) {
            return;
        }

        var ammoStack = hasAmmo ? ammoSource : createRepresentativeAmmo(selection);
        var infiniteAmmo = player.getAbilities().instabuild
                || ammoStack.getItem() instanceof ArrowItem arrowItem
                && arrowItem.isInfinite(ammoStack, stack, player);
        var power = getPowerForTime(drawDuration);
        if (power < 0.1F) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        fireVanillaArrow(level, player, stack, ammoStack, power, infiniteAmmo);
        if (!player.getAbilities().instabuild && hasAmmo && !infiniteAmmo) {
            consumeAmmo(player, ammoSource);
        }
        triggerReleaseAnimation(player, stack);
    }

    private void releaseElementalShot(ItemStack stack, Level level, Player player, int timeLeft, ResolvedDefinition mode) {
        var ammoSource = resolveVanillaAmmoSource(player, stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild;
        var drawDuration = stack.getUseDuration(player) - timeLeft;
        drawDuration = EventHooks.onArrowLoose(stack, level, player, drawDuration, ammoSource != null || canFireWithoutAmmo);
        if (drawDuration < READY_DRAW_TICKS) {
            return;
        }

        // 属性ショットは server 側でのみ最終判定と詠唱を行う。
        // client 側でも同じ再判定を通すと、server が先にマナを消費した直後の同期値を見て
        // 「現在マナ 0」の不足表示だけが二重に出ることがある。
        if (level.isClientSide) {
            return;
        }

        var profile = resolveSpellProfile(stack, mode);
        if (profile == null) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            if (ammoSource == null) {
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
            if (ammoSource != null) {
                consumeAmmo(player, ammoSource);
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

    @Nullable
    private SpellCastProfile resolveSpellProfile(ItemStack stack, @Nullable ResolvedDefinition mode) {
        if (mode == null) {
            return null;
        }
        return new SpellCastProfile(mode.spell(), mode.resolveSpellLevel(stack));
    }

    @Nullable
    public static DisplayedSpellProfile getDisplayedSpellProfile(ItemStack stack) {
        if (!(stack.getItem() instanceof ElementalBow elementalBow)) {
            return null;
        }

        var profile = elementalBow.resolveSpellProfile(stack, elementalBow.resolveConfiguredMagicMode(stack));
        if (profile == null) {
            return null;
        }
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
        var selections = buildAvailableSelections(currentSelection, ammoSummary, true);
        var views = new ArrayList<ModeSelectionView>(selections.size());
        for (var selection : selections) {
            views.add(createSelectionView(stack, selection, currentSelection, ammoSummary));
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
        for (var selectable : buildAvailableSelections(currentSelection, summarizeAmmoInventory(player), true)) {
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
        return selectionId != null ? new ModeSelection(shotMode, selectionId) : null;
    }

    @Nullable
    private ItemStack resolveAmmoSource(Player player, ItemStack bowStack, ModeSelection selection) {
        return switch (selection.kind()) {
            case NORMAL, MAGIC -> resolveVanillaAmmoSource(player, bowStack);
            case SPECIAL -> resolveSpecialAmmoSource(player, selection.id());
            case MOD -> resolveModAmmoSource(player, selection.id());
        };
    }

    @Nullable
    private ItemStack resolveVanillaAmmoSource(Player player, ItemStack bowStack) {
        // 外部矢筒系は格納仕様/API が mod ごとに揺れやすいため、現時点ではまず vanilla の矢取得面を固定する。
        // Supplementaries / Relics の個別連携は、1 本消費経路を安全に確定できた時点で別差分に分離する。
        var projectile = player.getProjectile(bowStack);
        return projectile.isEmpty() ? null : projectile;
    }

    @Nullable
    private ItemStack resolveSpecialAmmoSource(Player player, @Nullable ResourceLocation selectionId) {
        if (selectionId == null) {
            return null;
        }

        for (var ammoStack : collectCandidateAmmoStacks(player)) {
            if (ammoStack.isEmpty()) {
                continue;
            }

            if (SPECTRAL_ARROW_ID.equals(selectionId)) {
                if (ammoStack.is(Items.SPECTRAL_ARROW)) {
                    return ammoStack;
                }
                continue;
            }

            if (!ammoStack.is(Items.TIPPED_ARROW)) {
                continue;
            }

            var potion = PotionContentsHelper.getPotion(ammoStack);
            var potionId = potion == null ? null : BuiltInRegistries.POTION.getKey(potion);
            if (selectionId.equals(potionId)) {
                return ammoStack;
            }
        }
        return null;
    }

    @Nullable
    private ItemStack resolveModAmmoSource(Player player, @Nullable ResourceLocation selectionId) {
        if (selectionId == null) {
            return null;
        }

        for (var ammoStack : collectCandidateAmmoStacks(player)) {
            if (ammoStack.isEmpty()) {
                continue;
            }

            var item = ammoStack.getItem();
            var itemId = BuiltInRegistries.ITEM.getKey(item);
            if (item instanceof ArrowItem && !VANILLA_ARROW_ITEMS.contains(item) && selectionId.equals(itemId)) {
                return ammoStack;
            }
        }
        return null;
    }

    private void consumeAmmo(Player player, ItemStack ammoStack) {
        ammoStack.shrink(1);
    }

    private float getAdditionalManaCost(Player player, ResolvedDefinition mode, SpellCastProfile profile) {
        return ElementalBowOverheatManager.getAdditionalManaCost(player, mode.schoolId(), profile.spell().getManaCost(profile.spellLevel()));
    }

    private void displayOverheatManaWarning(Player player, ItemStack stack, ResolvedDefinition mode) {
        var profile = resolveSpellProfile(stack, mode);
        if (profile == null) {
            return;
        }

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

    private static Component resolveSchoolDisplayName(ResourceLocation schoolId) {
        var mode = ElementalBowModeManager.getResolvedDefinition(schoolId);
        return mode != null ? mode.schoolType().getDisplayName() : Component.literal(schoolId.toString());
    }

    private static boolean hasInfinity(ItemStack stack) {
        return getEnchantmentLevel(stack, Enchantments.INFINITY.location()) > 0;
    }

    public static double resolveDrawAnimationSpeed(@Nullable ItemStack stack) {
        return DRAW_ANIMATION_SOURCE_SECONDS / resolveDrawDurationSeconds(stack);
    }

    private static float resolveDrawDurationSeconds(@Nullable ItemStack stack) {
        return READY_DRAW_TICKS / 20.0F;
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

    private static List<ModeSelection> buildAvailableSelections(
            ModeSelection currentSelection,
            AmmoInventorySummary ammoSummary,
            boolean preserveCurrentUnavailableSelection
    ) {
        var selections = new ArrayList<ModeSelection>();
        selections.add(ModeSelection.normal());
        selections.addAll(collectSpecialArrowSelections(ammoSummary, currentSelection, preserveCurrentUnavailableSelection));
        selections.addAll(collectModArrowSelections(ammoSummary, currentSelection, preserveCurrentUnavailableSelection));
        for (var resolvedDefinition : ElementalBowModeManager.getResolvedDefinitions()) {
            selections.add(new ModeSelection(ShotModeKind.MAGIC, resolvedDefinition.schoolId()));
        }
        return selections;
    }

    private static List<ModeSelection> collectSpecialArrowSelections(
            AmmoInventorySummary ammoSummary,
            ModeSelection currentSelection,
            boolean preserveCurrentUnavailableSelection
    ) {
        var selections = new ArrayList<ModeSelection>();
        if (ammoSummary.specialArrowCounts().containsKey(SPECTRAL_ARROW_ID)
                || shouldPreserveUnavailableCurrentSelection(currentSelection, preserveCurrentUnavailableSelection, ShotModeKind.SPECIAL, SPECTRAL_ARROW_ID)) {
            selections.add(new ModeSelection(ShotModeKind.SPECIAL, SPECTRAL_ARROW_ID));
        }

        for (var potion : BuiltInRegistries.POTION) {
            var potionId = BuiltInRegistries.POTION.getKey(potion);
            if (potionId != null && (ammoSummary.specialArrowCounts().containsKey(potionId)
                    || shouldPreserveUnavailableCurrentSelection(currentSelection, preserveCurrentUnavailableSelection, ShotModeKind.SPECIAL, potionId))) {
                selections.add(new ModeSelection(ShotModeKind.SPECIAL, potionId));
            }
        }

        return selections;
    }

    private static List<ModeSelection> collectModArrowSelections(
            AmmoInventorySummary ammoSummary,
            ModeSelection currentSelection,
            boolean preserveCurrentUnavailableSelection
    ) {
        var selections = new ArrayList<ModeSelection>();

        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ArrowItem) || VANILLA_ARROW_ITEMS.contains(item)) {
                continue;
            }

            var itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null && (ammoSummary.modArrowCounts().containsKey(itemId)
                    || shouldPreserveUnavailableCurrentSelection(currentSelection, preserveCurrentUnavailableSelection, ShotModeKind.MOD, itemId))) {
                selections.add(new ModeSelection(ShotModeKind.MOD, itemId));
            }
        }

        return selections;
    }

    private static boolean shouldPreserveUnavailableCurrentSelection(
            ModeSelection currentSelection,
            boolean preserveCurrentUnavailableSelection,
            ShotModeKind kind,
            ResourceLocation selectionId
    ) {
        return preserveCurrentUnavailableSelection
                && currentSelection.kind() == kind
                && selectionId.equals(currentSelection.id());
    }

    private static AmmoInventorySummary summarizeAmmoInventory(Player player) {
        int normalArrowCount = 0;
        var specialArrowCounts = new LinkedHashMap<ResourceLocation, Integer>();
        var modArrowCounts = new LinkedHashMap<ResourceLocation, Integer>();

        for (var ammoStack : collectCandidateAmmoStacks(player)) {
            if (ammoStack.isEmpty()) {
                continue;
            }

            if (ammoStack.is(Items.ARROW)) {
                normalArrowCount += ammoStack.getCount();
                continue;
            }

            if (ammoStack.is(Items.SPECTRAL_ARROW)) {
                specialArrowCounts.merge(SPECTRAL_ARROW_ID, ammoStack.getCount(), Integer::sum);
                continue;
            }

            if (ammoStack.is(Items.TIPPED_ARROW)) {
                var potion = PotionContentsHelper.getPotion(ammoStack);
                var potionId = potion == null ? null : BuiltInRegistries.POTION.getKey(potion);
                if (potionId != null) {
                    specialArrowCounts.merge(potionId, ammoStack.getCount(), Integer::sum);
                }
                continue;
            }

            if (ammoStack.getItem() instanceof ArrowItem arrowItem && !VANILLA_ARROW_ITEMS.contains(arrowItem)) {
                var itemId = BuiltInRegistries.ITEM.getKey(arrowItem);
                if (itemId != null) {
                    modArrowCounts.merge(itemId, ammoStack.getCount(), Integer::sum);
                }
            }
        }

        return new AmmoInventorySummary(normalArrowCount, Map.copyOf(specialArrowCounts), Map.copyOf(modArrowCounts));
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
            return new ModeSelectionView(
                    selection.toKey(),
                    resolveSelectionDisplayName(stack, selection),
                    SelectionIconKind.SPELL,
                    ItemStack.EMPTY,
                    spellIcon,
                    levelText,
                    badgeColor,
                    isCurrentSelection
            );
        }

        int count = switch (selection.kind()) {
            case NORMAL -> ammoSummary.normalArrowCount();
            case SPECIAL -> selection.id() == null ? 0 : ammoSummary.specialArrowCounts().getOrDefault(selection.id(), 0);
            case MOD -> selection.id() == null ? 0 : ammoSummary.modArrowCounts().getOrDefault(selection.id(), 0);
            case MAGIC -> 0;
        };
        var badgeText = selection.kind() == ShotModeKind.NORMAL && hasInfinity(stack)
                ? "∞"
                : formatSelectionCount(count);
        int badgeColor = isCurrentSelection && count <= 0 ? 0xFF5555 : 0xFFFFFF;
        return new ModeSelectionView(
                selection.toKey(),
                resolveSelectionDisplayName(stack, selection),
                SelectionIconKind.ITEM,
                createRepresentativeAmmo(selection),
                null,
                badgeText,
                badgeColor,
                isCurrentSelection
        );
    }

    private static String formatSelectionCount(int count) {
        return count >= 1000 ? (count / 1000) + "k" : Integer.toString(Math.max(count, 0));
    }

    private static void setModeSelection(ItemStack stack, ModeSelection selection) {
        switch (selection.kind()) {
            case NORMAL -> clearAllModeTags(stack);
            case SPECIAL, MOD -> {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putString(SHOT_MODE_TAG, selection.kind().serializedName());
                    tag.putString(AMMO_SELECTION_TAG, selection.id().toString());
                });
                clearStoredValue(stack, MODE_TAG);
            }
            case MAGIC -> {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putString(SHOT_MODE_TAG, selection.kind().serializedName());
                    tag.putString(MODE_TAG, selection.id().toString());
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
            case NORMAL, SPECIAL, MOD -> resolveAmmoDisplayName(selection).copy();
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
        return switch (selection.kind()) {
            case NORMAL -> new ItemStack(Items.ARROW).getHoverName();
            case SPECIAL -> selection.id() == null ? Component.literal("?") : resolveSpecialAmmoDisplayName(selection.id());
            case MOD -> selection.id() == null ? Component.literal("?") : resolveModArrowDisplayName(selection.id());
            case MAGIC -> Component.literal(selection.id() == null ? "?" : selection.id().toString());
        };
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

    private record SpellCastProfile(AbstractSpell spell, int spellLevel) {
    }

    public record DisplayedSpellProfile(AbstractSpell spell, int spellLevel) {
    }

    public enum SelectionIconKind {
        ITEM,
        SPELL
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
            boolean currentSelection
    ) {
        public ModeSelectionView {
            iconStack = iconStack.copy();
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

    private enum ShotModeKind {
        NORMAL("normal"),
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

        private ModeSelectionKey toKey() {
            return new ModeSelectionKey(kind.serializedName(), id);
        }
    }
}
