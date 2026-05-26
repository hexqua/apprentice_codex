package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
        if (!supportedCastType || !isSupportedByRemoteOwnerOrFallback(spell)) {
            return Optional.of(spell.getDisplayName(player));
        }
        return Optional.empty();
    }

    private static boolean isSupportedByRemoteOwnerOrFallback(AbstractSpell spell) {
        return ApprenticeCodexServerConfig.chargedTwinBladeStaffUsesRemoteOwnerProfiles()
                && RemoteOwnerCastProfileManager.isSupportedByRemoteOwnerCast(
                        spell,
                        RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT
                )
                || SpellDispenserSpellProfileManager.getProfile(spell).isPresent();
    }
}
