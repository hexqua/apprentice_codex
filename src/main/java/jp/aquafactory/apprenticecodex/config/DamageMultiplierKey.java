package jp.aquafactory.apprenticecodex.config;

public enum DamageMultiplierKey {
    SKY_EDGE("skyEdge"),
    ARCANE_BLAST("arcaneBlast"),
    ARCHER_MULTIPLE("archerMultiple"),
    BREACHING_ENEMY("breachingEnemy"),
    ARCANE_BEAM("arcaneBeam"),
    BULLET_STREAM("bulletStream"),
    HIGANBANA("higanbana"),
    WORLD_FLATTER("worldFlatter"),
    FLY_SWATTER("flySwatter"),
    COMPOUND_PHIAL("compoundPhial"),
    COMMENCE_FIRE("commenceFire"),
    PHALANX_CHARGE("phalanxCharge"),
    SLASH_BLADE("slashBlade"),
    THERMAL_PROCESS("thermalProcess"),
    MANTIS_LEAP("mantisLeap"),
    FEATHER_RUSH("featherRush"),
    TINY_LUMBERJACK("tinyLumberjack"),
    MOON_LIGHT("moonLight"),
    QUICK_ARMS("quickArms"),
    MANA_CHARGE("manaCharge"),
    FORCE_FIELD("forceField");

    private final String configKey;

    DamageMultiplierKey(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
