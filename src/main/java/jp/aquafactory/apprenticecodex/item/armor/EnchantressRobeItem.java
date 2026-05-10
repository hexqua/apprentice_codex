package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
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
    private static final double IMBUED_SCHOOL_SPELL_POWER_BONUS = 0.05D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers robeAttributeModifiers;

    public EnchantressRobeItem(Type type) {
        super(Holder.direct(EnchantressRobeStats.MATERIAL), type, EnchantressRobeStats.createProperties(type));
        this.robeAttributeModifiers = EnchantressRobeStats.createAttributeModifiers(type);
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
        EnchantressRobeStats.addSpellPowerModifier(
                builder,
                getType(),
                ApprenticeCodexServerConfig.enchantressRobeSpellPowerBonusPerPiece()
        );
        addImbuedSchoolSpellPowerModifier(builder, stack);
        return builder.build();
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

    private void addImbuedSchoolSpellPowerModifier(ItemAttributeModifiers.Builder builder, ItemStack stack) {
        if (!hasImbueSlot()) {
            return;
        }

        // 学派依存の補正は胴体の Imbue 状態を読む必要があるため、固定値テーブルではなく stack 側で付与する。
        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        var imbuedSpellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        if (imbuedSpellPowerAttribute == null) {
            return;
        }

        MagicArmorAttributeHelper.addModifier(
                builder,
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(imbuedSpellPowerAttribute),
                IMBUED_SCHOOL_SPELL_POWER_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                EquipmentSlotGroup.bySlot(getType().getSlot()),
                "enchantress_robe_chestplate_imbued_spell_power"
        );
    }

    private static boolean isSupportedRobeEnchantment(ResourceLocation enchantmentId) {
        return enchantmentId.equals(Enchantments.WISDOM.location());
    }
}
