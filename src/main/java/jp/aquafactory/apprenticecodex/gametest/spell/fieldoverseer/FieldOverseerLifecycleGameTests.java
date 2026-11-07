package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerManager;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FieldOverseerLifecycleGameTests {
    private FieldOverseerLifecycleGameTests() {
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerUsesDurationBoundPersistencePolicy(GameTestHelper helper) {
        var fixture = DeployedSpellTestFixture.of(helper);
        var owner = fixture.player(new BlockPos(0, 2, -2), "field_save_reload");
        var anchor = new BlockPos(0, 2, 0);
        fixture.setBlock(anchor.below(), Blocks.STONE);
        var staff = FieldOverseerTestSupport.createForDuration(helper, owner, 1, anchor, 4);
        // Iron'sはrecast残時間を複数tick単位で減らすため、4tickのentity期限より先に切れうる。
        // このケースは絶対期限とその終了処理を確認し、recast自身の時間切れとは分ける。
        var recasts = MagicData.getPlayerMagicData(owner).getPlayerRecasts();
        var spell = SpellRegistry.FIELD_OVERSEER.get();
        var originalRecast = recasts.getRecastInstance(spell.getSpellId());
        recasts.forceAddRecast(new RecastInstance(spell.getSpellId(), 1, originalRecast.getTotalRecasts(),
                200, CastSource.SPELLBOOK, originalRecast.getCastData()));
        helper.assertTrue(staff.shouldBeSaved() && !staff.removeWhenFarAway(Double.MAX_VALUE),
                "Duration-bound staff must persist without distance despawning");
        var saved = new CompoundTag();
        staff.saveWithoutId(saved);
        long expiration = helper.getLevel().getGameTime() + 4;
        helper.assertTrue(saved.contains("ExpirationGameTime") && saved.getLong("ExpirationGameTime") == expiration,
                "Saved staff must retain the original absolute expiration");
        staff.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        var restored = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), helper.getLevel());
        fixture.track(restored);
        helper.runAfterDelay(2, () -> {
            restored.load(saved);
            helper.assertTrue(helper.getLevel().getGameTime() < expiration,
                    "Reload must happen before the deadline: now=" + helper.getLevel().getGameTime() + ", expires=" + expiration);
            fixture.add(restored);
            var reloadedTag = new CompoundTag();
            restored.saveWithoutId(reloadedTag);
            helper.assertTrue(restored.getUUID().equals(staff.getUUID())
                            && reloadedTag.getLong("ExpirationGameTime") == expiration,
                    "Reload must retain staff identity and must not restart its lifetime");
        });
        helper.runAfterDelay(3, () -> {
            helper.assertFalse(restored.isRemoved(), "Reloaded staff removed before the original deadline: now="
                    + helper.getLevel().getGameTime() + ", expires=" + expiration
                    + ", ownerVisible=" + (helper.getLevel().getEntity(owner.getUUID()) == owner)
                    + ", recast=" + MagicData.getPlayerMagicData(owner).getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()));
            fixture.assertVisible(restored);
            helper.assertTrue(!restored.isRemoved() && MagicData.getPlayerMagicData(owner).getPlayerRecasts()
                            .hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "Reloaded staff must remain active before the original expiration");
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(restored.isRemoved() && !MagicData.getPlayerMagicData(owner).getPlayerRecasts()
                            .hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "Reloaded staff and recast must expire at the original deadline");
            helper.succeed();
        });
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerRecastRemovesPlacedStaff(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, -2), "field_overseer_recast_remove_test");
        var anchorPos = new BlockPos(0, 2, 0);
        DeployedSpellTestFixture.of(helper).setBlock(anchorPos.below(), Blocks.STONE);
        var staff = FieldOverseerTestSupport.createForDuration(
                helper, owner, 1, anchorPos, ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer should create recast data after placement");

        ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get())
                .castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);

        helper.assertTrue(staff.isRemoved(),
                "FieldOverseer manual recast should remove the placed staff");
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer manual recast should consume the active recast");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerTimeoutRemovesPlacedStaff(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, -2), "field_overseer_timeout_test");
        var anchorPos = new BlockPos(0, 2, 0);
        DeployedSpellTestFixture.of(helper).setBlock(anchorPos.below(), Blocks.STONE);
        var staff = FieldOverseerTestSupport.createForDuration(helper, owner, 1, anchorPos, 1);
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer should have active recast data before timeout");

        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isRemoved(), "FieldOverseer timeout should remove the placed staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer timeout should consume the active recast");
        });
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerCancelledWhileUnloadedDoesNotReturn(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, -2), "field_overseer_unloaded_cancel_test");
        var anchorPos = new BlockPos(0, 2, 0);
        DeployedSpellTestFixture.of(helper).setBlock(anchorPos.below(), Blocks.STONE);
        var staff = FieldOverseerTestSupport.createForDuration(helper, owner, 1, anchorPos, 200);
        var savedStaff = new CompoundTag();
        staff.saveWithoutId(savedStaff);
        staff.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

        FieldOverseerManager.cancel(owner);
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer cancel should remove recast data while the staff is unloaded");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer cancel should apply the normal cooldown");

        var restored = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), helper.getLevel());
        restored.load(savedStaff);
        DeployedSpellTestFixture.of(helper).add(restored);
        helper.succeedWhen(() -> helper.assertTrue(restored.isRemoved(),
                "FieldOverseer loaded after cancellation should discard itself before acting"));
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_lifecycle")
    public static void fieldOverseerDestructionEndsMatchingRecast(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, -2), "field_overseer_destroy_test");
        var anchorPos = new BlockPos(0, 2, 0);
        DeployedSpellTestFixture.of(helper).setBlock(anchorPos.below(), Blocks.STONE);
        var staff = FieldOverseerTestSupport.createForDuration(helper, owner, 1, anchorPos, 200);
        var magicData = MagicData.getPlayerMagicData(owner);

        staff.remove(Entity.RemovalReason.KILLED);

        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "Destroying FieldOverseer should remove its matching recast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.FIELD_OVERSEER.get()),
                "Destroying FieldOverseer should apply the normal cooldown");
        helper.succeed();
    }
}
