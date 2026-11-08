package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BlockDropTestSupport {
    private BlockDropTestSupport() {}

    public static void invokeTouchDigDestroyBlock(TouchDigSpell spell, Level level, BlockPos pos, Player player) {
        try {
            var method = TouchDigSpell.class.getDeclaredMethod("doDestroyBlock", Level.class, BlockPos.class, LivingEntity.class);
            method.setAccessible(true);
            method.invoke(spell, level, pos, player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Touch Dig destroy helper for GameTest", exception);
        }
    }

    public static List<ItemEntity> getFreshItemDrops(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).inflate(radius),
                itemEntity -> itemEntity.getAge() <= 1
        );
    }

    public static int countFreshItemDrops(ServerLevel level, Item item, BlockPos pos, double radius) {
        return getFreshItemDrops(level, pos, radius).stream()
                .filter(itemEntity -> itemEntity.getItem().is(item))
                .mapToInt(itemEntity -> itemEntity.getItem().getCount())
                .sum();
    }

    public static boolean hasItemEntityWithin(ServerLevel level, Item item, Vec3 pos, double radius) {
        return !level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos, pos).inflate(radius),
                itemEntity -> !itemEntity.isRemoved() && itemEntity.getItem().is(item)
        ).isEmpty();
    }
}
