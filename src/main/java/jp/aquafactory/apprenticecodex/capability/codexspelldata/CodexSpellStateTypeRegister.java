package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AssistWingsState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FeatherRushState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MantisLeapState;

public final class CodexSpellStateTypeRegister {
    private CodexSpellStateTypeRegister(){}

    public static CodexSpellStateType<AssistWingsState> ASSIST_WINGS_STATE;
    public static CodexSpellStateType<FeatherRushState> FEATHER_RUSH_STATE;
    public static CodexSpellStateType<MantisLeapState> MANTIS_LEAP_STATE;

    public static void register() {
        ASSIST_WINGS_STATE = CodexSpellStates.register("assist_wings", AssistWingsState::new);
        FEATHER_RUSH_STATE = CodexSpellStates.register("feather_rush", FeatherRushState::new);
        MANTIS_LEAP_STATE = CodexSpellStates.register("mantis_leap", MantisLeapState::new);
    }
}
