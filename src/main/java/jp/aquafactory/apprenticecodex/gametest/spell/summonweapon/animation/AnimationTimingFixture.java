package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.support.SummonWeaponTestSupport;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class AnimationTimingFixture implements AutoCloseable {
    static final String TEMPLATE = "gametest/summon_animation";
    static final List<ReductionCase> REDUCTIONS = List.of(new ReductionCase("quarter", 0.25), new ReductionCase("half", 0.5));
    final TargetingTestFixture scope;
    final FakePlayer player;
    final MagicData data;
    final int baseCastTime;
    final int effectiveCastTime;

    AnimationTimingFixture(GameTestHelper helper, AbstractSpell spell, ReductionCase testCase) {
        scope = TargetingTestFixture.of(helper);
        player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "summon_animation_" + testCase.name()));
        player.setPos(helper.absoluteVec(new Vec3(16.5, 6, 16.5)));
        player.setNoGravity(true);
        SummonWeaponTestSupport.registerPlayer(helper, player);
        var attribute = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
        Objects.requireNonNull(attribute, "Missing cast-time reduction attribute").addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "summon_weapon_animation_test"), testCase.amount(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        baseCastTime = spell.getCastTime(1);
        effectiveCastTime = spell.getEffectiveCastTime(1, player);
        helper.assertTrue(effectiveCastTime > 0 && effectiveCastTime < baseCastTime,
                "Fixture must shorten casting: case=" + testCase.name() + ", base=" + baseCastTime + ", effective=" + effectiveCastTime);
        data = MagicData.getPlayerMagicData(player);
        data.setSyncedData(new SyncedSpellData(player));
        // private fieldの書換えではなく、詠唱開始APIからLONGの時刻・詠唱元を準備する。
        data.initiateCast(spell, 1, effectiveCastTime, CastSource.SPELLBOOK, "mainhand");
    }

    static void assertClose(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Float.isFinite(actual) && Math.abs(actual - expected) < 1.0e-4F,
                message + ": expected=" + expected + ", actual=" + actual);
    }

    static void assertReleaseIndependent(GameTestHelper helper, List<Float> standby, List<Float> release) {
        helper.assertTrue(Math.abs(standby.get(0) - standby.get(1)) > 1.0e-4F,
                "The two reduction cases must exercise different standby speeds: " + standby);
        helper.assertTrue(release.stream().allMatch(speed -> Float.isFinite(speed) && speed > 0),
                "Completed attacks must retain a finite positive animation speed: " + release);
        // 演出の既定値は固定せず、待機時の短縮倍率が抜刀へ残らないことを比較する。
        assertClose(helper, release.get(0), release.get(1), "Quickdraw speed must be independent of cast-time reduction");
    }

    record ReductionCase(String name, double amount) {
    }

    @Override
    public void close() {
        scope.close();
    }
}
