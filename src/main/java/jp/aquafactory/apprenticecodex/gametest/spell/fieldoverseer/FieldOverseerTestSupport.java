package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerManager;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class FieldOverseerTestSupport {
    private FieldOverseerTestSupport() {
    }

    public static FieldOverseerStaffEntity create(
            GameTestHelper helper, FakePlayer owner, BlockPos anchorPos, float maxMana, int attackManaCost) {
        return createDurationBoundFieldOverseer(
                helper, owner, 1, anchorPos, maxMana, attackManaCost,
                ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
    }

    public static FieldOverseerStaffEntity createForDuration(
            GameTestHelper helper, FakePlayer owner, int spellLevel, BlockPos anchorPos, int duration) {
        return createDurationBoundFieldOverseer(
                helper, owner, spellLevel, DeployedSpellTestFixture.of(helper).absolute(anchorPos), 100.0F, 40, duration);
    }

    private static FieldOverseerStaffEntity createDurationBoundFieldOverseer(
            GameTestHelper helper, FakePlayer owner, int spellLevel, BlockPos anchorPos,
            float maxMana, int attackManaCost, int duration) {
        var level = helper.getLevel();
        var fixture = DeployedSpellTestFixture.of(helper);
        fixture.add(owner);
        fixture.onClose(() -> FieldOverseerManager.cancel(owner));
        var center = Vec3.atBottomCenterOf(anchorPos);
        var staff = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), level);
        staff.configure(anchorPos, 4.0F, 24.0D, attackManaCost, maxMana, 20);
        staff.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        var castData = new FieldOverseer.FieldOverseerCastData();
        FieldOverseerManager.initialize(owner, staff, anchorPos, duration, castData);
        fixture.add(staff);
        var spell = (FieldOverseer) SpellRegistry.FIELD_OVERSEER.get();
        var magicData = MagicData.getPlayerMagicData(owner);
        magicData.getPlayerRecasts().addRecast(new RecastInstance(
                spell.getSpellId(), spellLevel, spell.getRecastCount(spellLevel, owner),
                duration, CastSource.SPELLBOOK, castData), magicData);
        helper.assertFalse(SummonManager.getSummons(owner).contains(staff.getUUID()),
                "FieldOverseer should not register itself with Iron's SummonManager");
        return staff;
    }
}
