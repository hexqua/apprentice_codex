package jp.aquafactory.apprenticecodex.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeam;
import jp.aquafactory.apprenticecodex.spell.arcaneblast.ArcaneBlast;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultiple;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWings;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnet;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurret;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBow;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSword;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemy;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStream;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFire;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunk;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhial;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensor;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWings;
import jp.aquafactory.apprenticecodex.spell.divinepossession.DivinePossession;
import jp.aquafactory.apprenticecodex.spell.earthforge.EarthForge;
import jp.aquafactory.apprenticecodex.spell.echocast.EchoCast;
import jp.aquafactory.apprenticecodex.spell.extract.Extract;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRush;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatter;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceField;
import jp.aquafactory.apprenticecodex.spell.frostrune.FrostRune;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRain;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunner;
import jp.aquafactory.apprenticecodex.spell.harvestmoon.HarvestMoon;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloom;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFist;
import jp.aquafactory.apprenticecodex.spell.higanbana.Higanbana;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellar;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIce;
import jp.aquafactory.apprenticecodex.spell.longstride.LongStride;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpear;
import jp.aquafactory.apprenticecodex.spell.manacharge.ManaCharge;
import jp.aquafactory.apprenticecodex.spell.manifestationgrimoire.ManifestationGrimoire;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeap;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlash;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidance;
import jp.aquafactory.apprenticecodex.spell.mistform.MistForm;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLight;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShield;
import jp.aquafactory.apprenticecodex.spell.paletteshift.PaletteShift;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxCharge;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJack;
import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEye;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeacon;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.shock.Shock;
import jp.aquafactory.apprenticecodex.spell.silentassassin.SilentAssassin;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBlade;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArms;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHole;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdge;
import jp.aquafactory.apprenticecodex.spell.spectralwing.SpectralWing;
import jp.aquafactory.apprenticecodex.spell.tamerspocket.TamersPocket;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcess;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjack;
import jp.aquafactory.apprenticecodex.spell.tirovolley.TiroVolley;
import jp.aquafactory.apprenticecodex.spell.treasuredivination.TreasureDivination;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLuna;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatter;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@SuppressWarnings("unused") // 参照しない魔法があっても警告を抑制.
public final class SpellRegistry {
    private SpellRegistry() {}
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, ApprenticeCodex.MODID);

    private static RegistryObject<AbstractSpell> reg(String id, Supplier<? extends AbstractSpell> factory) {
        return SPELLS.register(id, factory);
    }

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    // 血.
    public static final RegistryObject<AbstractSpell> HIGANBANA = reg("higanbana", Higanbana::new);
    public static final RegistryObject<AbstractSpell> MIST_FORM = reg("mist_form", MistForm::new);

    // エンダー.
    public static final RegistryObject<AbstractSpell> ARCANE_BLAST = reg("arcane_blast", ArcaneBlast::new);
    public static final RegistryObject<AbstractSpell> ARCANE_BEAM = reg("arcane_beam", ArcaneBeam::new);
    public static final RegistryObject<AbstractSpell> PERSONAL_SHELF = reg("personal_shelf", PersonalShelf::new);
    public static final RegistryObject<AbstractSpell> ASSIST_WINGS = reg("assist_wings", AssistWings::new);
    public static final RegistryObject<AbstractSpell> MANIFESTATION_GRIMOIRE = reg("manifestation_grimoire", ManifestationGrimoire::new);
    public static final RegistryObject<AbstractSpell> MANTIS_LEAP = reg("mantis_leap", MantisLeap::new);
    public static final RegistryObject<AbstractSpell> AUTO_MAGNET = reg("auto_magnet", AutoMagnet::new);
    public static final RegistryObject<AbstractSpell> REMOTE_EYE = reg("remote_eye", RemoteEye::new);
    public static final RegistryObject<AbstractSpell> MANA_SLASH = reg("mana_slash", ManaSlash::new);
    public static final RegistryObject<AbstractSpell> LONG_STRIDE = reg("long_stride", LongStride::new);
    public static final RegistryObject<AbstractSpell> RIFT_HOLE = reg("rift_hole", RiftHole::new);
    public static final RegistryObject<AbstractSpell> DEMICREATOR_WINGS = reg("demicreator_wings", DemicreatorWings::new);
    public static final RegistryObject<AbstractSpell> MIRAGE_AVOIDANCE = reg("mirage_avoidance", MirageAvoidance::new);

    // 召喚.
    public static final RegistryObject<AbstractSpell> ARCHER_MULTIPLE = reg("archer_multiple", ArcherMultiple::new);
    public static final RegistryObject<AbstractSpell> FEATHER_RUSH = reg("feather_rush", FeatherRush::new);
    public static final RegistryObject<AbstractSpell> SLASH_BLADE = reg("slash_blade", SlashBlade::new);
    public static final RegistryObject<AbstractSpell> PRECISION_JACK = reg("precision_jack", PrecisionJack::new);
    public static final RegistryObject<AbstractSpell> AUTO_TURRET = reg("auto_turret", AutoTurret::new);
    public static final RegistryObject<AbstractSpell> COMPANION_TRUNK = reg("companion_trunk", CompanionTrunk::new);
    public static final RegistryObject<AbstractSpell> SEARCH_BEACON = reg("search_beacon", SearchBeacon::new);
    public static final RegistryObject<AbstractSpell> TAMERS_POCKET = reg("tamers_pocket", TamersPocket::new);
    public static final RegistryObject<AbstractSpell> SILENT_ASSASSIN = reg("silent_assassin", SilentAssassin::new);
    public static final RegistryObject<AbstractSpell> TIRO_VOLLEY = reg("tiro_volley", TiroVolley::new);
    public static final RegistryObject<AbstractSpell> BOUND_SWORD = reg("bound_sword", BoundSword::new);
    public static final RegistryObject<AbstractSpell> BOUND_BOW = reg("bound_bow", BoundBow::new);

    // 炎.
    public static final RegistryObject<AbstractSpell> THERMAL_PROCESS = reg("thermal_process", ThermalProcess::new);
    public static final RegistryObject<AbstractSpell> MAGIC_SPEAR = reg("magic_spear", MagicSpear::new);

    // 聖.
    public static final RegistryObject<AbstractSpell> MAGE_LIGHT = reg("mage_light", MageLight::new);
    public static final RegistryObject<AbstractSpell> PHALANX_CHARGE = reg("phalanx_charge", PhalanxCharge::new);
    public static final RegistryObject<AbstractSpell> MANA_CHARGE = reg("mana_charge", ManaCharge::new);
    public static final RegistryObject<AbstractSpell> FORCE_FIELD = reg("force_field", ForceField::new);
    public static final RegistryObject<AbstractSpell> SENSE_EVIL = reg("sense_evil", SenseEvil::new);
    public static final RegistryObject<AbstractSpell> ILLUMINATE_STELLAR = reg("illuminate_stellar", IlluminateStellar::new);
    public static final RegistryObject<AbstractSpell> UNITE_LUNA = reg("unite_luna", UniteLuna::new);
    public static final RegistryObject<AbstractSpell> MYSTIC_SHIELD = reg("mystic_shield", MysticShield::new);
    public static final RegistryObject<AbstractSpell> DIVINE_POSSESSION = reg("divine_possession", DivinePossession::new);

    // 氷.
    public static final RegistryObject<AbstractSpell> FROST_RUNE = reg("frost_rune", FrostRune::new);
    public static final RegistryObject<AbstractSpell> INSCRIBE_ICE = reg("inscribe_ice", InscribeIce::new);

    // 雷.
    public static final RegistryObject<AbstractSpell> SKY_EDGE = reg("sky_edge", SkyEdge::new);
    public static final RegistryObject<AbstractSpell> COMMENCE_FIRE = reg("commence_fire", CommenceFire::new);
    public static final RegistryObject<AbstractSpell> QUICK_ARMS = reg("quick_arms", QuickArms::new);
    public static final RegistryObject<AbstractSpell> BREACHING_ENEMY = reg("breaching_enemy", BreachingEnemy::new);
    public static final RegistryObject<AbstractSpell> BULLET_STREAM = reg("bullet_stream", BulletStream::new);
    public static final RegistryObject<AbstractSpell> FLY_SWATTER = reg("fly_swatter", FlySwatter::new);
    public static final RegistryObject<AbstractSpell> SHOCK = reg("shock", Shock::new);

    // 自然.
    public static final RegistryObject<AbstractSpell> COMPOUND_PHIAL = reg("compound_phial", CompoundPhial::new);
    public static final RegistryObject<AbstractSpell> TINY_LUMBERJACK = reg("tiny_lumberjack", TinyLumberjack::new);
    public static final RegistryObject<AbstractSpell> GRACED_RAIN = reg("graced_rain", GracedRain::new);
    public static final RegistryObject<AbstractSpell> WORLD_FLATTER = reg("world_flatter", WorldFlatter::new);
    public static final RegistryObject<AbstractSpell> EARTH_FORGE = reg("earth_forge", EarthForge::new);
    public static final RegistryObject<AbstractSpell> GRIND_RUNNER = reg("grind_runner", GrindRunner::new);
    public static final RegistryObject<AbstractSpell> TREASURE_DIVINATION = reg("treasure_divination", TreasureDivination::new);
    public static final RegistryObject<AbstractSpell> HEALING_BLOOM = reg("healing_bloom", HealingBloom::new);
    public static final RegistryObject<AbstractSpell> HARVEST_MOON = reg("harvest_moon", HarvestMoon::new);
    public static final RegistryObject<AbstractSpell> EXTRACT = reg("extract", Extract::new);
    public static final RegistryObject<AbstractSpell> HEAVENLY_FIST = reg("heavenly_fist", HeavenlyFist::new);

    // エルドリッチ.
    public static final RegistryObject<AbstractSpell> PALETTE_SHIFT = reg("palette_shift", PaletteShift::new);
    public static final RegistryObject<AbstractSpell> MOON_LIGHT = reg("moon_light", MoonLight::new);
    public static final RegistryObject<AbstractSpell> DEEP_SENSOR = reg("deep_sensor", DeepSensor::new);
    public static final RegistryObject<AbstractSpell> SPECTRAL_WING = reg("spectral_wing", SpectralWing::new);
    public static final RegistryObject<AbstractSpell> ECHO_CAST = reg("echo_cast", EchoCast::new);
}
