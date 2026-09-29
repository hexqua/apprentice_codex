package jp.aquafactory.apprenticecodex.compat.spellcasterworkbench;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public final class SpellcasterWorkbenchDisplayExamples {
    public static final ResourceLocation ARCHIVISTS_GRIMOIRE_ROW_UPGRADE = id("archivists_grimoire_row_upgrade");
    public static final ResourceLocation SPELL_EXTRACTION = id("spell_extraction");
    public static final ResourceLocation SPELL_INVOKE_CARD_FROM_PAPER = id("spell_invoke_card_from_paper");
    public static final ResourceLocation SPELL_INVOKE_CARD_FROM_CARD = id("spell_invoke_card_from_card");
    public static final ResourceLocation SPELL_AUTONOMY_CARD_FROM_PAPER = id("spell_autonomy_card_from_paper");
    public static final ResourceLocation SPELL_AUTONOMY_CARD_FROM_CARD = id("spell_autonomy_card_from_card");

    private SpellcasterWorkbenchDisplayExamples() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "examples/spellcaster_workbench/" + path
        );
    }

    public static List<Example> all() {
        var examples = new ArrayList<Example>();
        // 実レシピには登録しない。JEIとPatchouliで同じ見本だけを共有する。
        if (ApprenticeCodexServerConfig.archivistsGrimoireInitialRows()
                < ApprenticeCodexServerConfig.archivistsGrimoireEffectiveMaxRows()) {
            examples.add(new Example(ARCHIVISTS_GRIMOIRE_ROW_UPGRADE, createArchivistsGrimoireUpgrade()));
        }
        examples.add(new Example(SPELL_EXTRACTION, createSpellExtraction()));

        var invokeCount = ApprenticeCodexServerConfig.spellInvokeCardCraftCount();
        var autonomyCount = ApprenticeCodexServerConfig.spellAutonomyCardCraftCount();
        examples.add(new Example(SPELL_INVOKE_CARD_FROM_PAPER, createSpellThrowableCard(
                Ingredient.of(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                invokeCount,
                Ingredient.of(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), invokeCount)
        )));
        examples.add(new Example(SPELL_INVOKE_CARD_FROM_CARD, createSpellThrowableCard(
                Ingredient.of(ItemRegistry.SPELL_INVOKE_CARD.get()),
                invokeCount,
                Ingredient.of(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), invokeCount)
        )));
        examples.add(new Example(SPELL_AUTONOMY_CARD_FROM_PAPER, createSpellThrowableCard(
                Ingredient.of(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                autonomyCount,
                Ingredient.of(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), autonomyCount)
        )));
        examples.add(new Example(SPELL_AUTONOMY_CARD_FROM_CARD, createSpellThrowableCard(
                Ingredient.of(ItemRegistry.SPELL_AUTONOMY_CARD.get()),
                autonomyCount,
                Ingredient.of(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), autonomyCount)
        )));
        return List.copyOf(examples);
    }

    private static SpellcasterWorkbenchRecipe createArchivistsGrimoireUpgrade() {
        return new SpellcasterWorkbenchRecipe(
                List.of(
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(ItemRegistry.ARCHIVISTS_GRIMOIRE.get()), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS), 1)
                ),
                List.of(new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get())),
                -10
        );
    }

    private static SpellcasterWorkbenchRecipe createSpellExtraction() {
        var magicMissile = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var imbuedSword = new ItemStack(Items.IRON_SWORD);
        var swordSpells = ISpellContainer.create(1, true, false).mutableCopy();
        swordSpells.addSpellAtIndex(magicMissile, 1, 0, true);
        ISpellContainer.set(imbuedSword, swordSpells.toImmutable());

        var extractedScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(magicMissile, 1, extractedScroll);

        // 実処理は2入力と空スロットを要求するため、見本の3枠目も空Ingredientにする。
        return new SpellcasterWorkbenchRecipe(
                List.of(
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(imbuedSword), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.of(ItemRegistry.SPELL_EXTRACT_SHARD.get()), 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.EMPTY, 1)
                ),
                List.of(extractedScroll),
                -20
        );
    }

    private static SpellcasterWorkbenchRecipe createSpellThrowableCard(
            Ingredient baseIngredient,
            int baseCount,
            Ingredient catalystIngredient,
            ItemStack result
    ) {
        return new SpellcasterWorkbenchRecipe(
                List.of(
                        new SpellcasterWorkbenchRecipe.SizedIngredient(baseIngredient, baseCount),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(catalystIngredient, 1),
                        new SpellcasterWorkbenchRecipe.SizedIngredient(
                                Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()), 1
                        )
                ),
                List.of(result),
                -20
        );
    }

    public record Example(ResourceLocation id, SpellcasterWorkbenchRecipe recipe) {
    }
}
