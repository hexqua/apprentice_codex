package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

import jp.aquafactory.apprenticecodex.renderer.armor.ApprenticeMageRobeRenderer;

public class ApprenticeMageRobeItem extends ArmorItem implements GeoItem, IPresetSpellContainer {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers robeAttributeModifiers;

    public ApprenticeMageRobeItem(Type type) {
        super(Holder.direct(ApprenticeMageRobeStats.MATERIAL), type, ApprenticeMageRobeStats.createProperties(type));
        this.robeAttributeModifiers = ApprenticeMageRobeStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ApprenticeMageRobeRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new ApprenticeMageRobeRenderer();
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
        if (itemStack == null || getType() != Type.CHESTPLATE || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        // 身体に魔法枠+1
        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, true));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var baseModifiers = super.getDefaultAttributeModifiers(stack);
        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        for (var entry : robeAttributeModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        ApprenticeMageRobeStats.addSpellPowerModifier(
                builder,
                getType(),
                ApprenticeCodexServerConfig.apprenticeMageRobeSpellPowerBonusPerPiece()
        );
        return builder.build();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, ItemStack repair) {
        return ApprenticeMageRobeStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ApprenticeMageRobeStats.enchantmentValue();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
