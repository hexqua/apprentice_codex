package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.entity.scythe.LocalizedMaelstromEntity;
import com.sammy.malum.visual_effects.WeaponParticleEffects;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

@OnlyIn(Dist.CLIENT)
final class MalumScytheMaelstromClient {
    // 値も弱参照にし、view -> level -> 投擲Entityの経路で退出済みworldを保持しない。
    private static final Map<ScytheThrowEntity, WeakReference<LocalizedMaelstromEntity>> VIEWS = new WeakHashMap<>();
    private MalumScytheMaelstromClient() {}

    static void tick(ScytheThrowEntity scythe) {
        // 描画APIの専用型を満たす位置ビュー。worldへ追加せず、tickも攻撃処理も実行しない。
        var reference = VIEWS.get(scythe);
        var view = reference == null ? null : reference.get();
        if (view == null) {
            view = new LocalizedMaelstromEntity(scythe.level());
            view.setItem(new net.minecraft.world.item.ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.SPELL_REAPER_SCYTHE.get()));
            VIEWS.put(scythe, new WeakReference<>(view));
        }
        view.setPos(scythe.position());
        WeaponParticleEffects.spawnMaelstromParticles(view);
    }
}
