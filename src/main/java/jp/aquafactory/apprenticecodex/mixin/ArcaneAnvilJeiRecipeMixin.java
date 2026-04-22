package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = ArcaneAnvilJeiRecipe.class, remap = false)
public abstract class ArcaneAnvilJeiRecipeMixin {
    @Unique
    private static final String IMBUE_RECIPE_TYPE_NAME = "Imbue";
    @Unique
    private static final Field ARCANE_ANVIL_JEI_RECIPE_TYPE_FIELD = apprentice_codex$findTypeField();

    @Shadow(remap = false)
    Item leftItem;

    @Inject(method = "getRecipeItems", at = @At("HEAD"), cancellable = true, remap = false)
    private void apprenticecodex$filterSpellGunImbueRecipes(
            CallbackInfoReturnable<ArcaneAnvilJeiRecipe.Tuple<List<ItemStack>, List<ItemStack>, List<ItemStack>>> cir
    ) {
        if (!apprentice_codex$isImbueRecipe()) {
            return;
        }

        if (leftItem instanceof ArcaneAnvilImbueBlockItem) {
            cir.setReturnValue(new ArcaneAnvilJeiRecipe.Tuple<>(List.of(), List.of(), List.of()));
            return;
        }

        if (!(leftItem instanceof RestrictedSpellImbuableItem spellImbueItem)) {
            return;
        }

        var leftInputs = new ArrayList<ItemStack>();
        var rightInputs = new ArrayList<ItemStack>();
        var outputs = new ArrayList<ItemStack>();
        leftInputs.add(new ItemStack(leftItem));

        // Iron's Spells 側の JEI 実装は canImbue だけを見るため、
        // 銃ごとの個別制限をここで再適用して実プレイ時の判定と表示を一致させる。
        for (var spell : io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells()) {
            for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
                if (!spellImbueItem.canImbueSpell(spell, level)) {
                    continue;
                }

                var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell, level, scrollStack);
                rightInputs.add(scrollStack);

                outputs.add(spellImbueItem.createArcaneAnvilImbueResult(new ItemStack(leftItem), new io.redspace.ironsspellbooks.api.spells.SpellData(spell, level)));
            }
        }

        cir.setReturnValue(new ArcaneAnvilJeiRecipe.Tuple<>(leftInputs, rightInputs, outputs));
    }

    @Unique
    private boolean apprentice_codex$isImbueRecipe() {
        try {
            var recipeType = ARCANE_ANVIL_JEI_RECIPE_TYPE_FIELD.get(this);
            return recipeType instanceof Enum<?> enumValue
                    && IMBUE_RECIPE_TYPE_NAME.equals(enumValue.name());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to inspect ArcaneAnvilJeiRecipe type.", e);
        }
    }

    @Unique
    private static Field apprentice_codex$findTypeField() {
        try {
            var field = ArcaneAnvilJeiRecipe.class.getDeclaredField("type");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to locate ArcaneAnvilJeiRecipe type field.", e);
        }
    }
}
