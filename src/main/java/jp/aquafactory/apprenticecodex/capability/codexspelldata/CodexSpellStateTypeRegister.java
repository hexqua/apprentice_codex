package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AssistWingsState;

public final class CodexSpellStateTypeRegister {
    private CodexSpellStateTypeRegister(){}

    public static CodexSpellStateType<AssistWingsState> ASSIST_WINGS_STATE;

    public static void register() {
        ASSIST_WINGS_STATE = CodexSpellStates.register("assist_wings", AssistWingsState::new);
    }
}
