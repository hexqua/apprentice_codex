package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo.EchoWeaponTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoWeaponAttackGameTests {
    private EchoWeaponAttackGameTests() {
    }

    // 全対応武器で「生成しただけ」「タイムアウトで消えただけ」を攻撃不成立として検出する。
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void everySupportedWeaponAttacksAndExpires(GameTestHelper helper) {
        var spells = List.of(SpellRegistry.SHIDEN.get(), SpellRegistry.BREACHING_ENEMY.get(),
                SpellRegistry.SLASH_BLADE.get(), SpellRegistry.MOON_LIGHT.get(), SpellRegistry.SILENT_ASSASSIN.get(),
                SpellRegistry.ARTISAN_SMASH.get(), SpellRegistry.PRECISION_JACK.get(),
                SpellRegistry.HIGANBANA.get(), SpellRegistry.LETHAL_ASSAULT.get());
        try (var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(true, 1.0D)) {
            for (var spell : spells) {
                // 追跡期間は弾の寿命に合わせて調整するため固定せず、補正は下の実ダメージで検証する。
                helper.assertTrue(MulticastEchoStaffAttackProfileManager.getProfile(spell).isPresent(),
                        "Supported weapon must have an attack profile: " + spell.getSpellId());
                var ids = new HashSet<UUID>();
                float normalDamage = 0;
                for (int cast = 0; cast < 3; cast++) {
                    var result = castAndFinishWeapon(helper, (AbstractSummonWeaponSpell<?>) spell, cast != 0);
                    helper.assertTrue(ids.add(result.id()), "Every cast must create a new weapon: " + spell.getSpellId());
                    if (cast == 0) {
                        normalDamage = result.damage();
                    } else {
                        helper.assertTrue(Math.abs(result.damage() - normalDamage * 0.5F) < 0.02F,
                                "Every attack including delayed hits must be halved exactly once: " + spell.getSpellId()
                                        + " normal=" + normalDamage + " repeated=" + result.damage());
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void rifleEchoTracksSupportedSummonWeapons(GameTestHelper helper) {
        ModConfigSpec.BooleanValue enabled = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.FullautoRapidcastSpellrifle.echoCastEnabled");
        ModConfigSpec.DoubleValue damage = ApprenticeCodexServerConfig.SPEC.getValues()
                .get("Items.FullautoRapidcastSpellrifle.echoCastDamageMultiplier");
        boolean oldEnabled = enabled.get();
        double oldDamage = damage.get();
        try (var staffSettings = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(false, 9)) {
            enabled.set(true);
            damage.set(1.0);
            for (var spell : List.of(SpellRegistry.SHIDEN.get(), SpellRegistry.BREACHING_ENEMY.get(),
                    SpellRegistry.SLASH_BLADE.get(), SpellRegistry.MOON_LIGHT.get(), SpellRegistry.SILENT_ASSASSIN.get(),
                    SpellRegistry.ARTISAN_SMASH.get(), SpellRegistry.PRECISION_JACK.get(),
                    SpellRegistry.HIGANBANA.get(), SpellRegistry.LETHAL_ASSAULT.get())) {
                var normal = castAndFinishWeapon(helper, (AbstractSummonWeaponSpell<?>) spell, false, true);
                var echo = castAndFinishWeapon(helper, (AbstractSummonWeaponSpell<?>) spell, true, true);
                helper.assertTrue(Math.abs(echo.damage() - normal.damage() * 0.5F) < 0.02F,
                        "Rifle summon attacks must be halved exactly once, including delayed hits: "
                                + spell.getSpellId() + " normal=" + normal.damage() + " echo=" + echo.damage());
            }
        } finally {
            enabled.set(oldEnabled);
            damage.set(oldDamage);
        }
        helper.succeed();
    }
}
