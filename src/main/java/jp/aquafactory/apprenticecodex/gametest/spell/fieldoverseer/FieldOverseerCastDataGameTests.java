package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FieldOverseerCastDataGameTests {
    private FieldOverseerCastDataGameTests() {
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.field_overseer_isolated")
    public static void fieldOverseerCastDataRoundTripsPlacementAndIdentity(GameTestHelper helper) {
        var source = new FieldOverseer.FieldOverseerCastData();
        var expectedPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var expectedStaffUuid = UUID.randomUUID();
        var expectedDimension = helper.getLevel().dimension().location();
        var sourceTag = new CompoundTag();
        sourceTag.putLong("Position", expectedPosition.asLong());
        sourceTag.putUUID("Staff", expectedStaffUuid);
        sourceTag.putString("Dimension", expectedDimension.toString());
        source.deserializeNBT(helper.getLevel().registryAccess(), sourceTag);

        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.writeToBuffer(buffer);

            var restored = new FieldOverseer.FieldOverseerCastData();
            restored.readFromBuffer(buffer);
            var restoredTag = restored.serializeNBT(helper.getLevel().registryAccess());
            helper.assertTrue(restoredTag.getLong("Position") == expectedPosition.asLong(),
                    "FieldOverseer cast data should preserve placement through network serialization");
            helper.assertTrue(restoredTag.getUUID("Staff").equals(expectedStaffUuid),
                    "FieldOverseer cast data should preserve the managed staff UUID");
            helper.assertTrue(restoredTag.getString("Dimension").equals(expectedDimension.toString()),
                    "FieldOverseer cast data should preserve the managed dimension");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
