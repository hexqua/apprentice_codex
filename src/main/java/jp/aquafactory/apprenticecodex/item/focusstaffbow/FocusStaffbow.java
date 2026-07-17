package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientConfigState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientLoanState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientRenderState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
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
import java.util.List;
import java.util.Set;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;

public final class FocusStaffbow extends CastingItem
        implements GeoItem, NonDamageableAnvilMergeItem, UniqueItem, CastAnimationOverrideItem, IJeiInfoItem,
        WisdomPolicy, PlunderTarget {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.focus_staffbow.desc_";
    private static final int MAX_USE_DURATION = 72000;
    private static final float CLIENT_MANA_SAFE_MARGIN = 0.001F;
    private static final Set<ResourceLocation> ALLOWED_EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "synthesis")
    );
    private static final Set<ResourceLocation> EXCLUDED_EXTRA_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "plunder"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "transcendence")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ResourceLocation SPELL_POWER_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex",
            "focus_staffbow_mainhand_spell_power"
    );
    private static final String BASE_CONTROLLER = "base";
    private static final String OVERLAY_CONTROLLER = "overlay";
    private static final String OVERLAY_IDLE_ANIMATION = "overlay_idle";
    private static final String CHARGE_RIGHT_ANIMATION = "charge_right";
    private static final String CHARGE_LEFT_ANIMATION = "charge_left";
    private static final String RELEASE_RIGHT_ANIMATION = "release_right";
    private static final String RELEASE_LEFT_ANIMATION = "release_left";
    private static final double CHARGING_CORE_IDLE_SPEED = 8.0D;
    private static final RawAnimation ANIM_CORE_IDLE = RawAnimation.begin().thenLoop("core_idle");
    private static final RawAnimation ANIM_OVERLAY_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_CHARGE_RIGHT = RawAnimation.begin().thenPlayAndHold("charge_right");
    private static final RawAnimation ANIM_CHARGE_LEFT = RawAnimation.begin().thenPlayAndHold("charge_left");
    private static final RawAnimation ANIM_RELEASE_RIGHT = RawAnimation.begin().thenPlay("release_right");
    private static final RawAnimation ANIM_RELEASE_LEFT = RawAnimation.begin().thenPlay("release_left");
    private static final int ENCHANTMENT_VALUE = 20;
    private static final double ATTACK_DAMAGE_BONUS = 3.0D;
    private static final double ATTACK_SPEED_BONUS = -3.0D;
    private static final double SPELL_POWER_BONUS = 0.1D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers mainhandModifiers = buildMainhandModifiers();

    public FocusStaffbow() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant()
                .attributes(buildMainhandModifiers()));
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
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.focus_staffbow.deny_offhand")
                                .withStyle(ChatFormatting.RED)
                ));
            }
            return InteractionResultHolder.fail(stack);
        }

        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData == SpellData.EMPTY || selection.spellData.getSpell() == SpellRegistry.none()) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            if (!canStartClientUse(player, stack, selection.spellData)) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        }

        var handled = FocusStaffbowCastManager.handleSelectedSpellInput(player, stack);
        if (!handled) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    public static Component createLoanBlockedMessage(float remainingLoanMana) {
        return Component.translatable(
                "ui.apprenticecodex.focus_staffbow.loan_mana",
                Mth.ceil(Math.max(0.0F, remainingLoanMana))
        ).withStyle(ChatFormatting.RED);
    }

    public static Component createInsufficientArrowMessage() {
        return Component.translatable("ui.apprenticecodex.focus_staffbow.insufficient_arrow")
                .withStyle(ChatFormatting.RED);
    }

    public static Component createContinuousDisabledMessage() {
        return Component.translatable("ui.apprenticecodex.focus_staffbow.continuous_disabled")
                .withStyle(ChatFormatting.RED);
    }

    public static Component createManaLoanDisabledMessage() {
        return Component.translatable("ui.apprenticecodex.focus_staffbow.loan_disabled")
                .withStyle(ChatFormatting.RED);
    }

    public static Component createManaLoanLimitMessage(float missingMana, float maxLoanMana) {
        return Component.translatable(
                "ui.apprenticecodex.focus_staffbow.loan_limit",
                Mth.ceil(Math.max(0.0F, missingMana)),
                Mth.ceil(Math.max(0.0F, maxLoanMana))
        ).withStyle(ChatFormatting.RED);
    }

    public static Component createSpellDenylistedMessage(Component spellName) {
        return Component.translatable("ui.apprenticecodex.focus_staffbow.spell_denylisted", spellName)
                .withStyle(ChatFormatting.RED);
    }

    public static Component createSpellNotAllowlistedMessage(Component spellName) {
        return Component.translatable("ui.apprenticecodex.focus_staffbow.spell_not_allowlisted", spellName)
                .withStyle(ChatFormatting.RED);
    }

    public static boolean isBowDrawUse(@Nullable LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) {
            return false;
        }

        if (entity.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }

        var useItem = entity.getUseItem();
        return !useItem.isEmpty() && useItem.getItem() instanceof FocusStaffbow;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }

        if (FocusStaffbowCastManager.hasActiveContinuousCast(player)) {
            FocusStaffbowCastManager.releaseContinuousCast(player, stack);
            return;
        }

        FocusStaffbowCastManager.releasePendingCast(player, stack, stack.getUseDuration(livingEntity) - timeLeft);
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
        return mainhandModifiers;
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
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null || EXCLUDED_EXTRA_ENCHANTMENTS.contains(enchantmentId)) {
            return false;
        }
        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }
        if (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)
                || MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }
        return ALLOWED_EXTRA_ENCHANTMENTS.contains(enchantmentId);
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
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, BASE_CONTROLLER, 0, state -> {
            var stack = state.getData(DataTickets.ITEMSTACK);
            var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            state.setAnimation(ANIM_CORE_IDLE);
            state.getController().setAnimationSpeed(
                    FocusStaffbowClientRenderState.shouldAccelerateCoreIdle(stack, perspective)
                            ? CHARGING_CORE_IDLE_SPEED
                            : 1.0D
            );
            return PlayState.CONTINUE;
        }));
        controllerRegistrar.add(new AnimationController<>(this, OVERLAY_CONTROLLER, 0, state -> {
            state.setAnimation(ANIM_OVERLAY_IDLE);
            state.getController().setAnimationSpeed(1.0D);
            return PlayState.CONTINUE;
        }).triggerableAnim(OVERLAY_IDLE_ANIMATION, ANIM_OVERLAY_IDLE)
                .triggerableAnim(CHARGE_RIGHT_ANIMATION, ANIM_CHARGE_RIGHT)
                .triggerableAnim(CHARGE_LEFT_ANIMATION, ANIM_CHARGE_LEFT)
                .triggerableAnim(RELEASE_RIGHT_ANIMATION, ANIM_RELEASE_RIGHT)
                .triggerableAnim(RELEASE_LEFT_ANIMATION, ANIM_RELEASE_LEFT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, AbstractSpell spell) {
        return spell != null;
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public boolean shouldOverrideCastFinishAnimation(ItemStack stack, AbstractSpell spell) {
        return spell != null;
    }

    @Override
    public AnimationHolder getCastFinishAnimation(ItemStack stack, AbstractSpell spell, boolean cancelled) {
        return AnimationHolder.none();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, AbstractSpell spell) {
        return false;
    }

    public void triggerChargeAnimation(ServerPlayer serverPlayer, ItemStack stack) {
        triggerOverlayAnimation(serverPlayer, stack, resolveChargeAnimation(serverPlayer));
    }

    public void triggerIdleAnimation(ServerPlayer serverPlayer, ItemStack stack) {
        triggerOverlayAnimation(serverPlayer, stack, OVERLAY_IDLE_ANIMATION);
    }

    public void triggerCastCompletionAnimation(ServerPlayer serverPlayer, ItemStack stack, boolean cancelled) {
        if (cancelled) {
            triggerIdleAnimation(serverPlayer, stack);
            return;
        }

        triggerOverlayAnimation(serverPlayer, stack, resolveReleaseAnimation(serverPlayer));
    }

    private static boolean isDurabilityTargetEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.value().canEnchant(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static boolean canStartClientUse(Player player, ItemStack stack, SpellData spellData) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == SpellRegistry.none()) {
            return false;
        }
        if (FocusStaffbowClientLoanState.hasOutstandingLoan()) {
            return false;
        }

        var spell = spellData.getSpell();
        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spell.getSpellId());
        if (cooldown != null && cooldown.getCooldownRemaining() > 0.0F) {
            return false;
        }
        if (spell.getCastType() == io.redspace.ironsspellbooks.api.spells.CastType.CONTINUOUS
                && !FocusStaffbowClientConfigState.continuousFocusedCastEnabled()) {
            return false;
        }
        if (!BowCastAmmoResolver.canStartFocusStaffbowUse(
                player,
                stack,
                FocusStaffbowClientConfigState.arrowCatalystRequired(),
                FocusStaffbowClientConfigState.arrowCatalystItemIds()
        )) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        return ClientMagicData.getPlayerMana() + CLIENT_MANA_SAFE_MARGIN >= spell.getManaCost(spellLevel);
    }

    private static ItemAttributeModifiers buildMainhandModifiers() {
        var builder = ItemAttributeModifiers.builder();
        builder.add(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        ATTACK_DAMAGE_BONUS,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_BONUS,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                AttributeRegistry.SPELL_POWER,
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        return builder.build();
    }

    private void triggerOverlayAnimation(ServerPlayer serverPlayer, ItemStack stack, String animationName) {
        var instanceId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        triggerAnim(serverPlayer, instanceId, OVERLAY_CONTROLLER, animationName);
    }

    private static String resolveChargeAnimation(Player player) {
        return player.getMainArm() == HumanoidArm.LEFT ? CHARGE_LEFT_ANIMATION : CHARGE_RIGHT_ANIMATION;
    }

    private static String resolveReleaseAnimation(Player player) {
        return player.getMainArm() == HumanoidArm.LEFT ? RELEASE_LEFT_ANIMATION : RELEASE_RIGHT_ANIMATION;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        if (!FocusStaffbowClientConfigState.arrowCatalystRequired()) {
            lines.add(Component.translatable(getDescriptionId() + ".require_arrow.disabled").withStyle(ChatFormatting.GRAY));
        } else if (getEnchantmentLevel(stack, jp.aquafactory.apprenticecodex.enchantment.Enchantments.SYNTHESIS.location()) > 0) {
            lines.add(Component.translatable(getDescriptionId() + ".require_arrow.with_synthesis").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(getDescriptionId() + ".require_arrow").withStyle(ChatFormatting.GRAY));
        }
        if (FocusStaffbowClientLoanState.hasOutstandingLoan()) {
            var remainingLoanMana = Mth.ceil(Math.max(0.0F, FocusStaffbowClientLoanState.remainingLoanMana()));
            lines.add(Component.translatable(getDescriptionId() + ".loan_mana").withStyle(ChatFormatting.RED));
            lines.add(Component.translatable(
                    getDescriptionId() + ".loan_mana.rest",
                    Component.literal(Integer.toString(remainingLoanMana)).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, lines, flag);
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
