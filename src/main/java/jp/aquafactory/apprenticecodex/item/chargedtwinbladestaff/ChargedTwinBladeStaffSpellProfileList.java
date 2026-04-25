package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ChargedTwinBladeStaffSpellProfileList(List<ChargedTwinBladeStaffSpellProfileDefinition> values) {
    public static final Codec<ChargedTwinBladeStaffSpellProfileList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChargedTwinBladeStaffSpellProfileDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(ChargedTwinBladeStaffSpellProfileList::values)
            ).apply(instance, ChargedTwinBladeStaffSpellProfileList::new)
    );
}
