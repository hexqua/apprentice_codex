package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

public class SchoolAffinityEffect extends MobEffect {
    private static final double SPELL_POWER_BONUS_PER_LEVEL = 0.10D;
    private static final int FALLBACK_COLOR = 0x1E90FF;
    private static final String DESCRIPTION_ID = "effect.apprenticecodex.school_affinity";

    private final int slotIndex;

    public SchoolAffinityEffect(int slotIndex) {
        super(MobEffectCategory.BENEFICIAL, FALLBACK_COLOR);
        this.slotIndex = slotIndex;
    }

    @Nullable
    public SchoolType getAssignedSchool() {
        return SchoolAffinityRegistry.getAssignedSchool(slotIndex).orElse(null);
    }

    @Override
    public String getDescriptionId() {
        return DESCRIPTION_ID;
    }

    @Override
    public Component getDisplayName() {
        var schoolType = getAssignedSchool();
        if (schoolType == null) {
            return Component.translatable(DESCRIPTION_ID);
        }
        return SchoolAffinityRegistry.createAffinityName(schoolType);
    }

    @Override
    public int getColor() {
        var schoolType = getAssignedSchool();
        return schoolType != null ? SchoolAffinityRegistry.resolveColor(schoolType) : FALLBACK_COLOR;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
        var schoolType = getAssignedSchool();
        if (schoolType == null) {
            return;
        }

        var spellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
        if (spellPowerAttribute == null) {
            return;
        }

        var attributeInstance = attributeMap.getInstance(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(spellPowerAttribute));
        if (attributeInstance == null) {
            return;
        }

        attributeInstance.removeModifier(getModifierId());
        attributeInstance.addPermanentModifier(createModifier(amplifier));
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        var schoolType = getAssignedSchool();
        if (schoolType == null) {
            return;
        }

        var spellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
        if (spellPowerAttribute == null) {
            return;
        }

        var attributeInstance = attributeMap.getInstance(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(spellPowerAttribute));
        if (attributeInstance != null) {
            attributeInstance.removeModifier(getModifierId());
        }
    }

    private ResourceLocation getModifierId() {
        return ResourceLocation.fromNamespaceAndPath("apprenticecodex", "school_affinity_slot_" + slotIndex);
    }

    private AttributeModifier createModifier(int amplifier) {
        return new AttributeModifier(
                getModifierId(),
                SPELL_POWER_BONUS_PER_LEVEL * (amplifier + 1),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
