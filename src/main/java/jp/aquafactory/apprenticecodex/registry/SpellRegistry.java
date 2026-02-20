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
import jp.aquafactory.apprenticecodex.spell.manifestationgrimoire.ManifestationGrimoire;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBlade;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArms;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdge;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjack;
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

    public static final RegistryObject<AbstractSpell> SKY_EDGE = reg("sky_edge", SkyEdge::new);
    public static final RegistryObject<AbstractSpell> ARCHER_MULTIPLE = reg("archer_multiple", ArcherMultiple::new);
    public static final RegistryObject<AbstractSpell> COMMENCE_FIRE = reg("commence_fire", CommenceFire::new);
    public static final RegistryObject<AbstractSpell> COMPOUND_PHIAL = reg("compound_phial", CompoundPhial::new);
    public static final RegistryObject<AbstractSpell> QUICK_ARMS = reg("quick_arms", QuickArms::new);
    public static final RegistryObject<AbstractSpell> BREACHING_ENEMY = reg("breaching_enemy", BreachingEnemy::new);
    public static final RegistryObject<AbstractSpell> BULLET_STREAM = reg("bullet_stream", BulletStream::new);
    public static final RegistryObject<AbstractSpell> ARCANE_BLAST = reg("arcane_blast", ArcaneBlast::new);
    public static final RegistryObject<AbstractSpell> ARCANE_BEAM = reg("arcane_beam", ArcaneBeam::new);
    public static final RegistryObject<AbstractSpell> TINY_LUMBERJACK = reg("tiny_lumberjack", TinyLumberjack::new);
    public static final RegistryObject<AbstractSpell> GRACED_RAIN = reg("graced_rain", GracedRain::new);
    public static final RegistryObject<AbstractSpell> MAGE_LIGHT = reg("mage_light", MageLight::new);
    public static final RegistryObject<AbstractSpell> PERSONAL_SHELF = reg("personal_shelf", PersonalShelf::new);
    public static final RegistryObject<AbstractSpell> FLY_SWATTER = reg("fly_swatter", FlySwatter::new);
    public static final RegistryObject<AbstractSpell> ASSIST_WINGS = reg("assist_wings", AssistWings::new);
    public static final RegistryObject<AbstractSpell> WORLD_FLATTER = reg("world_flatter", WorldFlatter::new);
    public static final RegistryObject<AbstractSpell> SLASH_BLADE = reg("slash_blade", SlashBlade::new);
    public static final RegistryObject<AbstractSpell> MANIFESTATION_GRIMOIRE = reg("manifestation_grimoire", ManifestationGrimoire::new);

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }
}
