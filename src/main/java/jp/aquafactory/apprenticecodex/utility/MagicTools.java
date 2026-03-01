package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.mixin.SchoolTypeAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class MagicTools {
    private MagicTools() {
    }

    // 規約外の属性 ID を使う拡張学派がある場合はここで明示対応する.
    // 現状メイン1.20.1環境で使っているアドオンは対応できている.
    private static final Map<ResourceLocation, ResourceLocation> SCHOOL_POWER_ATTRIBUTE_FALLBACK_MAP = Map.of();


    @Nullable
    public static SchoolType getImbuedSpellSchool(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY) {
            return null;
        }

        return spellData.getSpell().getSchoolType();
    }

    @Nullable
    public static Attribute resolveSchoolPowerAttribute(@Nullable SchoolType schoolType) {
        if (schoolType == null) {
            return null;
        }

        if (schoolType instanceof SchoolTypeAccessor accessor) {
            var supplier = accessor.apprenticecodex$getPowerAttribute();
            if (supplier != null) {
                var resolved = supplier.get();
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        var schoolId = schoolType.getId();
        var fallbackAttributeId = SCHOOL_POWER_ATTRIBUTE_FALLBACK_MAP.get(schoolId);
        if (fallbackAttributeId != null) {
            var fallbackAttribute = ForgeRegistries.ATTRIBUTES.getValue(fallbackAttributeId);
            if (fallbackAttribute != null) {
                return fallbackAttribute;
            }
        }

        // 多くの拡張学派は "<school_id>_spell_power" 命名に従うため、最後に規約名を参照する.
        // 現状これで拾えてる...
        var guessedAttributeId = ResourceLocation.fromNamespaceAndPath(
                schoolId.getNamespace(),
                schoolId.getPath() + "_spell_power"
        );
        return ForgeRegistries.ATTRIBUTES.getValue(guessedAttributeId);
    }
}
