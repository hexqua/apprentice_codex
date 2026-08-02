package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.CastDataNetworkSnapshot;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CastDataNetworkSnapshotGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private CastDataNetworkSnapshotGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void allOwnEmptyCastDataCanBeSnapshotted(GameTestHelper helper) {
        var serializableCount = 0;
        for (var spellEntry : SpellRegistry.SPELLS.getEntries()) {
            var spell = spellEntry.get();
            var source = spell.getEmptyCastData();
            if (source == null) {
                continue;
            }

            serializableCount++;
            var snapshot = CastDataNetworkSnapshot.snapshotForAsyncPacketEncoding(spell, source);
            helper.assertTrue(snapshot instanceof ICastDataSerializable,
                    "Own serializable cast data should produce a serializable snapshot: " + spell.getSpellId());
            helper.assertTrue(snapshot != source,
                    "Own serializable cast data should be detached from its source: " + spell.getSpellId());
            assertWireRoundTrip(helper, spell, (ICastDataSerializable) snapshot);
        }

        helper.assertTrue(serializableCount > 0, "Expected at least one own serializable cast data implementation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void populatedCastDataSnapshotsSurviveSourceReset(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var weaponTag = new CompoundTag();
        weaponTag.putUUID("Entity", UUID.fromString("5c7415e1-7f42-4c65-b10f-03ca2485f953"));
        assertSnapshotSurvivesReset(helper, SpellRegistry.DUAL_ACROBAT.get(), weaponTag, provider);

        var mageLightTag = new CompoundTag();
        mageLightTag.putInt("PositionX", 12);
        mageLightTag.putInt("PositionY", 34);
        mageLightTag.putInt("PositionZ", -56);
        assertSnapshotSurvivesReset(helper, SpellRegistry.MAGE_LIGHT.get(), mageLightTag, provider);

        var tamersPocketTag = new CompoundTag();
        tamersPocketTag.putString("Mode", "WITHDRAW_AREA");
        var petList = new ListTag();
        var petTag = new CompoundTag();
        petTag.putUUID("Uuid", UUID.fromString("64f3eff3-127d-4f51-b4e3-6d14d9a7f33e"));
        petList.add(petTag);
        tamersPocketTag.put("LockedPetUuids", petList);
        var positionList = new ListTag();
        var positionTag = new CompoundTag();
        var position = new BlockPos(7, 8, 9);
        positionTag.putInt("X", position.getX());
        positionTag.putInt("Y", position.getY());
        positionTag.putInt("Z", position.getZ());
        positionList.add(positionTag);
        tamersPocketTag.put("LockedDeployPositions", positionList);
        assertSnapshotSurvivesReset(helper, SpellRegistry.TAMERS_POCKET.get(), tamersPocketTag, provider);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void snapshotFailureFallsBackAndExternalDataIsUntouched(GameTestHelper helper) {
        var ownSpell = new SnapshotTestSpell(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "snapshot_failure_test")
        );
        var failingSource = new SnapshotTestCastData(true, 42);
        var fallback = CastDataNetworkSnapshot.snapshotForAsyncPacketEncoding(ownSpell, failingSource);
        helper.assertTrue(fallback instanceof SnapshotTestCastData,
                "Snapshot failure should fall back to the same empty cast data type");
        helper.assertTrue(fallback != failingSource,
                "Snapshot failure should not return the mutable source cast data");
        helper.assertTrue(((SnapshotTestCastData) fallback).value == 0,
                "Snapshot failure should return an empty cast data instance");

        var externalSpell = new SnapshotTestSpell(
                ResourceLocation.fromNamespaceAndPath("external_test", "snapshot_skip_test")
        );
        var externalSource = new SnapshotTestCastData(false, 17);
        var externalResult = CastDataNetworkSnapshot.snapshotForAsyncPacketEncoding(externalSpell, externalSource);
        helper.assertTrue(externalResult == externalSource,
                "External cast data should remain untouched by Apprentice's Codex protection");
        helper.succeed();
    }

    private static void assertSnapshotSurvivesReset(
            GameTestHelper helper,
            AbstractSpell spell,
            CompoundTag sourceTag,
            HolderLookup.Provider provider
    ) {
        var source = spell.getEmptyCastData();
        helper.assertTrue(source != null, "Expected serializable cast data for " + spell.getSpellId());
        source.deserializeNBT(provider, sourceTag);
        var expected = source.serializeNBT(provider).copy();

        var snapshot = CastDataNetworkSnapshot.snapshotForAsyncPacketEncoding(spell, source);
        helper.assertTrue(snapshot instanceof ICastDataSerializable,
                "Expected serializable cast data snapshot for " + spell.getSpellId());
        helper.assertTrue(snapshot != source, "Cast data snapshot should be detached for " + spell.getSpellId());

        source.reset();
        var actual = ((ICastDataSerializable) snapshot).serializeNBT(provider);
        helper.assertTrue(expected.equals(actual),
                "Cast data snapshot changed after source reset for " + spell.getSpellId()
                        + ": expected=" + expected + ", actual=" + actual);
    }

    private static void assertWireRoundTrip(
            GameTestHelper helper,
            AbstractSpell spell,
            ICastDataSerializable source
    ) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.writeToBuffer(buffer);
            var decoded = spell.getEmptyCastData();
            helper.assertTrue(decoded != null, "Expected empty cast data for " + spell.getSpellId());
            decoded.readFromBuffer(buffer);
            helper.assertTrue(!buffer.isReadable(),
                    "Cast data decoder should consume the complete payload for " + spell.getSpellId());
        } finally {
            buffer.release();
        }
    }

    private static final class SnapshotTestSpell extends AbstractSpell {
        private final ResourceLocation spellId;
        private final DefaultConfig config = new DefaultConfig();

        private SnapshotTestSpell(ResourceLocation spellId) {
            this.spellId = spellId;
        }

        @Override
        public ResourceLocation getSpellResource() {
            return spellId;
        }

        @Override
        public DefaultConfig getDefaultConfig() {
            return config;
        }

        @Override
        public CastType getCastType() {
            return CastType.INSTANT;
        }

        @Override
        public ICastDataSerializable getEmptyCastData() {
            return new SnapshotTestCastData(false, 0);
        }
    }

    private static final class SnapshotTestCastData implements ICastDataSerializable {
        private boolean failOnWrite;
        private int value;

        private SnapshotTestCastData(boolean failOnWrite, int value) {
            this.failOnWrite = failOnWrite;
            this.value = value;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            if (failOnWrite) {
                throw new IllegalStateException("Intentional snapshot failure");
            }
            buffer.writeInt(value);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            value = buffer.readInt();
        }

        @Override
        public void reset() {
            failOnWrite = false;
            value = 0;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            tag.putBoolean("FailOnWrite", failOnWrite);
            tag.putInt("Value", value);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            failOnWrite = nbt.getBoolean("FailOnWrite");
            value = nbt.getInt("Value");
        }
    }
}
