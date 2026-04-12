package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class EnchantmentTagGenerator extends EnchantmentTagsProvider {
    public EnchantmentTagGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(Enchantments.EXCLUSIVE_REFLUX_RESERVOIR)
                .add(Enchantments.REFLUX, Enchantments.RESERVOIR);
        tag(Enchantments.EXCLUSIVE_ALACRITY_TENSE)
                .add(Enchantments.ALACRITY, Enchantments.TENSE);
        tag(Enchantments.EXCLUSIVE_SURGE_ATTUNEMENT_TRANSCENDENCE)
                .add(Enchantments.SURGE, Enchantments.ATTUNEMENT, Enchantments.TRANSCENDENCE);
        tag(Enchantments.EXCLUSIVE_RED_GLOW_ENERGY)
                .add(Enchantments.RED_ENERGY, Enchantments.GLOW_ENERGY);

        tag(EnchantmentTags.NON_TREASURE)
                .add(
                        Enchantments.REFLUX,
                        Enchantments.RESERVOIR,
                        Enchantments.ALACRITY,
                        Enchantments.TENSE,
                        Enchantments.SURGE,
                        Enchantments.ATTUNEMENT,
                        Enchantments.WISDOM,
                        Enchantments.PLUNDER
                );
        tag(EnchantmentTags.TREASURE)
                .add(Enchantments.TRANSCENDENCE);

        tag(EnchantmentTags.IN_ENCHANTING_TABLE)
                .add(
                        Enchantments.REFLUX,
                        Enchantments.RESERVOIR,
                        Enchantments.ALACRITY,
                        Enchantments.TENSE,
                        Enchantments.SURGE,
                        Enchantments.ATTUNEMENT,
                        Enchantments.WISDOM,
                        Enchantments.PLUNDER,
                        Enchantments.GUZZLE,
                        Enchantments.LARGE_MUG,
                        Enchantments.RED_ENERGY,
                        Enchantments.GLOW_ENERGY
                );

        tag(EnchantmentTags.TRADEABLE)
                .add(
                        Enchantments.REFLUX,
                        Enchantments.RESERVOIR,
                        Enchantments.ALACRITY,
                        Enchantments.TENSE,
                        Enchantments.SURGE,
                        Enchantments.ATTUNEMENT,
                        Enchantments.TRANSCENDENCE,
                        Enchantments.WISDOM,
                        Enchantments.PLUNDER
                );

        tag(EnchantmentTags.ON_RANDOM_LOOT)
                .add(
                        Enchantments.REFLUX,
                        Enchantments.RESERVOIR,
                        Enchantments.ALACRITY,
                        Enchantments.TENSE,
                        Enchantments.SURGE,
                        Enchantments.ATTUNEMENT,
                        Enchantments.TRANSCENDENCE,
                        Enchantments.WISDOM,
                        Enchantments.PLUNDER
                );
    }
}
