package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager.PendingManaRecovery;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.CrystalBladedStaffRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;

public class CrystalBladedStaff extends AbstractRightClickMagicWeaponItem implements GeoItem, UniqueItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final String ACTIVATE_ANIMATION = "activate";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_ACTIVATE = RawAnimation.begin().thenPlay("activate");
    private static final double ATTACK_DAMAGE = 4.0D;
    private static final double ATTACK_SPEED = -2.4D;
    private static final double ENTITY_REACH_BONUS = 0.5D;
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final int ENCHANTMENT_VALUE = 14;
    private static final int MIN_ORB_COUNT = 4;
    private static final int MAX_ORB_COUNT = 8;
    private static final int SPELL_ORB_COUNT = 4;
    private static final int MIN_RETURN_DELAY_TICKS = 20;
    private static final int MAX_RETURN_DELAY_TICKS = 30;
    private static final int MIN_RETURN_DURATION_TICKS = 4;
    private static final int MAX_RETURN_DURATION_TICKS = 6;
    private static final float MANA_RECOVERY_PER_ORB = 2.5f;
    // クライアント描画と同じ値で待機開始位置を求め、launch 音の位置ずれを防ぐ。
    private static final float ORB_SCATTER_DURATION_TICKS = 6.0f;
    private static final double ORB_SCATTER_GRAVITY = 0.012d;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CrystalBladedStaff() {
        super(
                new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                SpellRegistry.MANA_SLASH,
                1,
                true,
                ENCHANTMENT_VALUE,
                "CrystalBladedStaff",
                ATTACK_DAMAGE,
                ATTACK_SPEED,
                bonus(net.minecraftforge.common.ForgeMod.ENTITY_REACH, ENTITY_REACH_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION, "entity_reach"),
                bonus(AttributeRegistry.SPELL_POWER, SPELL_POWER_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE, "spell_power")
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CrystalBladedStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new CrystalBladedStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                }).triggerableAnim(ACTIVATE_ANIMATION, ANIM_ACTIVATE)
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, lines, flag);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static boolean isCrystalBladedStaff(ItemStack stack) {
        return stack.getItem() instanceof CrystalBladedStaff;
    }

    public static void spawnManaSiphonOrbs(ServerPlayer serverPlayer, Vec3 impactPosition, int totalHitMobCount) {
        spawnManaSiphonOrbBurst(serverPlayer, impactPosition, resolveOrbCount(serverPlayer.serverLevel(), totalHitMobCount));
    }

    public static void spawnManaSiphonOrbsForSpell(ServerPlayer serverPlayer, Vec3 impactPosition) {
        spawnManaSiphonOrbBurst(serverPlayer, impactPosition, SPELL_ORB_COUNT);
    }

    private static void spawnManaSiphonOrbBurst(ServerPlayer serverPlayer, Vec3 impactPosition, int orbCount) {
        var serverLevel = serverPlayer.serverLevel();
        var random = serverLevel.random;
        var orbData = new ArrayList<ManaSiphonOrbEffectPacket.OrbData>(orbCount);

        for (int i = 0; i < orbCount; i++) {
            var scatter = randomScatterVector(random.nextDouble(), random.nextDouble(), random.nextDouble());
            var returnDelayTicks = random.nextInt(MAX_RETURN_DELAY_TICKS - MIN_RETURN_DELAY_TICKS + 1) + MIN_RETURN_DELAY_TICKS;
            var returnDurationTicks = random.nextInt(MAX_RETURN_DURATION_TICKS - MIN_RETURN_DURATION_TICKS + 1) + MIN_RETURN_DURATION_TICKS;
            var scale = 0.18f + random.nextFloat() * 0.06f;
            var phaseOffset = random.nextFloat() * ((float) Math.PI * 2.0f);
            var launchPosition = launchStartPosition(impactPosition, scatter, returnDelayTicks, phaseOffset);
            orbData.add(new ManaSiphonOrbEffectPacket.OrbData(
                    (float) scatter.x,
                    (float) scatter.y,
                    (float) scatter.z,
                    returnDelayTicks,
                    returnDurationTicks,
                    scale,
                    phaseOffset
            ));
            CrystalBladedStaffManaRecoveryManager.submit(serverLevel, new PendingManaRecovery(
                    serverPlayer.getUUID(),
                    serverLevel.getGameTime() + returnDelayTicks + returnDurationTicks,
                    MANA_RECOVERY_PER_ORB
            ));
            CrystalBladedStaffManaRecoveryManager.submitLaunchSound(serverLevel,
                    new CrystalBladedStaffManaRecoveryManager.PendingLaunchSound(
                            launchPosition,
                            serverLevel.getGameTime() + returnDelayTicks
                    ));
        }

        triggerActivateAnimation(serverPlayer, serverPlayer.getMainHandItem());
        triggerActivateAnimation(serverPlayer, serverPlayer.getOffhandItem());

        Networks.sendToTrackingEntityAndSelf(serverPlayer, new ManaSiphonOrbEffectPacket(
                impactPosition,
                serverPlayer.getId(),
                orbData
        ));
    }

    private static int resolveOrbCount(net.minecraft.server.level.ServerLevel serverLevel, int totalHitMobCount) {
        var random = serverLevel.random;
        var baseOrbCount = random.nextInt(MAX_ORB_COUNT - MIN_ORB_COUNT + 1) + MIN_ORB_COUNT;
        var orbPenalty = Math.max(0, totalHitMobCount - 1);
        return Math.max(1, baseOrbCount - orbPenalty);
    }

    private static net.minecraft.world.phys.Vec3 randomScatterVector(double rx, double ry, double rz) {
        var horizontalAngle = rx * Math.PI * 2.0;
        var horizontalSpeed = 0.16 + ry * 0.14;
        var verticalSpeed = 0.12 + rz * 0.14;
        return new net.minecraft.world.phys.Vec3(
                Math.cos(horizontalAngle) * horizontalSpeed,
                verticalSpeed,
                Math.sin(horizontalAngle) * horizontalSpeed
        );
    }

    private static Vec3 launchStartPosition(Vec3 impactPosition, Vec3 scatter, int returnDelayTicks, float phaseOffset) {
        var anchor = impactPosition.add(
                scatter.scale(ORB_SCATTER_DURATION_TICKS)
        ).add(0.0d, -ORB_SCATTER_GRAVITY * ORB_SCATTER_DURATION_TICKS * ORB_SCATTER_DURATION_TICKS, 0.0d);
        var hoverTicks = returnDelayTicks - ORB_SCATTER_DURATION_TICKS;
        var phase = hoverTicks * 0.14f + phaseOffset;
        return anchor.add(
                Math.cos(phase * 0.9f) * 0.05d,
                Math.sin(phase * 1.3f) * 0.03d,
                Math.sin(phase) * 0.05d
        );
    }

    private static void triggerActivateAnimation(ServerPlayer serverPlayer, ItemStack stack) {
        if (!isCrystalBladedStaff(stack)) {
            return;
        }

        var instanceId = GeoItem.getOrAssignId(stack, serverPlayer.serverLevel());
        ((CrystalBladedStaff) stack.getItem()).triggerAnim(serverPlayer, instanceId, MAIN_CONTROLLER, ACTIVATE_ANIMATION);
    }
}
