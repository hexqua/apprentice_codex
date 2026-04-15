package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
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
    private static final Field ARCANE_ANVIL_JEI_RECIPE_TYPE_FIELD = findTypeField();

    @Shadow(remap = false)
    Item leftItem;

    @Inject(method = "getRecipeItems", at = @At("HEAD"), cancellable = true, remap = false)
    private void apprenticecodex$filterSpellGunImbueRecipes(
            CallbackInfoReturnable<ArcaneAnvilJeiRecipe.Tuple<List<ItemStack>, List<ItemStack>, List<ItemStack>>> cir
    ) {
        if (!isImbueRecipe()) {
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

        // Iron's Spells 側の JEI レシピ生成は spell gun 個別制限を考慮しないため、
        // 実際に imbue できる組み合わせだけを再構築して UI と実挙動を一致させる。
        for (var spell : SpellRegistry.getEnabledSpells()) {
            for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
                if (!spellImbueItem.canImbueSpell(spell, level)) {
                    continue;
                }

                var scrollStack = new ItemStack(ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell, level, scrollStack);
                rightInputs.add(scrollStack);

                var imbuedStack = new ItemStack(leftItem);
                ISpellContainer.createScrollContainer(spell, level, imbuedStack);
                // 付与後に spell container を正規化しないと、盾の固有制約と表示がズレる。
                spellImbueItem.normalizeImbuedSpellContainer(imbuedStack);
                outputs.add(imbuedStack);
            }
        }

        cir.setReturnValue(new ArcaneAnvilJeiRecipe.Tuple<>(leftInputs, rightInputs, outputs));
    }

    @Unique
    private boolean isImbueRecipe() {
        try {
            var recipeType = ARCANE_ANVIL_JEI_RECIPE_TYPE_FIELD.get(this);
            return recipeType instanceof Enum<?> enumValue
                    && IMBUE_RECIPE_TYPE_NAME.equals(enumValue.name());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to inspect ArcaneAnvilJeiRecipe type.", e);
        }
    }

    @Unique
    private static Field findTypeField() {
        try {
            var field = ArcaneAnvilJeiRecipe.class.getDeclaredField("type");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to locate ArcaneAnvilJeiRecipe type field.", e);
        }
    }
}
