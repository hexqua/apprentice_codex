package jp.aquafactory.apprenticecodex.item.shield;

import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.renderer.item.ReflectcastShieldRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class ReflectcastShield extends AbstractImbueShieldItem implements GeoItem {
    public static final int DURABILITY = 1561;
    public static final int DURABILITY_SUPPRESSION_TICKS = 10;
    private static final float MINIMUM_DURABILITY_DAMAGE = 3.0F;
    private static final int ENCHANTMENT_VALUE = 1;
    private static final String LAST_DURABILITY_COST_TICK_TAG = "ApprenticeCodexReflectcastShieldLastDurabilityCostTick";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ReflectcastShield() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.UNCOMMON));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static int resolveBlockedDurabilityCost(float originalBlockedDamage, boolean spellTriggered) {
        if (originalBlockedDamage < MINIMUM_DURABILITY_DAMAGE) {
            return 0;
        }

        var vanillaCost = 1 + Mth.floor(originalBlockedDamage);
        return spellTriggered ? Math.min(vanillaCost, 1) : vanillaCost;
    }

    public static boolean isDurabilityConsumptionSuppressed(ItemStack stack, long gameTime) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }

        return isDurabilityConsumptionSuppressed(stack.getTag(), gameTime);
    }

    public static boolean isDurabilityConsumptionSuppressed(CompoundTag tag, long gameTime) {
        if (tag == null || !tag.contains(LAST_DURABILITY_COST_TICK_TAG)) {
            return false;
        }

        return gameTime - tag.getLong(LAST_DURABILITY_COST_TICK_TAG) <= DURABILITY_SUPPRESSION_TICKS;
    }

    public static void rememberDurabilityConsumed(ItemStack stack, long gameTime) {
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putLong(LAST_DURABILITY_COST_TICK_TAG, gameTime);
        }
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
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ReflectcastShieldRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ReflectcastShieldRenderer();
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
