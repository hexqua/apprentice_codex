package jp.aquafactory.apprenticecodex.item.armor;

import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.ChromaticMagiaDressRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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

public class ChromaticMagiaDressItem extends ArmorItem implements GeoItem, IPresetSpellContainer, WisdomPolicy {
    private static final ResourceLocation ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/chromatic_magia_dress.png");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Type armorType;
    private final Multimap<Attribute, AttributeModifier> armorAttributeModifiers;

    public ChromaticMagiaDressItem(Type type) {
        super(ChromaticMagiaDressStats.MATERIAL, type, new Properties().fireResistant());
        this.armorType = type;
        this.armorAttributeModifiers = ChromaticMagiaDressStats.createAttributeModifiers(type);
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

    public static List<SchoolType> readSchoolHistory(ItemStack stack) {
        return ChromaticMagiaDressHistory.readSchools(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new ChromaticMagiaDressRenderer();
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

        // 胴体だけを Arcane Anvil の Imbue 対象にする.
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
            return isSupportedArmorEnchantment(enchantment);
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

        var extraBuilder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        extraBuilder.putAll(armorAttributeModifiers);
        ChromaticMagiaDressStats.addSpellPowerModifier(
                extraBuilder,
                armorType,
                ApprenticeCodexServerConfig.chromaticMagiaDressSpellPowerBonusPerPiece()
        );
        addHistorySpellPowerModifiers(extraBuilder, stack);

        var mergedExtraModifiers = MagicArmorAttributeHelper.mergeTooltipEquivalentModifiers(
                extraBuilder.build(),
                "apprenticecodex.chromatic_magia_dress." + ChromaticMagiaDressStats.typeToken(armorType) + ".merged"
        );

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(mergedExtraModifiers);
        return builder.build();
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // GeoArmor 描画以外の vanilla 問い合わせでも、同じ既存テクスチャへ解決して警告を避ける.
        return ARMOR_TEXTURE.toString();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void addHistorySpellPowerModifiers(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack
    ) {
        var schools = ChromaticMagiaDressHistory.readSchools(stack);
        var schoolSpellPowerBonusPerHistory =
                ApprenticeCodexServerConfig.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();
        for (int i = 0; i < schools.size(); ++i) {
            var schoolPowerAttribute = MagicTools.resolveSchoolPowerAttribute(schools.get(i));
            MagicArmorAttributeHelper.addModifier(
                    builder,
                    schoolPowerAttribute,
                    schoolSpellPowerBonusPerHistory,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "apprenticecodex.chromatic_magia_dress."
                            + ChromaticMagiaDressStats.typeToken(armorType)
                            + ".history_spell_power."
                            + i
            );
        }
    }

    private boolean isSupportedArmorEnchantment(Enchantment enchantment) {
        return EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get();
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
