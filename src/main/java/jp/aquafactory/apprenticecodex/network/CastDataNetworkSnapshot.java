package jp.aquafactory.apprenticecodex.network;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CastDataNetworkSnapshot {
    private static final Set<ExternalCastDataKey> LOGGED_EXTERNAL_CAST_DATA = ConcurrentHashMap.newKeySet();

    private CastDataNetworkSnapshot() {
    }

    public static @Nullable ICastData snapshotForAsyncPacketEncoding(
            AbstractSpell spell,
            @Nullable ICastData castData
    ) {
        if (!(castData instanceof ICastDataSerializable source)) {
            return castData;
        }

        var spellId = spell.getSpellResource();
        if (!ApprenticeCodex.MODID.equals(spellId.getNamespace())) {
            var key = new ExternalCastDataKey(spellId.toString(), castData.getClass().getName());
            if (LOGGED_EXTERNAL_CAST_DATA.add(key)) {
                ApprenticeCodex.LOGGER.info(
                        "Detected serializable cast data for external spell {} ({}); Apprentice's Codex async encoding protection was not applied.",
                        spellId,
                        castData.getClass().getName()
                );
            }
            return castData;
        }

        // NeoForge 1.21.1 は custom payload を Netty thread で遅延 encode するため、
        // Iron's Spells が詠唱完了直後に元データを reset しても影響しない独立コピーを先に作る。
        var target = createEmptyCastData(spell, castData);
        if (target == null) {
            return null;
        }

        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.writeToBuffer(buffer);
            target.readFromBuffer(buffer);
            return target;
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Failed to snapshot cast data for spell {} ({}); using empty cast data to avoid disconnecting the client.",
                    spellId,
                    castData.getClass().getName(),
                    exception
            );
            return createEmptyCastData(spell, castData);
        } finally {
            buffer.release();
        }
    }

    private static @Nullable ICastDataSerializable createEmptyCastData(AbstractSpell spell, ICastData source) {
        try {
            var emptyCastData = spell.getEmptyCastData();
            if (emptyCastData == null) {
                ApprenticeCodex.LOGGER.warn(
                        "Spell {} returned no empty cast data for {}; omitting cast data to avoid disconnecting the client.",
                        spell.getSpellResource(),
                        source.getClass().getName()
                );
            }
            return emptyCastData;
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Failed to create empty cast data for spell {} ({}); omitting cast data to avoid disconnecting the client.",
                    spell.getSpellResource(),
                    source.getClass().getName(),
                    exception
            );
            return null;
        }
    }

    private record ExternalCastDataKey(String spellId, String castDataClass) {
    }
}
