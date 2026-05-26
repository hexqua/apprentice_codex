package jp.aquafactory.apprenticecodex.remoteownercast;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RemoteOwnerCastAnchorAttributes {
    private static final List<RegistryObject<Attribute>> BASE_SYNC_ATTRIBUTES = List.of(
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
            builder.add(attribute);
        }
        return builder;
    }

    public static void syncFromOwner(ServerPlayer owner, RemoteOwnerCastAnchorEntity anchor) {
        for (var attribute : resolveSyncAttributes()) {
            var anchorAttribute = anchor.getAttribute(attribute);
            if (anchorAttribute != null) {
                anchorAttribute.setBaseValue(owner.getAttributeValue(attribute));
            }
        }
    }

    public static Set<Attribute> resolveSyncAttributes() {
        var attributes = new LinkedHashSet<Attribute>();
        for (var attributeRef : BASE_SYNC_ATTRIBUTES) {
            attributes.add(attributeRef.get());
        }

        for (var schoolType : SchoolRegistry.REGISTRY.get().getValues()) {
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
