package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

public final class EpicFightSmashcastScepterCapability extends WeaponCapability {
    private static final AttributeModifier EPIC_FIGHT_ATTACK_SPEED_MODIFIER = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "smashcast_scepter_epicfight_attack_speed"),
            1.0D,
            AttributeModifier.Operation.ADD_VALUE
    );

    public EpicFightSmashcastScepterCapability(WeaponCapability.Builder builder) {
        super(builder);
    }

    @Override
    public @NotNull Style getStyle(LivingEntityPatch<?> entityPatch) {
        return CapabilityItem.Styles.ONE_HAND;
    }

    @Override
    public boolean checkOffhandValid(LivingEntityPatch<?> entityPatch) {
        // Epic Fight 21.17 の default_1h_wield_style は offhand 表示を許可するため、
        // Smashcast Scepter では旧実装どおりメインハンド時の併用も明示的に拒否する。
        return false;
    }

    @Override
    public boolean canHoldInOffhandAlone() {
        return false;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(LivingEntityPatch<?> entityPatch) {
        var modifiers = HashMultimap.create(super.getAttributeModifiers(entityPatch));
        modifiers.put(Attributes.ATTACK_SPEED, EPIC_FIGHT_ATTACK_SPEED_MODIFIER);
        return modifiers;
    }
}
