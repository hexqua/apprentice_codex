package jp.aquafactory.apprenticecodex.item.spellsideedge;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.SpellSideEdgeRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SpellSideEdge extends AbstractSpellSideEdgeItem {
    private static final String BETTER_COMBAT_DESCRIPTION_TRANSLATION_KEY =
            "item.apprenticecodex.spell_side_edge.desc.better_combat";

    @Override
    protected Supplier<? extends AbstractSpell> initialSpellSupplier() {
        return SpellRegistry.EDGE_DANCER;
    }

    @Override
    protected String betterCombatDescriptionTranslationKey() {
        return BETTER_COMBAT_DESCRIPTION_TRANSLATION_KEY;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SpellSideEdgeRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SpellSideEdgeRenderer();
                }

                return renderer;
            }
        });
    }

    public static boolean isSpellSideEdge(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SpellSideEdge;
    }
}
