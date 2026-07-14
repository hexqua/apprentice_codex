package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.item.pastelstaff.PastelStaff;
import jp.aquafactory.apprenticecodex.mixin.SchoolTypeAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class MagicTools {
    private static final float MANA_UI_SAFE_MARGIN = 0.1f;

    private MagicTools() {
    }

    // 規約外の属性 ID を使う拡張学派がある場合はここで明示対応する.
    // 現状メイン1.20.1環境で使っているアドオンは対応できている.
    private static final Map<ResourceLocation, ResourceLocation> SCHOOL_POWER_ATTRIBUTE_FALLBACK_MAP = Map.of();
    private static final Map<ResourceLocation, ResourceLocation> SCHOOL_RESIST_ATTRIBUTE_FALLBACK_MAP = Map.of();


    @Nullable
    public static SchoolType getImbuedSpellSchool(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return null;
        }

        // activeSpellCount より slot 0 の実データを優先し、preset Imbue 直後でも学派解決を安定させる.
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

    @Nullable
    public static Attribute resolveSchoolResistAttribute(@Nullable SchoolType schoolType) {
        if (schoolType == null) {
            return null;
        }

        if (schoolType instanceof SchoolTypeAccessor accessor) {
            var supplier = accessor.apprenticecodex$getResistanceAttribute();
            if (supplier != null) {
                var resolved = supplier.get();
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        var schoolId = schoolType.getId();
        var fallbackAttributeId = SCHOOL_RESIST_ATTRIBUTE_FALLBACK_MAP.get(schoolId);
        if (fallbackAttributeId != null) {
            var fallbackAttribute = ForgeRegistries.ATTRIBUTES.getValue(fallbackAttributeId);
            if (fallbackAttribute != null) {
                return fallbackAttribute;
            }
        }

        // Iron's 本体と多くの拡張学派は "<school_id>_magic_resist" 命名に従う.
        var guessedAttributeId = ResourceLocation.fromNamespaceAndPath(
                schoolId.getNamespace(),
                schoolId.getPath() + "_magic_resist"
        );
        return ForgeRegistries.ATTRIBUTES.getValue(guessedAttributeId);
    }

    public static int resolveSchoolTintColor(@Nullable SchoolType schoolType) {
        if (schoolType == null) {
            return PastelStaff.DEFAULT_STONE_TINT_COLOR;
        }

        var color = schoolType.getDisplayName().getStyle().getColor();
        if (color == null) {
            return PastelStaff.DEFAULT_STONE_TINT_COLOR;
        }

        return color.getValue();
    }

    public static void cancelCasting(@Nullable LivingEntity entity, boolean triggerCooldown) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(serverPlayer);
        if (magicData == null || !magicData.isCasting()) {
            return;
        }

        Utils.serverSideCancelCast(serverPlayer, triggerCooldown);
    }

    public static void recoverManaSafely(@Nullable LivingEntity entity, @Nullable MagicData magicData, float recoverMana) {
        if (entity == null || magicData == null || recoverMana <= 0f) {
            return;
        }

        var maxMana = (float) entity.getAttributeValue(AttributeRegistry.MAX_MANA.get());
        if (maxMana <= MANA_UI_SAFE_MARGIN) {
            return;
        }

        var currentMana = magicData.getMana();
        if (currentMana >= maxMana - MANA_UI_SAFE_MARGIN) {
            return;
        }

        // 最大マナぴったりで UI 更新が止まる不具合回避のため、少し手前で止める.
        if (currentMana + recoverMana > maxMana - MANA_UI_SAFE_MARGIN) {
            magicData.setMana(maxMana - MANA_UI_SAFE_MARGIN);
            return;
        }

        magicData.addMana(recoverMana);
    }
}
