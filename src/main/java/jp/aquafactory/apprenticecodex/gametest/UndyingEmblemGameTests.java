package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.CapabilityEvents;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemEvents;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class UndyingEmblemGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private UndyingEmblemGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void fatalDamageActivatesTotemEquivalentProtectionOnce(GameTestHelper helper) {
        var player = createEquippedPlayer(helper, "undying_emblem_death");
        player.setHealth(0.0F);
        var firstDeath = new LivingDeathEvent(player, player.damageSources().generic());
        UndyingEmblemEvents.onLivingDeath(firstDeath);

        helper.assertTrue(firstDeath.isCanceled(), "Undying Emblem should cancel eligible fatal damage");
        helper.assertValueEqual(player.getHealth(), 1.0F, "Undying Emblem surviving health");
        helper.assertValueEqual(
                UndyingEmblemRuntime.getRemainingCooldownTicks(player),
                UndyingEmblemRuntime.COOLDOWN_TICKS,
                "Undying Emblem cooldown after activation"
        );
        assertEffect(helper, player.getEffect(MobEffects.REGENERATION), 45 * 20, 1, "regeneration");
        assertEffect(helper, player.getEffect(MobEffects.FIRE_RESISTANCE), 40 * 20, 0, "fire resistance");
        assertEffect(helper, player.getEffect(MobEffects.ABSORPTION), 5 * 20, 1, "absorption");

        var secondDeath = new LivingDeathEvent(player, player.damageSources().generic());
        UndyingEmblemEvents.onLivingDeath(secondDeath);
        helper.assertFalse(secondDeath.isCanceled(), "Undying Emblem must not reactivate during cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathCloneResetsCooldown(GameTestHelper helper) {
        var original = createEquippedPlayer(helper, "undying_emblem_clone_original");
        var clone = createEquippedPlayer(helper, "undying_emblem_clone_new");
        Capabilities.withSpellData(original, data -> data.edit(
                CodexSpellStateTypeRegister.UNDYING_EMBLEM_STATE,
                state -> state.setRemainingCooldownTicks(UndyingEmblemRuntime.COOLDOWN_TICKS)
        ));
        Capabilities.withSpellData(clone, data -> data.edit(
                CodexSpellStateTypeRegister.UNDYING_EMBLEM_STATE,
                state -> state.setRemainingCooldownTicks(1)
        ));

        CapabilityEvents.onPlayerClone(new PlayerEvent.Clone(clone, original, true));

        helper.assertValueEqual(
                UndyingEmblemRuntime.getRemainingCooldownTicks(clone),
                0,
                "Undying Emblem cooldown after death clone"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bypassDamageIsRejectedExceptVanillaVoidDamage(GameTestHelper helper) {
        var player = createEquippedPlayer(helper, "undying_emblem_bypass");
        var bypassDeath = new LivingDeathEvent(player, player.damageSources().genericKill());
        UndyingEmblemEvents.onLivingDeath(bypassDeath);
        helper.assertFalse(bypassDeath.isCanceled(), "Invulnerability-bypassing damage should bypass Undying Emblem");
        helper.assertValueEqual(UndyingEmblemRuntime.getRemainingCooldownTicks(player), 0,
                "Rejected damage should not start cooldown");

        var voidDeath = new LivingDeathEvent(player, player.damageSources().fellOutOfWorld());
        UndyingEmblemEvents.onLivingDeath(voidDeath);
        helper.assertTrue(voidDeath.isCanceled(), "Vanilla void damage should activate Undying Emblem");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void idolReconstructionUsesConfiguredTenTickAcceleration(GameTestHelper helper) {
        var player = createEquippedPlayer(helper, "idol_reconstruction_acceleration");
        Capabilities.withSpellData(player, data -> data.edit(
                CodexSpellStateTypeRegister.UNDYING_EMBLEM_STATE,
                state -> state.setRemainingCooldownTicks(1_000)
        ));

        try (var ignored = ApprenticeCodexServerConfig
                .useUndyingEmblemReconstructionSpeedMultiplierOverrideForGameTest(10)) {
            var spell = SpellRegistry.IDOL_RECONSTRUCTION.get();
            helper.assertValueEqual(spell.getEffectiveCastTime(1, player), 100,
                    "Idol Reconstruction effective cast time");
            spell.onCast(
                    player.level(),
                    1,
                    player,
                    CastSource.SPELLBOOK,
                    MagicData.getPlayerMagicData(player)
            );
            helper.assertValueEqual(UndyingEmblemRuntime.getRemainingCooldownTicks(player), 910,
                    "Idol Reconstruction additional cooldown progress per pulse");
        }
        helper.succeed();
    }

    private static net.neoforged.neoforge.common.util.FakePlayer createEquippedPlayer(
            GameTestHelper helper,
            String profileName
    ) {
        var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                profileName
        );
        ApprenticeCodexGameTestScenarios.equipCurio(
                player,
                CuriosSlotConstants.CHARM,
                new ItemStack(ItemRegistry.UNDYING_EMBLEM.get())
        );
        return player;
    }

    private static void assertEffect(
            GameTestHelper helper,
            @Nullable net.minecraft.world.effect.MobEffectInstance effect,
            int duration,
            int amplifier,
            String name
    ) {
        if (effect == null) {
            helper.fail("Undying Emblem should grant " + name);
            return;
        }
        helper.assertValueEqual(effect.getDuration(), duration, "Undying Emblem " + name + " duration");
        helper.assertValueEqual(effect.getAmplifier(), amplifier, "Undying Emblem " + name + " amplifier");
    }
}
