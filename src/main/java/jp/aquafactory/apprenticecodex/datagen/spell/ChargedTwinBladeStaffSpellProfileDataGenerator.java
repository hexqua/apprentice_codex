package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfile;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileList;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChargedTwinBladeStaffSpellProfileDataGenerator extends JsonCodecProvider<ChargedTwinBladeStaffSpellProfileList> {
    public ChargedTwinBladeStaffSpellProfileDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                ChargedTwinBladeStaffSpellProfileManager.DIRECTORY,
                ChargedTwinBladeStaffSpellProfileList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                        new ChargedTwinBladeStaffSpellProfileList(List.of(
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLOOD_STEP_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HEARTSTOP_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.RAISE_DEAD_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.PLANAR_SIGHT_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.COUNTERSPELL_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ECHOING_STRIKES_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.EVASION_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SUMMON_SWORDS),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.INVISIBILITY_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SUMMON_HORSE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SUMMON_VEX_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BURNING_DASH_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HEAT_SURGE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ANGEL_WINGS_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CLEANSE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FORTIFY_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.GREATER_HEAL_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HASTE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HEAL_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICE_TOMB_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SUMMON_POLAR_BEAR_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ASCENSION_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CHARGE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHOCKWAVE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.VOLT_STRIKE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.GLUTTONY_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.OAKSKIN_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SPIDER_ASPECT_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),

                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCHER_MULTIPLE),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.HIGANBANA),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_MAGNET),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_MAGNET),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPANION_TRUNK),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SENSE_EVIL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMMENCE_FIRE),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.DEEP_SENSOR),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SPECTRAL_WING),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                )
                        ))
                )
        );
    }

    private static ResourceLocation getResourceLocationRegistry(RegistryObject<AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(
                Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(),
                Objects.requireNonNull(spellRegistryObject.getId()).getPath()
        );
    }
}
