package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.FocusStaffbowRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class FocusStaffbow extends CastingItem
        implements GeoItem, NonDamageableAnvilMergeItem, UniqueItem, CastAnimationOverrideItem, IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.focus_staffbow.desc_";
    private static final int MAX_USE_DURATION = 72000;
    private static final float CLIENT_MANA_SAFE_MARGIN = 0.001F;
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final Set<ResourceLocation> ALLOWED_MAGIC_ITEM_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "plunder")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("3c83fe4d-4081-47d3-8fb5-0a0fce4fd887");
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
    private final Multimap<Attribute, AttributeModifier> mainhandModifiers = buildMainhandModifiers();

    public FocusStaffbow() {
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

        FocusStaffbowCastManager.releasePendingCast(player, stack, getUseDuration(stack) - timeLeft);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return mainhandModifiers;
        }

        return super.getAttributeModifiers(slot, stack);
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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (MalumHauntedCompat.isAnimatedEnchantment(enchantmentId)) {
            return false;
        }

        if (MalumHauntedCompat.isHauntedEnchantment(enchantmentId)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            return true;
        }

        if (isMalumSpiritPlunder(stack, enchantmentId)) {
            return true;
        }

        return (EnchantmentRegistry.SYNTHESIS.isPresent() && enchantment == EnchantmentRegistry.SYNTHESIS.get())
                || ALLOWED_MAGIC_ITEM_ENCHANTMENTS.contains(enchantmentId);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private FocusStaffbowRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new FocusStaffbowRenderer();
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

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static boolean isMalumSpiritPlunder(ItemStack stack, ResourceLocation enchantmentId) {
        return MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_HUNTER_WEAPON);
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

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        ATTACK_DAMAGE_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        ATTACK_SPEED_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        "apprenticecodex.focus_staffbow.mainhand.spell_power",
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        if (!FocusStaffbowClientConfigState.arrowCatalystRequired()) {
            lines.add(Component.translatable(getDescriptionId() + ".require_arrow.disabled").withStyle(ChatFormatting.GRAY));
        } else if (EnchantmentRegistry.SYNTHESIS.isPresent()
                && stack.getEnchantmentLevel(EnchantmentRegistry.SYNTHESIS.get()) > 0) {
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
        super.appendHoverText(stack, level, lines, flag);
    }
}
