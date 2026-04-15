package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = SpectralHammer.class, remap = false)
public abstract class SpectralHammerMixin {
    @Shadow
    private Player owner;

    @Redirect(
            method = "lambda$tick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/spells/spectral_hammer/SpectralHammer;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"
            )
    )
    private List<ItemStack> redirectDropResources(BlockState state, Level level, BlockPos pos) {
        return apprentice_codex$dropResources(state, level, pos, owner);
    }

    @Unique
    private static List<ItemStack> apprentice_codex$dropResources(BlockState state, Level level, BlockPos pos, Player owner) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        var tool = CraftsmansDelight.createSpectralHammerTool(owner);
        var drops = Block.getDrops(state, serverLevel, pos, null, owner, tool);
        state.spawnAfterBreak(serverLevel, pos, tool, true);
        return drops;
    }
}
