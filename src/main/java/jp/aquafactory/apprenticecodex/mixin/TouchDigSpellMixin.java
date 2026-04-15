package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TouchDigSpell.class, remap = false)
public abstract class TouchDigSpellMixin {
    @Redirect(
            method = "doDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack redirectMainHandItem(LivingEntity livingEntity) {
        return CraftsmansDelight.createTouchDigTool(livingEntity);
    }

    @Redirect(
            method = "checkPreCastConditions",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;getTargetBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/ClipContext$Fluid;D)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    private BlockHitResult redirectPreCastTargetBlock(
            Level level,
            LivingEntity entity,
            ClipContext.Fluid fluid,
            double distance,
            Level originalLevel,
            int spellLevel,
            LivingEntity livingEntity,
            MagicData magicData
    ) {
        return Utils.getTargetBlock(level, entity, fluid, CraftsmansDelight.getTouchDigRange(livingEntity));
    }

    @Redirect(
            method = "onCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;getTargetBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/ClipContext$Fluid;D)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    private BlockHitResult redirectOnCastTargetBlock(
            Level level,
            LivingEntity entity,
            ClipContext.Fluid fluid,
            double distance,
            Level originalLevel,
            int spellLevel,
            LivingEntity livingEntity,
            CastSource castSource,
            MagicData magicData
    ) {
        return Utils.getTargetBlock(level, entity, fluid, CraftsmansDelight.getTouchDigRange(livingEntity));
    }

    @ModifyConstant(method = "getUniqueInfo", constant = @Constant(intValue = 8))
    private int modifyDisplayedDistance(int original, int spellLevel, LivingEntity livingEntity) {
        return CraftsmansDelight.getTouchDigRangeBlocks(livingEntity);
    }
}
