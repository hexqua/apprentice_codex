package jp.aquafactory.apprenticecodex.spell.healingbloom;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HealingBloomLightBlockEntity extends BlockEntity {
    private static final int SELF_CLEAN_INTERVAL_TICK = 20;
    private int selfCleanCooldown;

    public HealingBloomLightBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.HEALING_BLOOM_LIGHT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HealingBloomLightBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.selfCleanCooldown > 0) {
            --blockEntity.selfCleanCooldown;
            return;
        }
        blockEntity.selfCleanCooldown = SELF_CLEAN_INTERVAL_TICK;

        var bloomExists = level.getEntitiesOfClass(
                HealingBloomEntity.class,
                new AABB(pos.below()).inflate(0.75, 1.0, 0.75),
                entity -> entity.isAlive() && entity.managesLightAt(pos)
        ).stream().findAny().isPresent();

        if (!bloomExists) {
            level.removeBlock(pos, false);
        }
    }
}
