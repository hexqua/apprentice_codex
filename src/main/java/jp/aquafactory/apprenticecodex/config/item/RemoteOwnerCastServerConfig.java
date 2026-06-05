package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class RemoteOwnerCastServerConfig {
    private final ModConfigSpec.BooleanValue enableRemotePlayerGeometry;
    private final ModConfigSpec.ConfigValue<List<? extends String>> remoteOwnerCastDenylist;

    private Boolean enableRemotePlayerGeometryOverride;
    private List<String> remoteOwnerCastDenylistOverride;

    private RemoteOwnerCastServerConfig(
            ModConfigSpec.BooleanValue enableRemotePlayerGeometry,
            ModConfigSpec.ConfigValue<List<? extends String>> remoteOwnerCastDenylist
    ) {
        this.enableRemotePlayerGeometry = enableRemotePlayerGeometry;
        this.remoteOwnerCastDenylist = remoteOwnerCastDenylist;
    }

    public static RemoteOwnerCastServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("RemoteOwnerCast");
        var enableRemotePlayerGeometry = builder
                .comment("Enables remote_player_geometry casts. When disabled, those profiles stay imbuable but fail at cast time.")
                .define("enableRemotePlayerGeometry", true);
        var remoteOwnerCastDenylist = builder
                .comment("Spell IDs blocked at Remote Owner Cast execution time. Entries use \"modid:path\".")
                .defineListAllowEmpty("remoteOwnerCastDenylist", List.<String>of(), RemoteOwnerCastServerConfig::isSpellId);
        builder.pop();

        return new RemoteOwnerCastServerConfig(
                enableRemotePlayerGeometry,
                remoteOwnerCastDenylist
        );
    }

    public boolean enableRemotePlayerGeometry() {
        return Objects.requireNonNullElseGet(enableRemotePlayerGeometryOverride, enableRemotePlayerGeometry::get);
    }

    public boolean isRemoteOwnerCastSpellDenied(ResourceLocation spellId) {
        return containsSpellId(remoteOwnerCastDenylist(), spellId);
    }

    public List<String> remoteOwnerCastDenylist() {
        return Objects.requireNonNullElseGet(remoteOwnerCastDenylistOverride, () -> remoteOwnerCastDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public void setForGameTest(
            boolean enableRemotePlayerGeometry,
            List<String> remoteOwnerCastDenylist
    ) {
        this.enableRemotePlayerGeometryOverride = enableRemotePlayerGeometry;
        this.remoteOwnerCastDenylistOverride = List.copyOf(remoteOwnerCastDenylist);
    }

    private static boolean containsSpellId(List<String> configuredIds, ResourceLocation spellId) {
        if (spellId == null) {
            return false;
        }
        for (var configuredId : configuredIds) {
            if (spellId.equals(ResourceLocation.tryParse(configuredId))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
