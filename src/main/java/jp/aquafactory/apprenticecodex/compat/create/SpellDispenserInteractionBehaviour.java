package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class SpellDispenserInteractionBehaviour extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (activeHand == InteractionHand.OFF_HAND) {
            return false;
        }

        if (player.level().isClientSide) {
            return true;
        }

        // mounted storage の generic menu を明示的に開く。
        // SpellDispenser は movement actor でもあるため、client 側が相互作用可能と判断し損ねても
        // この interactor 経由なら server に処理を届けられる。
        return contraptionEntity.getContraption()
                .getStorage()
                .handlePlayerStorageInteraction(contraptionEntity.getContraption(), player, localPos);
    }
}
