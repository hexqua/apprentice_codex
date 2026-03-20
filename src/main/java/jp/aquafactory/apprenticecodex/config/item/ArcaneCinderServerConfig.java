package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArcaneCinderServerConfig {
    private final ModConfigSpec.BooleanValue limitArcaneCinderSpeedupToVanillaFurnaces;

    private ArcaneCinderServerConfig(ModConfigSpec.BooleanValue limitArcaneCinderSpeedupToVanillaFurnaces) {
        this.limitArcaneCinderSpeedupToVanillaFurnaces = limitArcaneCinderSpeedupToVanillaFurnaces;
    }

    public static ArcaneCinderServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ArcaneCinder");
        var limitArcaneCinderSpeedupToVanillaFurnaces =
                builder.comment(
                                "Limit Arcane Cinder speedup to vanilla furnace block entities only.",
                                "If disabled, behavior on non-vanilla AbstractFurnaceBlockEntity implementations is not guaranteed."
                        )
                        .define("limitArcaneCinderSpeedupToVanillaFurnaces", true);
        builder.pop();

        return new ArcaneCinderServerConfig(limitArcaneCinderSpeedupToVanillaFurnaces);
    }

    public boolean limitArcaneCinderSpeedupToVanillaFurnaces() {
        return limitArcaneCinderSpeedupToVanillaFurnaces.get();
    }
}
