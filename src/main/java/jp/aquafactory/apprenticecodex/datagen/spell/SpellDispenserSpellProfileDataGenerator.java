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
                        new SpellDispenserSpellProfileList(createProfileDefinitions())
                )
        );
    }

    public static List<SpellDispenserSpellProfileDefinition> createProfileDefinitions() {
        return List.of(
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ACUPUNCTURE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLOOD_NEEDLES_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLOOD_SLASH_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.DEVOUR_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.WITHER_SKULL_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SCULK_TENTACLES_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SONIC_BOOM_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLACK_HOLE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.DRAGON_BREATH_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.MAGIC_ARROW_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.MAGIC_MISSILE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHADOW_SLASH),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.STARFALL_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ARROW_VOLLEY_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CHAIN_CREEPER_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FANG_STRIKE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FANG_WARD_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIRECRACKER_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.GUST_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.LOB_CREEPER_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHIELD_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SLOW_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SPECTRAL_HAMMER_SPELL),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLAZE_STORM_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIRE_ARROW_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIRE_BREATH_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIREBALL_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIREBOLT_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FLAMING_STRIKE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.MAGMA_BOMB_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SCORCH_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLESSING_OF_LIFE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CLEANSE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.DIVINE_SMITE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FORTIFY_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.GUIDING_BOLT_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HASTE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HEALING_CIRCLE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SUNBEAM_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.WISP_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CONE_OF_COLD_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FROSTWAVE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICE_BLOCK_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICE_SPIKES_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ICICLE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.RAY_OF_FROST_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SNOWBALL_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BALL_LIGHTNING_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.CHAIN_LIGHTNING_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ELECTROCUTE_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.LIGHTNING_BOLT_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.LIGHTNING_LANCE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.SHOCKWAVE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ACID_ORB_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.BLIGHT_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.EARTHQUAKE_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FIREFLY_SWARM_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.POISON_ARROW_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.POISON_BREATH_SPELL),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.POISON_SPLASH_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.ROOT_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.STOMP_SPELL),
                                        SpellDispenserSpellProfile.PROXY_NEUTRAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.TOUCH_DIG),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BLAST),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BEAM),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL_BACKWARD
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL_UP
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
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.THERMAL_PROCESS),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ILLUMINATE_STELLAR),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SKY_EDGE),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.BREACHING_ENEMY),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.BULLET_STREAM),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.FLY_SWATTER),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPOUND_PHIAL),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.TINY_LUMBERJACK),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.GRACED_RAIN),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.WORLD_FLATTER),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.EARTH_FORGE),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.GRIND_RUNNER),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.HARVEST_MOON),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MOON_LIGHT),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.TIRO_VOLLEY),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.SILENT_ASSASSIN),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGIC_SPEAR),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.FROST_RUNE),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.INSCRIBE_ICE),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.LETHAL_ASSAULT),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.DUAL_ACROBAT),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        getResourceLocationRegistry(jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARTISAN_SMASH),
                                        SpellDispenserSpellProfile.OWNER_OPTIONAL
                                )
                        );
    }

    private static ResourceLocation getResourceLocationRegistry(RegistryObject<AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(), Objects.requireNonNull(spellRegistryObject.getId()).getPath());
    }
}
