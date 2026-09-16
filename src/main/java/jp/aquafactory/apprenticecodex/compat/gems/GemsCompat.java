package jp.aquafactory.apprenticecodex.compat.gems;

import jp.aquafactory.apprenticecodex.registry.GemsActionRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public final class GemsCompat {
    private GemsCompat() {
    }

    public static void register(IEventBus modEventBus) {
        // 未導入時は外部型を含む登録クラスをロードしない。datagenでも素材のCodec処理より先に登録する。
        if (ModList.get().isLoaded("irons_jewelry")) {
            GemsActionRegistry.register(modEventBus);
        }
    }
}
