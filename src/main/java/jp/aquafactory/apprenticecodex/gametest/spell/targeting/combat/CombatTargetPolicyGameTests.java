package jp.aquafactory.apprenticecodex.gametest.spell.targeting.combat;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.SummonedZombie;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CombatTargetPolicyGameTests {
    private CombatTargetPolicyGameTests() {
    }

    @GameTest(template = "gametest/combat_queries", batch = "apprenticecodex.combat_target_policy")
    public static void combatTargetPolicySeparatesSelfDamageFromAllyProtection(GameTestHelper helper) {
        try (var scope = TargetingTestFixture.of(helper)) {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(8, 4, 8), "combat_policy_self_owner");
            scope.track(owner);
            scope.assertInside(owner);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            owner, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Default combat policy should protect the caster");
            helper.assertFalse(CombatTools.isProtectedCombatTarget(
                            owner, owner, CombatTools.CombatTargetPolicy.ALLOW_SELF_PROTECT_ALLIES),
                    "Self-damage combat policy should allow damage to the caster");
            helper.assertFalse(CombatTools.isProtectedCombatTarget(
                            owner, null, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Ownerless damage should not infer protection");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/combat_queries", batch = "apprenticecodex.combat_target_policy")
    public static void combatTargetPolicyRespectsTeamFriendlyFireAndPvp(GameTestHelper helper) {
        try (var scope = TargetingTestFixture.of(helper)) {
            var level = helper.getLevel();
            var server = level.getServer();
            helper.assertTrue(server != null, "Combat policy PvP test requires a server");
            var owner = new ServerPlayer(server, level,
                    new GameProfile(UUID.randomUUID(), "combat_policy_team_owner"), ClientInformation.createDefault());
            var target = new ServerPlayer(server, level,
                    new GameProfile(UUID.randomUUID(), "combat_policy_team_target"), ClientInformation.createDefault());
            var scoreboard = level.getScoreboard();
            var team = scoreboard.addPlayerTeam("policy_" + UUID.randomUUID().toString().substring(0, 8));
            var previousPvp = server.isPvpAllowed();
            scope.onClose(() -> scoreboard.removePlayerTeam(team));
            scope.onClose(() -> server.setPvpAllowed(previousPvp));
            // 純粋な保護判定は接続・world登録を必要としない。
            scope.track(owner);
            scope.track(target);
            owner.setPos(helper.absoluteVec(new Vec3(8, 4, 8)));
            target.setPos(helper.absoluteVec(new Vec3(10, 4, 8)));
            scope.assertInside(owner);
            scope.assertInside(target);
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(target.getScoreboardName(), team);

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

        }
        helper.succeed();
    }

    @GameTest(template = "gametest/combat_queries", batch = "apprenticecodex.combat_target_policy")
    public static void combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(GameTestHelper helper) {
        try (var scope = TargetingTestFixture.of(helper)) {
            var level = helper.getLevel();
            var owner = EntityType.ZOMBIE.create(level);
            var passenger = EntityType.VILLAGER.create(level);
            helper.assertTrue(owner != null && passenger != null,
                    "Combat policy vehicle test should create its owner and passenger");
            var ownerPosition = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(8, 4, 8)));
            var passengerPosition = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(9, 4, 8)));
            owner.setPos(ownerPosition);
            passenger.setPos(passengerPosition);
            owner.setNoAi(true);
            owner.setNoGravity(true);
            scope.add(owner);
            passenger.setNoAi(true);
            passenger.setNoGravity(true);
            scope.add(passenger);
            var boat = new Boat(level, owner.getX(), owner.getY(), owner.getZ());
            boat.setNoGravity(true);
            scope.add(boat);
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
            wolf.setTame(true, true);
            wolf.setPos(ownerPosition.add(-2, 0, 0));
            wolf.setNoAi(true);
            wolf.setNoGravity(true);
            scope.add(wolf);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            wolf, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Owned tame animals should be protected");

            var summon = new SummonedZombie(
                    EntityRegistry.SUMMONED_ZOMBIE.get(), level);
            summon.setPos(ownerPosition.add(0, 0, 2));
            summon.setNoAi(true);
            summon.setNoGravity(true);
            scope.add(summon);
            SummonManager.setOwner(summon, owner);
            helper.assertTrue(CombatTools.isProtectedCombatTarget(
                            summon, owner, CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES),
                    "Iron's summons should be protected from their summoner");
        }
        helper.succeed();
    }
}