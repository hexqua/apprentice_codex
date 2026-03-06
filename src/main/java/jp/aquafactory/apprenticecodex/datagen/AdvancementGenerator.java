package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.PlayerPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.RecipeCraftedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public final class AdvancementGenerator implements AdvancementProvider.AdvancementGenerator {
    private static final ResourceLocation IRONS_SPELLBOOK_EQUIP_ADVANCEMENT = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "irons_spellbooks/spell_book_equip");

    private static ResourceLocation advancementId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/" + path);
    }

    @Override
    public void generate(
            HolderLookup.@NotNull Provider registries,
            @NotNull Consumer<AdvancementHolder> saver,
            @NotNull ExistingFileHelper existingFileHelper
    ) {
        var ironsSpellbookEquipPredicate = EntityPredicate.wrap(
                EntityPredicate.Builder.entity()
                        .subPredicate(PlayerPredicate.Builder.player()
                                .checkAdvancementDone(IRONS_SPELLBOOK_EQUIP_ADVANCEMENT, true)
                                .build())
                        .build()
        );

        var root = Advancement.Builder.advancement()
                .display(ItemRegistry.SKY_EDGE_SWORD.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.root.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.root.description"),
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/advancement_tile_background.png"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .addCriterion(
                        "has_irons_spellbook_advancement",
                        CriteriaTriggers.TICK.createCriterion(
                                new PlayerTrigger.TriggerInstance(Optional.of(ironsSpellbookEquipPredicate))
                        )
                )
                .save(saver, advancementId("root"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.APPRENTICE_DESK.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_apprentice_desk.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_apprentice_desk.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_apprentice_desk", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.APPRENTICE_DESK.getId()))
                .save(saver, advancementId("craft_apprentice_desk"), existingFileHelper);

        var jar = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ARCANUM_IN_A_JAR.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_arcanum_in_a_jar.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_arcanum_in_a_jar.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_arcanum_in_a_jar", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ARCANUM_IN_A_JAR.getId()))
                .save(saver, advancementId("craft_arcanum_in_a_jar"), existingFileHelper);

        var retrieveOnceJar = Advancement.Builder.advancement()
                .parent(jar)
                .display(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.retrieve_once_arcanum_in_a_jar.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.retrieve_once_arcanum_in_a_jar.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("retrieve_arcane_essence", new ImpossibleTrigger.TriggerInstance())
                .save(saver, advancementId("retrieve_once_arcanum_in_a_jar"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(retrieveOnceJar)
                .display(Items.CLOCK,
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.retrieve_max_arcanum_in_a_jar.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.retrieve_max_arcanum_in_a_jar.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true)
                .addCriterion("retrieve_fully_charged_arcanum", new ImpossibleTrigger.TriggerInstance())
                .save(saver, advancementId("retrieve_max_arcanum_in_a_jar"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ENDER_GRIMOIRE.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.get_ender_grimoire.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.get_ender_grimoire.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("has_grimoire_manifest", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.ENDER_GRIMOIRE.get()))
                .save(saver, advancementId("get_ender_grimoire"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.PASTEL_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_pastel_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_pastel_staff.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_pastel_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.PASTEL_STAFF.getId()))
                .save(saver, advancementId("craft_pastel_staff"), existingFileHelper);

        var ironAmp = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_spell_amplifier.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_spell_amplifier.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_iron_spell_amplifier", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.IRON_SPELL_AMPLIFIER.getId()))
                .save(saver, advancementId("craft_iron_spell_amplifier"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(ironAmp)
                .display(ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_copper_spell_amplifier.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_copper_spell_amplifier.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_copper_spell_amplifier", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.COPPER_SPELL_AMPLIFIER.getId()))
                .save(saver, advancementId("craft_copper_spell_amplifier"), existingFileHelper);

        var goldAmp = Advancement.Builder.advancement()
                .parent(ironAmp)
                .display(ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_gold_spell_amplifier.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_gold_spell_amplifier.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_gold_spell_amplifier", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.GOLD_SPELL_AMPLIFIER.getId()))
                .save(saver, advancementId("craft_gold_spell_amplifier"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(goldAmp)
                .display(ItemRegistry.PHOTON_SIPHON.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_photon_siphon.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_photon_siphon.description"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_photon_siphon", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.PHOTON_SIPHON.getId()))
                .save(saver, advancementId("craft_photon_siphon"), existingFileHelper);
    }
}
