package jp.aquafactory.apprenticecodex.gametest.support;

import java.util.List;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class EntityTestSupport {
    private EntityTestSupport() {}

    public static Zombie createTargetingZombie(
            GameTestHelper helper,
            BlockPos pos,
            Player target,
            String name
    ) {
        var level = helper.getLevel();
        var zombie = new Zombie(EntityType.ZOMBIE, level);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        zombie.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        zombie.setCustomName(Component.literal(name));
        zombie.setTarget(target);
        zombie.setLastHurtByMob(target);
        level.addFreshEntity(zombie);
        return zombie;
    }

    public static void placeAbsoluteFluidTestBasin(ServerLevel level, BlockPos fluidPos, BlockState fluidState) {
        level.setBlock(fluidPos.below(), Blocks.STONE.defaultBlockState(), 3);
        for (var direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(fluidPos.relative(direction), Blocks.STONE.defaultBlockState(), 3);
        }
        level.setBlock(fluidPos, fluidState, 3);
    }

    public static ItemEntity spawnItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var absolutePos = helper.absoluteVec(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D));
        var itemEntity = new ItemEntity(helper.getLevel(), absolutePos.x, absolutePos.y, absolutePos.z, stack);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
    }

    public static void placeWaterTestBasin(GameTestHelper helper, BlockPos waterPos) {
        helper.setBlock(waterPos.below(), Blocks.STONE);
        for (var direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(waterPos.relative(direction), Blocks.STONE);
        }
    }

    public static ItemEntity spawnNoGravityItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        var absolutePos = helper.absoluteVec(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D));
        var itemEntity = new ItemEntity(helper.getLevel(), absolutePos.x, absolutePos.y, absolutePos.z, stack);
        itemEntity.setNoGravity(true);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
    }

    static <T extends SummonWeaponEntity> List<T> getOwnedSummonWeapons(
            GameTestHelper helper,
            FakePlayer owner,
            Class<T> weaponType
    ) {
        return helper.getLevel().getEntitiesOfClass(
                weaponType,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                weapon -> {
                    var summonOwner = weapon.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }
}
