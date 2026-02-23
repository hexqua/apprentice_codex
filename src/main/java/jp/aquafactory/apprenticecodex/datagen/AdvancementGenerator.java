package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class AdvancementGenerator implements ForgeAdvancementProvider.AdvancementGenerator {
    private static ResourceLocation advancementId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/" + path);
    }

    @Override
    public void generate(HolderLookup.@NotNull Provider registries, @NotNull Consumer<Advancement> saver, @NotNull ExistingFileHelper existingFileHelper) {
        var root = Advancement.Builder.advancement()
                .display(ItemRegistry.SKY_EDGE_SWORD.get(),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.root.title"),
                        Component.translatable("advancements.apprenticecodex.apprentice_codex.root.description"),
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/advancement_tile_background.png"),
                        FrameType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("auto_visible", PlayerTrigger.TriggerInstance.tick())
                .save(saver, advancementId("root"), existingFileHelper);

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
                .addCriterion("has_grimoire_manifest", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.APPRENTICE_DESK.get()))
                .save(saver, advancementId("craft_apprentice_desk"), existingFileHelper);

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
                .addCriterion("has_grimoire_manifest", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.PASTEL_STAFF.get()))
                .save(saver, advancementId("craft_pastel_staff"), existingFileHelper);
    }
}
