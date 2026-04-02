package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public final class UndeadTools {
    private UndeadTools() {
    }

    public static boolean isUndead(EntityType<?> entityType) {
        return entityType.is(EntityTypeTags.UNDEAD)
                || entityType.is(EntityTypeTags.SENSITIVE_TO_SMITE)
                || entityType.is(EntityTypeTags.INVERTED_HEALING_AND_HARM)
                || entityType.is(TagRegistry.EntityTypes.COUNTS_AS_UNDEAD);
    }
}
