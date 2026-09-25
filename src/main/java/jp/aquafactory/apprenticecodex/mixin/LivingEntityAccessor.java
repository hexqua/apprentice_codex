package jp.aquafactory.apprenticecodex.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("checkAutoSpinAttack")
    void apprenticecodex$checkAutoSpinAttack(AABB before, AABB after);

    @Invoker("getJumpPower")
    float apprenticecodex$getJumpPower();

    @Accessor("useItem")
    void apprenticecodex$setUseItem(ItemStack itemStack);

    @Accessor("useItemRemaining")
    void apprenticecodex$setUseItemRemaining(int value);

    @Accessor("attackStrengthTicker")
    void apprenticecodex$setAttackStrengthTicker(int value);

    @Invoker("setLivingEntityFlag")
    void apprenticecodex$setLivingEntityFlag(int key, boolean value);
}
