package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptide;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptideAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChargedStaffRiptideMixin implements ChargedTwinBladeStaffRiptideAccess {
    @Shadow protected int autoSpinAttackTicks;
    @Shadow protected float autoSpinAttackDmg;
    @Shadow protected ItemStack autoSpinAttackItemStack;
    @Shadow protected abstract void setLivingEntityFlag(int flag, boolean value);
    @Unique private ChargedTwinBladeStaffRiptide.State apprenticecodex$staffRiptide;

    @Override
    public @Nullable ChargedTwinBladeStaffRiptide.State apprenticecodex$getStaffRiptide() {
        return apprenticecodex$staffRiptide;
    }

    @Override
    public void apprenticecodex$setStaffRiptide(@Nullable ChargedTwinBladeStaffRiptide.State state) {
        apprenticecodex$staffRiptide = state;
    }

    @Override
    public void apprenticecodex$updateStaffSpin(int ticks) {
        autoSpinAttackTicks = ticks;
        setLivingEntityFlag(4, ticks > 0);
        if (ticks <= 0) {
            autoSpinAttackDmg = 0;
            autoSpinAttackItemStack = null;
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void apprenticecodex$maintainStaffRiptide(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            ChargedTwinBladeStaffRiptide.tick(player);
        }
    }

    @Inject(method = "checkAutoSpinAttack", at = @At("TAIL"))
    private void apprenticecodex$finishStaffRiptideOnCollision(AABB before, AABB after, CallbackInfo ci) {
        // 衝突後に次tickの維持処理が回転攻撃を復活させないよう、その場で終了を記録する。
        //noinspection ConstantValue
        if (autoSpinAttackTicks <= 0 && (Object) this instanceof Player player) {
            ChargedTwinBladeStaffRiptide.interrupt(player);
        }
    }
}
