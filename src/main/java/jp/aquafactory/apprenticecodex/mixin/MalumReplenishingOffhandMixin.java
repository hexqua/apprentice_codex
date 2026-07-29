package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.item.spellsideedge.AbstractSpellSideEdgeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.sammy.malum.compat.irons_spellbooks.IronsSpellsCompat$LoadedOnly", remap = false)
public abstract class MalumReplenishingOffhandMixin {
    @Redirect(
            method = "triggerReplenishing",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;",
                    remap = true
            ),
            require = 0
    )
    private static ItemStack apprenticecodex$resolveReplenishingStack(ServerPlayer player) {
        // Malum のクールダウン計算は維持し、Side Edge だけは付呪済みオフハンドを判定対象へ差し替える。
        var mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof AbstractSpellSideEdgeItem)) {
            return mainHand;
        }

        var offhand = player.getOffhandItem();
        return MalumCompatibility.getEnchantmentLevel(offhand, MalumCompatibility.REPLENISHING) > 0
                ? offhand
                : mainHand;
    }
}
