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
                                        getResourceLocationRegistry(SpellRegistry.OAKSKIN_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.FORTIFY_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.HASTE_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.PLAYER_SELF
                                ),
                                new ChargedTwinBladeStaffSpellProfileDefinition(
                                        getResourceLocationRegistry(SpellRegistry.RAISE_DEAD_SPELL),
                                        ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST
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
