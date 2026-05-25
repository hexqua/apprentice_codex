package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record RemoteOwnerCastProfileDefinition(
        ResourceLocation spell,
        RemoteOwnerCastProfile profile
) {
    public static final Codec<RemoteOwnerCastProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("spell").forGetter(RemoteOwnerCastProfileDefinition::spell),
                    RemoteOwnerCastProfile.CODEC.fieldOf("profile").forGetter(RemoteOwnerCastProfileDefinition::profile)
            ).apply(instance, RemoteOwnerCastProfileDefinition::new)
    );
}
