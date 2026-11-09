package jp.aquafactory.apprenticecodex.gametest.spell.targeting.combat;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.SummonedZombie;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class CombatTargetPolicyScenarios {
    private CombatTargetPolicyScenarios() {}

    static void combatTargetPolicySeparatesSelfDamageFromAllyProtection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "combat_policy_self_owner");
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            owner, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Default combat policy should protect the caster");
            helper.assertFalse(CombatTools.isProtectedCombatTarget(
                            owner, owner, CombatTools.CombatTargetPolicy.ALLOW_SELF_PROTECT_ALLIES),
                    "Self-damage combat policy should allow damage to the caster");
            helper.assertFalse(CombatTools.isProtectedCombatTarget(
                            owner, null, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Ownerless damage should not infer protection");
        });
    }

    static void combatTargetPolicyRespectsTeamFriendlyFireAndPvp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var server = level.getServer();
            helper.assertTrue(server != null, "Combat policy PvP test requires a server");
            var owner = new ServerPlayer(server, level,
                    new GameProfile(UUID.randomUUID(), "combat_policy_team_owner"));
            var target = new ServerPlayer(server, level,
                    new GameProfile(UUID.randomUUID(), "combat_policy_team_target"));
            var scoreboard = level.getScoreboard();
            var team = scoreboard.addPlayerTeam("codex_policy");
            var previousPvp = server.isPvpAllowed();
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(target.getScoreboardName(), team);

            try {
                server.setPvpAllowed(true);
                team.setAllowFriendlyFire(false);
                helper.assertTrue(CombatTools.isProtectedCombatTarget(
                                target, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                        "Team members should be protected while friendly fire is disabled");

                team.setAllowFriendlyFire(true);
                helper.assertFalse(CombatTools.isProtectedCombatTarget(
                                target, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                        "Team members should not be protected while friendly fire is enabled");

                scoreboard.removePlayerFromTeam(owner.getScoreboardName(), team);
                scoreboard.removePlayerFromTeam(target.getScoreboardName(), team);
                server.setPvpAllowed(false);
                helper.assertTrue(CombatTools.isProtectedCombatTarget(
                                target, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                        "Players should be protected while server PvP is disabled");
            } finally {
                server.setPvpAllowed(previousPvp);
                scoreboard.removePlayerTeam(team);
            }
        });
    }

    static void combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "combat_policy_vehicle_owner");
            var passenger = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 0), "combat_policy_vehicle_passenger");
            level.addFreshEntity(owner);
            level.addFreshEntity(passenger);
            var boat = new Boat(level, owner.getX(), owner.getY(), owner.getZ());
            level.addFreshEntity(boat);
            helper.assertTrue(owner.startRiding(boat, true), "Combat policy test owner should board the boat");
            helper.assertTrue(passenger.startRiding(boat, true), "Combat policy test passenger should board the boat");
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            boat, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "The ridden vehicle should be protected");
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            passenger, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Passengers sharing the root vehicle should be protected");

            var wolf = new Wolf(EntityType.WOLF, level);
            wolf.setOwnerUUID(owner.getUUID());
            wolf.setTame(true);
            level.addFreshEntity(wolf);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            wolf, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Owned tame animals should be protected");

            var summon = new SummonedZombie(
                    EntityRegistry.SUMMONED_ZOMBIE.get(), level);
            level.addFreshEntity(summon);
            SummonManager.setOwner(summon, owner);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            summon, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Iron's summons should be protected from their summoner");
        });
    }
}
