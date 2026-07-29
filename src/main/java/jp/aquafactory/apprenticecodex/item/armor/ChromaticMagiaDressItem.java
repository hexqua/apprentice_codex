package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.VanillaEnchantmentCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.renderer.armor.ChromaticMagiaDressRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class ChromaticMagiaDressItem extends ArmorItem implements GeoItem, IPresetSpellContainer, WisdomPolicy {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers armorAttributeModifiers;

    public ChromaticMagiaDressItem(Type type) {
        super(Holder.direct(ChromaticMagiaDressStats.MATERIAL), type, ChromaticMagiaDressStats.createProperties(type).fireResistant());
        this.armorAttributeModifiers = ChromaticMagiaDressStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
    }

    public Type getArmorType() {
        return getType();
    }

    public boolean hasImbueSlot() {
        return getType() == Type.CHESTPLATE;
    }

    public static List<SchoolType> readSchoolHistory(ItemStack stack) {
        return ChromaticMagiaDressHistory.readSchools(stack);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ChromaticMagiaDressRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new ChromaticMagiaDressRenderer();
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

        // 胴体だけを Arcane Anvil の Imbue 対象にする。
        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, true));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : super.getDefaultAttributeModifiers(stack).modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        for (var entry : armorAttributeModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        ChromaticMagiaDressStats.addSpellPowerModifier(
                builder,
                getType(),
                ApprenticeCodexServerConfig.chromaticMagiaDressSpellPowerBonusPerPiece()
        );
        addHistorySpellPowerModifiers(builder, stack);
        return MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                builder.build(),
                "apprenticecodex.chromatic_magia_dress." + ChromaticMagiaDressStats.typeToken(getType()) + ".merged"
        );
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return enchantmentId != null && isSupportedArmorEnchantment(enchantmentId);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment)
                || VanillaEnchantmentCompatibility.isNonVanillaAndSupported(enchantment,
                supportedEnchantment -> supportsEnchantment(stack, supportedEnchantment));
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        return VanillaEnchantmentCompatibility.bookContainsOnlyVanillaOrSupported(book,
                supportedEnchantment -> supportsEnchantment(stack, supportedEnchantment));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ChromaticMagiaDressStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return ChromaticMagiaDressStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void addHistorySpellPowerModifiers(ItemAttributeModifiers.Builder builder, ItemStack stack) {
        var schools = ChromaticMagiaDressHistory.readSchools(stack);
        var schoolSpellPowerBonusPerHistory =
                ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();
        for (int i = 0; i < schools.size(); ++i) {
            var schoolPowerAttribute = MagicTools.resolveSchoolPowerAttribute(schools.get(i));
            if (schoolPowerAttribute == null) {
                continue;
            }

            MagicArmorAttributeHelper.addModifier(
                    builder,
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute),
                    schoolSpellPowerBonusPerHistory,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    EquipmentSlotGroup.bySlot(getType().getSlot()),
                    "chromatic_magia_dress_" + ChromaticMagiaDressStats.typeToken(getType()) + "_history_spell_power_" + i
            );
        }
    }

    private static boolean isSupportedArmorEnchantment(ResourceLocation enchantmentId) {
        return enchantmentId.equals(Enchantments.WISDOM.location());
    }
}
