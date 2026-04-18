package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TouchDigSpell.class, remap = false)
public abstract class TouchDigSpellMixin {
    // 外部 MOD クラス内の Minecraft メソッド呼び出し Redirect は本番 reobf 環境で崩れやすいため、
    // drop 処理全体をここで置き換えて Curio 側の疑似ツールを安定して渡す.
    // TouchDigの処理を持ってきているため、随時追従すること.
    @Inject(
            method = "doDestroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void apprenticecodex$replaceDropTool(Level level, BlockPos pos, LivingEntity livingEntity, CallbackInfo ci) {
        // Craftsman's Delightを装備していない時は実装変更の体験変化を極力回避するために元の処理に落とす.
        if (!CraftsmansDelight.isEquippedBy(livingEntity)) {
            return;
        }

        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) {
            return;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (!(blockState.getBlock() instanceof BaseFireBlock)) {
            level.levelEvent(2001, pos, Block.getId(blockState));
        }

        BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(blockState, level, pos, blockEntity, livingEntity, CraftsmansDelight.createTouchDigTool(livingEntity));
        if (level.setBlock(pos, fluidState.createLegacyBlock(), 3)) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(livingEntity, blockState));
        }
        ci.cancel();
    }

    @Redirect(
            method = "checkPreCastConditions",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;getTargetBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/ClipContext$Fluid;D)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    private BlockHitResult redirectPreCastTargetBlock(Level level, LivingEntity livingEntity, ClipContext.Fluid fluid, double distance) {
        return apprentice_codex$getTargetBlock(level, livingEntity, fluid);
    }

    @Redirect(
            method = "onCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;getTargetBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/ClipContext$Fluid;D)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    private BlockHitResult redirectOnCastTargetBlock(Level level, LivingEntity livingEntity, ClipContext.Fluid fluid, double distance) {
        return apprentice_codex$getTargetBlock(level, livingEntity, fluid);
    }

    @Redirect(
            method = "getUniqueInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"
            )
    )
    private Integer redirectDisplayedDistance(int distance, int spellLevel, LivingEntity livingEntity) {
        return CraftsmansDelight.getTouchDigRangeBlocks(livingEntity);
    }

    @Unique
    private static BlockHitResult apprentice_codex$getTargetBlock(Level level, LivingEntity livingEntity, ClipContext.Fluid fluid) {
        // TouchDig は upstream 側で 8.0D を直書きしているため、Curio 装備時だけここで射程を差し替える.
        return Utils.getTargetBlock(level, livingEntity, fluid, CraftsmansDelight.getTouchDigRange(livingEntity));
    }
}
