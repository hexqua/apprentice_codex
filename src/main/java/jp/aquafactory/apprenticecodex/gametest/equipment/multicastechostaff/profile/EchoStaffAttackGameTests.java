package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.profile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffAttackGameTests {
    private EchoStaffAttackGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = registeredPlayer(helper, "echo_direct_damage");
            var normalTarget = damageTarget(helper, player, new Vec3(2, 0, 0));
            var repeatedTarget = damageTarget(helper, player, new Vec3(2, 0, 3));
            var spell = SpellRegistry.SHOCK.get();
            var source = CombatTools.getDamageSource(helper.getLevel(), player, DamageTypes.SHOCK);
            var profile = new MulticastEchoStaffAttackProfile(0.25D, true, false, true, 0, 1);
            try (var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(true, 1.0D);
                 var profiles = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(spell.getSpellResource(), profile))) {
                var beforeNormal = normalTarget.getHealth();
                helper.assertTrue(CombatTools.applyDamage(normalTarget, 8, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Normal direct attack must deal damage");
                var beforeRepeat = repeatedTarget.getHealth();
                repeatedTarget.invulnerableTime = 20;
                MulticastEchoStaffAttackHandler.runRepeatedCast(player, spell, () ->
                        helper.assertTrue(CombatTools.applyDamage(repeatedTarget, 8, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK),
                                "Repeated direct attack must bypass the existing i-frame"));
                assertDamageRatio(helper, beforeNormal - normalTarget.getHealth(), beforeRepeat - repeatedTarget.getHealth(), 0.25F);
                helper.assertTrue(repeatedTarget.invulnerableTime == 1,
                        "Repeated direct attack must set the profile's post-hit i-frame: " + repeatedTarget.invulnerableTime);
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = registeredPlayer(helper, "echo_projectile_damage");
            var normalTarget = damageTarget(helper, player, new Vec3(2, 0, 0));
            var repeatedTarget = damageTarget(helper, player, new Vec3(2, 0, 3));
            var spell = SpellRegistry.MANA_SLASH.get();
            var scope = TargetingTestFixture.of(helper);
            var normalProjectile = projectile(helper, player);
            var repeatedProjectile = projectile(helper, player);
            var profile = new MulticastEchoStaffAttackProfile(0.5D, true, true, false, 20, 1);
            try (var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(true, 1.0D);
                 var profiles = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(spell.getSpellResource(), profile))) {
                scope.add(normalProjectile);
                MulticastEchoStaffAttackHandler.runRepeatedCast(player, spell, () -> scope.add(repeatedProjectile));
                // 実飛行ではなく、追加詠唱のcontext終了後にも登録した投射物の補正が残ることを実ダメージで確認する。
                var normalSource = CombatTools.getDamageSource(helper.getLevel(), normalProjectile, player, DamageTypes.MANA_SLASH);
                var repeatedSource = CombatTools.getDamageSource(helper.getLevel(), repeatedProjectile, player, DamageTypes.MANA_SLASH);
                var beforeNormal = normalTarget.getHealth();
                helper.assertTrue(CombatTools.applyDamage(normalTarget, 8, normalSource, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Untracked projectile must deal normal damage");
                var beforeRepeat = repeatedTarget.getHealth();
                repeatedTarget.invulnerableTime = 20;
                helper.assertTrue(CombatTools.applyDamage(repeatedTarget, 8, repeatedSource, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Tracked projectile must bypass the existing i-frame after cast context closes");
                assertDamageRatio(helper, beforeNormal - normalTarget.getHealth(), beforeRepeat - repeatedTarget.getHealth(), 0.5F);
                helper.assertTrue(repeatedTarget.invulnerableTime == 1,
                        "Tracked projectile must set the profile's post-hit i-frame: " + repeatedTarget.invulnerableTime);
            }
        });
    }

    private static Zombie damageTarget(GameTestHelper helper, FakePlayer player, Vec3 offset) {
        var target = target(helper, player, offset);
        // 補正率の比較に防具の非線形な軽減や死亡を混ぜず、同条件の独立した標的を使う。
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        target.setHealth(target.getMaxHealth());
        return target;
    }

    private static ManaSlashProjectileEntity projectile(GameTestHelper helper, FakePlayer player) {
        var projectile = TargetingTestFixture.of(helper).track(new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), helper.getLevel(), player));
        projectile.setPos(player.position().add(0, 1, 2));
        TargetingTestFixture.of(helper).assertInside(projectile);
        return projectile;
    }

    private static void assertDamageRatio(GameTestHelper helper, float normal, float repeated, float multiplier) {
        helper.assertTrue(normal > 0 && Math.abs(repeated - normal * multiplier) < 1.0e-4F,
                "Repeated damage must apply its multiplier exactly once: normal=" + normal + ", repeated=" + repeated + ", multiplier=" + multiplier);
    }
}
