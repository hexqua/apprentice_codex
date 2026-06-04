package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class ChargedTwinBladeStaffClientTooltip {
    private ChargedTwinBladeStaffClientTooltip() {
    }

    public static Optional<Component> resolveUnsupportedSpellName() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }

        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return Optional.empty();
        }

        var spell = selection.spellData.getSpell();
        if (spell == null || spell == SpellRegistry.none()) {
            return Optional.empty();
        }

        if (!RemoteOwnerCastRules.checkImbue(
                spell,
                selection.spellData.getLevel(),
                RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT
        ).isAllowed()) {
            return Optional.of(spell.getDisplayName(player));
        }
        return Optional.empty();
    }
}
