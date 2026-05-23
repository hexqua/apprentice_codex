package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record MulticastEchoStaffAttackProfileList(List<MulticastEchoStaffAttackProfileDefinition> values) {
    public static final Codec<MulticastEchoStaffAttackProfileList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MulticastEchoStaffAttackProfileDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(MulticastEchoStaffAttackProfileList::values)
            ).apply(instance, MulticastEchoStaffAttackProfileList::new)
    );
}
