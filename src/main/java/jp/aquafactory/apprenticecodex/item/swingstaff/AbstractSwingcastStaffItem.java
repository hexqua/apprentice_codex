package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.renderer.item.SwingcastStaffRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractSwingcastStaffItem extends AbstractSwingMagicItem implements GeoItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final double COMMON_SPELL_POWER_BONUS = 0.10D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation;
    private final SwingcastStaffTier tier;

    protected AbstractSwingcastStaffItem(String itemKey, SwingcastStaffTier tier) {
        super(
                new Item.Properties().stacksTo(1).rarity(tier.rarity()),
                tier.enchantmentValue(),
                itemKey,
                tier.attackDamageModifier(),
                tier.attackSpeedModifier(),
                tier.handBonuses().stream()
                        .map(AbstractSwingcastStaffItem::toAttributeBonus)
                        .toList()
        );
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "textures/geo/" + itemKey + ".png"
        );
        this.tier = tier;
        GeoItem.registerSyncedAnimatable(this);
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
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

    public SwingcastStaffTier getSwingcastStaffTier() {
        return tier;
    }

    @Nullable
    public SpellData getImbuedSpellData(ItemStack stack) {
        return getPrimarySpellData(stack);
    }

    public int resolveSwingcastCooldownTicks(Player player, ItemStack stack, AbstractSpell spell, int currentEffectiveCooldown) {
        int spellLevel = resolveEffectiveSpellLevel(player, stack, spell);
        return switch (tier.swingcastCooldownMode()) {
            case FIXED -> tier.fixedSwingcastCooldownTicks() != null ? tier.fixedSwingcastCooldownTicks() : currentEffectiveCooldown;
            case IMBUED_ONLY -> currentEffectiveCooldown;
            case IMBUED_PLUS_LONG_CAST_TIME -> currentEffectiveCooldown
                    + (spell.getCastType() == CastType.LONG ? spell.getEffectiveCastTime(spellLevel, player) : 0);
        };
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }

        var castType = SpellGunCastType.from(spell.getCastType());
        if (castType == null || !tier.supportedCastTypes().contains(castType)) {
            return false;
        }

        if (spell.getCastType() == CastType.CONTINUOUS) {
            return false;
        }

        if (tier.maxImbueSpellCooldownTicks() != null && spell.getSpellCooldown() > tier.maxImbueSpellCooldownTicks()) {
            return false;
        }

        if (tier.requireZeroRecast() && spell.getRecastCount(spellLevel, null) > 0) {
            return false;
        }

        return passesAdditionalImbueConditions(spell, spellLevel);
    }

    @Override
    protected @Nullable Integer getSwingTriggeredLongCastDurationOverrideTicks(
            Player player,
            ItemStack stack,
            AbstractSpell spell,
            int spellLevel,
            @Nullable MagicData magicData
    ) {
        return spell.getCastType() == CastType.LONG ? 0 : null;
    }

    @Override
    protected AutoCloseable openSwingTriggeredSpellCastContext(
            Player player,
            ItemStack stack,
            AbstractSpell spell,
            int spellLevel,
            @Nullable MagicData magicData
    ) {
        return SwingcastStaffCastContext.open(player.getUUID(), stack, spell);
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
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    protected boolean passesAdditionalImbueConditions(AbstractSpell spell, int spellLevel) {
        return true;
    }

    public boolean allowImbuedSpellInSpellWheel(ItemStack stack) {
        return tier.allowImbuedSpellInSpellWheel() && getImbuedSpellData(stack) != null;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendSwingcastStaffTooltip(lines);
    }

    protected int resolveEffectiveSpellLevel(Player player, ItemStack stack, AbstractSpell spell) {
        var spellData = getImbuedSpellData(stack);
        if (spellData != null && spell.equals(spellData.getSpell())) {
            return spell.getLevelFor(spellData.getLevel(), player);
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && spell.getSpellId().equals(magicData.getCastingSpellId()) && magicData.getCastingSpellLevel() > 0) {
            return magicData.getCastingSpellLevel();
        }

        return spell.getLevelFor(1, player);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SwingcastStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SwingcastStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                })
        );
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    protected static List<SwingcastStaffTier.BonusSpec> createCommonSpellPowerBonuses() {
        return List.of(
                bonusSpec(AttributeRegistry.SPELL_POWER, COMMON_SPELL_POWER_BONUS, AttributeModifier.Operation.MULTIPLY_BASE, "spell_power")
        );
    }

    protected static SwingcastStaffTier createTier(
            net.minecraft.world.item.Rarity rarity,
            int enchantmentValue,
            double displayedAttackDamage,
            List<SwingcastStaffTier.BonusSpec> handBonuses,
            Set<SpellGunCastType> supportedCastTypes,
            SwingcastCooldownMode swingcastCooldownMode
    ) {
        return SwingcastStaffTier.fromDisplayedWeaponStats(
                rarity,
                enchantmentValue,
                displayedAttackDamage,
                1.6D,
                handBonuses,
                supportedCastTypes,
                null,
                false,
                swingcastCooldownMode,
                null,
                true
        );
    }

    protected static Set<SpellGunCastType> instantOnlyCastTypes() {
        return EnumSet.of(SpellGunCastType.INSTANT);
    }

    protected static Set<SpellGunCastType> allNonContinuousCastTypes() {
        return EnumSet.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG);
    }

    private void appendSwingcastStaffTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectSwingcastAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_swingcast_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectSwingcastRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private List<Component> collectSwingcastAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        if (tier.supportedCastTypes().contains(SpellGunCastType.LONG)) {
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
            ));
        }

        switch (tier.swingcastCooldownMode()) {
            case FIXED -> {
                if (tier.fixedSwingcastCooldownTicks() != null) {
                    translatedLines.add(ImbueTooltipHelper.translatableGray(
                            "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_reduce_recast",
                            ImbueTooltipHelper.formatTooltipSeconds(tier.fixedSwingcastCooldownTicks())
                    ));
                }
            }
            case IMBUED_PLUS_LONG_CAST_TIME -> translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
            ));
            case IMBUED_ONLY -> {
            }
        }
        return translatedLines;
    }

    private List<Component> collectSwingcastRestrictTooltipSection() {
        var translatedLines = new ArrayList<>(ImbueTooltipHelper.collectCastTypeRestrictionLines(tier.supportedCastTypes()));
        ImbueTooltipHelper.appendMaxCooldownRestrictionLine(translatedLines, tier.maxImbueSpellCooldownTicks());
        ImbueTooltipHelper.appendNoRecastRestrictionLine(translatedLines, tier.requireZeroRecast());
        return translatedLines;
    }

    private static SwingcastStaffTier.BonusSpec bonusSpec(
            java.util.function.Supplier<? extends net.minecraft.world.entity.ai.attributes.Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            @Nullable String key
    ) {
        return new SwingcastStaffTier.BonusSpec(attributeSupplier, amount, operation, key);
    }

    private static AbstractRightClickMagicWeaponItem.AttributeBonus toAttributeBonus(SwingcastStaffTier.BonusSpec bonusSpec) {
        return bonus(
                bonusSpec.attributeSupplier(),
                bonusSpec.amount(),
                bonusSpec.operation(),
                bonusSpec.key()
        );
    }
}
