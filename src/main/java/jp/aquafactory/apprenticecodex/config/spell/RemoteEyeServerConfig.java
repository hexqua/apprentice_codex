package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class RemoteEyeServerConfig {
    private final SpellDimensionRestrictionServerConfig dimensionRestriction;

    private RemoteEyeServerConfig(SpellDimensionRestrictionServerConfig dimensionRestriction) {
        this.dimensionRestriction = dimensionRestriction;
    }

    public static RemoteEyeServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("RemoteEye");
        var dimensionRestriction = SpellDimensionRestrictionServerConfig.define(
                builder,
                "Remote Eye",
                "detach vision"
        );
        builder.pop();
        return new RemoteEyeServerConfig(dimensionRestriction);
    }

    public boolean isDimensionAllowed(ResourceLocation dimensionId) {
        return dimensionRestriction.isDimensionAllowed(dimensionId);
    }

    public List<String> dimensionDenylist() {
        return dimensionRestriction.dimensionDenylist();
    }

    public boolean enableDimensionAllowlist() {
        return dimensionRestriction.enableDimensionAllowlist();
    }

    public List<String> dimensionAllowlist() {
        return dimensionRestriction.dimensionAllowlist();
    }

    public void setForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        dimensionRestriction.setForGameTest(dimensionDenylist, enableDimensionAllowlist, dimensionAllowlist);
    }
}
