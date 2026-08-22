package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.ChatFormatting;
import jp.aquafactory.apprenticecodex.renderer.item.FloatmountBroomItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class FloatmountBroomItem extends Item implements GeoItem {
    private static final RawAnimation STATIC = RawAnimation.begin().thenLoop("mount");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public FloatmountBroomItem() {
        super(new Properties().stacksTo(1));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
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

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
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

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.floatmount_broom.desc_1",
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.floatmount_broom.desc_2",
                Component.keybind("key.sneak"),
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.floatmount_broom.desc_3",
                Component.literal(Integer.toString(FloatmountBroomConfigState.normalFlightManaThreshold()))
                        .withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.floatmount_broom.desc_4"
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(STATIC);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private FloatmountBroomItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new FloatmountBroomItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
