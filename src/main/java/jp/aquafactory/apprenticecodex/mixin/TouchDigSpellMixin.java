package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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
}
