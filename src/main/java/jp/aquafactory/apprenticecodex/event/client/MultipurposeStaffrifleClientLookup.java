package jp.aquafactory.apprenticecodex.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * スクロールの動的 component を接続先の registry で復元する。
 */
public final class MultipurposeStaffrifleClientLookup {
    private MultipurposeStaffrifleClientLookup() {
    }

    public static HolderLookup.Provider lookup() {
        var level = Minecraft.getInstance().level;
        return level == null ? RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY) : level.registryAccess();
    }
}
