package jp.aquafactory.apprenticecodex.item.magiengineersstaff;

import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCalibration;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.renderer.item.MagiEngineersStaffRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class MagiEngineersStaff extends Item implements GeoItem, WisdomPolicy, SpellCalibrationAdjustmentTarget {
    public static final String KEY = "item.apprenticecodex.magi_engineers_staff";
    public static final int CONVERSION_INTERVAL_TICKS = 10;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MagiEngineersStaff() {
        super(new Properties().stacksTo(1).fireResistant().attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
        GeoItem.registerSyncedAnimatable(this);
    }

    public static boolean available() {
        return ModList.get().isLoaded("magitech");
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack stack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack stack) {
        return available() ? MagiEngineersStaffCalibration.PROFILE : CalibrationAdjustmentProfile.of();
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        return available() && MagiEngineersStaffCompat.begin(player, stack, hand)
                ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remaining) {
        if (available()) MagiEngineersStaffCompat.tick(entity, stack, getUseDuration(stack, entity) - remaining);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int remaining) {
        if (available()) MagiEngineersStaffCompat.finish(entity);
    }

    @Override
    public void onStopUsing(@NotNull ItemStack stack, @NotNull LivingEntity entity, int remaining) {
        if (available()) MagiEngineersStaffCompat.cancel(entity);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (available() && !level.isClientSide && entity instanceof Player player
                && (player.getMainHandItem() == stack || player.getOffhandItem() == stack)
                && player.tickCount % CONVERSION_INTERVAL_TICKS == 0) {
            MagiEngineersStaffCompat.convertMana(player);
        }
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 22;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility ability) {
        return ability == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, ability);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)
                || enchantment.is(net.minecraft.world.item.enchantment.Enchantments.MENDING)) return false;
        var id = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        return new ItemStack(Items.DIAMOND_SWORD).supportsEnchantment(enchantment)
                || enchantment.is(Enchantments.WISDOM)
                || MalumCompatibility.isSpiritPlunderSupported(stack, id)
                || MalumCompatibility.isMagicCapableWeaponEnchantment(stack, id)
                || MalumCompatibility.isReplenishingEnchantment(id);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return super.isBookEnchantable(stack, book) && EnchantmentHelper.getEnchantmentsForCrafting(book).keySet()
                .stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        if (FMLEnvironment.dist == Dist.CLIENT) MagiEngineersStaffTooltip.append(stack, lines);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "staff", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MagiEngineersStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new MagiEngineersStaffRenderer();
                return renderer;
            }
        });
    }
}
