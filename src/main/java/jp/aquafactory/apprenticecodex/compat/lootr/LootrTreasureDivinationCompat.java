package jp.aquafactory.apprenticecodex.compat.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import noobanidus.mods.lootr.api.blockentity.ILootBlockEntity;

final class LootrTreasureDivinationCompat {
    static final String MOD_ID = "lootr";

    private LootrTreasureDivinationCompat() {
    }

    static boolean shouldIgnoreOpenedTarget(ServerLevel level, ServerPlayer player, BlockPos pos) {
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ILootBlockEntity lootBlockEntity)) {
            return false;
        }

        // Lootr 側の openers は軽量なプレイヤー別既読集合として永続化されるため、
        // TreasureDivination では refresh 状態までは追わず「既に開けた」の近似に使う。
        return lootBlockEntity.getOpeners().contains(player.getUUID());
    }
}
