package jp.aquafactory.apprenticecodex.gametest.spell.targeting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EntityAttackabilityGameTests {
    private EntityAttackabilityGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void ordinaryAttacksIgnoreSpellProjectile(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "projectile_attacker");
        var owner = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 3), "projectile_owner");
        var projectile = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), helper.getLevel());
        var velocity = new Vec3(0, 0, -1);
        projectile.setOwner(owner);
        projectile.setPos(player.getEyePosition().add(0, 0, 1));
        projectile.setDeltaMovement(velocity);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 2));
        try {
            helper.assertFalse(projectile.isAttackable(), "Ordinary attacks must skip the spell projectile");
            player.attack(projectile);
            helper.assertTrue(projectile.getOwner() == owner && projectile.getDeltaMovement().equals(velocity),
                    "Attacking the projectile must preserve its owner and motion");

            var health = target.getHealth();
            player.attack(target);
            helper.assertTrue(target.getHealth() < health, "Ordinary attacks must still damage a mob");
        } finally {
            projectile.discard();
            target.discard();
        }
        helper.succeed();
    }
}
