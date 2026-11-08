package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast.RecastSessionGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RecastAttackGameTests {
    private RecastAttackGameTests() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void finalRifleShotAndQuickArmsStillWork(GameTestHelper h) {
        for (var spell : List.of(SpellRegistry.COMMENCE_FIRE.get(), SpellRegistry.QUICK_ARMS.get())) {
            try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) spell)) {
                s.begin(); s.finish();
                var weapon = s.weapon();
                s.weaponTicks(20);
                if (spell == SpellRegistry.QUICK_ARMS.get()) {
                    h.assertTrue(s.target.getHealth() < 200, "Quick Arms initial delayed shot must remain functional");
                }
                float mana = s.data.getMana();
                int shots = s.data.getPlayerRecasts().getRemainingRecastsForSpell(spell);
                for (int i = 0; i < shots; i++) {
                    float before = s.target.getHealth();
                    s.begin(); s.finish();
                    h.assertTrue(s.target.getHealth() < before, "Every completed recast, including the last, must attack");
                    if (i < shots - 1) s.weaponTicks(20);
                }
                h.assertTrue(weapon.isRemoved() && s.data.getMana() == mana,
                        "Last normal recast must release the weapon without additional mana");
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void quickArmsInitialShotAndRecastRespectFiveTickInterval(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.QUICK_ARMS.get())) {
            s.begin(); s.finish();
            var weapon = (QuickArmsHandgunEntity) s.weapon();
            s.weaponTicks(4);
            h.assertTrue(s.target.getHealth() == 200 && !weapon.canFire(),
                    "Quick Arms must wait until the fifth tick before its initial shot");
            s.assertRejectedRecastPreservesState();
            s.weaponTicks(1);
            h.assertTrue(s.target.getHealth() < 200 && weapon.canFire() && weapon.duringRecoil(),
                    "The fifth tick must fire the initial shot and start recoil");
            for (int shot = 0; shot < 2; shot++) {
                // 拒否された連打で反動やリキャスト期限が延びないことも確認する。
                for (int tick = 0; tick < 5; tick++) {
                    s.assertRejectedRecastPreservesState();
                    s.assertRejectedRecastPreservesState();
                    s.weaponTicks(1);
                }
                h.assertFalse(weapon.duringRecoil(), "Recoil must end exactly five ticks after firing");
                float before = s.target.getHealth();
                s.begin(); s.finish();
                h.assertTrue(s.target.getHealth() < before && weapon.duringRecoil(),
                        "A recast at the five-tick boundary must attack and restart recoil");
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void quickArmsMissStillStartsRecoil(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.QUICK_ARMS.get())) {
            s.owner.setYRot(90); s.owner.setYHeadRot(90); s.owner.yHeadRotO = 90;
            s.begin(); s.finish();
            s.weaponTicks(5);
            h.assertTrue(s.target.getHealth() == 200, "The initial shot must miss the target behind the caster");
            s.assertRejectedRecastPreservesState();
            s.weaponTicks(5);
            s.begin(); s.finish();
            h.assertTrue(s.target.getHealth() == 200, "The recast shot must also miss");
            for (int tick = 0; tick < 5; tick++) {
                s.assertRejectedRecastPreservesState();
                s.weaponTicks(1);
            }
            h.assertTrue(s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data),
                    "A missed shot must allow the next recast after five ticks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void quickArmsRecoilSurvivesNbtAndAcceptsLegacyData(GameTestHelper h) {
        try (var s = new Scene(h, (AbstractSummonWeaponRecastSpell<?>) SpellRegistry.QUICK_ARMS.get())) {
            s.begin(); s.finish();
            var weapon = (QuickArmsHandgunEntity) s.weapon();
            s.weaponTicks(8);
            var tag = weapon.saveWithoutId(new CompoundTag());
            h.assertTrue(tag.getInt("RecoilTick") == 2, "NBT must preserve the remaining recoil ticks");
            s.weaponTicks(2);
            h.assertFalse(weapon.duringRecoil(), "Recoil must expire before restoring the saved state");
            weapon.load(tag);
            // 召喚武器はNBT読込時に所有者を消す仕様なので、反動の検証用に再設定する。
            weapon.setOwner(s.owner);
            s.assertRejectedRecastPreservesState();
            s.weaponTicks(1);
            s.assertRejectedRecastPreservesState();
            s.weaponTicks(1);
            h.assertFalse(weapon.duringRecoil(), "Restored recoil must retain its remaining duration");
            tag.remove("RecoilTick");
            weapon.load(tag);
            weapon.setOwner(s.owner);
            h.assertFalse(weapon.duringRecoil(), "Legacy weapon NBT must default to no recoil");
            h.assertTrue(s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data),
                    "Legacy weapon NBT must remain eligible for recasting");
        }
        h.succeed();
    }
}
