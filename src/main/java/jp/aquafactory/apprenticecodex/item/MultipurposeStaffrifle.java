package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.MultipurposeStaffrifleRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

public final class MultipurposeStaffrifle extends Item
        implements GeoItem, NonDamageableAnvilMergeItem, IJeiInfoItem, CastAnimationOverrideItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.multipurpose_staffrifle.desc_";
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final double ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL = 0.02D;
    private static final double REFLUX_MANA_REGEN_PER_LEVEL = 0.05D;
    private static final double RESERVOIR_MAX_MANA_PER_LEVEL = 20.0D;
    private static final double SURGE_SPELL_POWER_PER_LEVEL = 0.02D;
    private static final double TENSE_CAST_TIME_REDUCTION_PER_LEVEL = 0.05D;
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("06ad0fe5-4dc8-4d4b-933c-d0d0e6675a39");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Attribute, AttributeModifier> baseMainhandModifiers = buildBaseMainhandModifiers();

    public MultipurposeStaffrifle() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }

        return buildMainhandModifiers(stack);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return isSupportedStaffrifleEnchantment(enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(MultipurposeStaffrifle::isSupportedStaffrifleEnchantment);
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return isSupportedStaffrifleEnchantment(enchantment);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MultipurposeStaffrifleRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MultipurposeStaffrifleRenderer();
                }

                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return hand == InteractionHand.MAIN_HAND
                        ? HumanoidModel.ArmPose.CROSSBOW_HOLD
                        : HumanoidModel.ArmPose.ITEM;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                   ItemStack itemInHand, float partialTick, float equipProcess,
                                                   float swingProcess) {
                applyChargedCrossbowHandTransform(poseStack, arm, equipProcess, swingProcess);
                return true;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return true;
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return false;
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return true;
    }

    private Multimap<Attribute, AttributeModifier> buildMainhandModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isEnchanted()) {
            return baseMainhandModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        addEnchantmentModifier(
                builder,
                AttributeRegistry.SPELL_POWER.get(),
                SPELL_POWER_BONUS + getEnchantmentLevel(stack, EnchantmentRegistry.SURGE) * SURGE_SPELL_POWER_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.spell_power"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.COOLDOWN_REDUCTION.get(),
                getEnchantmentLevel(stack, EnchantmentRegistry.ALACRITY) * ALACRITY_COOLDOWN_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.alacrity.cooldown_reduction"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MANA_REGEN.get(),
                getEnchantmentLevel(stack, EnchantmentRegistry.REFLUX) * REFLUX_MANA_REGEN_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.reflux.mana_regen"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.MAX_MANA.get(),
                getEnchantmentLevel(stack, EnchantmentRegistry.RESERVOIR) * RESERVOIR_MAX_MANA_PER_LEVEL,
                AttributeModifier.Operation.ADDITION,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.reservoir.max_mana"
        );
        addEnchantmentModifier(
                builder,
                AttributeRegistry.CAST_TIME_REDUCTION.get(),
                getEnchantmentLevel(stack, EnchantmentRegistry.TENSE) * TENSE_CAST_TIME_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE,
                "apprenticecodex.multipurpose_staffrifle.mainhand.enchant.tense.cast_time_reduction"
        );
        return builder.build();
    }

    private static Multimap<Attribute, AttributeModifier> buildBaseMainhandModifiers() {
        return ImmutableMultimap.of(
                AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        "apprenticecodex.multipurpose_staffrifle.mainhand.spell_power",
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
    }

    private static void addEnchantmentModifier(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
                                               Attribute attribute, double amount, AttributeModifier.Operation operation,
                                               String modifierIdSeed) {
        if (amount == 0.0D) {
            return;
        }

        builder.put(
                attribute,
                new AttributeModifier(
                        UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8)),
                        modifierIdSeed,
                        amount,
                        operation
                )
        );
    }

    private static int getEnchantmentLevel(ItemStack stack, net.minecraftforge.registries.RegistryObject<Enchantment> enchantment) {
        return enchantment.isPresent() ? stack.getEnchantmentLevel(enchantment.get()) : 0;
    }

    private static boolean isSupportedStaffrifleEnchantment(Enchantment enchantment) {
        return (EnchantmentRegistry.ALACRITY.isPresent() && enchantment == EnchantmentRegistry.ALACRITY.get())
                || (EnchantmentRegistry.REFLUX.isPresent() && enchantment == EnchantmentRegistry.REFLUX.get())
                || (EnchantmentRegistry.RESERVOIR.isPresent() && enchantment == EnchantmentRegistry.RESERVOIR.get())
                || (EnchantmentRegistry.SURGE.isPresent() && enchantment == EnchantmentRegistry.SURGE.get())
                || (EnchantmentRegistry.TENSE.isPresent() && enchantment == EnchantmentRegistry.TENSE.get())
                || (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get())
                || (EnchantmentRegistry.PLUNDER.isPresent() && enchantment == EnchantmentRegistry.PLUNDER.get());
    }

    private static void applyChargedCrossbowHandTransform(PoseStack poseStack, HumanoidArm arm, float equipProcess,
                                                          float swingProcess) {
        var rightHanded = arm == HumanoidArm.RIGHT;
        var side = rightHanded ? 1 : -1;
        var xSwing = -0.4F * Mth.sin(Mth.sqrt(swingProcess) * (float)Math.PI);
        var ySwing = 0.2F * Mth.sin(Mth.sqrt(swingProcess) * ((float)Math.PI * 2F));
        var zSwing = -0.2F * Mth.sin(swingProcess * (float)Math.PI);
        poseStack.translate(side * xSwing, ySwing, zSwing);
        applyItemArmTransform(poseStack, arm, equipProcess);
        applyItemArmAttackTransform(poseStack, arm, swingProcess);
        if (swingProcess < 0.001F) {
            poseStack.translate(side * -0.641864F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 10.0F));
        }
    }

    private static void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProcess) {
        var side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
    }

    private static void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProcess) {
        var side = arm == HumanoidArm.RIGHT ? 1 : -1;
        var sinSwing = Mth.sin(swingProcess * swingProcess * (float)Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (45.0F + sinSwing * -20.0F)));
        var sinRootSwing = Mth.sin(Mth.sqrt(swingProcess) * (float)Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * sinRootSwing * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(sinRootSwing * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
    }
}
