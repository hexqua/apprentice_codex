package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.EnchantressRobeRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class EnchantressRobeItem extends ArmorItem implements GeoItem, IPresetSpellContainer {
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Type armorType;
    private final Multimap<Attribute, AttributeModifier> robeAttributeModifiers;

    public EnchantressRobeItem(Type type) {
        super(EnchantressRobeStats.MATERIAL, type, new Properties());
        this.armorType = type;
        this.robeAttributeModifiers = EnchantressRobeStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
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
                    renderer = new EnchantressRobeRenderer();
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

        // 胴体のみ Imbue を受けるための魔法枠を持たせる.
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
            return isSupportedRobeEnchantment(enchantment);
        }

        return enchantment.canApplyAtEnchantingTable(createArmorProbeStack());
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var baseModifiers = super.getAttributeModifiers(slot, stack);
        if (slot != armorType.getSlot()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(robeAttributeModifiers);
        builder.putAll(createEnchantmentAttributeModifiers(stack));
        return MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                builder.build(),
                "apprenticecodex.enchantress_robe." + armorType.getName() + ".merged"
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return EnchantressRobeStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return EnchantressRobeStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // GeoArmor 描画とは別に vanilla 防具テクスチャも問い合わせられるため、既存テクスチャへ解決して警告を出させない。
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/enchantress_robe.png").toString();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private Multimap<Attribute, AttributeModifier> createEnchantmentAttributeModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return ImmutableMultimap.of();
        }
        if (!EnchantmentRegistry.ALACRITY.isPresent()
                || !EnchantmentRegistry.REFLUX.isPresent()
                || !EnchantmentRegistry.RESERVOIR.isPresent()
                || !EnchantmentRegistry.SURGE.isPresent()
                || !EnchantmentRegistry.TENSE.isPresent()) {
            return ImmutableMultimap.of();
        }

        var alacrityLevel = stack.getEnchantmentLevel(EnchantmentRegistry.ALACRITY.get());
        var refluxLevel = stack.getEnchantmentLevel(EnchantmentRegistry.REFLUX.get());
        var reservoirLevel = stack.getEnchantmentLevel(EnchantmentRegistry.RESERVOIR.get());
        var surgeLevel = stack.getEnchantmentLevel(EnchantmentRegistry.SURGE.get());
        var tenseLevel = stack.getEnchantmentLevel(EnchantmentRegistry.TENSE.get());
        var attunementLevel =
                EnchantmentRegistry.ATTUNEMENT.isPresent() ? stack.getEnchantmentLevel(EnchantmentRegistry.ATTUNEMENT.get()) : 0;

        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && tenseLevel <= 0
                && attunementLevel <= 0) {
            return ImmutableMultimap.of();
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var prefix = "apprenticecodex.enchantress_robe." + armorType.getName() + ".enchant";

        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".alacrity.cooldown_reduction"
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.MANA_REGEN.get(),
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".reflux.mana_regen"
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.MAX_MANA.get(),
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADDITION,
                prefix + ".reservoir.max_mana"
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".surge.spell_power"
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                prefix + ".tense.cast_time_reduction"
        );

        if (hasImbueSlot() && attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            MagicArmorAttributeHelper.addModifier(
                    builder,
                    attunementSpellPowerAttribute,
                    attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    prefix + ".attunement.spell_power"
            );
        }

        return builder.build();
    }

    private boolean isSupportedRobeEnchantment(Enchantment enchantment) {
        return isCommonRobeEnchantment(enchantment) || (hasImbueSlot() && isChestOnlyRobeEnchantment(enchantment));
    }

    private static boolean isCommonRobeEnchantment(Enchantment enchantment) {
        return (EnchantmentRegistry.ALACRITY.isPresent() && enchantment == EnchantmentRegistry.ALACRITY.get())
                || (EnchantmentRegistry.REFLUX.isPresent() && enchantment == EnchantmentRegistry.REFLUX.get())
                || (EnchantmentRegistry.RESERVOIR.isPresent() && enchantment == EnchantmentRegistry.RESERVOIR.get())
                || (EnchantmentRegistry.SURGE.isPresent() && enchantment == EnchantmentRegistry.SURGE.get())
                || (EnchantmentRegistry.TENSE.isPresent() && enchantment == EnchantmentRegistry.TENSE.get())
                || (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get());
    }

    private static boolean isChestOnlyRobeEnchantment(Enchantment enchantment) {
        return (EnchantmentRegistry.ATTUNEMENT.isPresent() && enchantment == EnchantmentRegistry.ATTUNEMENT.get())
                || (EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get());
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
