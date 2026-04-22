package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.player.ClientPlayerEvents;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletTooltipHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ClientPlayerEvents.class, remap = false)
public abstract class ClientPlayerEventsAutocastAmuletTooltipMixin {

    @Inject(method = "handleImbuedSpellTooltip", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$replaceAutocastAmuletTooltip(ItemStack stack, LocalPlayer player, List<Component> lines,
                                                                     boolean advanced, CallbackInfo ci) {
        if (!(stack.getItem() instanceof AutocastAmulet)) {
            return;
        }

        AutocastAmuletTooltipHelper.appendImbuedSpellTooltip(stack, player, lines, advanced);
        ci.cancel();
    }
}
