package jp.aquafactory.apprenticecodex.mixin;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ApprenticeCodexMixinPlugin implements IMixinConfigPlugin {
    private static final String EASY_MAGIC_MOD_ID = "easymagic";
    private static final String APOTHEOSIS_MOD_ID = "apotheosis";
    private static final String JEI_MOD_ID = "jei";
    private static final String EPIC_FIGHT_MOD_ID = "epicfight";
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";
    private static final String EFIS_COMPAT_MOD_ID = "efiscompat";
    private static final String MALUM_MOD_ID = "malum";
    private static final String EASY_MAGIC_MIXIN = "jp.aquafactory.apprenticecodex.mixin.EasyMagicModEnchantmentMenuMixin";
    private static final String ARCANE_ANVIL_JEI_RECIPE_MIXIN =
            "jp.aquafactory.apprenticecodex.mixin.ArcaneAnvilJeiRecipeMixin";
    private static final String EPIC_FIGHT_MIXIN_PREFIX = "jp.aquafactory.apprenticecodex.mixin.EpicFight";
    private static final String BETTER_COMBAT_MIXIN_PREFIX = "jp.aquafactory.apprenticecodex.mixin.BetterCombat";
    private static final String EFIS_COMPAT_MIXIN_PREFIX = "jp.aquafactory.apprenticecodex.mixin.EfisCompat";
    private static final String MALUM_REPLENISHING_OFFHAND_MIXIN =
            "jp.aquafactory.apprenticecodex.mixin.MalumReplenishingOffhandMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (EASY_MAGIC_MIXIN.equals(mixinClassName)) {
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList != null
                    && loadingModList.getModFileById(EASY_MAGIC_MOD_ID) != null
                    && loadingModList.getModFileById(APOTHEOSIS_MOD_ID) == null;
        }

        if (ARCANE_ANVIL_JEI_RECIPE_MIXIN.equals(mixinClassName)) {
            // JEI未導入環境では対象クラス自体が存在しないため、optional compat は plugin 側で止める。
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList != null && loadingModList.getModFileById(JEI_MOD_ID) != null;
        }

        if (mixinClassName.startsWith(EPIC_FIGHT_MIXIN_PREFIX)) {
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList != null && loadingModList.getModFileById(EPIC_FIGHT_MOD_ID) != null;
        }

        if (mixinClassName.startsWith(BETTER_COMBAT_MIXIN_PREFIX)) {
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList != null && loadingModList.getModFileById(BETTER_COMBAT_MOD_ID) != null;
        }
        if (mixinClassName.startsWith(EFIS_COMPAT_MIXIN_PREFIX)) {
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList != null && loadingModList.getModFileById(EFIS_COMPAT_MOD_ID) != null;
        }
        if (MALUM_REPLENISHING_OFFHAND_MIXIN.equals(mixinClassName)) {
            var loadingModList = FMLLoader.getLoadingModList();
            return loadingModList != null && loadingModList.getModFileById(MALUM_MOD_ID) != null;
        }
        return true;
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
