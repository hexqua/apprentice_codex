package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.spell.mistform.MistFormCollisionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMistFormCollisionMixin {
    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true)
    private void apprenticecodex$removeMistFormPassableCollision(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!cir.getReturnValue().isEmpty() && MistFormCollisionHelper.canPassThrough(asState(), context)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
