package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.profile;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.spell.senseevil.SenseEvilScenarios.spawnPositionedZombie;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EchoStaffAttackScenarios {
    private EchoStaffAttackScenarios() {}

    static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_attack_direct_test");
            helper.getLevel().addFreshEntity(player);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 2.0D, 0.5D)));
            var spell = SpellRegistry.SHOCK.get();
            var profile = new MulticastEchoStaffAttackProfile(0.25D, true, false, true, 0, 1);
            var source = CombatTools.getDamageSource(helper.getLevel(), player, DamageTypes.SHOCK);
            var initialHealth = target.getHealth();

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(
                    true,
                    1.0D
            ); var ignoredProfile = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffAttackHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> {
                            target.invulnerableTime = 20;
                            CombatTools.applyDamage(target, 8.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                        }
                );
            }

            var damageTaken = initialHealth - target.getHealth();
            helper.assertTrue(damageTaken > 0.0F && damageTaken < 4.0F,
                    "Direct CombatTools multicast damage should use the profile multiplier before resistance: " + damageTaken);
            helper.assertTrue(target.invulnerableTime == 1,
                    "Direct CombatTools multicast damage should leave configured post-hit i-frame ticks: "
                            + target.invulnerableTime);
        });
    }

    static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_attack_projectile_test");
            helper.getLevel().addFreshEntity(player);
            var target = spawnPositionedZombie(helper.getLevel(), helper.absoluteVec(new Vec3(2.5D, 2.0D, 0.5D)));
            var spell = SpellRegistry.MANA_SLASH.get();
            var profile = new MulticastEchoStaffAttackProfile(0.5D, true, true, false, 20, 1);
            var projectile = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), helper.getLevel(), player);
            var source = CombatTools.getDamageSource(helper.getLevel(), projectile, player, DamageTypes.MANA_SLASH);
            var initialHealth = target.getHealth();

            try (var ignoredConfig = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(
                    true,
                    1.0D
            ); var ignoredProfile = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(Map.of(
                    spell.getSpellResource(),
                    profile
            ))) {
                MulticastEchoStaffAttackHandler.runRepeatedCast(
                        player,
                        spell,
                        () -> helper.getLevel().addFreshEntity(projectile)
                );
                target.invulnerableTime = 20;
                CombatTools.applyDamage(target, 8.0F, source, spell.getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }

            var damageTaken = initialHealth - target.getHealth();
            helper.assertTrue(damageTaken > 0.0F && damageTaken < 6.0F,
                    "Tracked projectile multicast damage should use the profile multiplier after cast context closes: "
                            + damageTaken);
            helper.assertTrue(target.invulnerableTime == 1,
                    "Tracked projectile multicast damage should leave configured post-hit i-frame ticks: "
                            + target.invulnerableTime);
        });
    }
}
