package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurret;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;

final class AutoTurretTestSupport {
    private AutoTurretTestSupport() {
    }

    static void castAutoTurret(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos) {
        var spell = (AutoTurret) SpellRegistry.AUTO_TURRET.get();
        var castData = new AutoTurret.AutoTurretCastData();
        var absoluteAnchorPos = DeployedSpellTestFixture.of(helper).absolute(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        castData.deserializeNBT(helper.getLevel().registryAccess(), tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        DeployedSpellTestFixture.of(helper).run(() -> spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData));
    }

    static AutoTurretEntity createAutoTurretRestockTestEntity(GameTestHelper helper, FakePlayer owner, BlockPos localPos, int bullets, int manaCost) {
        var fixture = DeployedSpellTestFixture.of(helper);
        // 補給の検証では落下をノイズにせず、支持面を外す検証と準備を分ける。
        fixture.setBlock(localPos.below(), Blocks.STONE);
        return fixture.turret(owner, localPos, bullets, manaCost);
    }

    static AutoTurretEntity getSingleLivingAutoTurret(GameTestHelper helper, FakePlayer owner) {
        var fixture = DeployedSpellTestFixture.of(helper);
        var turrets = fixture.entities(AutoTurretEntity.class).stream()
                .filter(t -> t.isAlive() && owner.getUUID().equals(t.getCombatOwnerUuid())).toList();
        helper.assertTrue(turrets.size() == 1, "Expected one owned AutoTurret but found " + turrets.size());
        fixture.assertVisible(turrets.getFirst());
        return turrets.getFirst();
    }
}
