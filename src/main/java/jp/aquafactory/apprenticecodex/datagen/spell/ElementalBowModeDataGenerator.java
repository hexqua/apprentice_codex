package jp.aquafactory.apprenticecodex.datagen.spell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeDefinition;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeList;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ElementalBowModeDataGenerator extends JsonCodecProvider<ElementalBowModeList> {
    public ElementalBowModeDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                ElementalBowModeManager.DIRECTORY,
                PackType.SERVER_DATA,
                ElementalBowModeList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "default_modes"),
                new ElementalBowModeList(List.of(
                        new ElementalBowModeDefinition(
                                getSpellId(SpellRegistry.FIRE_ARROW_SPELL),
                                ElementalBowModeDefinition.DEFAULT_REQUIRED_DRAW_TICKS
                        ),
                        new ElementalBowModeDefinition(
                                getSpellId(SpellRegistry.MAGIC_ARROW_SPELL),
                                ElementalBowModeDefinition.DEFAULT_REQUIRED_DRAW_TICKS
                        ),
                        new ElementalBowModeDefinition(
                                getSpellId(SpellRegistry.POISON_ARROW_SPELL),
                                ElementalBowModeDefinition.DEFAULT_REQUIRED_DRAW_TICKS
                        )
                ))
        );
    }

    private static ResourceLocation getSpellId(Supplier<? extends AbstractSpell> spellSupplier) {
        return Objects.requireNonNull(spellSupplier.get().getSpellResource());
    }
}
