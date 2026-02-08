package jp.aquafactory.apprenticecodex.common.registry;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.arcanebeam.ArcaneBeam;
import jp.aquafactory.apprenticecodex.common.spells.arcaneblast.ArcaneBlast;
import jp.aquafactory.apprenticecodex.common.spells.archermultiple.ArcherMultiple;
import jp.aquafactory.apprenticecodex.common.spells.breachingenemy.BreachingEnemy;
import jp.aquafactory.apprenticecodex.common.spells.bulletstream.BulletStream;
import jp.aquafactory.apprenticecodex.common.spells.commencefire.CommenceFire;
import jp.aquafactory.apprenticecodex.common.spells.compoundphial.CompoundPhial;
import jp.aquafactory.apprenticecodex.common.spells.quickarms.QuickArms;
import jp.aquafactory.apprenticecodex.common.spells.skyedge.SkyEdge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SpellsRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ApprenticeCodex.MODID);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> SKY_EDGE =
            SPELLS.register("sky_edge", SkyEdge::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> ARCHER_MULTIPLE =
            SPELLS.register("archer_multiple", ArcherMultiple::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> COMMENCE_FIRE =
            SPELLS.register("commence_fire", CommenceFire::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> COMPOUND_PHIAL =
            SPELLS.register("compound_phial", CompoundPhial::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> QUICK_ARMS =
            SPELLS.register("quick_arms", QuickArms::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> BREACHING_ENEMY =
            SPELLS.register("breaching_enemy", BreachingEnemy::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> BULLET_STREAM =
            SPELLS.register("bullet_stream", BulletStream::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> ARCANE_BLAST =
            SPELLS.register("arcane_blast", ArcaneBlast::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> ARCANE_BEAM =
            SPELLS.register("arcane_beam", ArcaneBeam::new);

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    private SpellsRegistry() {
        // do nothing.
    }
}
