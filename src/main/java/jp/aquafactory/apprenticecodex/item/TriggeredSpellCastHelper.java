package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class TriggeredSpellCastHelper {
    private TriggeredSpellCastHelper() {
    }

    public static void applyLongCastDurationOverride(Player player, int spellLevel, AbstractSpell spell,
                                                     @Nullable MagicData magicData, String slotId,
                                                     @Nullable Integer overriddenLongCastTicks) {
        if (spell.getCastType() != CastType.LONG) {
            return;
        }

        if (overriddenLongCastTicks == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var resolvedMagicData = magicData != null ? magicData : MagicData.getPlayerMagicData(serverPlayer);
        if (resolvedMagicData == null) {
            return;
        }

        if (overriddenLongCastTicks <= 0) {
            completeLongCastImmediately(serverPlayer, spellLevel, spell, resolvedMagicData);
            return;
        }

        // attemptInitiateCast は魔法本来の詠唱時間で状態を作るため、アイテム側指定値へ即座に上書きし同期し直す.
        resolvedMagicData.initiateCast(spell, spellLevel, overriddenLongCastTicks, CastSource.SWORD, slotId);
        PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(
                spell.getSpellId(),
                spellLevel,
                overriddenLongCastTicks,
                CastSource.SWORD,
                slotId
        ));
    }

    private static void completeLongCastImmediately(ServerPlayer player, int spellLevel, AbstractSpell spell, MagicData magicData) {
        // LONG の完了待ちだけを飛ばし、CastType 自体は維持して downstream の挙動を崩さない.
        spell.castSpell(player.level(), spellLevel, player, magicData.getCastSource(), true);
        spell.onServerCastTick(player.level(), spellLevel, player, magicData);
        spell.onServerCastComplete(player.level(), spellLevel, player, magicData, false);
    }
}
