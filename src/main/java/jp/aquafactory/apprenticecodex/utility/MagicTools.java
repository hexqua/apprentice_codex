package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.mixin.SchoolTypeAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class MagicTools {
    private static final float MANA_UI_SAFE_MARGIN = 0.1f;

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
            var holder = accessor.apprenticecodex$getPowerAttribute();
            if (holder != null) {
                var resolved = holder.value();
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        var schoolId = schoolType.getId();
        var fallbackAttributeId = SCHOOL_POWER_ATTRIBUTE_FALLBACK_MAP.get(schoolId);
        if (fallbackAttributeId != null) {
            var fallbackAttribute = BuiltInRegistries.ATTRIBUTE.getOptional(fallbackAttributeId).orElse(null);
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
        return BuiltInRegistries.ATTRIBUTE.getOptional(guessedAttributeId).orElse(null);
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

        var maxMana = (float) entity.getAttributeValue(AttributeRegistry.MAX_MANA);
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

