package jp.aquafactory.apprenticecodex.remoteownercast;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RemoteOwnerCastAnchorAttributes {
    private static final List<Holder<Attribute>> BASE_SYNC_ATTRIBUTES = List.of(
            AttributeRegistry.MAX_MANA,
            AttributeRegistry.MANA_REGEN,
            AttributeRegistry.COOLDOWN_REDUCTION,
            AttributeRegistry.SPELL_POWER,
            AttributeRegistry.SPELL_RESIST,
            AttributeRegistry.CAST_TIME_REDUCTION,
            AttributeRegistry.SUMMON_DAMAGE,
            AttributeRegistry.CASTING_MOVESPEED
    );

    private RemoteOwnerCastAnchorAttributes() {
    }

    public static AttributeSupplier.Builder addSyncAttributes(AttributeSupplier.Builder builder) {
        for (var attribute : resolveSyncAttributes()) {
            builder.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
        }
        return builder;
    }

    public static void syncFromOwner(ServerPlayer owner, RemoteOwnerCastAnchorEntity anchor) {
        for (var attribute : resolveSyncAttributes()) {
            var attributeHolder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
            var anchorAttribute = anchor.getAttribute(attributeHolder);
            if (anchorAttribute != null) {
                anchorAttribute.setBaseValue(owner.getAttributeValue(attributeHolder));
            }
        }
    }

    public static Set<Attribute> resolveSyncAttributes() {
        var attributes = new LinkedHashSet<Attribute>();
        for (var attributeRef : BASE_SYNC_ATTRIBUTES) {
            attributes.add(attributeRef.value());
        }

        for (var schoolType : SchoolRegistry.REGISTRY) {
            var powerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
            if (powerAttribute != null) {
                attributes.add(powerAttribute);
            }
            var resistAttribute = MagicTools.resolveSchoolResistAttribute(schoolType);
            if (resistAttribute != null) {
                attributes.add(resistAttribute);
            }
        }
        return attributes;
    }
}
