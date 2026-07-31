package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.renderer.item.SoulstainedSteelSwingcastStaffRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public final class SoulstainedSteelSwingcastStaff extends AbstractRightClickMagicWeaponItem
        implements SwingTriggeredMagicItem, GeoItem {
    public static final int MANA_COST_PER_BLADE = 10;
    public static final int MAX_BLADE_COUNT = 3;
    public static final double MAGIC_DAMAGE_BONUS = 3.0D;
    public static final int ENCHANTMENT_VALUE = 16;
    private static final String ITEM_KEY = "soulstained_steel_swingcast_staff";
    private static final ResourceLocation MAGIC_DAMAGE =
            ResourceLocation.fromNamespaceAndPath("lodestone", "magic_damage");
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
                -2.4D,
                bonus(
                        () -> ForgeRegistries.ATTRIBUTES.getValue(MAGIC_DAMAGE),
                        MAGIC_DAMAGE_BONUS,
                        AttributeModifier.Operation.ADDITION,
                        "lodestone.magic_damage"
                )
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        // Mnemonic Blade専用武器のため、Iron'sのspell containerは作らない。
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
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

        var bladeCount = player.getAbilities().instabuild
                ? MAX_BLADE_COUNT
                : resolveBladeCount(magicData.getMana());
        if (bladeCount <= 0 || !MalumMnemonicBladeBridge.fire(serverPlayer, player.getMainHandItem(), bladeCount)) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - bladeCount * MANA_COST_PER_BLADE));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    public static int resolveBladeCount(float availableMana) {
        return Math.min(MAX_BLADE_COUNT, Math.max(0, (int) Math.floor(availableMana / MANA_COST_PER_BLADE)));
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(
                "item.apprenticecodex.soulstained_steel_swingcast_staff.desc"
        ).withStyle(ChatFormatting.GRAY));
    }

    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SoulstainedSteelSwingcastStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SoulstainedSteelSwingcastStaffRenderer();
                }
                return renderer;
            }
        });
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
