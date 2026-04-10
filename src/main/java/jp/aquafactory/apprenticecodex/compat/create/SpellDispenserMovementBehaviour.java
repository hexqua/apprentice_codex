package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.dispenser.MountedDispenseBehavior;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SpellDispenserMovementBehaviour implements MovementBehaviour {
    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        var spellSource = getSpellSource(context);
        if (spellSource.isEmpty()) {
            playActivationSound(serverLevel, pos, false);
            return;
        }

        var ownerProfile = SpellDispenserBlockEntity.readOwnerProfile(context.blockEntityData);
        if (ownerProfile == null) {
            playActivationSound(serverLevel, pos, false);
            return;
        }

        var validation = SpellDispenserSpellValidator.validate(spellSource);
        if (!validation.isSupported()) {
            playActivationSound(serverLevel, pos, false);
            return;
        }

        var spellData = validation.spellData();
        var castType = spellData.getSpell().getCastType();
        if (castType != CastType.INSTANT && castType != CastType.LONG) {
            // v1 は通常ディスペンサー相当の単発挙動に限定し、CONTINUOUS の保持制御は地上設置側へ残す.
            playActivationSound(serverLevel, pos, false);
            return;
        }

        var facing = resolveFacing(context);
        var castResult = SpellDispenserCastHelper.tryCast(serverLevel, pos, facing, spellSource.copy(), ownerProfile);
        playActivationSound(serverLevel, pos, castResult.succeeded());
    }

    @Override
    public @Nullable ItemStack canBeDisabledVia(MovementContext context) {
        // ContraptionControls のフィルタは「何のアクターか」で安定させる。
        // 中身の scroll を返すと Create 側 UI から SpellDispenser を選べなくなる。
        return new ItemStack(ItemRegistry.SPELL_DISPENSER.get());
    }

    private static ItemStack getSpellSource(MovementContext context) {
        var itemStorage = context.getItemStorage();
        if (itemStorage == null || itemStorage.getSlots() <= 0) {
            return ItemStack.EMPTY;
        }

        return itemStorage.getStackInSlot(0);
    }

    private static Direction resolveFacing(MovementContext context) {
        var normal = MountedDispenseBehavior.getDispenserNormal(context);
        return MountedDispenseBehavior.getClosestFacingDirection(normal);
    }

    private static void playActivationSound(ServerLevel level, BlockPos pos, boolean succeeded) {
        level.playSound(
                null,
                pos,
                succeeded ? SoundEvents.DISPENSER_DISPENSE : SoundEvents.DISPENSER_FAIL,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }
}
