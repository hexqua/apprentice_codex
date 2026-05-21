package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileDefinition;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileList;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MulticastEchoStaffMobEffectProfileDataGenerator
        extends JsonCodecProvider<MulticastEchoStaffMobEffectProfileList> {
    public MulticastEchoStaffMobEffectProfileDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                MulticastEchoStaffMobEffectProfileManager.DIRECTORY,
                MulticastEchoStaffMobEffectProfileList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                        new MulticastEchoStaffMobEffectProfileList(List.of(
                                defaultDefinition(SpellRegistry.ECHOING_STRIKES_SPELL),
                                defaultDefinition(SpellRegistry.BLOOD_STEP_SPELL),
                                defaultDefinition(SpellRegistry.EVASION_SPELL),
                                defaultDefinition(SpellRegistry.PLANAR_SIGHT_SPELL),
                                defaultDefinition(SpellRegistry.ABYSSAL_SHROUD_SPELL),
                                defaultDefinition(SpellRegistry.INVISIBILITY_SPELL),
                                defaultDefinition(SpellRegistry.ANGEL_WINGS_SPELL),
                                defaultDefinition(SpellRegistry.HASTE_SPELL),
                                defaultDefinition(SpellRegistry.FORTIFY_SPELL),
                                defaultDefinition(SpellRegistry.OAKSKIN_SPELL),
                                defaultDefinition(SpellRegistry.CHARGE_SPELL),
                                defaultDefinition(SpellRegistry.SPIDER_ASPECT_SPELL),
                                defaultDefinition(SpellRegistry.GLUTTONY_SPELL),
                                defaultDefinition(SpellRegistry.FROSTBITE_SPELL),
                                defaultDefinition(SpellRegistry.THUNDERSTORM_SPELL),
                                defaultDefinition(SpellRegistry.ASCENSION_SPELL),
                                defaultDefinition(SpellRegistry.BLIGHT_SPELL),
                                defaultDefinition(SpellRegistry.SLOW_SPELL),
                                defaultDefinition(SpellRegistry.HEAT_SURGE_SPELL),
                                defaultDefinition(SpellRegistry.FROSTWAVE_SPELL),

                                defaultDefinition(jp.aquafactory.apprenticecodex.registry.SpellRegistry.DEEP_SENSOR)
                        ))
                )
        );
    }

    private static MulticastEchoStaffMobEffectProfileDefinition defaultDefinition(
            RegistryObject<AbstractSpell> spellRegistryObject
    ) {
        return new MulticastEchoStaffMobEffectProfileDefinition(
                getResourceLocationRegistry(spellRegistryObject),
                MulticastEchoStaffMobEffectProfile.DEFAULT_DURATION_EXTENSION
        );
    }

    private static ResourceLocation getResourceLocationRegistry(RegistryObject<AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(
                Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(),
                Objects.requireNonNull(spellRegistryObject.getId()).getPath()
        );
    }
}
