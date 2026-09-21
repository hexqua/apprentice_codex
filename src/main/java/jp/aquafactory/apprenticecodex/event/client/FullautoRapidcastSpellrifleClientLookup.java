package jp.aquafactory.apprenticecodex.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;

/**
 * スクロールの動的 component を接続先の registry で復元する。
 */
public final class FullautoRapidcastSpellrifleClientLookup {
    private FullautoRapidcastSpellrifleClientLookup() {
    }

    public static HolderLookup.Provider lookup() {
        var level = Minecraft.getInstance().level;
        return level == null ? RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY) : level.registryAccess();
    }
}
