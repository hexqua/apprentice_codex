package jp.aquafactory.apprenticecodex.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.armor.EnchantressRobeRenderer;
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

public class SoulcollectorRobeItem extends ArmorItem implements GeoItem, IPresetSpellContainer, WisdomPolicy {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Type armorType;
    private final Multimap<Attribute, AttributeModifier> robeAttributeModifiers;

    public SoulcollectorRobeItem(Type type) {
        super(SoulcollectorRobeStats.MATERIAL, type, new Properties());
        this.armorType = type;
        this.robeAttributeModifiers = SoulcollectorRobeStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
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
                    // todo:ソウルコレクターに差し替える。
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
        SoulcollectorRobeStats.addSpellPowerModifier(
                builder,
                armorType,
                ApprenticeCodexServerConfig.soulcollectorRobeSpellPowerBonusPerPiece()
        );
        return builder.build();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return SoulcollectorRobeStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return SoulcollectorRobeStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // GeoArmor 描画とは別に vanilla 防具テクスチャも問い合わせられるため、既存テクスチャへ解決して警告を出させない。
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/soulcollector_robe.png").toString();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private boolean isSupportedRobeEnchantment(Enchantment enchantment) {
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
