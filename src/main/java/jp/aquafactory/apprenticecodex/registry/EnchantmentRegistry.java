package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentRegistry {
    private EnchantmentRegistry() {
    }

    public static final EnchantmentRef REFLUX = ref(Enchantments.REFLUX);
    public static final EnchantmentRef RESERVOIR = ref(Enchantments.RESERVOIR);
    public static final EnchantmentRef ALACRITY = ref(Enchantments.ALACRITY);
    public static final EnchantmentRef TENSE = ref(Enchantments.TENSE);
    public static final EnchantmentRef SURGE = ref(Enchantments.SURGE);
    public static final EnchantmentRef ATTUNEMENT = ref(Enchantments.ATTUNEMENT);
    public static final EnchantmentRef TRANSCENDENCE = ref(Enchantments.TRANSCENDENCE);
    public static final EnchantmentRef WISDOM = ref(Enchantments.WISDOM);
    public static final EnchantmentRef PLUNDER = ref(Enchantments.PLUNDER);
    public static final EnchantmentRef GUZZLE = ref(Enchantments.GUZZLE);
    public static final EnchantmentRef LARGE_MUG = ref(Enchantments.LARGE_MUG);
    public static final EnchantmentRef RED_ENERGY = ref(Enchantments.RED_ENERGY);
    public static final EnchantmentRef GLOW_ENERGY = ref(Enchantments.GLOW_ENERGY);
    public static final EnchantmentRef SYNTHESIS = ref(Enchantments.SYNTHESIS);
    public static final EnchantmentRef SHELL = ref(Enchantments.SHELL);
    public static final EnchantmentRef SYNCHRONIZATION = ref(Enchantments.SYNCHRONIZATION);
    public static final EnchantmentRef NEUTRALIZATION = ref(Enchantments.NEUTRALIZATION);

    private static EnchantmentRef ref(ResourceKey<Enchantment> key) {
        return new EnchantmentRef(key);
    }

    public record EnchantmentRef(ResourceKey<Enchantment> key) {
        public Holder<Enchantment> get() {
            return BuiltInRegistries.ENCHANTMENT.getHolderOrThrow(key);
        }

        public boolean isPresent() {
            return BuiltInRegistries.ENCHANTMENT.containsKey(location());
        }

        public ResourceLocation location() {
            return key.location();
        }

        public ResourceLocation getId() {
            return location();
        }
    }
}
