package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ApprenticeAttributeRegistry {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, ApprenticeCodex.MODID);

    public static final DeferredHolder<Attribute, Attribute> SPELLGUN_AMMO_CONSERVATION =
            ATTRIBUTES.register("spellgun_ammo_conservation", () -> new PercentageAttribute(
                    "attribute.name." + ApprenticeCodex.MODID + ".spellgun_ammo_conservation",
                    1.0D, 0.0D, 1.0D
            ).setSentiment(Attribute.Sentiment.NEGATIVE).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MAX_ENCHANTMENT_TABLE_LEVEL =
            ATTRIBUTES.register(
                    "max_enchantment_table_level",
                    () -> new RangedAttribute(
                            "attribute.name." + ApprenticeCodex.MODID + ".max_enchantment_table_level",
                            0.0D,
                            0.0D,
                            2048.0D
                    ).setSyncable(true)
            );

    private ApprenticeAttributeRegistry() {
    }

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}
