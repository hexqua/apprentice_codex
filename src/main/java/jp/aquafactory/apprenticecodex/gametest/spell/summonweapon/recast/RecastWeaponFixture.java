package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.recast;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.support.SummonWeaponTestSupport;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.UUID;

final class RecastWeaponFixture implements AutoCloseable {
    final GameTestHelper helper;
    final AbstractSummonWeaponRecastSpell<?> spell;
    final FakePlayer owner;
    final MagicData data;
    final Zombie target;
    final TargetingTestFixture scope;

    RecastWeaponFixture(GameTestHelper helper, AbstractSummonWeaponRecastSpell<?> spell) {
        this.helper = helper;
        this.spell = spell;
        scope = TargetingTestFixture.of(helper);
        var origin = helper.absoluteVec(new Vec3(32, 6, 32));
        owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "summon_recast_test"));
        owner.setPos(origin);
        owner.setYRot(-90);
        owner.setXRot(0);
        owner.setNoGravity(true);
        // CommenceFireはgetViewVectorで頭の向きを参照する。FakePlayerには通常の更新tickがない。
        owner.setYHeadRot(-90);
        owner.yHeadRotO = -90;
        owner.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(1000);
        SummonWeaponTestSupport.registerPlayer(helper, owner);
        data = MagicData.getPlayerMagicData(owner);
        // FakePlayerは通常ログインを通らないため同期状態を初期化する。
        data.setSyncedData(new SyncedSpellData(owner));
        data.setMana(1000);
        target = EntityType.ZOMBIE.create(helper.getLevel());
        target.setPos(origin.add(8, 0, 0));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        scope.add(target);
    }

    void assertRejectedRecastPreservesState() {
        var recast = data.getPlayerRecasts().getRecastInstance(spell.getSpellId());
        int count = recast.getRemainingRecasts();
        int remainingTicks = recast.getTicksRemaining();
        float mana = data.getMana();
        float health = target.getHealth();
        helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, owner, data),
                "Quick Arms must reject a recast while preparing or recoiling");
        helper.assertTrue(recast.getRemainingRecasts() == count && recast.getTicksRemaining() == remainingTicks
                        && data.getMana() == mana && target.getHealth() == health && !data.isCasting(),
                "A rejected recast must preserve shots, timeout, mana, health, and casting state");
    }

    void begin() {
        helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, owner, data), "Scene cast must pass preconditions: " + diagnostics());
        data.initiateCast(spell, 1, spell.getEffectiveCastTime(1, owner), CastSource.SPELLBOOK, "mainhand");
        spell.onServerPreCast(helper.getLevel(), 1, owner, data);
    }

    void finish() {
        target.invulnerableTime = 0;
        spell.castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);
        spell.onServerCastComplete(helper.getLevel(), 1, owner, data, false);
    }

    List<SummonWeaponEntity> weapons() {
        var weapons = scope.entities(SummonWeaponEntity.class).stream().filter(e -> e.getOwner() == owner).toList();
        weapons.forEach(scope::assertVisible);
        return weapons;
    }

    SummonWeaponEntity weapon() {
        var weapons = weapons();
        helper.assertTrue(weapons.size() == 1, "Expected exactly one owned recast weapon: " + diagnostics());
        return weapons.getFirst();
    }

    void weaponTicks(int ticks) {
        var weapon = weapon();
        for (int i = 0; i < ticks; i++) {
            scope.run(() -> weapon.tickOnServer(helper.getLevel()));
            if (!weapon.isRemoved()) scope.assertVisible(weapon);
        }
    }

    String diagnostics() {
        return "spell=" + spell.getSpellId() + ", mana=" + data.getMana() + ", casting=" + data.isCasting()
                + ", targetHealth=" + target.getHealth() + ", " + scope.diagnostics(target);
    }

    @Override
    public void close() {
        scope.close();
    }


    static final String TEMPLATE = "gametest/summon_recast";
    static final String BATCH = "apprenticecodex.summon_weapon_recast";
}