package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.VanillaEnchantmentCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.renderer.armor.SoulcollectorRobeRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class SoulcollectorRobeItem extends ArmorItem implements GeoItem, IPresetSpellContainer, WisdomPolicy {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers baseModifiers;

    public SoulcollectorRobeItem(Type type) {
        super(Holder.direct(SoulcollectorRobeStats.MATERIAL), type, SoulcollectorRobeStats.createProperties(type));
        this.baseModifiers = SoulcollectorRobeStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SoulcollectorRobeRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T entity, ItemStack stack,
                                                                                   @Nullable EquipmentSlot slot,
                                                                                   @Nullable HumanoidModel<T> original) {
                if (renderer == null) renderer = new SoulcollectorRobeRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (getType() == Type.CHESTPLATE && !ISpellContainer.isSpellContainer(stack)) {
            ISpellContainer.set(stack, ISpellContainer.create(1, true, true));
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : super.getDefaultAttributeModifiers(stack).modifiers()) builder.add(entry.attribute(), entry.modifier(), entry.slot());
        for (var entry : baseModifiers.modifiers()) builder.add(entry.attribute(), entry.modifier(), entry.slot());
        SoulcollectorRobeStats.addConfiguredModifiers(builder, getType(),
                ApprenticeCodexServerConfig.soulcollectorRobeSpellPowerBonusPerPiece(),
                ApprenticeCodexServerConfig.soulcollectorRobeMagicProficiencyBonusPerPiece());
        return builder.build();
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment) || enchantment.is(Enchantments.WISDOM);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment)
                || VanillaEnchantmentCompatibility.isNonVanillaAndSupported(enchantment,
                supportedEnchantment -> supportsEnchantment(stack, supportedEnchantment));
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return super.isBookEnchantable(stack, book)
                && VanillaEnchantmentCompatibility.bookContainsOnlyVanillaOrSupported(book,
                supportedEnchantment -> supportsEnchantment(stack, supportedEnchantment));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return SoulcollectorRobeStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repair) {
        return SoulcollectorRobeStats.isRepairIngredient(repair) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
