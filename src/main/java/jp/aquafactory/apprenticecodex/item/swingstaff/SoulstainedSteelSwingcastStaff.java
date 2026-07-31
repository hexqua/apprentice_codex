package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public final class SoulstainedSteelSwingcastStaff extends AbstractRightClickMagicWeaponItem
        implements SwingTriggeredMagicItem, GeoItem {
    public static final int MANA_COST_PER_BLADE = 10;
    public static final double MIN_CHARGE_RECOVERY_RATE = 0.01D;
    public static final int MAX_BLADE_COUNT = 3;
    public static final double MAGIC_DAMAGE_BONUS = 3.0D;
    public static final int ENCHANTMENT_VALUE = 16;
    private static final String ITEM_KEY = "soulstained_steel_swingcast_staff";
    private static final ResourceLocation MAGIC_DAMAGE =
            ResourceLocation.fromNamespaceAndPath("lodestone", "magic_damage");
    private static final ResourceLocation MAGIC_DAMAGE_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, ITEM_KEY + "_magic_damage");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/" + ITEM_KEY + ".png");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SoulstainedSteelSwingcastStaff() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                false,
                ENCHANTMENT_VALUE,
                ITEM_KEY,
                2.0D,
                -2.4D
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        // Mnemonic Blade専用武器のため、Iron'sのspell containerは作らない。
    }

    @Override
    public boolean canTriggerSpellOnSwing(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
                && player.getMainHandItem().getItem() == this
                && MalumMnemonicBladeBridge.isAvailable();
    }

    @Override
    public boolean tryTriggerSpellOnSwing(Player player, InteractionHand hand, boolean bypassChargeCheck) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || hand != InteractionHand.MAIN_HAND
                || player.getMainHandItem().getItem() != this
                || (!bypassChargeCheck && !isFullyChargedAttack(player))) {
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return false;
        }

        var manaCost = getManaCostPerBlade(player);
        var bladeCount = player.getAbilities().instabuild
                ? MAX_BLADE_COUNT
                : resolveBladeCount(magicData.getMana(), manaCost);
        if (bladeCount <= 0 || !MalumMnemonicBladeBridge.fire(serverPlayer, player.getMainHandItem(), bladeCount)) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            magicData.setMana(Math.max(0.0F, (float) (magicData.getMana() - bladeCount * manaCost)));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    public static int resolveBladeCount(float availableMana) {
        return resolveBladeCount(availableMana, MANA_COST_PER_BLADE);
    }

    public static int resolveBladeCount(float availableMana, double manaCostPerBlade) {
        if (!Float.isFinite(availableMana) || availableMana <= 0.0F || !Double.isFinite(manaCostPerBlade)
                || manaCostPerBlade <= 0.0D) {
            return 0;
        }
        return Math.min(MAX_BLADE_COUNT, Math.max(0, (int) Math.floor(availableMana / manaCostPerBlade)));
    }

    public static double resolveManaCost(double chargeRecoveryRate) {
        if (!Double.isFinite(chargeRecoveryRate)) {
            chargeRecoveryRate = 1.0D;
        }
        return MANA_COST_PER_BLADE / Math.max(MIN_CHARGE_RECOVERY_RATE, chargeRecoveryRate);
    }

    private static double getManaCostPerBlade(Player player) {
        return resolveManaCost(MalumMnemonicBladeBridge.getChargeRecoveryRate(player));
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var baseModifiers = super.getDefaultAttributeModifiers(stack);
        var magicDamage = BuiltInRegistries.ATTRIBUTE.getOptional(MAGIC_DAMAGE).orElse(null);
        if (magicDamage == null) {
            return baseModifiers;
        }

        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        builder.add(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(magicDamage),
                new AttributeModifier(MAGIC_DAMAGE_MODIFIER, MAGIC_DAMAGE_BONUS, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
        );
        return builder.build();
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.@NotNull TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(
                "item.apprenticecodex.soulstained_steel_swingcast_staff.desc"
        ).withStyle(ChatFormatting.GRAY));
    }

    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
