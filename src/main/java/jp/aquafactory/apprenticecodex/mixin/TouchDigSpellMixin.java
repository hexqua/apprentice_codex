package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TouchDigSpell.class, remap = false)
public abstract class TouchDigSpellMixin {
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
