package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ChargecastCatalystbookClientTooltip {
    private ChargecastCatalystbookClientTooltip() {
    }

    public static ChargecastCatalystbook.TooltipValues resolve(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        var config = ChargecastCatalystbookClientConfigState.values();
        if (player == null) {
            return new ChargecastCatalystbook.TooltipValues(
                    config.castTimeTicks(),
                    config.spellPowerMultiplier()
            );
        }
        return new ChargecastCatalystbook.TooltipValues(
                ChargecastCatalystbook.resolveCastDurationTicks(player, stack, config),
                ChargecastCatalystbook.resolveFinalSpellPowerMultiplier(player, stack, config)
        );
    }
}
