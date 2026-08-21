package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.ironsjewelry.IronsJewelryProjectileHitCompat;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class IronsJewelryProjectileHitCompatGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private IronsJewelryProjectileHitCompatGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void taggedHitscanDamageCountsAsProjectileHit(GameTestHelper helper) {
        var directEntity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var source = CombatTools.getDamageSource(
                helper.getLevel(),
                directEntity,
                owner,
                DamageTypes.LETHAL_ASSAULT
        );

        helper.assertTrue(
                IronsJewelryProjectileHitCompat.isProjectileHit(false, source),
                "Tagged hitscan damage should count as a projectile hit for Iron's Jewelry"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unrelatedDamageDoesNotCountAsProjectileHit(GameTestHelper helper) {
        var directEntity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var source = CombatTools.getDamageSource(
                helper.getLevel(),
                directEntity,
                owner,
                DamageTypes.HIGANBANA
        );

        helper.assertFalse(
                IronsJewelryProjectileHitCompat.isProjectileHit(false, source),
                "Untagged damage should not count as a projectile hit for Iron's Jewelry"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void originalProjectileHitRemainsAccepted(GameTestHelper helper) {
        var source = helper.getLevel().damageSources().generic();

        helper.assertTrue(
                IronsJewelryProjectileHitCompat.isProjectileHit(true, source),
                "Iron's Jewelry original projectile result should remain accepted"
        );
        helper.succeed();
    }
}
