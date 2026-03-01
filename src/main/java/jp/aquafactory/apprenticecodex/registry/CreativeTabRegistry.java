package jp.aquafactory.apprenticecodex.registry;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTabRegistry {
    private CreativeTabRegistry() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ApprenticeCodex.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ApprenticeCodex.MODID + ".main"))
                    .icon(() -> new ItemStack(ItemRegistry.APPRENTICE_DESK.get()))
                    .displayItems(CreativeTabRegistry::addItemsToTab)
                    .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private static void addItemsToTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        output.accept(ItemRegistry.APPRENTICE_DESK.get());
        output.accept(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.GOLD_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.PHOTON_SIPHON.get());
        output.accept(ItemRegistry.PASTEL_STAFF.get());
        output.accept(ItemRegistry.GRIMOIRE_MANIFEST.get());
        output.accept(ItemRegistry.SCARLET_THIRST.get());
        output.accept(ItemRegistry.CRAFTSMANS_DELIGHT.get());
        output.accept(ItemRegistry.ENDER_GRIMOIRE.get());
        addSpellScrollsToTab(output);
    }

    private static void addSpellScrollsToTab(CreativeModeTab.Output output) {
        for (var spell : io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells()) {
            var spellResource = spell.getSpellResource();
            if (spellResource == null || !ApprenticeCodex.MODID.equals(spellResource.getNamespace())) {
                continue;
            }

            for (var level = spell.getMinLevel(); level <= spell.getMaxLevel(); ++level) {
                var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell, level, scrollStack);
                output.accept(scrollStack);
            }
        }
    }
}

