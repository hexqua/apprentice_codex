package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record MulticastEchoStaffAttackProfileDefinition(
        ResourceLocation spell,
        MulticastEchoStaffAttackProfile profile
) {
    public static final Codec<MulticastEchoStaffAttackProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("spell").forGetter(MulticastEchoStaffAttackProfileDefinition::spell),
                    MulticastEchoStaffAttackProfile.CODEC.fieldOf("profile")
                            .forGetter(MulticastEchoStaffAttackProfileDefinition::profile)
            ).apply(instance, MulticastEchoStaffAttackProfileDefinition::new)
    );
}
