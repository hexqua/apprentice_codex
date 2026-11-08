package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.sync;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.network.packet.SyncEchoProfileSpellIdsPacket;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoProfileSyncGameTests {
    private EchoProfileSyncGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.echo_tooltip_sync")
    public static void echoProfileSyncReplacesAndClearsClientIds(GameTestHelper helper) {
        var first = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var second = SpellRegistry.FIREBOLT_SPELL.get();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new SyncEchoProfileSpellIdsPacket(List.of(first.getSpellResource(), second.getSpellResource()));
            SyncEchoProfileSpellIdsPacket.encode(packet, buffer);
            var decoded = SyncEchoProfileSpellIdsPacket.decode(buffer);
            helper.assertTrue(packet.equals(decoded), "Echo profile IDs must survive packet encoding");
            MulticastEchoStaffAttackProfileManager.applyClientSyncedProfileSpellIds(decoded.profileSpellIds());
            helper.assertTrue(MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(first)
                    && MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(second), "Received IDs must be available");
            MulticastEchoStaffAttackProfileManager.applyClientSyncedProfileSpellIds(List.of(second.getSpellResource()));
            helper.assertTrue(!MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(first),
                    "Reload must remove previously supported IDs");
            MulticastEchoStaffAttackProfileManager.clearClientSyncedProfileSpellIds();
            helper.assertTrue(!MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(second),
                    "Logout must clear supported IDs");
        } finally {
            buffer.release();
            MulticastEchoStaffAttackProfileManager.clearClientSyncedProfileSpellIds();
        }
        helper.succeed();
    }
}
