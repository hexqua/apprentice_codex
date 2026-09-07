package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = Utils.class, remap = false)
public abstract class QuickcastCartridgeUtilsMixin {
    // Utils の入力ロック・方向/対象同期・既存詠唱の中断を通した後に発動元だけを識別する。
    @Redirect(method = {"serverSideInitiateCast", "serverSideInitiateQuickCast"}, at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;attemptInitiateCast(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;ZLjava/lang/String;)Z"))
    private static boolean cartridgeCast(AbstractSpell spell, ItemStack stack, int spellLevel, Level level,
                                         Player player, CastSource source, boolean cooldown, String slot) {
        if (QuickcastCartridgeCasting.SLOT.equals(slot) && player instanceof ServerPlayer serverPlayer) {
            return QuickcastCartridgeCasting.initiate(serverPlayer);
        }
        return spell.attemptInitiateCast(stack, spellLevel, level, player, source, cooldown, slot);
    }
}
