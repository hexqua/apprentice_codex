package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

import java.util.ArrayList;
import java.util.List;

public final class SchoolAffinityPotionBrewing {
    private SchoolAffinityPotionBrewing() {
    }

    public static List<PotionTransition> getTransitions() {
        var transitions = new ArrayList<PotionTransition>();

        for (var entry : SchoolAffinityRegistry.getBrewingDefinitionsByCatalyst().entrySet()) {
            var catalyst = entry.getKey();
            var definition = entry.getValue();

            transitions.add(new PotionTransition(
                    "affinity_from_intelligence",
                    catalyst,
                    PotionRegistry.INTELLIGENCE.get(),
                    definition.basePotion()
            ));
            transitions.add(new PotionTransition(
                    "affinity_from_long_intelligence",
                    catalyst,
                    PotionRegistry.LONG_INTELLIGENCE.get(),
                    definition.longPotion()
            ));
            transitions.add(new PotionTransition(
                    "affinity_from_strong_intelligence",
                    catalyst,
                    PotionRegistry.STRONG_INTELLIGENCE.get(),
                    definition.strongPotion()
            ));
            transitions.add(new PotionTransition(
                    "affinity_extend",
                    Items.REDSTONE,
                    definition.basePotion(),
                    definition.longPotion()
            ));
            transitions.add(new PotionTransition(
                    "affinity_amplify",
                    Items.GLOWSTONE_DUST,
                    definition.basePotion(),
                    definition.strongPotion()
            ));
        }

        return List.copyOf(transitions);
    }

    public record PotionTransition(
            String transitionKey,
            Item catalyst,
            Potion inputPotion,
            Potion outputPotion
    ) {
    }
}
