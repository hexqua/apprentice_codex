package jp.aquafactory.apprenticecodex.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeam;
import jp.aquafactory.apprenticecodex.spell.arcaneblast.ArcaneBlast;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultiple;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWings;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemy;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStream;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFire;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhial;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatter;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRain;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArms;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdge;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class SpellRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPELL_REGISTRY_KEY, ApprenticeCodex.MODID);

    public static final RegistryObject<AbstractSpell> SKY_EDGE =
            SPELLS.register("sky_edge", SkyEdge::new);

    public static final RegistryObject<AbstractSpell> ARCHER_MULTIPLE =
            SPELLS.register("archer_multiple", ArcherMultiple::new);

    public static final RegistryObject<AbstractSpell> COMMENCE_FIRE =
            SPELLS.register("commence_fire", CommenceFire::new);

    public static final RegistryObject<AbstractSpell> COMPOUND_PHIAL =
            SPELLS.register("compound_phial", CompoundPhial::new);

    public static final RegistryObject<AbstractSpell> QUICK_ARMS =
            SPELLS.register("quick_arms", QuickArms::new);

    public static final RegistryObject<AbstractSpell> BREACHING_ENEMY =
            SPELLS.register("breaching_enemy", BreachingEnemy::new);

    public static final RegistryObject<AbstractSpell> BULLET_STREAM =
            SPELLS.register("bullet_stream", BulletStream::new);

    public static final RegistryObject<AbstractSpell> ARCANE_BLAST =
            SPELLS.register("arcane_blast", ArcaneBlast::new);

    public static final RegistryObject<AbstractSpell> ARCANE_BEAM =
            SPELLS.register("arcane_beam", ArcaneBeam::new);

    public static final RegistryObject<AbstractSpell> TINY_LUMBERJACK =
            SPELLS.register("tiny_lumberjack", TinyLumberjack::new);

    public static final RegistryObject<AbstractSpell> GRACED_RAIN =
            SPELLS.register("graced_rain", GracedRain::new);

    public static final RegistryObject<AbstractSpell> MAGE_LIGHT =
            SPELLS.register("mage_light", MageLight::new);

    public static final RegistryObject<AbstractSpell> PERSONAL_SHELF =
            SPELLS.register("personal_shelf", PersonalShelf::new);

    public static final RegistryObject<AbstractSpell> FLY_SWATTER =
            SPELLS.register("fly_swatter", FlySwatter::new);

    public static final RegistryObject<AbstractSpell> ASSIST_WINGS =
            SPELLS.register("assist_wings", AssistWings::new);

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    private SpellRegistry() {
        // do nothing.
    }
}
