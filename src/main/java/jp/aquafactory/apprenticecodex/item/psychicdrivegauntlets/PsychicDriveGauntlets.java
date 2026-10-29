package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Optional;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsUseInput;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.PsychicDriveGauntletsItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PsychicDriveGauntlets extends Item implements GeoItem, WisdomPolicy, NonDamageableAnvilMergeItem,
        SpellCalibrationAdjustmentTarget {
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    private static final CalibrationAdjustmentProfile CALIBRATION_PROFILE = CalibrationAdjustmentProfile.of(
            CalibrationAdjustmentRule.unique("mining_tool", stack -> stack.is(ItemTags.PICKAXES),
                    CalibrationAdjustmentHint.taggedItems(
                            "container.apprenticecodex.spell_calibration_bench.tooltip.item_hint_mining_tools",
                            ItemTags.PICKAXES, () -> Items.DIAMOND_PICKAXE))
                    .withEffectLines(CalibrationAdjustmentEffects.addMiningFunction()));
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_PROFILE;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    public static ItemStack miningTool(ItemStack stack, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof PsychicDriveGauntlets gauntlets)) return ItemStack.EMPTY;
        for (int slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; slot++) {
            var tool = gauntlets.getCalibrationAdjustment(stack, slot);
            if (tool.is(ItemTags.PICKAXES)) return tool;
        }
        return ItemStack.EMPTY;
    }

    public PsychicDriveGauntlets() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        return ImmutableMultimap.of(
                Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 3.0,
                        AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                        20.0 / (PsychicDriveGauntletsMotion.PREPARE_TICKS + PsychicDriveGauntletsArm.MISS_TICKS
                                + PsychicDriveGauntletsArm.RETURN_TICKS) - 4.0, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) { return true; }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) { return 22; }

    @Override
    public boolean canApplyAtEnchantingTable(@NotNull ItemStack stack, @NotNull Enchantment enchantment) {
        var id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantment == Enchantments.SWEEPING_EDGE || MalumHauntedCompat.isAnimatedEnchantment(id)
                || enchantment.canApplyAtEnchantingTable(new ItemStack(Items.ELYTRA))) return false;
        return enchantment == EnchantmentRegistry.WISDOM.get()
                || "malum:spirit_plunder".equals(String.valueOf(id)) || MalumHauntedCompat.isHauntedEnchantment(id)
                || enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return super.isBookEnchantable(stack, book) && EnchantmentHelper.getEnchantments(book)
                .keySet().stream().allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                          @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        // 通常のinteractionが消費しなかった後にだけ呼ばれるItem.useで処理する。
        if (level.isClientSide) return PsychicDriveGauntletsUseInput.use(player, hand);
        if (PsychicDriveGauntletsSelection.duplicate(player)
                || PsychicDriveGauntletsSelection.canUse(player, hand)) return InteractionResultHolder.consume(stack);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PsychicDriveGauntletsItemRenderer renderer;

            @Override
            public HumanoidModel.ArmPose getArmPose(@NotNull LivingEntity entity, @NotNull InteractionHand hand,
                                                   @NotNull ItemStack stack) {
                return HumanoidModel.ArmPose.EMPTY;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new PsychicDriveGauntletsItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
