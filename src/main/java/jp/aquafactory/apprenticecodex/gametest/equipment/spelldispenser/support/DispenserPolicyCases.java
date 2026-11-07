package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import java.util.List;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;

public final class DispenserPolicyCases {
    private DispenserPolicyCases() {}

    public record Denial(String name, boolean enabled, boolean allowlist,
                         SpellDispenserSpellValidator.FailureReason reason) {}

    public static List<Denial> denials() {
        return List.of(
                new Denial("server_disabled", false, false, SpellDispenserSpellValidator.FailureReason.SERVER_DISABLED),
                new Denial("removed_from_allowlist", true, true, SpellDispenserSpellValidator.FailureReason.NOT_ALLOWLISTED));
    }
}
