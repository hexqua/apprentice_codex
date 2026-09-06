package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScytheClientConfigState;
import jp.aquafactory.apprenticecodex.network.packet.SyncSpellReaperScytheConfigPacket;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheConfigSyncGameTests {
    @GameTest(template = "gametest/basic_floor")
    public static void syncedValuesReplaceClientCostsWithoutChangingServerConfig(GameTestHelper helper) {
        var serverValues = ApprenticeCodexServerConfig.spellReaperScytheConfig();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            // 初回同期と再同期で全項目を入れ替え、同一JVMでもserver設定へ逆流しないことを確認する。
            for (int offset : new int[]{0, 100}) {
                var values = new SpellReaperScytheServerConfig.Values(
                        311 + offset, 17, 23, 127 + offset, 7, 163 + offset, 19, 419 + offset, 29);
                var packet = new SyncSpellReaperScytheConfigPacket(values);
                SyncSpellReaperScytheConfigPacket.STREAM_CODEC.encode(buffer, packet);
                var decoded = SyncSpellReaperScytheConfigPacket.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.equals(packet), "Config packet must preserve every scythe setting");
                SpellReaperScytheClientConfigState.set(decoded.values());
                var client = SpellReaperScytheClientConfigState.values();
                helper.assertTrue(client.ascensionManaCost(2) == 294 + offset
                                && client.reboundManaCost(2) == 144 + offset
                                && client.maelstromManaCost(2) == 390 + offset
                                && client.throwManaCost() == 127 + offset
                                && client.throwManaPerTick() == 7 && client.ascensionCooldownTicks() == 23,
                        "Client costs and cooldown must use the latest synchronized settings");
                helper.assertTrue(ApprenticeCodexServerConfig.spellReaperScytheConfig().equals(serverValues),
                        "Client synchronization must not modify authoritative server settings");
            }
            SpellReaperScytheClientConfigState.reset();
            helper.assertTrue(SpellReaperScytheClientConfigState.values().equals(SpellReaperScytheServerConfig.Values.DEFAULT),
                    "Disconnect reset must discard the previous server settings");
        } finally {
            buffer.release();
            SpellReaperScytheClientConfigState.reset();
        }
        helper.succeed();
    }
}
