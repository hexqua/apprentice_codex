package jp.aquafactory.apprenticecodex.common.registry;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.TestSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SpellsRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ApprenticeCodex.MODID);

    // Sample Spell.
    public static final RegistryObject<AbstractSpell> TEST_SPELL =
            SPELLS.register("test_spell", TestSpell::new);

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    private SpellsRegistry() {
        // do nothing.
    }
}
