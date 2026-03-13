package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.AlacrityEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.AttunementEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.PlunderEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.RefluxEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.ReservoirEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.SurgeEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.TenseEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceEnchantment;
import jp.aquafactory.apprenticecodex.enchantment.WisdomEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EnchantmentRegistry {
    private EnchantmentRegistry() {}

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ApprenticeCodex.MODID);

    public static final RegistryObject<Enchantment> REFLUX =
            ENCHANTMENTS.register("reflux", RefluxEnchantment::new);
    public static final RegistryObject<Enchantment> RESERVOIR =
            ENCHANTMENTS.register("reservoir", ReservoirEnchantment::new);
    public static final RegistryObject<Enchantment> ALACRITY =
            ENCHANTMENTS.register("alacrity", AlacrityEnchantment::new);
    public static final RegistryObject<Enchantment> TENSE =
            ENCHANTMENTS.register("tense", TenseEnchantment::new);
    public static final RegistryObject<Enchantment> SURGE =
            ENCHANTMENTS.register("surge", SurgeEnchantment::new);
    public static final RegistryObject<Enchantment> ATTUNEMENT =
            ENCHANTMENTS.register("attunement", AttunementEnchantment::new);
    public static final RegistryObject<Enchantment> TRANSCENDENCE =
            ENCHANTMENTS.register("transcendence", TranscendenceEnchantment::new);
    public static final RegistryObject<Enchantment> WISDOM =
            ENCHANTMENTS.register("wisdom", WisdomEnchantment::new);
    public static final RegistryObject<Enchantment> PLUNDER =
            ENCHANTMENTS.register("plunder", PlunderEnchantment::new);

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }
}
