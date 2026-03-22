package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemNameMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$overrideSchoolAffinityPotionName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (!((Object) this instanceof Item item)) {
            return;
        }
        if (!(item instanceof PotionItem) && !(item instanceof ArrowItem)) {
            return;
        }

        var potion = PotionUtils.getPotion(stack);
        if (!(potion instanceof SchoolAffinityPotion schoolAffinityPotion)) {
            return;
        }

        cir.setReturnValue(schoolAffinityPotion.getItemDisplayName(item));
    }
}
