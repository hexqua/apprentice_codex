package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.RecipeCraftedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class AdvancementGenerator implements ForgeAdvancementProvider.AdvancementGenerator {
    private static final ResourceLocation IRONS_SPELLBOOK_EQUIP_ADVANCEMENT = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "irons_spellbooks/spell_book_equip");

    private static ResourceLocation advancementId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/" + path);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void generate(HolderLookup.@NotNull Provider registries, @NotNull Consumer<Advancement> saver, @NotNull ExistingFileHelper existingFileHelper) {
        var ironsSpellbookEquipPredicate = EntityPredicate.wrap(
                EntityPredicate.Builder.entity()
                        .subPredicate(PlayerPredicate.Builder.player()
                                .checkAdvancementDone(IRONS_SPELLBOOK_EQUIP_ADVANCEMENT, true)
                                .build())
                        .build()
        );
        var apprenticeMageRobeEquipPredicate = EntityPredicate.wrap(
                EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment()
                                .head(ItemPredicate.Builder.item().of(ItemRegistry.APPRENTICE_MAGE_SCARF.get()).build())
                                .chest(ItemPredicate.Builder.item().of(ItemRegistry.APPRENTICE_MAGE_TORSO.get()).build())
                                .legs(ItemPredicate.Builder.item().of(ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get()).build())
                                .feet(ItemPredicate.Builder.item().of(ItemRegistry.APPRENTICE_MAGE_BOOTS.get()).build())
                                .build())
                        .build()
        );
        var enchantressRobeEquipPredicate = EntityPredicate.wrap(
                EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment()
                                .head(ItemPredicate.Builder.item().of(ItemRegistry.ENCHANTRESS_HAT.get()).build())
                                .chest(ItemPredicate.Builder.item().of(ItemRegistry.ENCHANTRESS_ROBE.get()).build())
                                .legs(ItemPredicate.Builder.item().of(ItemRegistry.ENCHANTRESS_LEGGINGS.get()).build())
                                .feet(ItemPredicate.Builder.item().of(ItemRegistry.ENCHANTRESS_BOOTS.get()).build())
                                .build())
                        .build()
        );
        var chromaticMagiaDressEquipPredicate = EntityPredicate.wrap(
                EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment()
                                .head(ItemPredicate.Builder.item().of(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()).build())
                                .chest(ItemPredicate.Builder.item().of(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()).build())
                                .legs(ItemPredicate.Builder.item().of(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()).build())
                                .feet(ItemPredicate.Builder.item().of(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()).build())
                                .build())
                        .build()
        );

        var root = Advancement.Builder.advancement()
                .display(ItemRegistry.SKY_EDGE_SWORD.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.root.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.root.description"),
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/advancement_tile_background.png"),
                        FrameType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("has_irons_spellbook_advancement", new PlayerTrigger.TriggerInstance(CriteriaTriggers.TICK.getId(), ironsSpellbookEquipPredicate))
                .save(saver, advancementId("root"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.EXPLORERS_CODEX.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_explorers_codex.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_explorers_codex.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_explorers_codex", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.EXPLORERS_CODEX.getId()))
                .save(saver, advancementId("craft_explorers_codex"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.EXPLORERS_CANE.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_explorers_cane.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_explorers_cane.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_explorers_cane", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.EXPLORERS_CANE.getId()))
                .save(saver, advancementId("craft_explorers_cane"), existingFileHelper);

        var apprenticeMageRobe = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.equip_apprentice_mage_robe.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.equip_apprentice_mage_robe.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("equip_apprentice_mage_robe", new PlayerTrigger.TriggerInstance(CriteriaTriggers.TICK.getId(), apprenticeMageRobeEquipPredicate))
                .save(saver, advancementId("equip_apprentice_mage_robe"), existingFileHelper);

        var enchantressRobe = Advancement.Builder.advancement()
                .parent(apprenticeMageRobe)
                .display(ItemRegistry.ENCHANTRESS_HAT.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.equip_enchantress_robe.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.equip_enchantress_robe.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("equip_enchantress_robe", new PlayerTrigger.TriggerInstance(CriteriaTriggers.TICK.getId(), enchantressRobeEquipPredicate))
                .save(saver, advancementId("equip_enchantress_robe"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(enchantressRobe)
                .display(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.equip_chromatic_magia_dress.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.equip_chromatic_magia_dress.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("equip_chromatic_magia_dress", new PlayerTrigger.TriggerInstance(CriteriaTriggers.TICK.getId(), chromaticMagiaDressEquipPredicate))
                .save(saver, advancementId("equip_chromatic_magia_dress"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.APPRENTICE_DESK.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_apprentice_desk.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_apprentice_desk.description"),
                        null,
                        FrameType.GOAL,
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
                        FrameType.TASK,
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
                        FrameType.GOAL,
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
                        FrameType.CHALLENGE,
                        true,
                        true,
                        true)
                .addCriterion("retrieve_fully_charged_arcanum", new ImpossibleTrigger.TriggerInstance())
                .save(saver, advancementId("retrieve_max_arcanum_in_a_jar"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ESSENCE_SMOKER.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_essence_smoker.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_essence_smoker.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_essence_smoker", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ESSENCE_SMOKER.getId()))
                .save(saver, advancementId("craft_essence_smoker"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ENDER_GRIMOIRE.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.get_ender_grimoire.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.get_ender_grimoire.description"),
                        null,
                        FrameType.GOAL,
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
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_pastel_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.PASTEL_STAFF.getId()))
                .save(saver, advancementId("craft_pastel_staff"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_circuit_heat_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_circuit_heat_staff.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_circuit_heat_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.CIRCUIT_HEAT_STAFF.getId()))
                .save(saver, advancementId("craft_circuit_heat_staff"), existingFileHelper);

        var ironAmp = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_spell_amplifier.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_spell_amplifier.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_iron_spell_amplifier", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.IRON_SPELL_AMPLIFIER.getId()))
                .save(saver, advancementId("craft_iron_spell_amplifier"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(ironAmp)
                .display(ItemRegistry.SILVER_SPELL_AMPLIFIER.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_silver_spell_amplifier.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_silver_spell_amplifier.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_silver_spell_amplifier", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.SILVER_SPELL_AMPLIFIER.getId()))
                .save(saver, advancementId("craft_silver_spell_amplifier"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(ironAmp)
                .display(ItemRegistry.PHOTON_SIPHON.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_photon_siphon.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_photon_siphon.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_photon_siphon", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.PHOTON_SIPHON.getId()))
                .save(saver, advancementId("craft_photon_siphon"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_absorption_amplify_amulet.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_absorption_amplify_amulet.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_absorption_amplify_amulet", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.getId()))
                .save(saver, advancementId("craft_absorption_amplify_amulet"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ASHEN_CIRCLET.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_ashen_circlet.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_ashen_circlet.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_ashen_circlet", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ASHEN_CIRCLET.getId()))
                .save(saver, advancementId("craft_ashen_circlet"), existingFileHelper);

        var ironGun = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_spellcaster_gun.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_spellcaster_gun.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_iron_spellcaster_gun", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.IRON_SPELLCASTER_GUN.getId()))
                .save(saver, advancementId("craft_iron_spellcaster_gun"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(ironGun)
                .display(ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_copper_spellcaster_gun.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_copper_spellcaster_gun.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_copper_spellcaster_gun", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.COPPER_SPELLCASTER_GUN.getId()))
                .save(saver, advancementId("craft_copper_spellcaster_gun"), existingFileHelper);

        var goldGun = Advancement.Builder.advancement()
                .parent(ironGun)
                .display(ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_gold_spellcaster_gun.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_gold_spellcaster_gun.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_gold_spellcaster_gun", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.GOLD_SPELLCASTER_GUN.getId()))
                .save(saver, advancementId("craft_gold_spellcaster_gun"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(goldGun)
                .display(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_diamond_spellcaster_gun.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_diamond_spellcaster_gun.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_diamond_spellcaster_gun", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.DIAMOND_SPELLCASTER_GUN.getId()))
                .save(saver, advancementId("craft_diamond_spellcaster_gun"), existingFileHelper);


        var spellcaster = Advancement.Builder.advancement()
                .parent(ironGun)
                .display(ItemRegistry.SPELLCASTER_WORKBENCH.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_spellcaster_workbench.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_spellcaster_workbench.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_spellcaster_workbench", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.SPELLCASTER_WORKBENCH.getId()))
                .save(saver, advancementId("craft_spellcaster_workbench"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(spellcaster)
                .display(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.extract_spellcaster_gun_scroll.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.extract_spellcaster_gun_scroll.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("extract_spellcaster_gun_scroll", new ImpossibleTrigger.TriggerInstance())
                .save(saver, advancementId("extract_spellcaster_gun_scroll"), existingFileHelper);

        var bladed = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_crystal_bladed_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_crystal_bladed_staff.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_crystal_bladed_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.CRYSTAL_BLADED_STAFF.getId()))
                .save(saver, advancementId("craft_crystal_bladed_staff"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(bladed)
                .display(ItemRegistry.REFLECTCAST_SHIELD.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_reflectcast_shield.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_reflectcast_shield.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("craft_reflectcast_shield", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.REFLECTCAST_SHIELD.getId()))
                .save(saver, advancementId("craft_reflectcast_shield"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(bladed)
                .display(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_charged_twin_blade_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_charged_twin_blade_staff.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_charged_twin_blade_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.getId()))
                .save(saver, advancementId("craft_charged_twin_blade_staff"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(bladed)
                .display(ItemRegistry.MANA_FORCE_BLADE.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_mana_force_blade.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_mana_force_blade.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_mana_force_blade", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.MANA_FORCE_BLADE.getId()))
                .save(saver, advancementId("craft_mana_force_blade"), existingFileHelper);
        
        var bow = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.ELEMENTAL_BOW.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_elemental_bow.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_elemental_bow.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_elemental_bow", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ELEMENTAL_BOW.getId()))
                .save(saver, advancementId("craft_elemental_bow"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(bow)
                .display(ItemRegistry.FOCUS_STAFFBOW.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_focus_staffbow.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_focus_staffbow.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_focus_staffbow", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.FOCUS_STAFFBOW.getId()))
                .save(saver, advancementId("craft_focus_staffbow"), existingFileHelper);

        var ironSwing = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_swingcast_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_iron_swingcast_staff.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_iron_swingcast_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.IRON_SWINGCAST_STAFF.getId()))
                .save(saver, advancementId("craft_iron_swingcast_staff"), existingFileHelper);

        var silverSwing = Advancement.Builder.advancement()
                .parent(ironSwing)
                .display(ItemRegistry.SILVER_SWINGCAST_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_silver_swingcast_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_silver_swingcast_staff.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_silver_swingcast_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.SILVER_SWINGCAST_STAFF.getId()))
                .save(saver, advancementId("craft_silver_swingcast_staff"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(silverSwing)
                .display(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_illuminate_stellar_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_illuminate_stellar_staff.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_illuminate_stellar_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ILLUMINATE_STELLAR_STAFF.getId()))
                .save(saver, advancementId("craft_illuminate_stellar_staff"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(silverSwing)
                .display(ItemRegistry.UNITE_LUNA_STAFF.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_unite_luna_staff.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_unite_luna_staff.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_unite_luna_staff", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.UNITE_LUNA_STAFF.getId()))
                .save(saver, advancementId("craft_unite_luna_staff"), existingFileHelper);

        var tablet = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_spellstained_runic_tablet.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_spellstained_runic_tablet.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_spellstained_runic_tablet", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.SPELLSTAINED_RUNIC_TABLET.getId()))
                .save(saver, advancementId("craft_spellstained_runic_tablet"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(tablet)
                .display(ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_archivists_grimoire.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_archivists_grimoire.description"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("crafted_archivists_grimoire", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ARCHIVISTS_GRIMOIRE.getId()))
                .save(saver, advancementId("craft_archivists_grimoire"), existingFileHelper);

        var flask = Advancement.Builder.advancement()
                .parent(root)
                .display(ItemRegistry.SPELLCASTERS_FLASK.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_spellcasters_flask.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_spellcasters_flask.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("crafted_spellcasters_flask", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.SPELLCASTERS_FLASK.getId()))
                .save(saver, advancementId("craft_spellcasters_flask"), existingFileHelper);

        var alchemistsFlask = Advancement.Builder.advancement()
                .parent(flask)
                .display(ItemRegistry.ALCHEMISTS_FLASK.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_alchemists_flask.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_alchemists_flask.description"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("has_alchemists_flask", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.ALCHEMISTS_FLASK.get()))
                .save(saver, advancementId("craft_alchemists_flask"), existingFileHelper);

        var intelligenceArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), PotionRegistry.INTELLIGENCE.get());
        Advancement.Builder.advancement()
                .parent(alchemistsFlask)
                .display(intelligenceArrow,
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_tipped_arrow_by_flask.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_tipped_arrow_by_flask.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion(AdvancementTools.CRAFT_TIPPED_ARROW_BY_FLASK_CRITERION, new ImpossibleTrigger.TriggerInstance())
                .save(saver, advancementId("craft_tipped_arrow_by_flask"), existingFileHelper);

        Advancement.Builder.advancement()
                .parent(flask)
                .display(ItemRegistry.ATELIER_STATION.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_atelier_station.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.craft_atelier_station.description"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("crafted_atelier_station", RecipeCraftedTrigger.TriggerInstance.craftedItem(ItemRegistry.ATELIER_STATION.getId()))
                .save(saver, advancementId("craft_atelier_station"), existingFileHelper);
    }
}
