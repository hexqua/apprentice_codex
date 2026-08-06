package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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
        broom.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        broom.setYRot(player.getYRot());
        var spawnBox = broom.getBoundingBox();
        if (!level.noCollision(broom, spawnBox)
                || !level.getEntities(broom, spawnBox, Entity::isPickable).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            level.addFreshEntity(broom);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(STATIC)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
