package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ChargedTwinBladeStaffSpellProfileDefinition(
        ResourceLocation spell,
        ChargedTwinBladeStaffSpellProfile profile
) {
    public static final Codec<ChargedTwinBladeStaffSpellProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("spell").forGetter(ChargedTwinBladeStaffSpellProfileDefinition::spell),
                    ChargedTwinBladeStaffSpellProfile.CODEC.fieldOf("profile")
                            .forGetter(ChargedTwinBladeStaffSpellProfileDefinition::profile)
            ).apply(instance, ChargedTwinBladeStaffSpellProfileDefinition::new)
    );
}
