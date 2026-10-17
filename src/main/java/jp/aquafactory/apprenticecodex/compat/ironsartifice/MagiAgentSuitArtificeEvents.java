package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.SubscribeEvent;

public final class MagiAgentSuitArtificeEvents {
    private MagiAgentSuitArtificeEvents() {}

    @SubscribeEvent
    public static void onCompose(ComposeShotEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !MagiAgentSuitEffects.isWearingSuitPiece(player, ArmorItem.Type.HELMET)) {
            return;
        }
        double chance = ApprenticeCodexServerConfig.magiAgentSuitAmmoNoConsumeChance();
        if (chance <= 0) return;
        // 抽選と消費キャンセルは上流に委ね、既存modifierと乗算合成して通知・効果音も維持する。
        event.getShotProfile().modifyValue(ShotComponents.AMMO_CONSUME_CHANCE,
                new ValueModifier(-chance, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.HARMFUL));
    }
}
