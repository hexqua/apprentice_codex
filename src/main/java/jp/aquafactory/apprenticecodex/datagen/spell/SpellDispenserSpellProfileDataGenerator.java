package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfile;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileList;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SpellDispenserSpellProfileDataGenerator extends JsonCodecProvider<SpellDispenserSpellProfileList> {
    public SpellDispenserSpellProfileDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SpellDispenserSpellProfileManager.DIRECTORY,
                SpellDispenserSpellProfileList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                        new SpellDispenserSpellProfileList(List.of(
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ACUPUNCTURE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLOOD_NEEDLES_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLOOD_SLASH_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.DEVOUR_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.RAY_OF_SIPHONING_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.WITHER_SKULL_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SCULK_TENTACLES_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SONIC_BOOM_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLACK_HOLE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.DRAGON_BREATH_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.MAGIC_ARROW_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.MAGIC_MISSILE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHADOW_SLASH),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.STARFALL_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ARROW_VOLLEY_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CHAIN_CREEPER_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FANG_STRIKE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FANG_WARD_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIRECRACKER_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.GUST_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.LOB_CREEPER_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHIELD_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SLOW_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SPECTRAL_HAMMER_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLAZE_STORM_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIRE_ARROW_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIRE_BREATH_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIREBALL_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIREBOLT_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FLAMING_STRIKE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.MAGMA_BOMB_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SCORCH_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLESSING_OF_LIFE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CLEANSE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.DIVINE_SMITE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FORTIFY_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.GUIDING_BOLT_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HASTE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HEALING_CIRCLE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SUNBEAM_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.WISP_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CONE_OF_COLD_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FROSTWAVE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICE_BLOCK_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICE_SPIKES_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICICLE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.RAY_OF_FROST_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SNOWBALL_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BALL_LIGHTNING_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CHAIN_LIGHTNING_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ELECTROCUTE_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.LIGHTNING_BOLT_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.LIGHTNING_LANCE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHOCKWAVE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ACID_ORB_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLIGHT_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.EARTHQUAKE_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIREFLY_SWARM_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.POISON_ARROW_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.POISON_BREATH_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.POISON_SPLASH_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ROOT_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.STOMP_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.TOUCH_DIG),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BLAST),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BEAM),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.QUICK_ARMS),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.FEATHER_RUSH),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.PRECISION_JACK),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_TURRET),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SEARCH_BEACON),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.THERMAL_PROCESS),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ILLUMINATE_STELLAR),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SKY_EDGE),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.BREACHING_ENEMY),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.BULLET_STREAM),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.FLY_SWATTER),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPOUND_PHIAL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.TINY_LUMBERJACK),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.GRACED_RAIN),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.WORLD_FLATTER),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.EARTH_FORGE),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.GRIND_RUNNER),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.HARVEST_MOON),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MOON_LIGHT),
                                        SpellDispenserSpellProfile.DEFAULT
                                )
                        ))
                )
        );
    }

    private static ResourceLocation getResourceLocationRegistry(RegistryObject<AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(), Objects.requireNonNull(spellRegistryObject.getId()).getPath());
    }
}
