package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
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

        var castType = spell.getCastType();
        var supportedCastType = castType == CastType.INSTANT || castType == CastType.LONG || castType == CastType.CONTINUOUS;
        if (!supportedCastType || !ChargedTwinBladeStaffSpellProfileManager.isSupportedByStaffOrFallback(spell)) {
            return Optional.of(spell.getDisplayName(player));
        }
        return Optional.empty();
    }
}
