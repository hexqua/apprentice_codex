package jp.aquafactory.apprenticecodex.item.armor;

import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.StoredSpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.MagiAgentSuitRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class MagiAgentSuitItem extends ArmorItem
        implements GeoItem, IPresetSpellContainer, SpellCalibrationAdjustmentTarget,
        StoredSpellCalibrationImbueTarget, WisdomPolicy {
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = EndgameArmorCalibration.SLOT_COUNT;

    private static final ResourceLocation ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/magi_agent_suit.png");
    private static final String RUNE_HINT_KEY = "item.apprenticecodex.magi_agent_suit.rune_hint";
    private static final String SCHOOL_RUNE_KEY = "item.apprenticecodex.magi_agent_suit.school_rune";
    private static final String SPELL_HINT_KEY = "item.apprenticecodex.common.desc.spell_hint";
    private static final String SPELL_HINT_OPEN_KEY = "item.apprenticecodex.common.desc.spell_hint_open";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Type armorType;
    private final Multimap<Attribute, AttributeModifier> armorAttributeModifiers;
    private final CalibrationAdjustmentProfile calibrationAdjustmentProfile;

    public MagiAgentSuitItem(Type type) {
        super(MagiAgentSuitStats.MATERIAL, type, new Properties().fireResistant());
        this.armorType = type;
        this.armorAttributeModifiers = MagiAgentSuitStats.createAttributeModifiers(type);
        this.calibrationAdjustmentProfile = EndgameArmorCalibration.createProfile(type, true);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
    }

    public Type getArmorType() {
        return armorType;
    }

    public boolean hasImbueSlot() {
        return armorType == Type.CHESTPLATE;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new MagiAgentSuitRenderer();
                }

                renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || !hasImbueSlot() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, true));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
            return EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get();
        }

        return enchantment.canApplyAtEnchantingTable(createArmorProbeStack());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var baseModifiers = super.getAttributeModifiers(slot, stack);
        if (slot != armorType.getSlot()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(armorAttributeModifiers);
        MagiAgentSuitStats.addSpellPowerModifier(
                builder,
                armorType,
                ApprenticeCodexServerConfig.magiAgentSuitSpellPowerBonus()
        );
        var schoolPowerAttribute = getResolvedSchoolPowerAttribute(stack);
        if (schoolPowerAttribute != null) {
            MagiAgentSuitStats.addSchoolSpellPowerModifier(
                    builder,
                    schoolPowerAttribute,
                    armorType,
                    ApprenticeCodexServerConfig.magiAgentSuitSchoolSpellPowerBonus()
            );
        }
        EndgameArmorCalibration.addAttributeModifiers(builder, stack, armorType, this);
        return builder.build();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return MagiAgentSuitStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return MagiAgentSuitStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendSuitEffectHoverText(lines);
        var school = getResolvedCalibrationSchool(stack);
        if (school == null) {
            lines.add(Component.translatable(RUNE_HINT_KEY).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(SCHOOL_RUNE_KEY, school.getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
        EndgameArmorCalibration.appendStoredScrollTooltip(stack, lines);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return ARMOR_TEXTURE.toString();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static @NotNull ItemStack readCalibrationAdjustment(@NotNull ItemStack suitStack, int slot) {
        return CalibrationAdjustmentStorage.get(suitStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    @Override
    public @NotNull java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(
            @NotNull ItemStack stack
    ) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return calibrationAdjustmentProfile;
    }

    @Override
    public boolean usesStoredCalibrationScrolls(@NotNull ItemStack targetStack) {
        return EndgameArmorCalibration.usesStoredCalibrationScrolls(targetStack);
    }

    @Override
    public boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack targetStack) {
        return EndgameArmorCalibration.hasAnyStoredScroll(targetStack);
    }

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    ) {
        return EndgameArmorCalibration.evaluateStoredScroll(targetStack, slot, spellData);
    }

    public static @Nullable SchoolType getResolvedCalibrationSchool(ItemStack stack) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(readCalibrationAdjustment(stack, slot)).orElse(null);
            if (school != null) {
                return school;
            }
        }
        return null;
    }

    @Override
    public boolean canWalkOnPowderedSnow(@NotNull ItemStack stack, @NotNull LivingEntity wearer) {
        return EndgameArmorCalibration.canWalkOnPowderedSnow(stack, wearer);
    }

    private void appendSuitEffectHoverText(List<Component> lines) {
        var descriptionKey = getDescriptionId() + ".desc";
        if (armorType == Type.HELMET) {
            lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
            return;
        }

        if (!ImbueTooltipHelper.hasDetailsKeyDown()) {
            lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(SPELL_HINT_KEY).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        lines.add(Component.translatable(SPELL_HINT_OPEN_KEY).withStyle(ChatFormatting.GRAY));
        for (var spell : MagiAgentSuitEffects.targetSpells()) {
            if (armorType == Type.LEGGINGS && spell.getCastType() == CastType.INSTANT) {
                continue;
            }
            lines.add(Component.literal("- ")
                    .append(spell.getDisplayName(null))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static @Nullable Attribute getResolvedSchoolPowerAttribute(ItemStack stack) {
        return MagicTools.resolveSchoolPowerAttribute(getResolvedCalibrationSchool(stack));
    }

    private ItemStack createArmorProbeStack() {
        return switch (armorType) {
            case HELMET -> new ItemStack(Items.LEATHER_HELMET);
            case CHESTPLATE -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGGINGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case BOOTS -> new ItemStack(Items.LEATHER_BOOTS);
        };
    }
}
