package jp.aquafactory.apprenticecodex.gametest.spell.lunaraim;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.spell.lunaraim.LunarAimFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LunarAimSelectionGameTests {
    private LunarAimSelectionGameTests() {
    }

    @GameTest(template = "gametest/lunar_aim_range", batch = BATCH)
    public static void selectionSupportsCrystalsRangeAssistAndWalls(GameTestHelper h) {
        try (var s = new LunarAimFixture(h, new Vec3(32, 4, 20))) {
            var crystal = new EndCrystal(EntityType.END_CRYSTAL, h.getLevel());
            crystal.setPos(s.origin.add(8, 0, 0));
            s.add(crystal);
            h.assertTrue(s.selected() == crystal, "End crystals must be selectable");
            crystal.discard();
            var target = s.zombie(8, 1.1);
            h.assertTrue(s.selected() == target, "One block aim assist must accept a near miss");
            move(h, target, s.origin.add(8, 0, 1.5));
            h.assertTrue(s.selected() == null, "Targets outside the assisted ray must be excluded");
            move(h, target, s.origin.add(8, 0, 0));
            var block = BlockPos.containing(s.owner.getEyePosition().add(4, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                h.assertTrue(s.selected() == null, "A wall must stop lock-on");
                move(h, target, new Vec3(block.getX() + 1.4, s.origin.y, s.origin.z));
                h.assertTrue(s.selected() == null, "Aim assist must not reach through the front of a wall");
            } finally {
                h.getLevel().setBlockAndUpdate(block, previous);
            }
            // 上面・下面の交点を含む128block境界を、専用structure内で検証する。
            s.owner.setXRot(-90);
            move(h, target, s.owner.getEyePosition().add(0, 127, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == target, "Targets within 128 blocks must lock");
            move(h, target, s.owner.getEyePosition().add(0, 129, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == null, "Targets beyond the ray endpoint must not lock");
            move(h, target, s.owner.getEyePosition().add(0, 128, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == target, "An intersection at the range endpoint must lock");
            move(h, s.owner, s.origin.add(0, 160, 0));
            s.owner.setXRot(90);
            move(h, target, s.owner.getEyePosition().add(0, -128 - target.getBbHeight(), 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == target, "The inclusive endpoint must also accept the upper face of a target");
            move(h, target, target.position().add(0, -0.001, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == null, "A target just beyond the endpoint must be excluded");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void selectionUsesCombatPolicyAndNearestIntersection(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var near = s.zombie(4, 0);
            var far = s.zombie(8, 0);
            h.assertTrue(s.selected() == near, "The nearest eligible intersection must win");
            var board = h.getLevel().getScoreboard();
            var team = board.addPlayerTeam("lunar_" + UUID.randomUUID().toString().substring(0, 8));
            try {
                board.addPlayerToTeam(s.owner.getScoreboardName(), team);
                board.addPlayerToTeam(near.getScoreboardName(), team);
                team.setAllowFriendlyFire(false);
                h.assertTrue(s.selected() == far, "Protected allies must be skipped");
                team.setAllowFriendlyFire(true);
                h.assertTrue(s.selected() == near, "Friendly fire must follow the common combat policy");
                near.discard();
                far.discard();
                h.assertTrue(s.selected() == null, "The caster must never select itself");
            } finally {
                board.removePlayerTeam(team);
            }
        }
        h.succeed();
    }
}
