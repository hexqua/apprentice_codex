package jp.aquafactory.apprenticecodex.datagen.spell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileDefinition;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileList;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class RemoteOwnerCastSpellProfileDataGenerator extends JsonCodecProvider<RemoteOwnerCastProfileList> {
    public RemoteOwnerCastSpellProfileDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                RemoteOwnerCastProfileManager.DIRECTORY,
                PackType.SERVER_DATA,
                RemoteOwnerCastProfileList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                new RemoteOwnerCastProfileList(createProfileDefinitions())
        );
    }

    public static List<RemoteOwnerCastProfileDefinition> createProfileDefinitions() {
        var profiles = new LinkedHashMap<ResourceLocation, RemoteOwnerCastProfile>();

        for (var definition : SpellDispenserSpellProfileDataGenerator.createProfileDefinitions()) {
            profiles.put(definition.spell(), RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY);
        }

        putChargedStaffOnlyProfile(profiles, SpellRegistry.BLOOD_STEP_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.HEARTSTOP_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.RAISE_DEAD_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.PLANAR_SIGHT_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.COUNTERSPELL_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.ECHOING_STRIKES_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.EVASION_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.SUMMON_SWORDS, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.INVISIBILITY_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.SUMMON_HORSE_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.SUMMON_VEX_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.BURNING_DASH_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.HEAT_SURGE_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.ANGEL_WINGS_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.CLEANSE_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.FORTIFY_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.GREATER_HEAL_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.HASTE_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.HEAL_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.ICE_TOMB_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.SUMMON_POLAR_BEAR_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.ASCENSION_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.CHARGE_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.SHOCKWAVE_SPELL, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.VOLT_STRIKE_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.GLUTTONY_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.OAKSKIN_SPELL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, SpellRegistry.SPIDER_ASPECT_SPELL, playerSelfProfile(false));

        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCHER_MULTIPLE, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.HIGANBANA, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_MAGNET, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPANION_TRUNK, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SENSE_EVIL, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMMENCE_FIRE, remoteGeometryProfile(true));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.DEEP_SENSOR, playerSelfProfile(false));
        putChargedStaffOnlyProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SPECTRAL_WING, playerSelfProfile(false));

        return profiles.entrySet().stream()
                .map(entry -> new RemoteOwnerCastProfileDefinition(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static void putChargedStaffOnlyProfile(
            Map<ResourceLocation, RemoteOwnerCastProfile> profiles,
            Supplier<? extends AbstractSpell> spell,
            RemoteOwnerCastProfile profile
    ) {
        profiles.put(getResourceLocationRegistry(spell), profile);
    }

    private static RemoteOwnerCastProfile playerSelfProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.PLAYER_SELF,
                RemoteOwnerOriginMode.PLAYER_SELF,
                RemoteOwnerDirectionMode.PLAYER_LOOK,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                allowInitialRecast
        );
    }

    private static RemoteOwnerCastProfile remoteGeometryProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                RemoteOwnerOriginMode.PROVIDED_ORIGIN,
                RemoteOwnerDirectionMode.PROVIDED_FORWARD,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                allowInitialRecast
        );
    }

    private static ResourceLocation getResourceLocationRegistry(Supplier<? extends AbstractSpell> spellRegistryObject) {
        return Objects.requireNonNull(spellRegistryObject.get().getSpellResource());
    }
}
