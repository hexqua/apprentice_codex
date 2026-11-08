package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurret;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;

final class AutoTurretTestSupport {
    private AutoTurretTestSupport() {}

    static AutoTurretEntity createAutoTurretRestockTestEntity(
            GameTestHelper helper,
            FakePlayer owner,
            BlockPos localPos,
            int initialBulletCount,
            int restockManaCost
    ) {
        var level = helper.getLevel();
        var absolutePos = helper.absolutePos(localPos);
        var center = Vec3.atBottomCenterOf(absolutePos);
        var turret = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), level);
        turret.setOwner(owner);
        turret.setAnchorPos(absolutePos);
        turret.setDamage(4.0F);
        turret.setRestockData(initialBulletCount, restockManaCost);
        turret.setTurretMaxHealth(20.0F);
        turret.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        level.addFreshEntity(turret);
        return turret;
    }

    static void castAutoTurret(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos) {
        var spell = (AutoTurret) SpellRegistry.AUTO_TURRET.get();
        var castData = new AutoTurret.AutoTurretCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    static AutoTurretEntity getSingleLivingAutoTurret(GameTestHelper helper, FakePlayer owner) {
        var turrets = new ArrayList<AutoTurretEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof AutoTurretEntity turret
                    && turret.isAlive()
                    && turret.getOwner() != null
                    && owner.getUUID().equals(turret.getOwner().getUUID())) {
                turrets.add(turret);
            }
        }
        helper.assertTrue(turrets.size() == 1, "Expected exactly one living AutoTurret but found " + turrets.size());
        return turrets.get(0);
    }
}
