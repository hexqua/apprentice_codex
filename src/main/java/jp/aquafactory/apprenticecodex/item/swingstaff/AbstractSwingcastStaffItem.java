package jp.aquafactory.apprenticecodex.item.swingstaff;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.renderer.item.SwingcastStaffRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicAttributeModifierHelper;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AbstractSwingcastStaffItem extends AbstractSwingMagicItem
        implements GeoItem, IJeiInfoItem, AttributeEnchantmentPolicy {

    protected enum RecastTypes{
        RequireZeroRecast,
        NoRecastRestriction,
    }

    private static final String JEI_INFO_GROUP_ID = "swingcast_staves";
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.swingcast_staves.desc_";

    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation textureLocation;
    private final SwingcastStaffTier tier;

    private static final Set<AttributeEnchantmentType> DIRECT_ATTRIBUTE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.TENSE
    );

    protected AbstractSwingcastStaffItem(
            String itemKey,
            SwingcastStaffTier tier,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel
    ) {
        super(
                new Item.Properties().stacksTo(1).rarity(tier.rarity()),
                configuredSpell,
                configuredSpellLevel,
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

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var baseModifiers = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND || stack == null || stack.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        var modifierSeedPrefix = "apprenticecodex."
                + normalizeKeyToken(getDescriptionId())
                + ".mainhand.stack";
        return AttributeEnchantmentResolver.resolveMergedModifiers(
                baseModifiers,
                stack,
                modifierSeedPrefix,
                this::addStackDependentModifiers
        );
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
    protected void onInvalidSwingTriggeredSpell(Player player, ItemStack stack, SpellData spellData) {
        player.displayClientMessage(
                Component.translatable(
                        "ui.apprenticecodex.swingcast.cannot_swing_cast",
                        spellData.getSpell().getDisplayName(player)
                ).withStyle(ChatFormatting.RED),
                true
        );
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

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return DIRECT_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var attributeEnchantment = AttributeEnchantmentType.from(enchantment);
        return attributeEnchantment.map(this::supportsDirectAttributeEnchantment).orElse(super.canApplyAtEnchantingTable(stack, enchantment));
    }

    protected boolean passesAdditionalImbueConditions(AbstractSpell spell, int spellLevel) {
        return true;
    }

    protected boolean addStackDependentModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix
    ) {
        return false;
    }

    protected final boolean addImbuedSchoolSpellPowerModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack,
            String modifierSeedPrefix,
            double amount
    ) {
        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        var imbuedSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        if (imbuedSpellPowerAttribute == null) {
            return false;
        }

        addMainhandModifier(
                builder,
                imbuedSpellPowerAttribute,
                amount,
                AttributeModifier.Operation.MULTIPLY_BASE,
                modifierSeedPrefix + ".imbued_spell_power"
        );
        return true;
    }

    public boolean allowImbuedSpellInSpellWheel(ItemStack stack) {
        return tier.allowImbuedSpellInSpellWheel() && getImbuedSpellData(stack) != null;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendAdditionalSwingcastTooltip(stack, level, lines, flag);
        appendSwingcastStaffTooltip(lines);
    }

    protected void appendAdditionalSwingcastTooltip(
            ItemStack stack,
            @Nullable Level level,
            List<Component> lines,
            TooltipFlag flag
    ) {
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

    protected static SwingcastStaffTier createTier(
            Rarity rarity,
            int enchantmentValue,
            double displayedAttackDamage,
            Set<SpellGunCastType> supportedCastTypes,
            SwingcastCooldownMode swingcastCooldownMode,
            RecastTypes recastType,
            AttributeBonus... handBonuses
    ) {
        // Swingcast Staff でも Offhand 系と同じ `bonus(...)` 記法を使えるよう、
        // mainhand 用 AttributeBonus を tier 定義専用の BonusSpec へ変換して受け取る。
        var tierHandBonuses = Stream.of(handBonuses)
                .map(AbstractSwingcastStaffItem::toBonusSpec)
                .toList();
        return createTierFromBonusSpecs(
                rarity,
                enchantmentValue,
                displayedAttackDamage,
                1.6D,
                tierHandBonuses,
                supportedCastTypes,
                swingcastCooldownMode,
                recastType
        );
    }

    protected static SwingcastStaffTier createTier(
            net.minecraft.world.item.Rarity rarity,
            int enchantmentValue,
            double displayedAttackDamage,
            double displayedAttackSpeed,
            Set<SpellGunCastType> supportedCastTypes,
            SwingcastCooldownMode swingcastCooldownMode,
            RecastTypes recastType,
            AttributeBonus... handBonuses
    ) {
        var tierHandBonuses = Stream.of(handBonuses)
                .map(AbstractSwingcastStaffItem::toBonusSpec)
                .toList();
        return createTierFromBonusSpecs(
                rarity,
                enchantmentValue,
                displayedAttackDamage,
                displayedAttackSpeed,
                tierHandBonuses,
                supportedCastTypes,
                swingcastCooldownMode,
                recastType
        );
    }

    private static SwingcastStaffTier createTierFromBonusSpecs(
            Rarity rarity,
            int enchantmentValue,
            double displayedAttackDamage,
            double displayedAttackSpeed,
            List<SwingcastStaffTier.BonusSpec> handBonuses,
            Set<SpellGunCastType> supportedCastTypes,
            SwingcastCooldownMode swingcastCooldownMode,
            RecastTypes recastType) {
        return SwingcastStaffTier.fromDisplayedWeaponStats(
                rarity,
                enchantmentValue,
                displayedAttackDamage,
                displayedAttackSpeed,
                handBonuses,
                supportedCastTypes,
                null,
                recastType == RecastTypes.RequireZeroRecast,
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
            case IMBUED_PLUS_LONG_CAST_TIME -> {
                if (tier.supportedCastTypes().contains(SpellGunCastType.LONG)) {
                    translatedLines.add(ImbueTooltipHelper.translatableGray(
                            "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
                    ));
                }
            }
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

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return collectSwingcastRestrictTooltipSection();
    }

    private static AbstractRightClickMagicWeaponItem.AttributeBonus toAttributeBonus(SwingcastStaffTier.BonusSpec bonusSpec) {
        return bonus(
                bonusSpec.attributeSupplier(),
                bonusSpec.amount(),
                bonusSpec.operation(),
                bonusSpec.key()
        );
    }

    private static SwingcastStaffTier.BonusSpec toBonusSpec(AttributeBonus bonus) {
        return new SwingcastStaffTier.BonusSpec(
                bonus.attributeSupplier(),
                bonus.amount(),
                bonus.operation(),
                bonus.key()
        );
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public String getJeiInfoGroupId() {
        return JEI_INFO_GROUP_ID;
    }

    private static void addMainhandModifier(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            @Nullable Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String modifierIdSeed
    ) {
        MagicAttributeModifierHelper.addModifier(builder, attribute, amount, operation, modifierIdSeed);
    }
}
