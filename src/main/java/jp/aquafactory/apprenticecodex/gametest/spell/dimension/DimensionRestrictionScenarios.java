package jp.aquafactory.apprenticecodex.gametest.spell.dimension;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWings;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsManager;
import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEye;
import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEyeBodyControlEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.event.TickEvent;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.EquipmentStateTestSupport.getRemoteEyeState;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

final class DimensionRestrictionScenarios {
    private DimensionRestrictionScenarios() {}

    static void riftHoleDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        var currentDimensionId = helper.getLevel().dimension().location().toString();
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(currentDimensionId),
                false,
                List.of()
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(helper.getLevel().dimension().location()),
                    "RiftHole dimension denylist should reject the current dimension"
            );
        }
        helper.succeed();
    }

    static void riftHoleDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(),
                true,
                List.of("minecraft:the_nether")
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(currentDimension),
                    "RiftHole dimension allowlist should reject unlisted dimensions"
            );
        }
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertTrue(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(currentDimension),
                    "RiftHole dimension allowlist should accept the current dimension"
            );
        }
        helper.succeed();
    }

    static void riftHoleDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRiftHoleConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRiftHoleDimensionAllowed(currentDimension),
                    "RiftHole dimension denylist should override the allowlist"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "demicreator_wings_dimension_deny_test");
        var spell = SpellRegistry.DEMICREATOR_WINGS.get();
        var magicData = MagicData.getPlayerMagicData(player);
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                false,
                List.of()
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension denylist should reject the current dimension"
            );
            helper.assertFalse(
                    spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "DemicreatorWings pre-cast should reject a denied dimension"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(),
                true,
                List.of("minecraft:the_nether")
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension allowlist should reject unlisted dimensions"
            );
        }
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertTrue(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension allowlist should accept the current dimension"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(currentDimension),
                    "DemicreatorWings dimension denylist should override the allowlist"
            );
        }
        helper.succeed();
    }

    static void demicreatorWingsDimensionRestrictionAllowsCloseCast(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "demicreator_wings_close_dimension_test");
        var spell = (DemicreatorWings) SpellRegistry.DEMICREATOR_WINGS.get();
        var magicData = MagicData.getPlayerMagicData(player);

        try {
            DemicreatorWingsManager.activate(player, 1, CastSource.SPELLBOOK, magicData, spell);
            try (var ignored = ApprenticeCodexServerConfig.useDemicreatorWingsConfigOverrideForGameTest(
                    List.of(currentDimension.toString()),
                    false,
                    List.of()
            )) {
                helper.assertTrue(
                        spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                        "DemicreatorWings close cast should remain allowed in a denied dimension"
                );
                helper.assertTrue(
                        magicData.getAdditionalCastData() instanceof DemicreatorWings.DemicreatorWingsCastData castData
                                && castData.isCloseCast(),
                        "DemicreatorWings close cast should mark close cast data"
                );
            }
        } finally {
            DemicreatorWingsManager.deactivate(player, true);
        }
        helper.succeed();
    }

    static void remoteEyeDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "remote_eye_dimension_deny_test");
        var spell = SpellRegistry.REMOTE_EYE.get();
        var magicData = MagicData.getPlayerMagicData(player);
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                false,
                List.of()
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension denylist should reject the current dimension"
            );
            helper.assertFalse(
                    spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "RemoteEye pre-cast should reject a denied dimension"
            );
        }
        helper.succeed();
    }

    static void remoteEyeDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(),
                true,
                List.of("minecraft:the_nether")
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension allowlist should reject unlisted dimensions"
            );
        }
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertTrue(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension allowlist should accept the current dimension"
            );
        }
        helper.succeed();
    }

    static void remoteEyeDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        var currentDimension = helper.getLevel().dimension().location();
        try (var ignored = ApprenticeCodexServerConfig.useRemoteEyeConfigOverrideForGameTest(
                List.of(currentDimension.toString()),
                true,
                List.of(currentDimension.toString())
        )) {
            helper.assertFalse(
                    ApprenticeCodexServerConfig.isRemoteEyeDimensionAllowed(currentDimension),
                    "RemoteEye dimension denylist should override the allowlist"
            );
        }
        helper.succeed();
    }

    static void remoteEyeSanitizerKeepsStoredLongDuration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "remote_eye_long_duration_repair_test");
            var activeUntilGameTime = level.getGameTime() + 1200L;

            Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, state -> {
                state.activeUntilGameTime = activeUntilGameTime;
                state.activeDurationTicks = 1200L;
                state.anchorX = player.getX();
                state.anchorY = player.getY();
                state.anchorZ = player.getZ();
                state.anchorYaw = player.getYRot();
                state.anchorPitch = player.getXRot();
            }));

            RemoteEyeBodyControlEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var state = getRemoteEyeState(player);
            helper.assertTrue(state.activeUntilGameTime == activeUntilGameTime,
                    "RemoteEye sanitizer should not clamp active state below the stored cast duration");
        });
    }

    static void remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "remote_eye_legacy_duration_repair_test");
            var expectedActiveUntilGameTime = level.getGameTime() + RemoteEye.PERSISTED_STATE_REPAIR_MAX_ACTIVE_TICKS;

            Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, state -> {
                state.activeUntilGameTime = expectedActiveUntilGameTime + 1200L;
                state.activeDurationTicks = 0L;
                state.anchorX = player.getX();
                state.anchorY = player.getY();
                state.anchorZ = player.getZ();
                state.anchorYaw = player.getYRot();
                state.anchorPitch = player.getXRot();
            }));

            RemoteEyeBodyControlEvent.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var state = getRemoteEyeState(player);
            helper.assertTrue(state.activeUntilGameTime == expectedActiveUntilGameTime,
                    "RemoteEye legacy state should still use the 30 second persisted repair fallback");
        });
    }
}
