package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public final class ScrollcasterSchoolRuneResolver {
    private static final String RUNE_SUFFIX = "_rune";

    private ScrollcasterSchoolRuneResolver() {
    }

    public static Optional<SchoolType> resolveSchool(@NotNull ItemStack stack) {
        if (stack.isEmpty() || stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SCHOOL_RUNE_DENYLIST)) {
            return Optional.empty();
        }

        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }

        return resolveSchool(itemId, ScrollcasterSchoolRuneOverrideManager.getResolvedOverrides());
    }

    public static Optional<SchoolType> resolveSchool(ResourceLocation runeId) {
        return resolveSchool(runeId, ScrollcasterSchoolRuneOverrideManager.getResolvedOverrides());
    }

    public static Optional<SchoolType> resolveSchool(
            ResourceLocation runeId,
            Map<ResourceLocation, ResourceLocation> manualOverrides
    ) {
        var overrideSchoolId = manualOverrides.get(runeId);
        if (overrideSchoolId != null) {
            return Optional.ofNullable(SchoolRegistry.getSchool(overrideSchoolId));
        }

        var path = runeId.getPath();
        if (!path.endsWith(RUNE_SUFFIX) || path.length() <= RUNE_SUFFIX.length()) {
            return Optional.empty();
        }

        // Iron's本体とアドオンの慣例に合わせ、<namespace>:<school>_rune から同namespaceのschoolを逆引きする。
        var schoolId = ResourceLocation.fromNamespaceAndPath(
                runeId.getNamespace(),
                path.substring(0, path.length() - RUNE_SUFFIX.length())
        );
        return Optional.ofNullable(SchoolRegistry.getSchool(schoolId));
    }

    public static boolean isSchoolRune(@NotNull ItemStack stack) {
        return resolveSchool(stack).isPresent();
    }
}
