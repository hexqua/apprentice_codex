package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowEnchantmentBonus;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeDefinition;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeList;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ElementalBowModeDataGenerator extends JsonCodecProvider<ElementalBowModeList> {
    public ElementalBowModeDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                ElementalBowModeManager.DIRECTORY,
                ElementalBowModeList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "default_modes"),
                        new ElementalBowModeList(List.of(
                                new ElementalBowModeDefinition(
                                        SchoolRegistry.FIRE_RESOURCE,
                                        getSpellId(SpellRegistry.FIRE_ARROW_SPELL),
                                        ElementalBowModeDefinition.DEFAULT_REQUIRED_DRAW_TICKS,
                                        List.of(new ElementalBowEnchantmentBonus(getEnchantmentId(Enchantments.FLAMING_ARROWS), 0, 2))
                                ),
                                new ElementalBowModeDefinition(
                                        SchoolRegistry.ENDER_RESOURCE,
                                        getSpellId(SpellRegistry.MAGIC_ARROW_SPELL),
                                        ElementalBowModeDefinition.DEFAULT_REQUIRED_DRAW_TICKS,
                                        List.of()
                                ),
                                new ElementalBowModeDefinition(
                                        SchoolRegistry.NATURE_RESOURCE,
                                        getSpellId(SpellRegistry.POISON_ARROW_SPELL),
                                        ElementalBowModeDefinition.DEFAULT_REQUIRED_DRAW_TICKS,
                                        List.of()
                                )
                        ))
                )
        );
    }

    private static ResourceLocation getSpellId(RegistryObject<AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(
                Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(),
                Objects.requireNonNull(spellRegistryObject.getId()).getPath()
        );
    }

    private static ResourceLocation getEnchantmentId(Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return Objects.requireNonNull(enchantmentId);
    }
}
