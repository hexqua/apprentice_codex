package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SoulstainedSteelSwingcastStaffServerConfig;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
import java.util.Optional;
import java.util.function.Consumer;

public final class SoulstainedSteelSwingcastStaff extends AbstractRightClickMagicWeaponItem
        implements SwingTriggeredMagicItem, GeoItem {
    public static final double DEFAULT_MANA_COST_PER_BLADE =
            SoulstainedSteelSwingcastStaffServerConfig.DEFAULT_MANA_COST_PER_BLADE;
    public static final int MAX_BLADE_COUNT = 3;
    private static final double MANA_COST_CEILING_EPSILON = 1.0e-9D;
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

        var manaCostPerBlade = ApprenticeCodexServerConfig.soulstainedSteelSwingcastStaffManaCostPerBlade();
        var bladeCount = player.getAbilities().instabuild
                ? MAX_BLADE_COUNT
                : resolveBladeCount(magicData.getMana(), manaCostPerBlade);
        if (bladeCount <= 0 || !MalumMnemonicBladeBridge.fire(serverPlayer, player.getMainHandItem(), bladeCount)) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            magicData.setMana(Math.max(0.0F, (float) (magicData.getMana() - bladeCount * manaCostPerBlade)));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    public static int resolveBladeCount(float availableMana) {
        return resolveBladeCount(availableMana, DEFAULT_MANA_COST_PER_BLADE);
    }

    public static int resolveBladeCount(float availableMana, double manaCostPerBlade) {
        if (!Double.isFinite(manaCostPerBlade) || manaCostPerBlade < 0.0D) {
            return 0;
        }
        if (manaCostPerBlade == 0.0D) {
            return MAX_BLADE_COUNT;
        }
        if (!Float.isFinite(availableMana) || availableMana <= 0.0F) {
            return 0;
        }
        return Math.min(MAX_BLADE_COUNT, Math.max(0, (int) Math.floor(availableMana / manaCostPerBlade)));
    }

    public static long resolveDisplayedTotalManaCost(double baseManaCostPerBlade) {
        if (!Double.isFinite(baseManaCostPerBlade) || baseManaCostPerBlade <= 0.0D) {
            return 0L;
        }
        return (long) Math.ceil(baseManaCostPerBlade * MAX_BLADE_COUNT - MANA_COST_CEILING_EPSILON);
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
        resolveClientManaCostTooltip().ifPresent(lines::add);
    }

    private static Optional<Component> resolveClientManaCostTooltip() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return Optional.empty();
        }
        return Optional.ofNullable(SoulstainedSteelSwingcastStaffClientTooltip.createLine());
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
