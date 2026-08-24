package jp.aquafactory.apprenticecodex.mixin;

import com.sammy.malum.common.item.curiosities.tools.spellweaver.SpellweavingPickaxeItem;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.compat.malum.MalumTouchDigSpellweavingContext;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TouchDigSpell.class, remap = false)
public abstract class MalumTouchDigSpellweavingMixin {
    @Redirect(
            method = "doDestroyBlock",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    remap = true
            ),
            require = 0
    )
    private boolean apprenticecodex$triggerSpellweavingAfterSuccessfulTouchDig(
            Level targetLevel,
            BlockPos targetPos,
            BlockState replacementState,
            int flags,
            Level level,
            BlockPos pos,
            LivingEntity livingEntity
    ) {
        var brokenState = targetLevel.getBlockState(targetPos);
        var destroyed = targetLevel.setBlock(targetPos, replacementState, flags);
        if (!destroyed || !(livingEntity instanceof ServerPlayer player)) {
            return destroyed;
        }

        var tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof SpellweavingPickaxeItem)
                || !CraftsmansDelight.isEquippedBy(player)
                || SpellweavingPickaxeItem.matches(brokenState, tool)) {
            return true;
        }

        var breakEvent = new BlockEvent.BreakEvent(level, pos, brokenState, player);
        MalumTouchDigSpellweavingContext.runWithInitialToolMatchBypass(
                () -> SpellweavingPickaxeItem.triggerSpellweavingEffect(breakEvent)
        );
        return true;
    }
}
