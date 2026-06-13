package jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.SpellBook;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellStainedRunicTabletServerConfig;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellStainedRunicTablet extends SpellBook implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spellstained_runic_tablet.desc_";
    private static final String TOOLTIP_KEY_PREFIX = "item.apprenticecodex.spellstained_runic_tablet.desc";

    public SpellStainedRunicTablet() {
        super(8, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
    }

    @Override
    public @NotNull Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            ResourceLocation id,
            ItemStack stack
    ) {
        var baseModifiers = super.getAttributeModifiers(slotContext, id, stack);
        if (!Curios.SPELLBOOK_SLOT.equals(slotContext.identifier())) {
            return baseModifiers;
        }

        var dynamicModifiers = buildDynamicSpellbookAttributes(slotContext, stack);
        if (dynamicModifiers.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(dynamicModifiers);
        return builder.build();
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        if (isShiftDown()) {
            appendDetailTooltip(stack, lines);
        } else {
            lines.add(Component.translatable(TOOLTIP_KEY_PREFIX).withStyle(ChatFormatting.YELLOW));
            lines.add(Component.translatable(TOOLTIP_KEY_PREFIX + ".hint").withStyle(ChatFormatting.YELLOW));
        }
        lines.add(Component.empty());
        super.appendHoverText(stack, context, lines, flag);
    }

    /**
     * spellbook 内容依存の補正値を 1 箇所へ集約する。
     */
    protected Multimap<Holder<Attribute>, AttributeModifier> buildDynamicSpellbookAttributes(SlotContext slotContext, ItemStack stack) {
        var stats = collectSpellStats(stack);
        if (!stats.hasActiveSpells()) {
            return ImmutableMultimap.of();
        }

        var config = ApprenticeCodexServerConfig.spellStainedRunicTabletConfig();
        double cooldownReductionBonus = config.cooldownReduction().resolve(stats.distinctSchoolCount());
        double castTimeReductionBonus = config.castTimeReduction().resolve(stats.maxSchoolSpellCount());

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        addModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                stats.maxManaBonus(),
                AttributeModifier.Operation.ADD_VALUE,
                slotContext,
                "max_mana"
        );
        addModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                stats.globalSpellPowerBonus(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotContext,
                "global_spell_power"
        );
        addModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                cooldownReductionBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotContext,
                "cooldown_reduction"
        );
        addModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                castTimeReductionBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotContext,
                "cast_time_reduction"
        );

        for (var entry : stats.schoolSpellPowerBonuses().entrySet()) {
            var attribute = BuiltInRegistries.ATTRIBUTE.get(entry.getKey());
            if (attribute == null) {
                continue;
            }

            addModifier(
                    builder,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute),
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    slotContext,
                    "school_spell_power/" + entry.getKey().getNamespace() + "/" + entry.getKey().getPath()
            );
        }

        return builder.build();
    }

    private void appendDetailTooltip(ItemStack stack, List<Component> lines) {
        var config = ApprenticeCodexServerConfig.spellStainedRunicTabletConfig();
        var stats = collectSpellStats(stack);
        appendScalingBonusTooltipLine(
                lines,
                TOOLTIP_KEY_PREFIX + ".cooldown",
                TOOLTIP_KEY_PREFIX + ".cooldown.insufficient",
                config.cooldownReduction(),
                stats.distinctSchoolCount(),
                stats.hasActiveSpells()
        );
        appendScalingBonusTooltipLine(
                lines,
                TOOLTIP_KEY_PREFIX + ".cast_time",
                TOOLTIP_KEY_PREFIX + ".cast_time.insufficient",
                config.castTimeReduction(),
                stats.maxSchoolSpellCount(),
                stats.hasActiveSpells()
        );
    }

    private void appendScalingBonusTooltipLine(
            List<Component> lines,
            String achievedKey,
            String insufficientKey,
            SpellStainedRunicTabletServerConfig.ScalingBonus bonus,
            int count,
            boolean hasActiveSpells
    ) {
        if (hasActiveSpells && count >= bonus.minimumCount()) {
            lines.add(Component.translatable(
                    achievedKey,
                    formatBonusPercent(bonus.resolve(count) * 100.0D),
                    Component.literal(Integer.toString(count)).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.add(Component.translatable(
                insufficientKey,
                Component.literal("0").withStyle(ChatFormatting.RED),
                Component.literal(Integer.toString(bonus.minimumCount())).withStyle(ChatFormatting.YELLOW)
        ).withStyle(ChatFormatting.GRAY));
    }

    private Component formatBonusPercent(double value) {
        return Component.literal(formatPercentNumber(value)).withStyle(resolveBonusColor(value));
    }

    private String formatPercentNumber(double value) {
        if (Math.abs(value) < 1.0e-9D) {
            return "0";
        }

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private ChatFormatting resolveBonusColor(double value) {
        if (value > 0.0D) {
            return ChatFormatting.AQUA;
        }
        if (value < 0.0D) {
            return ChatFormatting.RED;
        }
        return ChatFormatting.GRAY;
    }

    private SpellStats collectSpellStats(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return SpellStats.EMPTY;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return SpellStats.EMPTY;
        }

        var activeSpells = spellContainer.getActiveSpells();
        if (activeSpells.isEmpty()) {
            return SpellStats.EMPTY;
        }

        var config = ApprenticeCodexServerConfig.spellStainedRunicTabletConfig();
        double maxManaBonus = 0.0D;
        double globalSpellPowerBonus = 0.0D;
        var schoolSpellPowerBonuses = new HashMap<ResourceLocation, Double>();
        var schoolSpellCounts = new HashMap<ResourceLocation, Integer>();

        for (var spellSlot : activeSpells) {
            var spellData = spellSlot.spellData();
            var spell = spellData.getSpell();
            var rarity = spellData.getRarity();
            var schoolType = spell.getSchoolType();
            var schoolAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);

            maxManaBonus += config.maxMana().forRarity(rarity);

            if (schoolAttribute != null) {
                var schoolKey = BuiltInRegistries.ATTRIBUTE.getKey(schoolAttribute);
                if (schoolKey != null) {
                    var schoolSpellPowerBonus = config.schoolSpellPower().forRarity(rarity);
                    schoolSpellPowerBonuses.merge(schoolKey, schoolSpellPowerBonus, Double::sum);
                }
            }

            globalSpellPowerBonus += config.generalSpellPower().forRarity(rarity);

            schoolSpellCounts.merge(schoolType.getId(), 1, Integer::sum);
        }

        var maxSchoolSpellCount = schoolSpellCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        return new SpellStats(
                maxManaBonus,
                globalSpellPowerBonus,
                filterAppliedSchoolSpellPowerBonuses(schoolSpellPowerBonuses, config),
                schoolSpellCounts.size(),
                maxSchoolSpellCount
        );
    }

    private Map<ResourceLocation, Double> filterAppliedSchoolSpellPowerBonuses(
            Map<ResourceLocation, Double> schoolSpellPowerBonuses,
            SpellStainedRunicTabletServerConfig.Values config
    ) {
        var appliedBonuses = new HashMap<ResourceLocation, Double>();
        for (var entry : schoolSpellPowerBonuses.entrySet()) {
            var bonus = entry.getValue();
            if (shouldApplySchoolSpellPowerBonus(bonus, config)) {
                appliedBonuses.put(entry.getKey(), bonus);
            }
        }
        return Map.copyOf(appliedBonuses);
    }

    private boolean shouldApplySchoolSpellPowerBonus(
            double bonus,
            SpellStainedRunicTabletServerConfig.Values config
    ) {
        if (bonus > 0.0D) {
            return bonus >= config.minimumAppliedPositiveBonus();
        }
        if (bonus < 0.0D) {
            return Math.abs(bonus) >= config.minimumAppliedNegativePenalty();
        }
        return false;
    }

    private void addModifier(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation,
            SlotContext slotContext,
            String modifierKey
    ) {
        if (amount == 0.0D) {
            return;
        }

        builder.put(
                attribute,
                new AttributeModifier(
                        createModifierId(slotContext, modifierKey),
                        amount,
                        operation
                )
        );
    }

    private static ResourceLocation createModifierId(SlotContext slotContext, String modifierKey) {
        var sanitizedKey = modifierKey.replace('/', '_');
        return ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "spellstained_runic_tablet/" + Curios.SPELLBOOK_SLOT + "_" + slotContext.index() + "_" + sanitizedKey
        );
    }

    private static boolean isShiftDown() {
        return FMLEnvironment.dist == Dist.CLIENT && SpellStainedRunicTabletClientHelper.hasShiftDown();
    }

    private record SpellStats(
            double maxManaBonus,
            double globalSpellPowerBonus,
            Map<ResourceLocation, Double> schoolSpellPowerBonuses,
            int distinctSchoolCount,
            int maxSchoolSpellCount
    ) {
        private static final SpellStats EMPTY = new SpellStats(0.0D, 0.0D, Map.of(), 0, 0);

        private boolean hasActiveSpells() {
            return distinctSchoolCount > 0;
        }
    }
}
