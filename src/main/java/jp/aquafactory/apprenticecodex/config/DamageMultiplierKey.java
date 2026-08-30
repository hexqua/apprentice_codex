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
    PRECISION_JACK("precisionJack"),
    GRIND_RUNNER("grindRunner"),
    MANA_SLASH("manaSlash"),
    AUTO_TURRET("autoTurret"),
    ILLUMINATE_STELLAR("illuminateStellar"),
    UNITE_LUNA("uniteLuna"),
    HEALING_BLOOM("healingBloom"),
    SHOCK("shock"),
    SILENT_ASSASSIN("silentAssassin"),
    TIRO_VOLLEY("tiroVolley"),
    MAGIC_SPEAR("magicSpear"),
    FROST_RUNE("frostRune"),
    MYSTIC_SHIELD("mysticShield"),
    BOUND_SWORD("boundSword"),
    INSCRIBE_ICE("inscribeIce"),
    HEAVENLY_FIST("heavenlyFist"),
    LETHAL_ASSAULT("lethalAssault"),
    DUAL_ACROBAT("dualAcrobat"),
    ARTISAN_SMASH("artisanSmash"),
    ANCHOR_BLINK("anchorBlink"),
    TOTEM_OF_PERMAFROST("totemOfPermafrost"),
    FIELD_OVERSEER("fieldOverseer"),
    SERVANT_GAZE("servantGaze"),
    FUJIN("fujin"),
    COMBUSTION_JET("combustion_jet"),
    BLOOD_BRAND("blood_brand"),
    SHIDEN("shiden"),
    ;

    private final String configKey;

    DamageMultiplierKey(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
