package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class DemicreatorWingsServerConfig {
    private final SpellDimensionRestrictionServerConfig dimensionRestriction;

    private DemicreatorWingsServerConfig(SpellDimensionRestrictionServerConfig dimensionRestriction) {
        this.dimensionRestriction = dimensionRestriction;
    }

    public static DemicreatorWingsServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("DemicreatorWings");
        var dimensionRestriction = SpellDimensionRestrictionServerConfig.define(
                builder,
                "Demicreator Wings",
                "grant flight"
        );
        builder.pop();
        return new DemicreatorWingsServerConfig(dimensionRestriction);
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
