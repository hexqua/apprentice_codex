package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record MulticastEchoStaffMobEffectProfileList(List<MulticastEchoStaffMobEffectProfileDefinition> values) {
    public static final Codec<MulticastEchoStaffMobEffectProfileList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MulticastEchoStaffMobEffectProfileDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(MulticastEchoStaffMobEffectProfileList::values)
            ).apply(instance, MulticastEchoStaffMobEffectProfileList::new)
    );
}
