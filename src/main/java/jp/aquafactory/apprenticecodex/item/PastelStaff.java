package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.renderer.item.PastelStaffRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class PastelStaff extends Item implements GeoItem {
    public static final String STONE_TINT_COLOR_TAG = "StoneTintColor";
    public static final int DEFAULT_STONE_TINT_COLOR = 0xFFFFFF;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PastelStaff() {
        super(new Item.Properties().stacksTo(1));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PastelStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new PastelStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        // アニメーションは後続でレンダラー制御するため未登録.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public int getStoneTintColor(ItemStack stack) {
        return readStoneTintColor(stack);
    }

    public static int readStoneTintColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        var tag = stack.getTag();
        if (tag == null || !tag.contains(STONE_TINT_COLOR_TAG, Tag.TAG_ANY_NUMERIC)) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        return tag.getInt(STONE_TINT_COLOR_TAG) & 0xFFFFFF;
    }

    public static void writeStoneTintColor(ItemStack stack, int rgb) {
        stack.getOrCreateTag().putInt(STONE_TINT_COLOR_TAG, rgb & 0xFFFFFF);
    }
}
