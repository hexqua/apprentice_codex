package jp.aquafactory.apprenticecodex.registry;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;

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
        bus.addListener(CreativeTabRegistry::filterHiddenAffinityPotions);
    }

    private static void addItemsToTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        output.accept(ItemRegistry.APPRENTICE_DESK.get());
        output.accept(ItemRegistry.SPELLCASTER_WORKBENCH.get());
        output.accept(ItemRegistry.SPELL_DISPENSER.get());
        output.accept(ItemRegistry.ARCANUM_IN_A_JAR.get());
        output.accept(ItemRegistry.ESSENCE_SMOKER.get());
        output.accept(ItemRegistry.ATELIER_STATION.get());
        output.accept(ItemRegistry.ARCANE_CINDER.get());
        output.accept(ItemRegistry.COMFORT_BERRIES.get());
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
        output.accept(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get());
        output.accept(ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get());
        output.accept(ItemRegistry.IRON_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.COPPER_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.GOLD_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get());
        output.accept(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.GOLD_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.SILVER_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get());
        output.accept(ItemRegistry.IRON_SWINGCAST_STAFF.get());
        output.accept(ItemRegistry.COPPER_SWINGCAST_STAFF.get());
        output.accept(ItemRegistry.GOLD_SWINGCAST_STAFF.get());
        output.accept(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get());
        output.accept(ItemRegistry.SILVER_SWINGCAST_STAFF.get());
        output.accept(ItemRegistry.NETHERITE_SWINGCAST_STAFF.get());
        output.accept(ItemRegistry.PHOTON_SIPHON.get());
        output.accept(ItemRegistry.EXPLORERS_CANE.get());
        output.accept(ItemRegistry.SPELLCASTERS_FLASK.get());
        output.accept(ItemRegistry.ALCHEMISTS_FLASK.get());
        output.accept(ItemRegistry.PASTEL_STAFF.get());
        output.accept(ItemRegistry.FOCUS_STAFFBOW.get());
        output.accept(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        output.accept(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        output.accept(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        output.accept(ItemRegistry.MANA_FORCE_BLADE.get());
        output.accept(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
        output.accept(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get());
        output.accept(ItemRegistry.UNITE_LUNA_STAFF.get());
        output.accept(ItemRegistry.ELEMENTAL_BOW.get());
        output.accept(ItemRegistry.REFLECTCAST_SHIELD.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_SCARF.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_TORSO.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get());
        output.accept(ItemRegistry.APPRENTICE_MAGE_BOOTS.get());
        output.accept(ItemRegistry.ENCHANTRESS_HAT.get());
        output.accept(ItemRegistry.ENCHANTRESS_ROBE.get());
        output.accept(ItemRegistry.ENCHANTRESS_LEGGINGS.get());
        output.accept(ItemRegistry.ENCHANTRESS_BOOTS.get());
        output.accept(ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get());
        output.accept(ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get());
        output.accept(ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get());
        output.accept(ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get());
        output.accept(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get());
        output.accept(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
        output.accept(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
        output.accept(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
        output.accept(ItemRegistry.GRIMOIRE_MANIFEST.get());
        output.accept(ItemRegistry.SCARLET_THIRST.get());
        output.accept(ItemRegistry.CRAFTSMANS_DELIGHT.get());
        output.accept(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get());
        output.accept(ItemRegistry.SPELLCASTER_AMMO_POUCH.get());
        output.accept(ItemRegistry.SPELLCASTER_QUIVER.get());
        output.accept(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get());
        output.accept(ItemRegistry.AUTOCAST_AMULET.get().getDefaultInstance());
        output.accept(ItemRegistry.ASHEN_CIRCLET.get());
        output.accept(ItemRegistry.ENCHANTED_CIRCLET.get());
        output.accept(ItemRegistry.MANA_SHIELD_CHARM.get());
        output.accept(ItemRegistry.ENDER_GRIMOIRE.get());
        output.accept(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
        output.accept(ItemRegistry.EXPLORERS_CODEX.get());
        output.accept(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
        output.accept(ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get());
        output.accept(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get());
        addSpellScrollsToTab(output);
    }

    private static void addSpellScrollsToTab(CreativeModeTab.Output output) {
        for (var spell : getCreativeTabSpells()) {
            for (var level = spell.getMinLevel(); level <= spell.getMaxLevel(); ++level) {
                var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell, level, scrollStack);
                output.accept(scrollStack);
            }
        }
    }

    // School ごとに固めておくと、登録順が崩れても creative tab 上で魔法が混ざりにくい。
    public static List<AbstractSpell> getCreativeTabSpells() {
        var schoolOrder = new LinkedHashMap<ResourceLocation, Integer>();
        var orderIndex = 0;
        for (var schoolType : io.redspace.ironsspellbooks.api.registry.SchoolRegistry.REGISTRY.get().getValues()) {
            schoolOrder.putIfAbsent(schoolType.getId(), orderIndex++);
        }

        return io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells().stream()
                .filter(CreativeTabRegistry::isApprenticeSpell)
                .sorted(java.util.Comparator.comparingInt(spell -> resolveSchoolOrderIndex(spell, schoolOrder)))
                .toList();
    }

    private static boolean isApprenticeSpell(AbstractSpell spell) {
        var spellResource = spell.getSpellResource();
        return spellResource != null && ApprenticeCodex.MODID.equals(spellResource.getNamespace());
    }

    private static int resolveSchoolOrderIndex(AbstractSpell spell, LinkedHashMap<ResourceLocation, Integer> schoolOrder) {
        var schoolType = spell.getSchoolType();
        if (schoolType == null) {
            return Integer.MAX_VALUE;
        }
        return schoolOrder.getOrDefault(schoolType.getId(), Integer.MAX_VALUE);
    }

    private static void filterHiddenAffinityPotions(BuildCreativeModeTabContentsEvent event) {
        // 固定スロット登録の副作用で未割当の親和ポーション候補が自動露出するため、ここで除外する.
        var iterator = event.getEntries().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (shouldHideFromCreativeTab(entry.getKey())) {
                iterator.remove();
            }
        }
    }

    private static boolean shouldHideFromCreativeTab(ItemStack stack) {
        var potion = PotionUtils.getPotion(stack);
        if (!(potion instanceof SchoolAffinityPotion schoolAffinityPotion)) {
            return false;
        }

        return SchoolAffinityRegistry.getAssignedSchool(schoolAffinityPotion.getSlotIndex()).isEmpty();
    }
}
