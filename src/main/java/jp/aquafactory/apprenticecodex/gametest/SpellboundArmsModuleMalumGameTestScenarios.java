package jp.aquafactory.apprenticecodex.gametest;

import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.core.handlers.SoulDataHandler;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

final class SpellboundArmsModuleMalumGameTestScenarios {
    private SpellboundArmsModuleMalumGameTestScenarios() {}

    static void run(GameTestHelper h) {
        var player = SpellboundArmsModuleGameTestScenarios.player(h);
        var target = SpellboundArmsModuleGameTestScenarios.target(h, player);
        var lethal = SpellboundArmsModuleGameTestScenarios.target(h, player);
        var profile = SpellboundArmsModuleGameTestScenarios.compose(player, SpellboundArmsModuleGameTestScenarios.gun(1, 0, false));
        var bullet = SpellboundArmsModuleGameTestScenarios.bullet(h, player, profile.copy());
        var lethalBullet = SpellboundArmsModuleGameTestScenarios.bullet(h, player, profile.copy());
        try {
            h.assertTrue(bullet.getTags().contains(SoulDataHandler.SOUL_SHATTER_ENTITY_TAG), "Spawned module bullet must carry Malum soul damage");
            bullet.hit(new EntityHitResult(target, target.getEyePosition()));
            h.assertTrue(MalumLivingEntityDataCapability.getCapability(target).soulData.exposedSoulDuration > 0, "Nonlethal hit must expose the soul");
            lethal.setHealth(1);
            var area = AABB.ofSize(lethal.position(), 8, 8, 8);
            int before = h.getLevel().getEntitiesOfClass(SpiritItemEntity.class, area).size();
            lethalBullet.hit(new EntityHitResult(lethal, lethal.getEyePosition()));
            h.assertTrue(MalumLivingEntityDataCapability.getCapability(lethal).soulData.soulless, "Lethal hit must complete Malum soul harvest");
            h.assertTrue(h.getLevel().getEntitiesOfClass(SpiritItemEntity.class, area).size() > before, "Lethal hit must drop spirits through Malum");
            var plain = SpellboundArmsModuleGameTestScenarios.bullet(h, player,
                    SpellboundArmsModuleGameTestScenarios.compose(player, SpellboundArmsModuleGameTestScenarios.gun(0, 0, false)));
            try {
                h.assertTrue(!plain.getTags().contains(SoulDataHandler.SOUL_SHATTER_ENTITY_TAG), "Unmodified bullet must not gain soul damage");
            } finally { plain.discard(); }
        } finally { bullet.discard(); lethalBullet.discard(); target.discard(); lethal.discard(); player.discard(); }
        h.succeed();
    }
}
