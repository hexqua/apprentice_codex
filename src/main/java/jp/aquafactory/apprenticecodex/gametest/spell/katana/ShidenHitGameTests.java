package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.shiden.ShidenKatanaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.spell.katana.KatanaHitFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ShidenHitGameTests {
    private ShidenHitGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void shidenUsesTallNarrowAttackAreaAndCanBeInterrupted(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var owner = scene.player("shiden_area_owner", new Vec3(4.5D, 2.0D, 2.5D));
            owner.setYRot(0.0F);
            var weapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, owner);
            weapon.setDamage(4.0F);
            scene.add(weapon);

            var origin = weapon.position();
            // 入力・期待・生成した対象を同じケースに持ち、配列の添字対応をなくす。
            var cases = List.of(
                    new HitCase("center", new Vec3(0, 0, 2), true),
                    new HitCase("upper", new Vec3(0, 1.25, 3), true),
                    new HitCase("lateral miss", new Vec3(1, 0, 2), false),
                    new HitCase("vertical miss", new Vec3(0, 1.9, 2), false),
                    new HitCase("depth miss", new Vec3(0, 0, 5), false),
                    new HitCase("rear miss", new Vec3(0, 0, -0.8), false));
            var targets = cases.stream().map(testCase -> {
                var target = scene.zombie(origin.add(testCase.offset()));
                target.getAttribute(Attributes.ARMOR).setBaseValue(0);
                return new ObservedTarget(testCase, target, target.getHealth());
            }).toList();
            scene.act(() -> weapon.slash(level));
            for (var observed : targets) {
                float damage = observed.before() - observed.entity().getHealth();
                helper.assertTrue(observed.testCase().shouldHit() ? damage > HEALTH_EPSILON : Math.abs(damage) < HEALTH_EPSILON,
                        "Shiden target case=" + observed.testCase().name() + ", shouldHit=" + observed.testCase().shouldHit()
                                + ", damage=" + damage + ", " + scene.scope.diagnostics(observed.entity()));
            }
            helper.assertTrue(SpellRegistry.SHIDEN.get().canBeInterrupted(owner),
                    "Shiden should remain interruptible when its caster is damaged");

        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void shidenReducesDamageAndKnockbackThroughWall(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var blockedOwner = scene.player("shiden_blocked_owner", new Vec3(2.5D, 2.0D, 2.5D));
            blockedOwner.setYRot(0.0F);
            var blockedWeapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, blockedOwner);
            blockedWeapon.setDamage(4.0F);
            blockedWeapon.setBlockPenetrationDamageMultiplier(0.5F);
            scene.add(blockedWeapon);
            var blockedTarget = scene.zombie(blockedWeapon.position().add(0.0D, 0.0D, 2.0D));
            blockedTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            var blockedInitialHealth = blockedTarget.getHealth();
            var blockedInitialMovement = new Vec3(0.125D, 0.0D, -0.25D);
            blockedTarget.setDeltaMovement(blockedInitialMovement);
            var wallCenter = BlockPos.containing(blockedWeapon.position().add(0.0D, 0.0D, 1.0D));
            for (var x = wallCenter.getX() - 1; x <= wallCenter.getX() + 1; ++x) {
                for (var y = wallCenter.getY(); y <= wallCenter.getY() + 2; ++y) {
                    scene.blockAbsolute(new BlockPos(x, y, wallCenter.getZ()), Blocks.STONE.defaultBlockState());
                }
            }

            var visibleOwner = scene.player("shiden_visible_owner", new Vec3(6.5D, 2.0D, 2.5D));
            visibleOwner.setYRot(0.0F);
            var visibleWeapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, visibleOwner);
            visibleWeapon.setDamage(4.0F);
            visibleWeapon.setBlockPenetrationDamageMultiplier(0.5F);
            scene.add(visibleWeapon);
            var visibleTarget = scene.zombie(visibleWeapon.position().add(0.0D, 0.0D, 2.0D));
            visibleTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            var visibleInitialHealth = visibleTarget.getHealth();
            var visibleInitialMovement = new Vec3(-0.125D, 0.0D, -0.25D);
            visibleTarget.setDeltaMovement(visibleInitialMovement);

            scene.act(() -> blockedWeapon.slash(level));
            scene.act(() -> visibleWeapon.slash(level));

            var blockedDamage = blockedInitialHealth - blockedTarget.getHealth();
            var visibleDamage = visibleInitialHealth - visibleTarget.getHealth();
            helper.assertTrue(visibleDamage > HEALTH_EPSILON,
                    "Shiden should damage a visible target");
            helper.assertTrue(Math.abs(blockedDamage - visibleDamage * 0.5F) < HEALTH_EPSILON,
                    "Shiden must retain the 0.5 wall damage ratio: blocked=" + blockedDamage + ", visible=" + visibleDamage);
            helper.assertTrue(blockedTarget.getDeltaMovement().distanceTo(blockedInitialMovement) < POSITION_EPSILON,
                    "Shiden block penetration damage should not knock the target back");
            helper.assertTrue(visibleTarget.getDeltaMovement().distanceTo(visibleInitialMovement) > POSITION_EPSILON,
                    "Shiden should keep default knockback against a visible target");

        }
        helper.succeed();
    }

    private record HitCase(String name, Vec3 offset, boolean shouldHit) {
    }

    private record ObservedTarget(HitCase testCase, Zombie entity, float before) {
    }

}
