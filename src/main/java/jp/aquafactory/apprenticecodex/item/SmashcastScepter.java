package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.renderer.item.SmashcastScepterRenderer;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public final class SmashcastScepter extends AbstractRightClickMagicWeaponItem
        implements GeoItem, RestrictedSpellImbuableItem, IJeiInfoItem {
    public static final double ATTACK_DAMAGE_MODIFIER = 5.0D;
    public static final double ATTACK_SPEED_MODIFIER = -3.4D;
    public static final float SMASH_ATTACK_FALL_DISTANCE_THRESHOLD = 1.5F;
    public static final float HEAVY_SMASH_SOUND_FALL_DISTANCE_THRESHOLD = 5.0F;
    public static final double SMASH_KNOCKBACK_RADIUS = 3.5D;
    public static final double SMASH_KNOCKBACK_POWER = 0.7D;
    public static final double WIND_BURST_MOTION_EPSILON = 0.01D;

    private static final String ITEM_KEY = "smashcast_scepter";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.smashcast_scepter.desc_";
    private static final String IDLE_CONTROLLER = "idle";
    private static final String SMASH_CONTROLLER = "smash";
    private static final String SMASH_ANIMATION = "smash";
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double MAX_SMASH_SPELL_POWER_MULTIPLIER = 10.0D;
    private static final double LOW_FALL_SPELL_POWER_PER_BLOCK = 0.10D;
    private static final double HIGH_FALL_SPELL_POWER_PER_BLOCK = 0.05D;
    private static final ResourceLocation SMASH_SPELL_POWER_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter_smash_spell_power");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_READY = RawAnimation.begin().thenLoop("ready");
    private static final RawAnimation ANIM_SMASH = RawAnimation.begin().thenPlay("smash");
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ItemStack VANILLA_MACE_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.MACE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SmashcastScepter() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                true,
                ENCHANTMENT_VALUE,
                ITEM_KEY,
                ATTACK_DAMAGE_MODIFIER,
                ATTACK_SPEED_MODIFIER
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        initializeSpellContainer(stack);
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none() && spell.getCastType() != CastType.CONTINUOUS;
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        SpellData spellData = SpellData.EMPTY;
        if (ISpellContainer.isSpellContainer(stack)) {
            var spellContainer = ISpellContainer.get(stack);
            if (spellContainer != null && spellContainer.getActiveSpellCount() > 0) {
                spellData = spellContainer.getSpellAtIndex(0);
            }
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false);
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        } else {
            PresetSpellContainerStateHelper.clearRememberedState(stack);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    public @Nullable SpellData getImbuedSpellData(ItemStack stack) {
        return getPrimarySpellData(stack);
    }

    public boolean canStartSmashcast(Player player, ItemStack stack) {
        if (!isSmashAttack(player) || !isSameItem(stack)) {
            return false;
        }
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }
        return true;
    }

    public void triggerSmashAnimation(ServerPlayer player, ItemStack stack) {
        if (!isSameItem(stack)) {
            return;
        }

        var instanceId = GeoItem.getOrAssignId(stack, player.serverLevel());
        triggerAnim(player, instanceId, SMASH_CONTROLLER, SMASH_ANIMATION);
    }

    public boolean tryCastSmashSpell(Player player, ItemStack stack, float fallDistance) {
        if (!isSameItem(stack)) {
            return false;
        }
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellData = getImbuedSpellData(stack);
        if (spellData == null || !canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && (magicData.isCasting() || magicData.getPlayerCooldowns().isOnCooldown(spell))) {
            return false;
        }

        var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
        if (spellPowerAttribute != null) {
            spellPowerAttribute.removeModifier(SMASH_SPELL_POWER_MODIFIER_ID);
            var spellPowerMultiplier = calculateSmashSpellPowerMultiplier(stack, fallDistance);
            if (spellPowerMultiplier > 1.0D) {
                spellPowerAttribute.addTransientModifier(new AttributeModifier(
                        SMASH_SPELL_POWER_MODIFIER_ID,
                        spellPowerMultiplier - 1.0D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        }

        try {
            var casted = spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    CastSource.SWORD,
                    true,
                    SpellSelectionManager.MAINHAND
            );
            if (!casted) {
                return false;
            }

            TriggeredSpellCastHelper.applyLongCastDurationOverride(
                    player,
                    spellLevel,
                    spell,
                    magicData,
                    SpellSelectionManager.MAINHAND,
                    spell.getCastType() == CastType.LONG ? 0 : null
            );
            return true;
        } finally {
            if (spellPowerAttribute != null) {
                spellPowerAttribute.removeModifier(SMASH_SPELL_POWER_MODIFIER_ID);
            }
        }
    }

    public static boolean isSmashAttack(LivingEntity entity) {
        return entity != null
                && entity.fallDistance > SMASH_ATTACK_FALL_DISTANCE_THRESHOLD
                && !entity.onGround()
                && !entity.isFallFlying()
                && !entity.isInWater()
                && !entity.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
    }

    public static float calculateSmashBonusDamage(ItemStack stack, float fallDistance) {
        return calculateBaseSmashBonusDamage(fallDistance);
    }

    public static float calculateSmashBonusDamage(ItemStack stack, float fallDistance, ServerLevel level,
                                                  Entity target, DamageSource source) {
        return calculateBaseSmashBonusDamage(fallDistance)
                + EnchantmentHelper.modifyFallBasedDamage(level, stack, target, source, 0.0F) * Math.max(0.0F, fallDistance);
    }

    private static float calculateBaseSmashBonusDamage(float fallDistance) {
        var clampedFallDistance = Math.max(0.0F, fallDistance);
        return clampedFallDistance <= 3.0F
                ? 4.0F * clampedFallDistance
                : clampedFallDistance <= 8.0F
                ? 12.0F + 2.0F * (clampedFallDistance - 3.0F)
                : 22.0F + clampedFallDistance - 8.0F;
    }

    public static double calculateSmashSpellPowerMultiplier(ItemStack stack, float fallDistance) {
        var clampedFallDistance = Math.max(0.0D, fallDistance);
        var baseBonus = clampedFallDistance <= 10.0D
                ? clampedFallDistance * LOW_FALL_SPELL_POWER_PER_BLOCK
                : 10.0D * LOW_FALL_SPELL_POWER_PER_BLOCK
                + (clampedFallDistance - 10.0D) * HIGH_FALL_SPELL_POWER_PER_BLOCK;
        var densityBonus = clampedFallDistance * getVanillaEnchantmentLevel(stack, net.minecraft.world.item.enchantment.Enchantments.DENSITY)
                * 0.02D;
        return 1.0D + Math.min(MAX_SMASH_SPELL_POWER_MULTIPLIER, baseBonus + densityBonus);
    }

    @Override
    protected boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        var spellData = getPrimarySpellData(stack);
        if (spellData == null || spellData.canRemove() || !canImbueSpell(spellData)) {
            return false;
        }

        var normalized = ISpellContainer.create(1, true, false).mutableCopy();
        if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false)) {
            return false;
        }

        ISpellContainer.set(stack, normalized.toImmutable());
        PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        return true;
    }

    @Override
    public float getAttackDamageBonus(@NotNull Entity target, float damage, @NotNull DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity attacker) || !isSmashAttack(attacker)) {
            return 0.0F;
        }

        var stack = attacker.getWeaponItem();
        if (!(stack.getItem() instanceof SmashcastScepter)) {
            return 0.0F;
        }

        return attacker.level() instanceof ServerLevel serverLevel
                ? calculateSmashBonusDamage(stack, attacker.fallDistance, serverLevel, target, source)
                : calculateSmashBonusDamage(stack, attacker.fallDistance);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null || isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (MalumCompatibility.ANIMATED.equals(enchantmentId)
                || MalumCompatibility.SPIRIT_PLUNDER.equals(enchantmentId)) {
            return true;
        }

        return enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.PLUNDER)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE)
                || enchantment.value().canEnchant(VANILLA_MACE_ENCHANTMENT_PROBE_STACK);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SmashcastScepterRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new SmashcastScepterRenderer();
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
        controllerRegistrar.add(new AnimationController<>(this, IDLE_CONTROLLER, 0, state -> {
            var stack = state.getData(DataTickets.ITEMSTACK);
            var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            state.setAnimation(SmashcastScepterClientRenderState.shouldPlayReadyAnimation(stack, perspective)
                    ? ANIM_READY
                    : ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
        controllerRegistrar.add(new AnimationController<>(this, SMASH_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(SMASH_ANIMATION, ANIM_SMASH));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc_1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(getDescriptionId() + ".desc_2").withStyle(ChatFormatting.GRAY));
        appendSmashcastScepterTooltip(lines);
    }

    private void appendSmashcastScepterTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(ImbueTooltipHelper.translatableGray(
                        "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
                )),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_smashcast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                List.of(ImbueTooltipHelper.translatableGray(
                        "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_not_continuous"
                )),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static boolean isDurabilityTargetEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.value().canEnchant(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static int getVanillaEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var holder : enchantments.keySet()) {
            if (holder.is(enchantmentKey)) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }

}
