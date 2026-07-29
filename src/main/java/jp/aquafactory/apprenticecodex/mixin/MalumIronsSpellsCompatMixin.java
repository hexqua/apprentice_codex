package jp.aquafactory.apprenticecodex.mixin;

import com.sammy.malum.compability.irons_spellbooks.IronsSpellsCompat;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Malum 1.20.1-1.6.7 と Iron's 3.15.x以降 の API 差分を吸収する互換パッチ。
 */
@Mixin(value = IronsSpellsCompat.LoadedOnly.class, remap = false)
public abstract class MalumIronsSpellsCompatMixin {
    /**
     * Malum 1.6.7 は削除済み UpdateClient を呼ぶため、Iron's 現行のマナ同期パケットへ置き換える。
     * 1.21.1 へ移植する際は Iron's 側の同期 API と Malum の公式修正有無を再確認すること。
     *
     * @author Apprentice's Codex
     * @reason Iron's 3.15.x で恐らく廃止された UpdateClient 参照を現行の SyncManaPacket 送信へ置換する.
     */
    @Overwrite
    public static void generateMana(ServerPlayer player, float amount) {
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.addMana(amount);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }
}
