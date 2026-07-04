package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class AutocastAmuletClientTooltip {
    private AutocastAmuletClientTooltip() {
    }

    static int getRemainingCooldownSeconds(String spellId) {
        var cooldown = ClientMagicData.getCooldowns().getSpellCooldowns().get(spellId);
        if (cooldown == null) {
            return 0;
        }
        return AutocastAmuletNotificationController.toDisplayCooldownSeconds(cooldown.getCooldownRemaining());
    }
}
