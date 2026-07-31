package jp.aquafactory.apprenticecodex.network;

import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientAnchorBlinkPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientChangeArchivistsGrimoireRowPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmElementalBowModePacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmLuminousDeviceSelectionPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmSneakSelectionPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientEpicFightAttackcastRingTargetsPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientFocusStaffbowCancelPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientJumpcastCharmCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMirageAvoidanceCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientManaThrusterInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellgunCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.HeavenlyFistPulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.HealingBloomPulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowOverheatPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEquipmentSpellTimingConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncAutocastAmuletNotificationPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncAutocastAmuletProfileSpellIdsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncApprenticeDeskConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncBoundBowStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncBoundSwordStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncCircuitHeatStaffOverheatPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncCircuitHeatStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncIronSwingcastStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEdgeDancerStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowCastStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncChargecastCatalystbookConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowLoanPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowPresentationPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncIsekaiTravelGuidebookConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemainingCountNotificationPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncLuminousDeviceConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMageLightConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaForceBladeConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaShieldCharmConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaThrusterActivePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaThrusterConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMirageAvoidanceStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMultipurposeStaffrifleFireEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncPhotonSiphonCombatStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncReflectcastShieldEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncScarletThirstHealthPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncSchoolAffinityAssignmentsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncSatelliteFollowcastAmuletStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncSmashcastScepterReadyStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncTamersPocketCountPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncZenithStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.TerraResonanceHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.TerraResonancePulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.TotemOfPermafrostPulsePacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Networks {
    private static final String PROTOCOL_VERSION = "71";

    private Networks() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networks::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                ClientBlockTargetCastPacket.TYPE,
                ClientBlockTargetCastPacket.STREAM_CODEC,
                ClientBlockTargetCastPacket::handle
        );
        registrar.playToServer(
                ClientFocusStaffbowCancelPacket.TYPE,
                ClientFocusStaffbowCancelPacket.STREAM_CODEC,
                ClientFocusStaffbowCancelPacket::handle
        );
        registrar.playToClient(
                SyncFocusStaffbowConfigPacket.TYPE,
                SyncFocusStaffbowConfigPacket.STREAM_CODEC,
                SyncFocusStaffbowConfigPacket::handle
        );
        registrar.playToClient(
                SyncElementalBowConfigPacket.TYPE,
                SyncElementalBowConfigPacket.STREAM_CODEC,
                SyncElementalBowConfigPacket::handle
        );
        registrar.playToClient(
                SyncChargecastCatalystbookConfigPacket.TYPE,
                SyncChargecastCatalystbookConfigPacket.STREAM_CODEC,
                SyncChargecastCatalystbookConfigPacket::handle
        );
        registrar.playToServer(
                ClientConfirmElementalBowModePacket.TYPE,
                ClientConfirmElementalBowModePacket.STREAM_CODEC,
                ClientConfirmElementalBowModePacket::handle
        );
        registrar.playToServer(
                ClientConfirmLuminousDeviceSelectionPacket.TYPE,
                ClientConfirmLuminousDeviceSelectionPacket.STREAM_CODEC,
                ClientConfirmLuminousDeviceSelectionPacket::handle
        );
        registrar.playToServer(
                ClientConfirmSneakSelectionPacket.TYPE,
                ClientConfirmSneakSelectionPacket.STREAM_CODEC,
                ClientConfirmSneakSelectionPacket::handle
        );
        registrar.playToServer(
                ClientChangeArchivistsGrimoireRowPacket.TYPE,
                ClientChangeArchivistsGrimoireRowPacket.STREAM_CODEC,
                ClientChangeArchivistsGrimoireRowPacket::handle
        );
        registrar.playToServer(
                ClientSwingMagicAttackPacket.TYPE,
                ClientSwingMagicAttackPacket.STREAM_CODEC,
                ClientSwingMagicAttackPacket::handle
        );
        registrar.playToServer(
                ClientEpicFightAttackcastRingTargetsPacket.TYPE,
                ClientEpicFightAttackcastRingTargetsPacket.STREAM_CODEC,
                ClientEpicFightAttackcastRingTargetsPacket::handle
        );
        registrar.playToServer(
                ClientMultipurposeStaffrifleCastPacket.TYPE,
                ClientMultipurposeStaffrifleCastPacket.STREAM_CODEC,
                ClientMultipurposeStaffrifleCastPacket::handle
        );
        registrar.playToServer(
                ClientSpellgunCastPacket.TYPE,
                ClientSpellgunCastPacket.STREAM_CODEC,
                ClientSpellgunCastPacket::handle
        );
        registrar.playToServer(
                ClientManaThrusterInputPacket.TYPE,
                ClientManaThrusterInputPacket.STREAM_CODEC,
                ClientManaThrusterInputPacket::handle
        );
        registrar.playToServer(
                ClientJumpcastCharmCastPacket.TYPE,
                ClientJumpcastCharmCastPacket.STREAM_CODEC,
                ClientJumpcastCharmCastPacket::handle
        );
        registrar.playToServer(
                ClientAnchorBlinkPacket.TYPE,
                ClientAnchorBlinkPacket.STREAM_CODEC,
                ClientAnchorBlinkPacket::handle
        );
        registrar.playToServer(
                ClientMirageAvoidanceCastPacket.TYPE,
                ClientMirageAvoidanceCastPacket.STREAM_CODEC,
                ClientMirageAvoidanceCastPacket::handle
        );
        registrar.playToClient(
                SyncEnderGrimoireSpellbookPacket.TYPE,
                SyncEnderGrimoireSpellbookPacket.STREAM_CODEC,
                SyncEnderGrimoireSpellbookPacket::handle
        );
        registrar.playToClient(
                SyncIsekaiTravelGuidebookConfigPacket.TYPE,
                SyncIsekaiTravelGuidebookConfigPacket.STREAM_CODEC,
                SyncIsekaiTravelGuidebookConfigPacket::handle
        );
        registrar.playToClient(
                SyncApprenticeDeskConfigPacket.TYPE,
                SyncApprenticeDeskConfigPacket.STREAM_CODEC,
                SyncApprenticeDeskConfigPacket::handle
        );
        registrar.playToClient(
                SyncAssistWingsJumpPacket.TYPE,
                SyncAssistWingsJumpPacket.STREAM_CODEC,
                SyncAssistWingsJumpPacket::handle
        );
        registrar.playToClient(
                SyncManaForceBladeConfigPacket.TYPE,
                SyncManaForceBladeConfigPacket.STREAM_CODEC,
                SyncManaForceBladeConfigPacket::handle
        );
        registrar.playToClient(
                SyncManaShieldCharmConfigPacket.TYPE,
                SyncManaShieldCharmConfigPacket.STREAM_CODEC,
                SyncManaShieldCharmConfigPacket::handle
        );
        registrar.playToClient(
                SyncManaThrusterConfigPacket.TYPE,
                SyncManaThrusterConfigPacket.STREAM_CODEC,
                SyncManaThrusterConfigPacket::handle
        );
        registrar.playToClient(
                SyncManaThrusterActivePacket.TYPE,
                SyncManaThrusterActivePacket.STREAM_CODEC,
                SyncManaThrusterActivePacket::handle
        );
        registrar.playToClient(
                SyncZenithStaffConfigPacket.TYPE,
                SyncZenithStaffConfigPacket.STREAM_CODEC,
                SyncZenithStaffConfigPacket::handle
        );
        registrar.playToClient(
                SyncEquipmentSpellTimingConfigPacket.TYPE,
                SyncEquipmentSpellTimingConfigPacket.STREAM_CODEC,
                SyncEquipmentSpellTimingConfigPacket::handle
        );
        registrar.playToClient(
                SyncLuminousDeviceConfigPacket.TYPE,
                SyncLuminousDeviceConfigPacket.STREAM_CODEC,
                SyncLuminousDeviceConfigPacket::handle
        );
        registrar.playToClient(
                SyncMageLightConfigPacket.TYPE,
                SyncMageLightConfigPacket.STREAM_CODEC,
                SyncMageLightConfigPacket::handle
        );
        registrar.playToClient(
                SyncScarletThirstHealthPacket.TYPE,
                SyncScarletThirstHealthPacket.STREAM_CODEC,
                SyncScarletThirstHealthPacket::handle
        );
        registrar.playToClient(
                SyncSchoolAffinityAssignmentsPacket.TYPE,
                SyncSchoolAffinityAssignmentsPacket.STREAM_CODEC,
                SyncSchoolAffinityAssignmentsPacket::handle
        );
        registrar.playToClient(
                ForceFieldDefenseEffectPacket.TYPE,
                ForceFieldDefenseEffectPacket.STREAM_CODEC,
                ForceFieldDefenseEffectPacket::handle
        );
        registrar.playToClient(
                HealingBloomPulsePacket.TYPE,
                HealingBloomPulsePacket.STREAM_CODEC,
                HealingBloomPulsePacket::handle
        );
        registrar.playToClient(
                HeavenlyFistPulsePacket.TYPE,
                HeavenlyFistPulsePacket.STREAM_CODEC,
                HeavenlyFistPulsePacket::handle
        );
        registrar.playToClient(
                TotemOfPermafrostPulsePacket.TYPE,
                TotemOfPermafrostPulsePacket.STREAM_CODEC,
                TotemOfPermafrostPulsePacket::handle
        );
        registrar.playToClient(
                ManaSiphonOrbEffectPacket.TYPE,
                ManaSiphonOrbEffectPacket.STREAM_CODEC,
                ManaSiphonOrbEffectPacket::handle
        );
        registrar.playToClient(
                SyncRemoteEyeStatePacket.TYPE,
                SyncRemoteEyeStatePacket.STREAM_CODEC,
                SyncRemoteEyeStatePacket::handle
        );
        registrar.playToClient(
                SyncMirageAvoidanceStatePacket.TYPE,
                SyncMirageAvoidanceStatePacket.STREAM_CODEC,
                SyncMirageAvoidanceStatePacket::handle
        );
        registrar.playToClient(
                SyncElementalBowOverheatPacket.TYPE,
                SyncElementalBowOverheatPacket.STREAM_CODEC,
                SyncElementalBowOverheatPacket::handle
        );
        registrar.playToClient(
                SyncCircuitHeatStaffOverheatPacket.TYPE,
                SyncCircuitHeatStaffOverheatPacket.STREAM_CODEC,
                SyncCircuitHeatStaffOverheatPacket::handle
        );
        registrar.playToClient(
                SyncCircuitHeatStaffConfigPacket.TYPE,
                SyncCircuitHeatStaffConfigPacket.STREAM_CODEC,
                SyncCircuitHeatStaffConfigPacket::handle
        );
        registrar.playToClient(
                SyncIronSwingcastStaffConfigPacket.TYPE,
                SyncIronSwingcastStaffConfigPacket.STREAM_CODEC,
                SyncIronSwingcastStaffConfigPacket::handle
        );
        registrar.playToClient(
                SyncAutocastAmuletNotificationPacket.TYPE,
                SyncAutocastAmuletNotificationPacket.STREAM_CODEC,
                SyncAutocastAmuletNotificationPacket::handle
        );
        registrar.playToClient(
                SyncAutocastAmuletProfileSpellIdsPacket.TYPE,
                SyncAutocastAmuletProfileSpellIdsPacket.STREAM_CODEC,
                SyncAutocastAmuletProfileSpellIdsPacket::handle
        );
        registrar.playToClient(
                SyncRemainingCountNotificationPacket.TYPE,
                SyncRemainingCountNotificationPacket.STREAM_CODEC,
                SyncRemainingCountNotificationPacket::handle
        );
        registrar.playToClient(
                SyncFocusStaffbowCastStatePacket.TYPE,
                SyncFocusStaffbowCastStatePacket.STREAM_CODEC,
                SyncFocusStaffbowCastStatePacket::handle
        );
        registrar.playToClient(
                SyncFocusStaffbowLoanPacket.TYPE,
                SyncFocusStaffbowLoanPacket.STREAM_CODEC,
                SyncFocusStaffbowLoanPacket::handle
        );
        registrar.playToClient(
                SyncFocusStaffbowPresentationPacket.TYPE,
                SyncFocusStaffbowPresentationPacket.STREAM_CODEC,
                SyncFocusStaffbowPresentationPacket::handle
        );
        registrar.playToClient(
                SyncBoundBowStatePacket.TYPE,
                SyncBoundBowStatePacket.STREAM_CODEC,
                SyncBoundBowStatePacket::handle
        );
        registrar.playToClient(
                SyncBoundSwordStatePacket.TYPE,
                SyncBoundSwordStatePacket.STREAM_CODEC,
                SyncBoundSwordStatePacket::handle
        );
        registrar.playToClient(
                SyncEdgeDancerStatePacket.TYPE,
                SyncEdgeDancerStatePacket.STREAM_CODEC,
                SyncEdgeDancerStatePacket::handle
        );
        registrar.playToClient(
                SyncTamersPocketCountPacket.TYPE,
                SyncTamersPocketCountPacket.STREAM_CODEC,
                SyncTamersPocketCountPacket::handle
        );
        registrar.playToClient(
                SyncPhotonSiphonCombatStatePacket.TYPE,
                SyncPhotonSiphonCombatStatePacket.STREAM_CODEC,
                SyncPhotonSiphonCombatStatePacket::handle
        );
        registrar.playToClient(
                SyncMultipurposeStaffrifleFireEffectPacket.TYPE,
                SyncMultipurposeStaffrifleFireEffectPacket.STREAM_CODEC,
                SyncMultipurposeStaffrifleFireEffectPacket::handle
        );
        registrar.playToClient(
                SyncSmashcastScepterReadyStatePacket.TYPE,
                SyncSmashcastScepterReadyStatePacket.STREAM_CODEC,
                SyncSmashcastScepterReadyStatePacket::handle
        );
        registrar.playToClient(
                SyncSatelliteFollowcastAmuletStatePacket.TYPE,
                SyncSatelliteFollowcastAmuletStatePacket.STREAM_CODEC,
                SyncSatelliteFollowcastAmuletStatePacket::handle
        );
        registrar.playToClient(
                SenseEvilHighlightsPacket.TYPE,
                SenseEvilHighlightsPacket.STREAM_CODEC,
                SenseEvilHighlightsPacket::handle
        );
        registrar.playToClient(
                AtelierStationFluidEffectPacket.TYPE,
                AtelierStationFluidEffectPacket.STREAM_CODEC,
                AtelierStationFluidEffectPacket::handle
        );
        registrar.playToClient(
                SyncReflectcastShieldEffectPacket.TYPE,
                SyncReflectcastShieldEffectPacket.STREAM_CODEC,
                SyncReflectcastShieldEffectPacket::handle
        );
        registrar.playToClient(
                TerraResonancePulsePacket.TYPE,
                TerraResonancePulsePacket.STREAM_CODEC,
                TerraResonancePulsePacket::handle
        );
        registrar.playToClient(
                TerraResonanceHighlightsPacket.TYPE,
                TerraResonanceHighlightsPacket.STREAM_CODEC,
                TerraResonanceHighlightsPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer serverPlayer, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(serverPlayer, packet);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, CustomPacketPayload packet) {
        var radiusSqr = radius * radius;
        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                sendToPlayer(player, packet);
            }
        }
    }
}
