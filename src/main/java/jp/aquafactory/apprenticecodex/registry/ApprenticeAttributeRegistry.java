package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ApprenticeAttributeRegistry {
    private ApprenticeAttributeRegistry() {
    }

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ApprenticeCodex.MODID);

    public static final RegistryObject<Attribute> MAX_ENCHANTMENT_TABLE_LEVEL =
            ATTRIBUTES.register(
                    "max_enchantment_table_level",
                    () -> new RangedAttribute(
                            "attribute.name." + ApprenticeCodex.MODID + ".max_enchantment_table_level",
                            0.0D,
                            0.0D,
                            2048.0D
                    ).setSyncable(true)
            );

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}
