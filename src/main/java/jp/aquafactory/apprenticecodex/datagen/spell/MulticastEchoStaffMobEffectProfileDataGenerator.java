package jp.aquafactory.apprenticecodex.datagen.spell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileDefinition;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileList;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffMobEffectProfileManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class MulticastEchoStaffMobEffectProfileDataGenerator
        extends JsonCodecProvider<MulticastEchoStaffMobEffectProfileList> {
    public MulticastEchoStaffMobEffectProfileDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                MulticastEchoStaffMobEffectProfileManager.DIRECTORY,
                PackType.SERVER_DATA,
                MulticastEchoStaffMobEffectProfileList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
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
        );
    }

    private static MulticastEchoStaffMobEffectProfileDefinition defaultDefinition(
            Supplier<? extends AbstractSpell> spellRegistryObject
    ) {
        return new MulticastEchoStaffMobEffectProfileDefinition(
                spellRegistryObject.get().getSpellResource(),
                MulticastEchoStaffMobEffectProfile.DEFAULT_DURATION_EXTENSION
        );
    }
}
