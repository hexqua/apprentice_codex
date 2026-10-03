package jp.aquafactory.apprenticecodex.compat.patchouli;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import vazkii.patchouli.api.PatchouliAPI;

public final class PatchouliBookSupport {
    public static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentices_memo");

    private PatchouliBookSupport() {
    }

    public static ItemStack createBookStack() {
        if (!ModList.get().isLoaded("patchouli")) {
            return ItemStack.EMPTY;
        }

        return LoadedPatchouli.createBookStack();
    }

    // 任意依存のAPI参照は、Patchouliの導入確認後にだけ読み込むクラスへ隔離する。
    private static final class LoadedPatchouli {
        private static ItemStack createBookStack() {
            return PatchouliAPI.get().getBookStack(BOOK_ID);
        }
    }
}
