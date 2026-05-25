package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RemoteOwnerCastProfileList(List<RemoteOwnerCastProfileDefinition> values) {
    public static final Codec<RemoteOwnerCastProfileList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RemoteOwnerCastProfileDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(RemoteOwnerCastProfileList::values)
            ).apply(instance, RemoteOwnerCastProfileList::new)
    );
}
