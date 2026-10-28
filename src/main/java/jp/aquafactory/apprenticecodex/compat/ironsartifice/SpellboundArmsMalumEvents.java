package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import com.sammy.malum.core.handlers.SoulDataHandler;
import io.redspace.irons_artifice.entity.Bullet;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleModifier;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SpellboundArmsMalumEvents {
    private SpellboundArmsMalumEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Bullet bullet
                && bullet.getProfile().peek(SpellboundArmsModuleModifier.MODULE_COUNT) > 0) {
            // Malum自身の露出・死亡処理を使い、魂なし状態やspawner制限を迂回しない。
            bullet.addTag(SoulDataHandler.SOUL_SHATTER_ENTITY_TAG);
        }
    }
}
