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
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ElementalBowRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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

import java.util.List;
import java.util.function.Consumer;

public class ElementalBow extends BowItem implements GeoItem, IPresetSpellContainer, ArcaneAnvilImbueBlockItem {
    public static final int READY_DRAW_TICKS = 22;
    private static final String MAIN_CONTROLLER = "main";
    private static final String RELEASE_ANIMATION = "release";
    private static final String MODE_TAG = "ElementalBowMode";
    private static final ItemStack ENCHANTMENT_PROBE_STACK = new ItemStack(Items.BOW);
    private static final float MANA_SAFE_MARGIN = 0.001F;
    private static final float PARTICLE_SIZE = 0.12F;
    private static final int PARTICLE_WHITEN_TICKS = 2;
    private static final int OVERHEAT_WARNING_INTERVAL_TICKS = 10;
    private static final float DRAW_ANIMATION_SOURCE_SECONDS = 0.32F;
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
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                var nextModeId = resolveNextModeId(stack);
                setMode(stack, nextModeId);
                player.displayClientMessage(
                        Component.translatable(
                                        "ui.apprenticecodex.elemental_bow.mode_switched",
                                        getModeDisplayName(stack)
                                )
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 1.1F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        var mode = normalizeModeState(stack);
        if (mode == null) {
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

        var ammoSource = resolveAmmoSource(player, stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
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

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var mode = normalizeModeState(stack);
        var profile = resolveSpellProfile(stack, mode);
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

        var mode = normalizeModeState(stack);
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
        if (drawDuration % 2 != 0) {
            return;
        }

        spawnChargeParticles(serverLevel, entity, mode);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        initializeSpellContainer(stack);

        var mode = normalizeModeState(stack);
        if (mode == null) {
            releaseVanillaShot(stack, level, player, timeLeft);
            return;
        }

        releaseElementalShot(stack, level, player, timeLeft, mode);
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

    private void releaseVanillaShot(ItemStack stack, Level level, Player player, int timeLeft) {
        var ammoSource = resolveAmmoSource(player, stack);
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

    private void releaseElementalShot(ItemStack stack, Level level, Player player, int timeLeft, ResolvedDefinition mode) {
        var ammoSource = resolveAmmoSource(player, stack);
        var canFireWithoutAmmo = player.getAbilities().instabuild || hasInfinity(stack);
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
            if (ammoSource == null && !hasInfinity(stack)) {
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
            if (ammoSource != null && !hasInfinity(stack)) {
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

    private void spawnChargeParticles(ServerLevel level, LivingEntity entity, ResolvedDefinition mode) {
        var color = mode.color();
        var red = ((color >> 16) & 0xFF) / 255.0F;
        var green = ((color >> 8) & 0xFF) / 255.0F;
        var blue = (color & 0xFF) / 255.0F;
        var look = entity.getLookAngle();
        var side = look.cross(new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.0001D) {
            side = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize().scale(0.16D);
        var base = entity.getEyePosition().add(look.scale(0.45D)).add(0.0D, -0.18D, 0.0D);

        level.sendParticles(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), PARTICLE_SIZE, red, green, blue, PARTICLE_WHITEN_TICKS),
                base.x + side.x,
                base.y + side.y,
                base.z + side.z,
                1,
                0.01D,
                0.01D,
                0.01D,
                0.0D
        );
        level.sendParticles(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), PARTICLE_SIZE, red, green, blue, PARTICLE_WHITEN_TICKS),
                base.x - side.x,
                base.y - side.y,
                base.z - side.z,
                1,
                0.01D,
                0.01D,
                0.01D,
                0.0D
        );
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

        var profile = elementalBow.resolveSpellProfile(stack, elementalBow.resolveConfiguredMode(stack));
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

        var mode = elementalBow.resolveConfiguredMode(stack);
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

    @Nullable
    private ItemStack resolveAmmoSource(Player player, ItemStack bowStack) {
        // 外部矢筒系は格納仕様/API が mod ごとに揺れやすいため、現時点ではまず vanilla の矢取得面を固定する。
        // Supplementaries / Relics の個別連携は、1 本消費経路を安全に確定できた時点で別差分に分離する。
        var projectile = player.getProjectile(bowStack);
        return projectile.isEmpty() ? null : projectile;
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
    private ResolvedDefinition resolveConfiguredMode(ItemStack stack) {
        return ElementalBowModeManager.getResolvedDefinition(getStoredModeId(stack));
    }

    @Nullable
    private ResolvedDefinition normalizeModeState(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(MODE_TAG)) {
            return null;
        }

        var storedModeId = ResourceLocation.tryParse(tag.getString(MODE_TAG));
        var resolvedMode = ElementalBowModeManager.getResolvedDefinition(storedModeId);
        if (resolvedMode != null) {
            return resolvedMode;
        }

        clearStoredMode(stack);
        return null;
    }

    @Nullable
    private static ResourceLocation getStoredModeId(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(MODE_TAG)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(MODE_TAG));
    }

    @Nullable
    private static ResourceLocation resolveNextModeId(ItemStack stack) {
        var resolvedDefinitions = ElementalBowModeManager.getResolvedDefinitions();
        if (resolvedDefinitions.isEmpty()) {
            return null;
        }

        var currentModeId = getStoredModeId(stack);
        if (currentModeId == null) {
            return resolvedDefinitions.getFirst().schoolId();
        }

        for (int index = 0; index < resolvedDefinitions.size(); index++) {
            if (!resolvedDefinitions.get(index).schoolId().equals(currentModeId)) {
                continue;
            }
            return index + 1 < resolvedDefinitions.size() ? resolvedDefinitions.get(index + 1).schoolId() : null;
        }

        return resolvedDefinitions.getFirst().schoolId();
    }

    private static void setMode(ItemStack stack, @Nullable ResourceLocation modeId) {
        if (modeId == null) {
            clearStoredMode(stack);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(MODE_TAG, modeId.toString()));
        }
        if (stack.getItem() instanceof ElementalBow elementalBow) {
            elementalBow.initializeSpellContainer(stack);
        }
    }

    private static void clearStoredMode(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(MODE_TAG));
    }

    private Component getModeDisplayName(ItemStack stack) {
        var mode = resolveConfiguredMode(stack);
        return mode != null ? mode.schoolType().getDisplayName() : Component.translatable("item.apprenticecodex.elemental_bow.mode.none");
    }

    private record SpellCastProfile(AbstractSpell spell, int spellLevel) {
    }

    public record DisplayedSpellProfile(AbstractSpell spell, int spellLevel) {
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
}
