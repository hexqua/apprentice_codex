package jp.aquafactory.apprenticecodex.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeam;
import jp.aquafactory.apprenticecodex.spell.arcaneblast.ArcaneBlast;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultiple;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWings;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnet;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemy;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStream;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFire;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhial;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensor;
import jp.aquafactory.apprenticecodex.spell.earthforge.EarthForge;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRush;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatter;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceField;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRain;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunner;
import jp.aquafactory.apprenticecodex.spell.higanbana.Higanbana;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import jp.aquafactory.apprenticecodex.spell.manacharge.ManaCharge;
import jp.aquafactory.apprenticecodex.spell.manifestationgrimoire.ManifestationGrimoire;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeap;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLight;
import jp.aquafactory.apprenticecodex.spell.paletteshift.PaletteShift;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxCharge;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJack;
import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEye;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvil;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBlade;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArms;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdge;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcess;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjack;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@SuppressWarnings("unused") // 参照しない魔法があっても警告を抑制.
public final class SpellRegistry {
    private SpellRegistry() {}
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, ApprenticeCodex.MODID);

    private static DeferredHolder<AbstractSpell, AbstractSpell> reg(String id, Supplier<? extends AbstractSpell> factory) {
        return SPELLS.register(id, factory);
    }

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    // 血.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HIGANBANA = reg("higanbana", Higanbana::new);

    // エンダー.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ARCANE_BLAST = reg("arcane_blast", ArcaneBlast::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ARCANE_BEAM = reg("arcane_beam", ArcaneBeam::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PERSONAL_SHELF = reg("personal_shelf", PersonalShelf::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ASSIST_WINGS = reg("assist_wings", AssistWings::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANIFESTATION_GRIMOIRE = reg("manifestation_grimoire", ManifestationGrimoire::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANTIS_LEAP = reg("mantis_leap", MantisLeap::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> AUTO_MAGNET = reg("auto_magnet", AutoMagnet::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> REMOTE_EYE = reg("remote_eye", RemoteEye::new);

    // 召喚.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> ARCHER_MULTIPLE = reg("archer_multiple", ArcherMultiple::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FEATHER_RUSH = reg("feather_rush", FeatherRush::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SLASH_BLADE = reg("slash_blade", SlashBlade::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PRECISION_JACK = reg("precision_jack", PrecisionJack::new);

    // 炎.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> THERMAL_PROCESS = reg("thermal_process", ThermalProcess::new);

    // 聖.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MAGE_LIGHT = reg("mage_light", MageLight::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PHALANX_CHARGE = reg("phalanx_charge", PhalanxCharge::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MANA_CHARGE = reg("mana_charge", ManaCharge::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FORCE_FIELD = reg("force_field", ForceField::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SENSE_EVIL = reg("sense_evil", SenseEvil::new);

    // 氷.
    // まだなし...

    // 雷.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SKY_EDGE = reg("sky_edge", SkyEdge::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COMMENCE_FIRE = reg("commence_fire", CommenceFire::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> QUICK_ARMS = reg("quick_arms", QuickArms::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BREACHING_ENEMY = reg("breaching_enemy", BreachingEnemy::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BULLET_STREAM = reg("bullet_stream", BulletStream::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FLY_SWATTER = reg("fly_swatter", FlySwatter::new);


    // 自然.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COMPOUND_PHIAL = reg("compound_phial", CompoundPhial::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TINY_LUMBERJACK = reg("tiny_lumberjack", TinyLumberjack::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GRACED_RAIN = reg("graced_rain", GracedRain::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WORLD_FLATTER = reg("world_flatter", WorldFlatter::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EARTH_FORGE = reg("earth_forge", EarthForge::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GRIND_RUNNER = reg("grind_runner", GrindRunner::new);

    // エルドリッチ.
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PALETTE_SHIFT = reg("palette_shift", PaletteShift::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MOON_LIGHT = reg("moon_light", MoonLight::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> DEEP_SENSOR = reg("deep_sensor", DeepSensor::new);
}

