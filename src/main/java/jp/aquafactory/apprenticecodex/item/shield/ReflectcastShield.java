package jp.aquafactory.apprenticecodex.item.shield;

import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ReflectcastShield extends AbstractImbueShieldItem implements GeoItem {
    private static final int ENCHANTMENT_VALUE = 1;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ReflectcastShield() {
        super(new Item.Properties().stacksTo(1).durability(336).rarity(Rarity.UNCOMMON));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
