package jp.aquafactory.apprenticecodex.datagen;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.ConditionalAdvancement;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ConditionalAdvancementDataGenerator implements DataProvider, IConditionBuilder {
    private static final String APOTHEOSIS_MOD_ID = "apotheosis";

    private final PackOutput.PathProvider pathProvider;

    public ConditionalAdvancementDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var advancements = List.of(
                conditionalAdvancement(
                        advancementId("enchant_max_level"),
                        ConditionalAdvancement.builder()
                                .addCondition(not(modLoaded(APOTHEOSIS_MOD_ID)))
                                .addAdvancement(Advancement.Builder.advancement()
                                        .parent(advancementId("equip_enchantress_robe"))
                                        .display(Items.ENCHANTING_TABLE,
                                                Component.translatable("advancements.apprenticecodex.apprentice_codex.enchant_max_level.title"),
                                                Component.translatable("advancements.apprenticecodex.apprentice_codex.enchant_max_level.description"),
                                                null,
                                                FrameType.CHALLENGE,
                                                true,
                                                true,
                                                false)
                                        .addCriterion("enchant_max_level", new ImpossibleTrigger.TriggerInstance()))
                )
        );

        return CompletableFuture.allOf(advancements.stream()
                .map(advancement -> DataProvider.saveStable(cachedOutput, advancement.json(), pathProvider.json(advancement.id())))
                .toArray(CompletableFuture[]::new));
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Conditional Advancements";
    }

    private static ConditionalAdvancementEntry conditionalAdvancement(ResourceLocation id, ConditionalAdvancement.Builder builder) {
        return new ConditionalAdvancementEntry(id, builder.write());
    }

    private static ResourceLocation advancementId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/" + path);
    }

    private record ConditionalAdvancementEntry(
            ResourceLocation id,
            JsonObject json
    ) {
    }
}
