package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AssistWingsState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AbsorptionAmplifyAmuletState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AutoMagnetState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.CompanionTrunkState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.DemicreatorWingsState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FeatherRushState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ForceFieldState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MantisLeapState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SpectralWingState;

public final class CodexSpellStateTypeRegister {
    private CodexSpellStateTypeRegister(){}

    public static CodexSpellStateType<AbsorptionAmplifyAmuletState> ABSORPTION_AMPLIFY_AMULET_STATE;
    public static CodexSpellStateType<AssistWingsState> ASSIST_WINGS_STATE;
    public static CodexSpellStateType<AutoMagnetState> AUTO_MAGNET_STATE;
    public static CodexSpellStateType<CompanionTrunkState> COMPANION_TRUNK_STATE;
    public static CodexSpellStateType<DemicreatorWingsState> DEMICREATOR_WINGS_STATE;
    public static CodexSpellStateType<FeatherRushState> FEATHER_RUSH_STATE;
    public static CodexSpellStateType<ForceFieldState> FORCE_FIELD_STATE;
    public static CodexSpellStateType<MantisLeapState> MANTIS_LEAP_STATE;
    public static CodexSpellStateType<RemoteEyeState> REMOTE_EYE_STATE;
    public static CodexSpellStateType<SearchBeaconState> SEARCH_BEACON_STATE;
    public static CodexSpellStateType<SpectralWingState> SPECTRAL_WING_STATE;

    public static void register() {
        ABSORPTION_AMPLIFY_AMULET_STATE = CodexSpellStates.register("absorption_amplify_amulet", AbsorptionAmplifyAmuletState::new);
        ASSIST_WINGS_STATE = CodexSpellStates.register("assist_wings", AssistWingsState::new);
        AUTO_MAGNET_STATE = CodexSpellStates.register("auto_magnet", AutoMagnetState::new);
        COMPANION_TRUNK_STATE = CodexSpellStates.register("companion_trunk", CompanionTrunkState::new);
        DEMICREATOR_WINGS_STATE = CodexSpellStates.register("demicreator_wings", DemicreatorWingsState::new);
        FEATHER_RUSH_STATE = CodexSpellStates.register("feather_rush", FeatherRushState::new);
        FORCE_FIELD_STATE = CodexSpellStates.register("force_field", ForceFieldState::new);
        MANTIS_LEAP_STATE = CodexSpellStates.register("mantis_leap", MantisLeapState::new);
        REMOTE_EYE_STATE = CodexSpellStates.register("remote_eye", RemoteEyeState::new);
        SEARCH_BEACON_STATE = CodexSpellStates.register("search_beacon", SearchBeaconState::new);
        SPECTRAL_WING_STATE = CodexSpellStates.register("spectral_wing", SpectralWingState::new);
    }
}
