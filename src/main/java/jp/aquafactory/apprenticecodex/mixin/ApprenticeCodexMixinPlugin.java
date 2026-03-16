package jp.aquafactory.apprenticecodex.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ApprenticeCodexMixinPlugin implements IMixinConfigPlugin {
    private static final String EASY_MAGIC_MOD_ID = "easymagic";
    private static final String APOTHEOSIS_MOD_ID = "apotheosis";
    private static final String EASY_MAGIC_MIXIN = "jp.aquafactory.apprenticecodex.mixin.EasyMagicModEnchantmentMenuMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!EASY_MAGIC_MIXIN.equals(mixinClassName)) {
            return true;
        }

        var loadingModList = FMLLoader.getLoadingModList();
        return loadingModList != null
                && loadingModList.getModFileById(EASY_MAGIC_MOD_ID) != null
                && loadingModList.getModFileById(APOTHEOSIS_MOD_ID) == null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
