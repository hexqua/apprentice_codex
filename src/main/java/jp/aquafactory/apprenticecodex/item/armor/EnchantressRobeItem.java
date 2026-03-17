package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.renderer.armor.EnchantressRobeRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class EnchantressRobeItem extends ArmorItem implements GeoItem, IPresetSpellContainer {
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double ATTUNEMENT_SPELL_POWER_PER_LEVEL = 0.04D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;
    private static final String ENCHANTMENT_PREFIX = "enchantress_robe";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers robeAttributeModifiers;
    private final String typeToken;

    public EnchantressRobeItem(Type type) {
        super(Holder.direct(EnchantressRobeStats.MATERIAL), type, EnchantressRobeStats.createProperties(type));
        this.robeAttributeModifiers = EnchantressRobeStats.createAttributeModifiers(type);
        this.typeToken = switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case BODY -> "body";
        };
        GeoItem.registerSyncedAnimatable(this);
    }

    public boolean hasImbueSlot() {
        return getType() == Type.CHESTPLATE;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private EnchantressRobeRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new EnchantressRobeRenderer();
                }
                return this.renderer;
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
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : super.getDefaultAttributeModifiers(stack).modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        for (var entry : robeAttributeModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        return buildRuntimeAttributeModifiers(stack, builder.build());
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return enchantmentId != null && isSupportedRobeEnchantment(enchantmentId);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return EnchantressRobeStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return EnchantressRobeStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private ItemAttributeModifiers buildRuntimeAttributeModifiers(ItemStack stack, ItemAttributeModifiers defaultModifiers) {
        var baseModifiers = stripManagedEnchantmentModifiers(defaultModifiers);
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return baseModifiers;
        }

        var alacrityLevel = Enchantments.getLevel(stack, Enchantments.ALACRITY);
        var refluxLevel = Enchantments.getLevel(stack, Enchantments.REFLUX);
        var reservoirLevel = Enchantments.getLevel(stack, Enchantments.RESERVOIR);
        var surgeLevel = Enchantments.getLevel(stack, Enchantments.SURGE);
        var attunementLevel = Enchantments.getLevel(stack, Enchantments.ATTUNEMENT);
        var tenseLevel = Enchantments.getLevel(stack, Enchantments.TENSE);

        if (alacrityLevel <= 0
                && refluxLevel <= 0
                && reservoirLevel <= 0
                && surgeLevel <= 0
                && attunementLevel <= 0
                && tenseLevel <= 0) {
            return baseModifiers;
        }

        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        var slotGroup = EquipmentSlotGroup.bySlot(getType().getSlot());
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION,
                alacrityLevel * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotGroup,
                managedModifierPath("alacrity_cooldown_reduction")
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.MANA_REGEN,
                refluxLevel * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotGroup,
                managedModifierPath("reflux_mana_regen")
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.MAX_MANA,
                reservoirLevel * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADD_VALUE,
                slotGroup,
                managedModifierPath("reservoir_max_mana")
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.SPELL_POWER,
                surgeLevel * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotGroup,
                managedModifierPath("surge_spell_power")
        );
        MagicArmorAttributeHelper.addModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION,
                tenseLevel * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                slotGroup,
                managedModifierPath("tense_cast_time_reduction")
        );

        if (hasImbueSlot() && attunementLevel > 0) {
            var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
            var attunementSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            if (attunementSpellPowerAttribute != null) {
                MagicArmorAttributeHelper.addModifier(
                        builder,
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attunementSpellPowerAttribute),
                        attunementLevel * ATTUNEMENT_SPELL_POWER_PER_LEVEL,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                        slotGroup,
                        managedModifierPath("attunement_spell_power")
                );
            }
        }

        return MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                builder.build(),
                ENCHANTMENT_PREFIX + "_" + typeToken + "_merged"
        );
    }

    private ItemAttributeModifiers stripManagedEnchantmentModifiers(ItemAttributeModifiers modifiers) {
        if (modifiers.modifiers().isEmpty()) {
            return modifiers;
        }

        var managedPrefix = ENCHANTMENT_PREFIX + "_" + typeToken + "_enchant_";
        var mergedPrefix = ENCHANTMENT_PREFIX + "_" + typeToken + "_merged_";
        var builder = ItemAttributeModifiers.builder();
        boolean changed = false;
        for (var entry : modifiers.modifiers()) {
            var modifierPath = entry.modifier().id().getPath();
            if (modifierPath.startsWith(managedPrefix) || modifierPath.startsWith(mergedPrefix)) {
                changed = true;
                continue;
            }

            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        return changed ? builder.build() : modifiers;
    }

    private static boolean isSupportedRobeEnchantment(ResourceLocation enchantmentId) {
        return enchantmentId.equals(Enchantments.WISDOM.location());
    }

    private String managedModifierPath(String key) {
        return ENCHANTMENT_PREFIX + "_" + typeToken + "_enchant_" + key;
    }
}
