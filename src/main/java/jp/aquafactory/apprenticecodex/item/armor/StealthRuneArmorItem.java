package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.StealthRuneArmorRenderer;
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

public class StealthRuneArmorItem extends ArmorItem implements GeoItem, IPresetSpellContainer {
    private static final ResourceLocation ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/stealth_rune_armor.png");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Type armorType;
    private final Multimap<Attribute, AttributeModifier> armorAttributeModifiers;

    public StealthRuneArmorItem(Type type) {
        super(StealthRuneArmorStats.MATERIAL, type, new Properties());
        this.armorType = type;
        this.armorAttributeModifiers = StealthRuneArmorStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    public boolean hasImbueSlot() {
        return armorType == Type.CHESTPLATE;
    }

    public static boolean isStealthRuneArmor(ItemStack stack) {
        return stack.getItem() instanceof StealthRuneArmorItem;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                   EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new StealthRuneArmorRenderer();
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

        // 胴だけに魔法枠を持たせ、既存ローブ系と同じ装備感に揃える.
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
        if (slot != armorType.getSlot() || armorAttributeModifiers.isEmpty()) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.putAll(baseModifiers);
        builder.putAll(armorAttributeModifiers);
        return builder.build();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return StealthRuneArmorStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return false;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // GeoArmor とは別に vanilla の問い合わせも来るため、既存テクスチャへ明示解決して警告を避ける.
        return ARMOR_TEXTURE.toString();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
