package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record MulticastEchoStaffMobEffectProfileDefinition(
        ResourceLocation spell,
        MulticastEchoStaffMobEffectProfile profile
) {
    public static final Codec<MulticastEchoStaffMobEffectProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("spell").forGetter(MulticastEchoStaffMobEffectProfileDefinition::spell),
                    MulticastEchoStaffMobEffectProfile.CODEC.fieldOf("profile")
                            .forGetter(MulticastEchoStaffMobEffectProfileDefinition::profile)
            ).apply(instance, MulticastEchoStaffMobEffectProfileDefinition::new)
    );
}
