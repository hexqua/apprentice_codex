package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleConfigState;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

// 共通イベントのロード時にclient専用型を解決させない。
public final class ManaStabilizerClientBridge {
    public static Predicate<LivingEntity> aiming = ignored -> false;
    private ManaStabilizerClientBridge() {}
    public static boolean aiming(LivingEntity entity) { return aiming.test(entity); }
    public static Values values() { return ManaStabilizerModuleConfigState.values(); }
}
