package jp.aquafactory.apprenticecodex.gametest.spell.targeting;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.item.tool.toolitem.SynthesisedToolItem;
import net.stln.magitech.helper.CombatHelper;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class MagitechTargetingScenarios {
    private MagitechTargetingScenarios() {}

    static void mining(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 1), "magitech_mining_target");
        player.setYRot(0);
        player.setXRot(0);
        var tool = (SynthesisedToolItem) ItemInit.PICKAXE.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
        helper.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(0, 0, player.entityInteractionRange());
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, player, 4, 0);
        var projectile = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level);
        var target = EntityType.ZOMBIE.create(level);
        helper.assertTrue(target != null, "Magitech targeting test must create a combat target");
        try {
            familiar.setPos(start.add(0, -0.2, 0.8));
            projectile.setPos(start.add(0, -0.1, 1.3));
            level.addFreshEntity(familiar);
            level.addFreshEntity(projectile);
            helper.assertTrue(CombatHelper.getEntityHitResult(player, start, end, level) == null,
                    "Magitech line targeting must ignore the familiar and untagged projectile");
            helper.assertTrue(tool.onLeftClick(player, InteractionHand.MAIN_HAND, level) == InteractionResult.PASS,
                    "The familiar must leave block mining to the normal left-click path");

            // 視線上を外しても円柱判定では拾われる配置で、直線側だけの修正を検出する。
            familiar.setPos(start.add(0.45, -0.2, 0.8));
            helper.assertTrue(CombatHelper.getCylinderHit(player, player.entityInteractionRange(), start,
                            0.5, null, level, end) == null,
                    "Magitech cylinder targeting must ignore non-attackable constructs");
            helper.assertTrue(tool.onLeftClick(player, InteractionHand.MAIN_HAND, level) == InteractionResult.PASS,
                    "The cylinder fallback must not consume a mining click");

            target.setNoAi(true);
            target.setPos(start.add(0, -0.5, 1.5));
            level.addFreshEntity(target);
            var hit = CombatHelper.getEntityHitResult(player, start, end, level);
            helper.assertTrue(hit != null && hit.getEntity() == target,
                    "Ignoring constructs must preserve the combat target behind them");
            target.setPos(start.add(0.45, -0.5, 1.5));
            var cylinderHit = CombatHelper.getCylinderHit(player, player.entityInteractionRange(), start,
                    0.5, null, level, end);
            helper.assertTrue(cylinderHit != null && cylinderHit.getEntity() == target,
                    "The cylinder fallback must still detect ordinary combat targets");
        } finally {
            familiar.discard();
            projectile.discard();
            target.discard();
        }
        helper.succeed();
    }
}
