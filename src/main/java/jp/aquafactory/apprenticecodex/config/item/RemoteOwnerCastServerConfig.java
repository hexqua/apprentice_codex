package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class RemoteOwnerCastServerConfig {
    private final ForgeConfigSpec.BooleanValue enableRemotePlayerGeometry;
    private final ForgeConfigSpec.BooleanValue forceProxyOwnerMagic;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> remotePlayerGeometryDenylist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> remoteOwnerCastDenylist;
    private final ForgeConfigSpec.BooleanValue satelliteFollowcastUsesRemoteOwnerProfiles;
    private final ForgeConfigSpec.BooleanValue chargedTwinBladeStaffUsesRemoteOwnerProfiles;

    private Boolean enableRemotePlayerGeometryOverride;
    private Boolean forceProxyOwnerMagicOverride;
    private List<String> remotePlayerGeometryDenylistOverride;
    private List<String> remoteOwnerCastDenylistOverride;
    private Boolean satelliteFollowcastUsesRemoteOwnerProfilesOverride;
    private Boolean chargedTwinBladeStaffUsesRemoteOwnerProfilesOverride;

    private RemoteOwnerCastServerConfig(
            ForgeConfigSpec.BooleanValue enableRemotePlayerGeometry,
            ForgeConfigSpec.BooleanValue forceProxyOwnerMagic,
            ForgeConfigSpec.ConfigValue<List<? extends String>> remotePlayerGeometryDenylist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> remoteOwnerCastDenylist,
            ForgeConfigSpec.BooleanValue satelliteFollowcastUsesRemoteOwnerProfiles,
            ForgeConfigSpec.BooleanValue chargedTwinBladeStaffUsesRemoteOwnerProfiles
    ) {
        this.enableRemotePlayerGeometry = enableRemotePlayerGeometry;
        this.forceProxyOwnerMagic = forceProxyOwnerMagic;
        this.remotePlayerGeometryDenylist = remotePlayerGeometryDenylist;
        this.remoteOwnerCastDenylist = remoteOwnerCastDenylist;
        this.satelliteFollowcastUsesRemoteOwnerProfiles = satelliteFollowcastUsesRemoteOwnerProfiles;
        this.chargedTwinBladeStaffUsesRemoteOwnerProfiles = chargedTwinBladeStaffUsesRemoteOwnerProfiles;
    }

    public static RemoteOwnerCastServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("RemoteOwnerCast");
        var enableRemotePlayerGeometry = builder
                .comment("Enables remote_player_geometry casts. Disable this to downgrade those profiles to proxy_owner_magic before casting.")
                .define("enableRemotePlayerGeometry", true);
        var forceProxyOwnerMagic = builder
                .comment("Forces remote_player_geometry profiles to run as proxy_owner_magic before casting.")
                .define("forceProxyOwnerMagic", false);
        var remotePlayerGeometryDenylist = builder
                .comment("Spell IDs that should not use remote_player_geometry. Entries use \"modid:path\".")
                .defineList("remotePlayerGeometryDenylist", List.<String>of(), RemoteOwnerCastServerConfig::isSpellId);
        var remoteOwnerCastDenylist = builder
                .comment("Spell IDs blocked for all Remote Owner Cast origins. Entries use \"modid:path\".")
                .defineList("remoteOwnerCastDenylist", List.<String>of(), RemoteOwnerCastServerConfig::isSpellId);
        var satelliteFollowcastUsesRemoteOwnerProfiles = builder
                .comment("Makes Satellite Followcast Amulet prefer Remote Owner Cast profiles before legacy fallbacks.")
                .define("satelliteFollowcastUsesRemoteOwnerProfiles", true);
        var chargedTwinBladeStaffUsesRemoteOwnerProfiles = builder
                .comment("Makes Charged Twin Blade Staff impact casts prefer Remote Owner Cast profiles before legacy fallbacks.")
                .define("chargedTwinBladeStaffUsesRemoteOwnerProfiles", true);
        builder.pop();

        return new RemoteOwnerCastServerConfig(
                enableRemotePlayerGeometry,
                forceProxyOwnerMagic,
                remotePlayerGeometryDenylist,
                remoteOwnerCastDenylist,
                satelliteFollowcastUsesRemoteOwnerProfiles,
                chargedTwinBladeStaffUsesRemoteOwnerProfiles
        );
    }

    public boolean enableRemotePlayerGeometry() {
        return Objects.requireNonNullElseGet(enableRemotePlayerGeometryOverride, enableRemotePlayerGeometry::get);
    }

    public boolean forceProxyOwnerMagic() {
        return Objects.requireNonNullElseGet(forceProxyOwnerMagicOverride, forceProxyOwnerMagic::get);
    }

    public boolean satelliteFollowcastUsesRemoteOwnerProfiles() {
        return Objects.requireNonNullElseGet(
                satelliteFollowcastUsesRemoteOwnerProfilesOverride,
                satelliteFollowcastUsesRemoteOwnerProfiles::get
        );
    }

    public boolean chargedTwinBladeStaffUsesRemoteOwnerProfiles() {
        return Objects.requireNonNullElseGet(
                chargedTwinBladeStaffUsesRemoteOwnerProfilesOverride,
                chargedTwinBladeStaffUsesRemoteOwnerProfiles::get
        );
    }

    public boolean isRemotePlayerGeometrySpellDenied(ResourceLocation spellId) {
        return containsSpellId(remotePlayerGeometryDenylist(), spellId);
    }

    public boolean isRemoteOwnerCastSpellDenied(ResourceLocation spellId) {
        return containsSpellId(remoteOwnerCastDenylist(), spellId);
    }

    public List<String> remotePlayerGeometryDenylist() {
        return Objects.requireNonNullElseGet(remotePlayerGeometryDenylistOverride, () -> remotePlayerGeometryDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public List<String> remoteOwnerCastDenylist() {
        return Objects.requireNonNullElseGet(remoteOwnerCastDenylistOverride, () -> remoteOwnerCastDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public void setForGameTest(
            boolean enableRemotePlayerGeometry,
            boolean forceProxyOwnerMagic,
            List<String> remotePlayerGeometryDenylist,
            List<String> remoteOwnerCastDenylist,
            boolean satelliteFollowcastUsesRemoteOwnerProfiles,
            boolean chargedTwinBladeStaffUsesRemoteOwnerProfiles
    ) {
        this.enableRemotePlayerGeometryOverride = enableRemotePlayerGeometry;
        this.forceProxyOwnerMagicOverride = forceProxyOwnerMagic;
        this.remotePlayerGeometryDenylistOverride = List.copyOf(remotePlayerGeometryDenylist);
        this.remoteOwnerCastDenylistOverride = List.copyOf(remoteOwnerCastDenylist);
        this.satelliteFollowcastUsesRemoteOwnerProfilesOverride = satelliteFollowcastUsesRemoteOwnerProfiles;
        this.chargedTwinBladeStaffUsesRemoteOwnerProfilesOverride = chargedTwinBladeStaffUsesRemoteOwnerProfiles;
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
