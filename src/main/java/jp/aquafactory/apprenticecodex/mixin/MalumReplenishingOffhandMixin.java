package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.spellsideedge.AbstractSpellSideEdgeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.sammy.malum.compability.irons_spellbooks.IronsSpellsCompat$LoadedOnly", remap = false)
public abstract class MalumReplenishingOffhandMixin {
    @Unique
    private static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath("malum", "replenishing");

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
        // Malum 側のクールダウン計算を維持したまま、Side Edge のみオフハンドを判定対象へ差し替える。
        // 対象メソッドが将来変更された場合は require=0 でこの連携だけを無効化する。
        var mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof AbstractSpellSideEdgeItem)) {
            return mainHand;
        }

        var replenishing = ForgeRegistries.ENCHANTMENTS.getValue(MALUM_REPLENISHING);
        var offhand = player.getOffhandItem();
        if (replenishing != null && offhand.getEnchantmentLevel(replenishing) > 0) {
            return offhand;
        }
        return mainHand;
    }
}
