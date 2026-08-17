package jp.aquafactory.apprenticecodex.item.broom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public abstract class AbstractBroomItem extends Item implements GeoItem, ICurioItem {
    private static final RawAnimation STATIC = RawAnimation.begin().thenLoop("mount");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractBroomItem() {
        super(new Properties().stacksTo(1));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // Curios外へ強制移動・複製されたstackへ展開状態を持ち込まず、従来の通常設置へ戻す。
            BroomDeploymentState.clear(stack);
        }
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        var eye = player.getEyePosition();
        var look = player.getViewVector(1.0F);
        var obstructionBox = player.getBoundingBox().expandTowards(look.scale(5.0D)).inflate(1.0D);
        for (Entity entity : level.getEntities(player, obstructionBox, Entity::isPickable)) {
            var box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) {
                return InteractionResultHolder.pass(stack);
            }
        }

        var broom = createBroom(level);
        var customName = stack.hasCustomHoverName() ? stack.getHoverName() : null;
        broom.setCustomName(customName);
        broom.setCustomNameVisible(customName != null);
        broom.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        broom.setYRot(player.getYRot());
        var spawnBox = broom.getBoundingBox();
        if (!level.noCollision(broom, spawnBox)
                || !level.getEntities(broom, spawnBox, Entity::isPickable).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            // 他MODが生成イベントを拒否した場合などに、生成されていない箒の消費を防ぐ.
            // 基本はバニラボートと同じよう警告表示は現時点ではいれない.
            if (!level.addFreshEntity(broom)) {
                return InteractionResultHolder.fail(stack);
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    protected abstract AbstractBroomEntity createBroom(Level level);

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return BroomCurioSupport.canEquip(slotContext);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // 従来の設置操作を維持し、装備はCurios画面からの明示操作に限定する.
        return false;
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            CallBroomDeploymentManager.onUnequip(player, stack);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            CallBroomDeploymentManager.reconcileEquippedStack(player, stack);
        }
    }

    protected static void appendPlacementAndRecoveryTooltip(
            @NotNull List<Component> tooltipComponents,
            String keyPrefix
    ) {
        tooltipComponents.add(Component.translatable(
                keyPrefix + ".desc_1",
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                keyPrefix + ".desc_2",
                Component.keybind("key.sneak"),
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public abstract void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                         @NotNull List<Component> tooltipComponents,
                                         @NotNull TooltipFlag tooltipFlag);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(STATIC);
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
