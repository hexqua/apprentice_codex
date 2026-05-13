package jp.aquafactory.apprenticecodex.item.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ReflectcastShield extends AbstractImbueShieldItem implements GeoItem, IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.reflectcast_shield.desc_";

    public static final int DURABILITY = 1561;
    public static final int DURABILITY_SUPPRESSION_TICKS = 10;
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_shatter_capable_weapon")
    );
    private static final float MINIMUM_DURABILITY_DAMAGE = 3.0F;
    private static final int ENCHANTMENT_VALUE = 1;
    private static final String LAST_DURABILITY_COST_TICK_TAG = "ApprenticeCodexReflectcastShieldLastDurabilityCostTick";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ReflectcastShield() {
        super(new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.UNCOMMON));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    protected void appendAlwaysVisibleImbueTooltip(List<Component> lines) {
        lines.add(ImbueTooltipHelper.translatableGray("item." + ApprenticeCodex.MODID + ".reflectcast_shield.hint"));
        lines.add(ImbueTooltipHelper.translatableGray("item." + ApprenticeCodex.MODID + ".reflectcast_shield.cast_hint"));
    }

    public static int resolveBlockedDurabilityCost(float originalBlockedDamage, boolean spellTriggered) {
        if (originalBlockedDamage < MINIMUM_DURABILITY_DAMAGE) {
            return 0;
        }

        var vanillaCost = 1 + Mth.floor(originalBlockedDamage);
        return spellTriggered ? Math.min(vanillaCost, 1) : vanillaCost;
    }

    public static boolean isDurabilityConsumptionSuppressed(ItemStack stack, long gameTime) {
        if (stack.isEmpty()) {
            return false;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && isDurabilityConsumptionSuppressed(customData.copyTag(), gameTime);
    }

    public static boolean isDurabilityConsumptionSuppressed(CompoundTag tag, long gameTime) {
        if (tag == null || !tag.contains(LAST_DURABILITY_COST_TICK_TAG)) {
            return false;
        }

        return gameTime - tag.getLong(LAST_DURABILITY_COST_TICK_TAG) <= DURABILITY_SUPPRESSION_TICKS;
    }

    public static void rememberDurabilityConsumed(ItemStack stack, long gameTime) {
        if (!stack.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(LAST_DURABILITY_COST_TICK_TAG, gameTime));
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
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return enchantmentId != null && isMalumSpiritPlunder(stack, enchantmentId);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static boolean isMalumSpiritPlunder(ItemStack stack, ResourceLocation enchantmentId) {
        return MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON);
    }
}
