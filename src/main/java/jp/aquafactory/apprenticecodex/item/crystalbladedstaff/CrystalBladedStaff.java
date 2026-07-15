package jp.aquafactory.apprenticecodex.item.crystalbladedstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager.PendingLaunchSound;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffManaRecoveryManager.PendingManaRecovery;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.CrystalBladedStaffRenderer;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;

public class CrystalBladedStaff extends AbstractSwingMagicItem implements GeoItem, UniqueItem {
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
                ENCHANTMENT_VALUE,
                "CrystalBladedStaff",
                ATTACK_DAMAGE,
                ATTACK_SPEED,
                bonus(Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_BONUS, AttributeModifier.Operation.ADD_VALUE, "entity_reach"),
                bonus(AttributeRegistry.SPELL_POWER.value(), SPELL_POWER_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "spell_power")
        );
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        initializeSpellContainer(stack);
    }

    @Override
    protected boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return false;
        }

        if (!spellContainer.isSpellWheel()) {
            return super.normalizeLegacyOverriddenSpellContainerIfNeeded(stack);
        }

        // 旧版の刃の結晶杖は Mana Slash/差し替え Imbue を spell wheel に出していた。
        // 左クリック発動へ移行した後も既存ワールドの杖が同じ魔法を保持しつつ、wheel には出ないよう正規化する。
        var spellData = spellContainer.getSpellAtIndex(0);
        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            var locked = matchesConfiguredPresetSpell(spellData) && !spellData.canRemove();
            if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, locked)) {
                return false;
            }

            ISpellContainer.set(stack, normalized.toImmutable());
            if (locked) {
                PresetSpellContainerStateHelper.clearRememberedState(stack);
            } else {
                PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
            }
            return true;
        }

        InitialSpellContainerHelper.addInitialSpellIfEnabled(normalized, SpellRegistry.MANA_SLASH, 1, 0, true);
        ISpellContainer.set(stack, normalized.toImmutable());
        PresetSpellContainerStateHelper.clearRememberedState(stack);
        return true;
    }

    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private CrystalBladedStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected String getSwingCastTooltipTranslationKey() {
        return getDescriptionId() + ".swing_miss.desc";
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.DIAMOND) || super.isValidRepairItem(toRepair, repair);
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
            CrystalBladedStaffManaRecoveryManager.submitLaunchSound(serverLevel, new PendingLaunchSound(
                    launchPosition,
                    serverLevel.getGameTime() + returnDelayTicks
            ));
        }

        triggerActivateAnimation(serverPlayer, serverPlayer.getMainHandItem());
        triggerActivateAnimation(serverPlayer, serverPlayer.getOffhandItem());
        Networks.sendToTrackingEntityAndSelf(serverPlayer, new ManaSiphonOrbEffectPacket(impactPosition, serverPlayer.getId(), orbData));
    }

    private static int resolveOrbCount(ServerLevel serverLevel, int totalHitMobCount) {
        var random = serverLevel.random;
        var baseOrbCount = random.nextInt(MAX_ORB_COUNT - MIN_ORB_COUNT + 1) + MIN_ORB_COUNT;
        var orbPenalty = Math.max(0, totalHitMobCount - 1);
        return Math.max(1, baseOrbCount - orbPenalty);
    }

    private static Vec3 randomScatterVector(double rx, double ry, double rz) {
        var horizontalAngle = rx * Math.PI * 2.0;
        var horizontalSpeed = 0.16 + ry * 0.14;
        var verticalSpeed = 0.12 + rz * 0.14;
        return new Vec3(
                Math.cos(horizontalAngle) * horizontalSpeed,
                verticalSpeed,
                Math.sin(horizontalAngle) * horizontalSpeed
        );
    }

    private static Vec3 launchStartPosition(Vec3 impactPosition, Vec3 scatter, int returnDelayTicks, float phaseOffset) {
        var anchor = impactPosition.add(scatter.scale(ORB_SCATTER_DURATION_TICKS))
                .add(0.0d, -ORB_SCATTER_GRAVITY * ORB_SCATTER_DURATION_TICKS * ORB_SCATTER_DURATION_TICKS, 0.0d);
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
