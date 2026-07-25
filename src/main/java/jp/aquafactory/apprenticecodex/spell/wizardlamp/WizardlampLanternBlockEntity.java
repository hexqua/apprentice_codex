package jp.aquafactory.apprenticecodex.spell.wizardlamp;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WizardlampLanternBlockEntity extends BlockEntity {
    public WizardlampLanternBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.WIZARDLAMP_LANTERN.get(), pos, state);
    }
}
