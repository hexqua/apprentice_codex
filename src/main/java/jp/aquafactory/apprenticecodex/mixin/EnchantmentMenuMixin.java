package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.armor.EnchantressEnchantingTableBonusHelper;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Unique
    private int apprenticecodex$attemptedEnchantCost;

    @Shadow
    @Final
    public int[] costs;

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void apprenticecodex$captureEnchantCost(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        apprenticecodex$attemptedEnchantCost = buttonId >= 0 && buttonId < costs.length ? costs[buttonId] : 0;
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void apprenticecodex$awardMaxEnchantAdvancement(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                || !(player instanceof ServerPlayer serverPlayer)
                || EnchantressEnchantingTableBonusHelper.isFeatureDisabled()
                || buttonId != EnchantressEnchantingTableBonusHelper.TARGET_ENCHANT_ROW
                || apprenticecodex$attemptedEnchantCost < 50) {
            return;
        }

        AdvancementTools.award(
                serverPlayer,
                AdvancementTools.ENCHANT_MAX_LEVEL,
                AdvancementTools.ENCHANT_MAX_LEVEL_CRITERION
        );
    }
}
