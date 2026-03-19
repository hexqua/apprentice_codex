package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ExplorersCaneRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class ExplorersCane extends AbstractOffhandMagicItem implements GeoItem, UniqueItem {
    private static final int ENCHANTMENT_VALUE = 15;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExplorersCane() {
        super(
                SpellRegistry.LONG_STRIDE,
                1,
                Rarity.RARE,
                "explorers_cane",
                bonus(Attributes.MOVEMENT_SPEED, 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL),
                bonus(ForgeMod.STEP_HEIGHT_ADDITION, 0.5D, AttributeModifier.Operation.ADDITION)
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ExplorersCaneRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ExplorersCaneRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
