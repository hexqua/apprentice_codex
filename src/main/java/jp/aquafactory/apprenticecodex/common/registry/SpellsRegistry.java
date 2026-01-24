package jp.aquafactory.apprenticecodex.common.registry;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.archermultiple.ArcherMultiple;
import jp.aquafactory.apprenticecodex.common.spells.skyedge.SkyEdge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SpellsRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ApprenticeCodex.MODID);

    @SuppressWarnings("unused")
    public static final RegistryObject<AbstractSpell> SKY_EDGE =
            SPELLS.register("sky_edge", SkyEdge::new);

    @SuppressWarnings("unused")
    public static final RegistryObject<AbstractSpell> ARCHER_MULTIPLE =
            SPELLS.register("archer_multiple", ArcherMultiple::new);

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    private SpellsRegistry() {
        // do nothing.
    }
}
