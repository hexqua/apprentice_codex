package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import io.redspace.ironsspellbooks.render.ClientStaffItemExtensions;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.PastelStaffRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class PastelStaff extends StaffItem implements GeoItem, IPresetSpellContainer, UniqueItem {
    public static final String STONE_TINT_COLOR_TAG = "StoneTintColor";
    public static final int DEFAULT_STONE_TINT_COLOR = 0xFFFFFF;
    private static final StaffTier PASTEL_STAFF_TIER = new StaffTier(
            3.0F,
            -3.0F,
            new AttributeContainer(
                    AttributeRegistry.CAST_TIME_REDUCTION,
                    0.15D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ),
            new AttributeContainer(
                    AttributeRegistry.COOLDOWN_REDUCTION,
                    0.15D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ),
            new AttributeContainer(
                    AttributeRegistry.SPELL_POWER,
                    0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            )
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PastelStaff() {
        super(new Item.Properties().stacksTo(1), PASTEL_STAFF_TIER);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ClientStaffItemExtensions() {
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
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, true, false).mutableCopy();
        spellContainer.addSpell(SpellRegistry.PALETTE_SHIFT.get(), 1, true);
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
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
