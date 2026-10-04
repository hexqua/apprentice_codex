package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.silentassassin.SilentAssassin;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SilentAssassinGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.silent_assassin";

    private SilentAssassinGameTests() {
    }

    // AIの標的設定によらず、99.5%境界と頭・胴の区別で初撃ボーナスを決める。
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void almostFullHealthBonusRequiresHeadshotAndIncludesBoundary(GameTestHelper helper) {
        var caster = createCaster(helper);
        try (var ignored = ApprenticeCodexServerConfig.useDamageMultiplierOverrideForGameTest(
                DamageMultiplierKey.SILENT_ASSASSIN, 1)) {
            var full = damageOnce(helper, caster, 1000, true, false);
            var boundary = damageOnce(helper, caster, 995, true, true);
            var below = damageOnce(helper, caster, 994.9F, true, false);
            var body = damageOnce(helper, caster, 1000, false, false);
            assertClose(helper, full, below * 2, "Full health headshot must double damage");
            assertClose(helper, boundary, full, "99.5% health must qualify even while targeting caster");
            assertClose(helper, body, 15, "Full health body shot must receive no bonus");
        } finally {
            caster.discard();
        }
        helper.succeed();
    }

    // 固定基礎威力を維持しつつ、頭倍率には通常威力とschool威力の積を反映する。
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void spellPowerScalesHeadshotsButNotBodyShots(GameTestHelper helper) {
        var caster = createCaster(helper);
        Objects.requireNonNull(caster.getAttribute(AttributeRegistry.SPELL_POWER.get())).setBaseValue(1.3);
        Objects.requireNonNull(caster.getAttribute(AttributeRegistry.EVOCATION_SPELL_POWER.get())).setBaseValue(1.5);
        try (var ignored = ApprenticeCodexServerConfig.useDamageMultiplierOverrideForGameTest(
                DamageMultiplierKey.SILENT_ASSASSIN, 1)) {
            assertClose(helper, damageOnce(helper, caster, 990, false, false), 15,
                    "Spell power must not increase body damage");
            assertClose(helper, damageOnce(helper, caster, 990, true, false), 61.8F,
                    "Level 3 specialized headshot must use a 412% multiplier");
            assertClose(helper, damageOnce(helper, caster, 1000, true, false), 123.6F,
                    "Level 3 specialized first headshot must double damage before mitigation");
        } finally {
            caster.discard();
        }
        helper.succeed();
    }

    // 生存・無敵・傷ついた対象のキル・胴撃ちキルでは、周囲の敵の認識を消さない。
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void awarenessSuppressionRequiresActualBonusKill(GameTestHelper helper) {
        var caster = createCaster(helper);
        try (var ignored = ApprenticeCodexServerConfig.useDamageMultiplierOverrideForGameTest(
                DamageMultiplierKey.SILENT_ASSASSIN, 1)) {
            assertSuppression(helper, caster, 100, 100, true, false, false);
            assertSuppression(helper, caster, 20, 20, true, true, false);
            assertSuppression(helper, caster, 20, 20, true, false, true);
            assertSuppression(helper, caster, 100, 20, true, false, false);
            assertSuppression(helper, caster, 10, 10, false, false, false);
        } finally {
            caster.discard();
        }
        helper.succeed();
    }

    private static FakePlayer createCaster(GameTestHelper helper) {
        var caster = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "silent_assassin_test"));
        caster.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        // 成功表示ブロックやtemplateの壁が射線を遮らない高さで検証する。
        caster.setPos(helper.absoluteVec(new Vec3(1, 30, 1)));
        Objects.requireNonNull(caster.getAttribute(AttributeRegistry.SPELL_POWER.get())).setBaseValue(1);
        Objects.requireNonNull(caster.getAttribute(AttributeRegistry.EVOCATION_SPELL_POWER.get())).setBaseValue(1);
        helper.getLevel().addFreshEntity(caster);
        return caster;
    }

    private static Zombie createTarget(GameTestHelper helper, Vec3 position, float maxHealth, float health) {
        var target = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()),
                "Failed to create Silent Assassin target");
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(position);
        Objects.requireNonNull(target.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(maxHealth);
        Objects.requireNonNull(target.getAttribute(Attributes.ARMOR)).setBaseValue(0);
        target.setHealth(health);
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static float damageOnce(GameTestHelper helper, FakePlayer caster, float health,
                                    boolean headshot, boolean aware) {
        var target = createTarget(helper, caster.position().add(0, 0, 5), 1000, health);
        if (aware) {
            target.setTarget(caster);
        }
        try {
            fire(helper, caster, target, headshot);
            return health - target.getHealth();
        } finally {
            target.discard();
        }
    }

    private static void assertSuppression(GameTestHelper helper, FakePlayer caster, float maxHealth, float health,
                                          boolean headshot, boolean invulnerable, boolean expected) {
        var target = createTarget(helper, caster.position().add(0, 0, 5), maxHealth, health);
        var observer = createTarget(helper, caster.position().add(3, 0, 5), 100, 100);
        target.setInvulnerable(invulnerable);
        observer.setTarget(caster);
        observer.setLastHurtByMob(caster);
        try {
            fire(helper, caster, target, headshot);
            helper.assertTrue((observer.getTarget() == null) == expected,
                    "Awareness suppression must match actual bonus kill: maxHealth=" + maxHealth
                            + " health=" + health + " headshot=" + headshot + " invulnerable=" + invulnerable);
            helper.assertTrue((observer.getLastHurtByMob() == null) == expected,
                    "Last attacker memory must only clear after a bonus kill");
            if (expected) {
                helper.assertTrue(target.isDeadOrDying(), "Successful assassination must actually kill target");
            }
        } finally {
            target.discard();
            observer.discard();
        }
    }

    private static void fire(GameTestHelper helper, FakePlayer caster, Zombie target, boolean headshot) {
        var aim = headshot ? target.getEyePosition() : target.position().add(0, 0.5, 0);
        var direction = aim.subtract(caster.getEyePosition());
        caster.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        caster.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        var result = SummonedFirearmTools.resolveAssistedAim(caster, 128, e -> e == target);
        helper.assertTrue(result.hitEntity() == target && SummonedFirearmTools.isHeadShot(result) == headshot,
                "Test aim must hit the intended body region");
        var spell = (SilentAssassin) SpellRegistry.SILENT_ASSASSIN.get();
        var magic = MagicData.getPlayerMagicData(caster);
        var weapon = spell.onCastNoWeapon(helper.getLevel(), 3, caster, magic);
        try {
            spell.onCastCompleteWithWeapon(helper.getLevel(), 3, caster, magic, false, weapon);
        } finally {
            weapon.discard();
        }
    }

    private static void assertClose(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.001F,
                message + ": actual=" + actual + " expected=" + expected);
    }
}
