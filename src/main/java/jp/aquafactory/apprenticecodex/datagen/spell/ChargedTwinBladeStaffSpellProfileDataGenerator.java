package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfile;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileList;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChargedTwinBladeStaffSpellProfileDataGenerator extends JsonCodecProvider<ChargedTwinBladeStaffSpellProfileList> {
    public ChargedTwinBladeStaffSpellProfileDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                ChargedTwinBladeStaffSpellProfileManager.DIRECTORY,
                PackType.SERVER_DATA,
                ChargedTwinBladeStaffSpellProfileList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                new ChargedTwinBladeStaffSpellProfileList(List.of(
                        profile("oakskin", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("fortify", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("haste", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("raise_dead", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST)
                ))
        );
    }

    private static ChargedTwinBladeStaffSpellProfileDefinition profile(
            String path,
            ChargedTwinBladeStaffSpellProfile profile
    ) {
        return new ChargedTwinBladeStaffSpellProfileDefinition(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", path),
                profile
        );
    }
}
