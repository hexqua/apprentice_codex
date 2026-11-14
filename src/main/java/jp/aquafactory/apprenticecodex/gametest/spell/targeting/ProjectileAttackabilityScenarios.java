package jp.aquafactory.apprenticecodex.gametest.spell.targeting;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ProjectileAttackabilityScenarios {
    private ProjectileAttackabilityScenarios() {}

    static void deflection(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "projectile_deflection");
        var owner = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 3), "projectile_original_owner");
        player.setYRot(0);
        player.setXRot(0);
        var projectile = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level);
        projectile.setOwner(owner);
        projectile.setPos(player.getEyePosition().add(0, 0, 1));
        projectile.setDeltaMovement(new Vec3(0, 0, -1));
        var holder = projectile.getType().builtInRegistryHolder();
        var originalTags = holder.tags().toList();
        try {
            var tags = new ArrayList<>(originalTags);
            tags.remove(EntityTypeTags.REDIRECTABLE_PROJECTILE);
            bindTags(holder, tags);
            player.attack(projectile);
            helper.assertTrue(projectile.getOwner() == owner && projectile.getDeltaMovement().z < 0,
                    "An untagged projectile must not be redirected by ordinary attacks");

            tags.add(EntityTypeTags.REDIRECTABLE_PROJECTILE);
            bindTags(holder, tags);
            player.attack(projectile);
            helper.assertTrue(projectile.getOwner() == player && projectile.getDeltaMovement().z > 0,
                    "The redirectable tag must preserve player deflection and transfer ownership");
        } finally {
            bindTags(holder, originalTags);
            projectile.discard();
        }
        helper.succeed();
    }

    private static void bindTags(Holder.Reference<EntityType<?>> holder, Collection<TagKey<EntityType<?>>> tags) {
        // 本番データへテスト用tagを追加せず、同一tick内で復元して他のGameTestへ持ち越さない。
        try {
            var method = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
            method.setAccessible(true);
            method.invoke(holder, tags);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not bind projectile deflection test tags", exception);
        }
    }
}
