package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class RaycastAimAssistGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private RaycastAimAssistGameTests() {}

    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "aim_assist_test"));
        player.setPos(helper.absoluteVec(new BlockPos(4, 18, 1).getCenter()));
        player.setYRot(0);
        player.setXRot(0);
        return player;
    }

    private static Zombie target(GameTestHelper helper, FakePlayer player, Vec3 offset) {
        var target = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(player.position().add(offset));
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    @GameTest(template = TEMPLATE)
    public static void legacyWidthDoesNotWidenFinalHitAndKeepsImpactRules(GameTestHelper helper) {
        var player = player(helper);
        // 判定幅と命中座標の互換性を調べるため、プレイヤー視線ではなく既知の線分を入力する。
        var forward = new Vec3(0, 0, 1);
        var target = target(helper, player, new Vec3(1, 0, 5));
        try {
            for (double width : new double[]{1, 4}) {
                helper.assertTrue(RaycastTools.raycast(player, forward, 8, width, e -> e == target).hitEntity() == null,
                        "Legacy candidate widths must not widen the fixed final hit margin");
            }
            target.setPos(player.position().add(0.5, 0, 5));
            var fallback = RaycastTools.raycast(player, forward, 8, 1, e -> e == target);
            helper.assertTrue(fallback.hitEntity() == target
                            && fallback.hitPosition().distanceToSqr(target.getBoundingBox().getCenter()) < 1.0E-8,
                    "Legacy fixed 0.3 hit margin must retain its center fallback outside the 0.1 impact box: result="
                            + fallback + ", box=" + target.getBoundingBox() + ", eye=" + player.getEyePosition());
            target.setPos(player.position().add(0, 0, 5));
            var direct = RaycastTools.raycast(player, forward, 8, 1, e -> e == target);
            helper.assertTrue(direct.hitEntity() == target
                            && Math.abs(direct.hitPosition().z - (target.getBoundingBox().minZ - 0.1)) < 1.0E-6,
                    "Legacy direct impact must retain the separate 0.1 inflation");
            // 小さい発射元では検索幅だけの変更でも候補入りが変わるため、幅の完全な無効化とは区別する。
            var pos = player.position();
            player.setBoundingBox(new AABB(pos.x - 0.01, pos.y, pos.z - 0.01,
                    pos.x + 0.01, pos.y + 2, pos.z + 0.01));
            target.setPos(pos.add(0.5, 0, 5));
            helper.assertTrue(RaycastTools.raycast(player, forward, 8, 0.1, e -> e == target).hitEntity() == null
                            && RaycastTools.raycast(player, forward, 8, 4, e -> e == target).hitEntity() == target,
                    "Legacy width must still control candidate inclusion");
        } finally {
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assistedWidthCapturesOffsetTargetAndReturnsActualImpact(GameTestHelper helper) {
        var player = player(helper);
        var target = target(helper, player, new Vec3(1, 0, 5));
        try {
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(player, 8, 1, e -> e == target).hitEntity() == null,
                    "Thin aim assist must miss a target one block beside the ray");
            var wide = RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> e == target);
            helper.assertTrue(wide.hitEntity() == target && target.getBoundingBox().contains(wide.hitPosition()),
                    "Wide aim assist must capture the offset target and return a point inside its actual box");
            target.setPos(player.position().add(3, 0, 5));
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> e == target).hitEntity() == null,
                    "Targets outside the assist width must remain misses");
            target.setPos(player.position());
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(player, 8, 1, e -> e == target).hitEntity() == target,
                    "Aim assist must handle an eye starting inside the target");
        } finally {
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assistedRayRejectsInflatedBoxesBeyondRangeAndWall(GameTestHelper helper) {
        var player = player(helper);
        var target = target(helper, player, new Vec3(0, 0, 8.8));
        var wall = BlockPos.containing(player.getEyePosition().add(0, 0, 4));
        try {
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> e == target).hitEntity() == null,
                    "An inflated box entering range must not capture an out-of-range target");
            target.setPos(player.position().add(0, 0, 5));
            helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            var blocked = RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> e == target);
            helper.assertTrue(blocked.hitType() == RaycastTools.TargetType.BLOCK,
                    "An inflated target box reaching in front of a wall must not bypass the wall");
            helper.assertTrue(RaycastTools.raycastFromEye(player, 8, 4, e -> e == target).hitType()
                            == RaycastTools.TargetType.BLOCK,
                    "Legacy rays must continue stopping at collision blocks");
        } finally {
            target.discard();
            helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assistedRayChecksOffsetOcclusionAndSkipsHiddenNearestTarget(GameTestHelper helper) {
        var player = player(helper);
        var hidden = target(helper, player, new Vec3(1.5, 0, 3));
        var visible = target(helper, player, new Vec3(-1, 0, 6));
        var wallOrigin = BlockPos.containing(player.position().add(1, 0, 2));
        try {
            // 中心線は空いていても、横にある候補への視線だけが壁で遮られる配置。
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 3; x++) {
                    helper.getLevel().setBlockAndUpdate(wallOrigin.offset(x, y, 0), Blocks.STONE.defaultBlockState());
                }
            }
            var hit = RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> e == hidden || e == visible);
            helper.assertTrue(hit.hitEntity() == visible,
                    "A hidden offset candidate must not block selection of a farther visible candidate");
            var sight = helper.getLevel().clip(new ClipContext(player.getEyePosition(),
                    hit.hitPosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            helper.assertTrue(sight.getType() == HitResult.Type.MISS,
                    "The assisted impact itself must be visible from the eye");
        } finally {
            hidden.discard();
            visible.discard();
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 3; x++) {
                    helper.getLevel().setBlockAndUpdate(wallOrigin.offset(x, y, 0), Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assistedRayWorksForDiagonalUpwardAimAndEmptyShots(GameTestHelper helper) {
        var player = player(helper);
        player.setYRot(-45);
        player.setXRot(-30);
        var forward = player.getViewVector(1).normalize();
        var right = forward.cross(new Vec3(0, 1, 0)).normalize();
        var target = target(helper, player, Vec3.ZERO);
        target.setPos(player.getEyePosition().add(forward.scale(5)).add(right.scale(1)).add(0, -0.9, 0));
        try {
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> e == target).hitEntity() == target,
                    "Aim assist must also capture offset targets along a diagonal upward view");
            var empty = RaycastTools.raycastFromEyeWithAimAssist(player, 8, 4, e -> false);
            helper.assertTrue(empty.hitType() == RaycastTools.TargetType.NONE
                            && empty.hitPosition().distanceToSqr(player.getEyePosition().add(forward.scale(8))) < 1.0E-8,
                    "An empty assisted ray must retain its full-range endpoint");
        } finally {
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shockUsesWideFallbackButKeepsThinTargetPriority(GameTestHelper helper) {
        var player = player(helper);
        var offset = target(helper, player, new Vec3(1, 0, 4));
        var direct = target(helper, player, new Vec3(0, 0, 6));
        try {
            var spell = SpellRegistry.SHOCK.get();
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
            helper.assertTrue(direct.getHealth() < direct.getMaxHealth() && offset.getHealth() == offset.getMaxHealth(),
                    "Shock must prioritize the thin-ray target over a closer wide-only target");
            direct.discard();
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
            helper.assertTrue(offset.getHealth() < offset.getMaxHealth(),
                    "Shock must damage a wide-only target when the thin ray misses");
        } finally {
            offset.discard();
            direct.discard();
        }
        helper.succeed();
    }
}
