package jp.aquafactory.apprenticecodex.registry;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CreativeTabRegistry {
    private CreativeTabRegistry() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ApprenticeCodex.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN =
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
        output.accept(ItemRegistry.SPELLCASTER_WORKBENCH.get());
        output.accept(ItemRegistry.ARCANUM_IN_A_JAR.get());
        output.accept(ItemRegistry.ESSENCE_SMOKER.get());
        output.accept(ItemRegistry.RAPID_SPELLCASTER_ROUND.get());
        output.accept(ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get());
        output.accept(ItemRegistry.BASIC_SPELLCASTER_ROUND.get());
        output.accept(ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get());
        output.accept(ItemRegistry.ARCANE_SPELLCASTER_ROUND.get());
        output.accept(ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get());
        output.accept(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get());
        output.accept(ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get());
        output.accept(ItemRegistry.SPELL_DOMINATOR_ROUND.get());
        output.accept(ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get());
        output.accept(ItemRegistry.IRON_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.COPPER_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.GOLD_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.GOLD_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.PHOTON_SIPHON.get());
        output.accept(ItemRegistry.PASTEL_STAFF.get());
        output.accept(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_SCARF.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_BOOTS.get());
        output.accept(ItemRegistry.ENCHANTRESS_HAT.get());
        output.accept(ItemRegistry.ENCHANTRESS_ROBE.get());
        output.accept(ItemRegistry.ENCHANTRESS_LEGGINGS.get());
        output.accept(ItemRegistry.ENCHANTRESS_BOOTS.get());
        output.accept(ItemRegistry.GRIMOIRE_MANIFEST.get());
        output.accept(ItemRegistry.SCARLET_THIRST.get());
        output.accept(ItemRegistry.CRAFTSMANS_DELIGHT.get());
        output.accept(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get());
        output.accept(ItemRegistry.SPELLCASTER_AMMO_POUCH.get());
        output.accept(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get());
        output.accept(ItemRegistry.ENDER_GRIMOIRE.get());
        output.accept(ItemRegistry.EXPLORERS_CODEX.get());
        output.accept(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get());
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
