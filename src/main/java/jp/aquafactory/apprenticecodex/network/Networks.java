package jp.aquafactory.apprenticecodex.network;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.AlchemyBrewerWaterSupplyEffectPacket;
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
import jp.aquafactory.apprenticecodex.network.packet.ClientOpenSpellcasterAccessoryCasePacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientManaThrusterInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBroomInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBroomDismountInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMultipurposeStaffrifleCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellgunCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import jp.aquafactory.apprenticecodex.network.packet.DeepSensorObservationsPacket;
import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.GunSpellTracerPacket;
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
import jp.aquafactory.apprenticecodex.network.packet.SyncBoundBowStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncBoundSwordStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncCircuitHeatStaffOverheatPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncCircuitHeatStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncHighTierSwingcastStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncIronSwingcastStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncSoulstainedSteelSwingcastStaffConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncSpellgunConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEdgeDancerStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowCastStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncChargecastCatalystbookConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowLoanPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowPresentationPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncIsekaiTravelGuidebookConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncInstantSearchBrazierConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemainingCountNotificationPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncLuminousDeviceConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncLinearBuildConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMageLightConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaForceBladeConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaShieldCharmConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaThrusterActivePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaThrusterConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFloatmountBroomConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomReleaseResultPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomImpulseEffectPacket;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class Networks {
    private static final String PROTOCOL_VERSION = "89";
    private static int nextPacketId = 0;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private Networks() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientBlockTargetCastPacket.class,
                ClientBlockTargetCastPacket::encode,
                ClientBlockTargetCastPacket::decode,
                ClientBlockTargetCastPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientFocusStaffbowCancelPacket.class,
                ClientFocusStaffbowCancelPacket::encode,
                ClientFocusStaffbowCancelPacket::decode,
                ClientFocusStaffbowCancelPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncFocusStaffbowCastStatePacket.class,
                SyncFocusStaffbowCastStatePacket::encode,
                SyncFocusStaffbowCastStatePacket::decode,
                SyncFocusStaffbowCastStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncFocusStaffbowLoanPacket.class,
                SyncFocusStaffbowLoanPacket::encode,
                SyncFocusStaffbowLoanPacket::decode,
                SyncFocusStaffbowLoanPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncFocusStaffbowPresentationPacket.class,
                SyncFocusStaffbowPresentationPacket::encode,
                SyncFocusStaffbowPresentationPacket::decode,
                SyncFocusStaffbowPresentationPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncBoundBowStatePacket.class,
                SyncBoundBowStatePacket::encode,
                SyncBoundBowStatePacket::decode,
                SyncBoundBowStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncBoundSwordStatePacket.class,
                SyncBoundSwordStatePacket::encode,
                SyncBoundSwordStatePacket::decode,
                SyncBoundSwordStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncEdgeDancerStatePacket.class,
                SyncEdgeDancerStatePacket::encode,
                SyncEdgeDancerStatePacket::decode,
                SyncEdgeDancerStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncFocusStaffbowConfigPacket.class,
                SyncFocusStaffbowConfigPacket::encode,
                SyncFocusStaffbowConfigPacket::decode,
                SyncFocusStaffbowConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncElementalBowConfigPacket.class,
                SyncElementalBowConfigPacket::encode,
                SyncElementalBowConfigPacket::decode,
                SyncElementalBowConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncChargecastCatalystbookConfigPacket.class,
                SyncChargecastCatalystbookConfigPacket::encode,
                SyncChargecastCatalystbookConfigPacket::decode,
                SyncChargecastCatalystbookConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientConfirmElementalBowModePacket.class,
                ClientConfirmElementalBowModePacket::encode,
                ClientConfirmElementalBowModePacket::decode,
                ClientConfirmElementalBowModePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientConfirmLuminousDeviceSelectionPacket.class,
                ClientConfirmLuminousDeviceSelectionPacket::encode,
                ClientConfirmLuminousDeviceSelectionPacket::decode,
                ClientConfirmLuminousDeviceSelectionPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientConfirmSneakSelectionPacket.class,
                ClientConfirmSneakSelectionPacket::encode,
                ClientConfirmSneakSelectionPacket::decode,
                ClientConfirmSneakSelectionPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientChangeArchivistsGrimoireRowPacket.class,
                ClientChangeArchivistsGrimoireRowPacket::encode,
                ClientChangeArchivistsGrimoireRowPacket::decode,
                ClientChangeArchivistsGrimoireRowPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientOpenSpellcasterAccessoryCasePacket.class,
                ClientOpenSpellcasterAccessoryCasePacket::encode,
                ClientOpenSpellcasterAccessoryCasePacket::decode,
                ClientOpenSpellcasterAccessoryCasePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientSwingMagicAttackPacket.class,
                ClientSwingMagicAttackPacket::encode,
                ClientSwingMagicAttackPacket::decode,
                ClientSwingMagicAttackPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientEpicFightAttackcastRingTargetsPacket.class,
                ClientEpicFightAttackcastRingTargetsPacket::encode,
                ClientEpicFightAttackcastRingTargetsPacket::decode,
                ClientEpicFightAttackcastRingTargetsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientMultipurposeStaffrifleCastPacket.class,
                ClientMultipurposeStaffrifleCastPacket::encode,
                ClientMultipurposeStaffrifleCastPacket::decode,
                ClientMultipurposeStaffrifleCastPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientSpellgunCastPacket.class,
                ClientSpellgunCastPacket::encode,
                ClientSpellgunCastPacket::decode,
                ClientSpellgunCastPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientManaThrusterInputPacket.class,
                ClientManaThrusterInputPacket::encode,
                ClientManaThrusterInputPacket::decode,
                ClientManaThrusterInputPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientBroomInputPacket.class,
                ClientBroomInputPacket::encode,
                ClientBroomInputPacket::decode,
                ClientBroomInputPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                HoverrideBroomReleaseResultPacket.class,
                HoverrideBroomReleaseResultPacket::encode,
                HoverrideBroomReleaseResultPacket::decode,
                HoverrideBroomReleaseResultPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                HoverrideBroomAssistWingsJumpPacket.class,
                HoverrideBroomAssistWingsJumpPacket::encode,
                HoverrideBroomAssistWingsJumpPacket::decode,
                HoverrideBroomAssistWingsJumpPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                HoverrideBroomImpulseEffectPacket.class,
                HoverrideBroomImpulseEffectPacket::encode,
                HoverrideBroomImpulseEffectPacket::decode,
                HoverrideBroomImpulseEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientBroomDismountInputPacket.class,
                ClientBroomDismountInputPacket::encode,
                ClientBroomDismountInputPacket::decode,
                ClientBroomDismountInputPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientJumpcastCharmCastPacket.class,
                ClientJumpcastCharmCastPacket::encode,
                ClientJumpcastCharmCastPacket::decode,
                ClientJumpcastCharmCastPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientAnchorBlinkPacket.class,
                ClientAnchorBlinkPacket::encode,
                ClientAnchorBlinkPacket::decode,
                ClientAnchorBlinkPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ClientMirageAvoidanceCastPacket.class,
                ClientMirageAvoidanceCastPacket::encode,
                ClientMirageAvoidanceCastPacket::decode,
                ClientMirageAvoidanceCastPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncEnderGrimoireSpellbookPacket.class,
                SyncEnderGrimoireSpellbookPacket::encode,
                SyncEnderGrimoireSpellbookPacket::decode,
                SyncEnderGrimoireSpellbookPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncIsekaiTravelGuidebookConfigPacket.class,
                SyncIsekaiTravelGuidebookConfigPacket::encode,
                SyncIsekaiTravelGuidebookConfigPacket::decode,
                SyncIsekaiTravelGuidebookConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncInstantSearchBrazierConfigPacket.class,
                SyncInstantSearchBrazierConfigPacket::encode,
                SyncInstantSearchBrazierConfigPacket::decode,
                SyncInstantSearchBrazierConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncApprenticeDeskConfigPacket.class,
                SyncApprenticeDeskConfigPacket::encode,
                SyncApprenticeDeskConfigPacket::decode,
                SyncApprenticeDeskConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncManaForceBladeConfigPacket.class,
                SyncManaForceBladeConfigPacket::encode,
                SyncManaForceBladeConfigPacket::decode,
                SyncManaForceBladeConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncManaShieldCharmConfigPacket.class,
                SyncManaShieldCharmConfigPacket::encode,
                SyncManaShieldCharmConfigPacket::decode,
                SyncManaShieldCharmConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncManaThrusterConfigPacket.class,
                SyncManaThrusterConfigPacket::encode,
                SyncManaThrusterConfigPacket::decode,
                SyncManaThrusterConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncFloatmountBroomConfigPacket.class,
                SyncFloatmountBroomConfigPacket::encode,
                SyncFloatmountBroomConfigPacket::decode,
                SyncFloatmountBroomConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncManaThrusterActivePacket.class,
                SyncManaThrusterActivePacket::encode,
                SyncManaThrusterActivePacket::decode,
                SyncManaThrusterActivePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncZenithStaffConfigPacket.class,
                SyncZenithStaffConfigPacket::encode,
                SyncZenithStaffConfigPacket::decode,
                SyncZenithStaffConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncEquipmentSpellTimingConfigPacket.class,
                SyncEquipmentSpellTimingConfigPacket::encode,
                SyncEquipmentSpellTimingConfigPacket::decode,
                SyncEquipmentSpellTimingConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncLuminousDeviceConfigPacket.class,
                SyncLuminousDeviceConfigPacket::encode,
                SyncLuminousDeviceConfigPacket::decode,
                SyncLuminousDeviceConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncMageLightConfigPacket.class,
                SyncMageLightConfigPacket::encode,
                SyncMageLightConfigPacket::decode,
                SyncMageLightConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncLinearBuildConfigPacket.class,
                SyncLinearBuildConfigPacket::encode,
                SyncLinearBuildConfigPacket::decode,
                SyncLinearBuildConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncScarletThirstHealthPacket.class,
                SyncScarletThirstHealthPacket::encode,
                SyncScarletThirstHealthPacket::decode,
                SyncScarletThirstHealthPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncSchoolAffinityAssignmentsPacket.class,
                SyncSchoolAffinityAssignmentsPacket::encode,
                SyncSchoolAffinityAssignmentsPacket::decode,
                SyncSchoolAffinityAssignmentsPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ForceFieldDefenseEffectPacket.class,
                ForceFieldDefenseEffectPacket::encode,
                ForceFieldDefenseEffectPacket::decode,
                ForceFieldDefenseEffectPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                HealingBloomPulsePacket.class,
                HealingBloomPulsePacket::encode,
                HealingBloomPulsePacket::decode,
                HealingBloomPulsePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                HeavenlyFistPulsePacket.class,
                HeavenlyFistPulsePacket::encode,
                HeavenlyFistPulsePacket::decode,
                HeavenlyFistPulsePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                TotemOfPermafrostPulsePacket.class,
                TotemOfPermafrostPulsePacket::encode,
                TotemOfPermafrostPulsePacket::decode,
                TotemOfPermafrostPulsePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ManaSiphonOrbEffectPacket.class,
                ManaSiphonOrbEffectPacket::encode,
                ManaSiphonOrbEffectPacket::decode,
                ManaSiphonOrbEffectPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncRemoteEyeStatePacket.class,
                SyncRemoteEyeStatePacket::encode,
                SyncRemoteEyeStatePacket::decode,
                SyncRemoteEyeStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncMirageAvoidanceStatePacket.class,
                SyncMirageAvoidanceStatePacket::encode,
                SyncMirageAvoidanceStatePacket::decode,
                SyncMirageAvoidanceStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncElementalBowOverheatPacket.class,
                SyncElementalBowOverheatPacket::encode,
                SyncElementalBowOverheatPacket::decode,
                SyncElementalBowOverheatPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncCircuitHeatStaffOverheatPacket.class,
                SyncCircuitHeatStaffOverheatPacket::encode,
                SyncCircuitHeatStaffOverheatPacket::decode,
                SyncCircuitHeatStaffOverheatPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncCircuitHeatStaffConfigPacket.class,
                SyncCircuitHeatStaffConfigPacket::encode,
                SyncCircuitHeatStaffConfigPacket::decode,
                SyncCircuitHeatStaffConfigPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncIronSwingcastStaffConfigPacket.class,
                SyncIronSwingcastStaffConfigPacket::encode,
                SyncIronSwingcastStaffConfigPacket::decode,
                SyncIronSwingcastStaffConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncHighTierSwingcastStaffConfigPacket.class,
                SyncHighTierSwingcastStaffConfigPacket::encode,
                SyncHighTierSwingcastStaffConfigPacket::decode,
                SyncHighTierSwingcastStaffConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncSoulstainedSteelSwingcastStaffConfigPacket.class,
                SyncSoulstainedSteelSwingcastStaffConfigPacket::encode,
                SyncSoulstainedSteelSwingcastStaffConfigPacket::decode,
                SyncSoulstainedSteelSwingcastStaffConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncSpellgunConfigPacket.class,
                SyncSpellgunConfigPacket::encode,
                SyncSpellgunConfigPacket::decode,
                SyncSpellgunConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncAutocastAmuletNotificationPacket.class,
                SyncAutocastAmuletNotificationPacket::encode,
                SyncAutocastAmuletNotificationPacket::decode,
                SyncAutocastAmuletNotificationPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncAutocastAmuletProfileSpellIdsPacket.class,
                SyncAutocastAmuletProfileSpellIdsPacket::encode,
                SyncAutocastAmuletProfileSpellIdsPacket::decode,
                SyncAutocastAmuletProfileSpellIdsPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncRemainingCountNotificationPacket.class,
                SyncRemainingCountNotificationPacket::encode,
                SyncRemainingCountNotificationPacket::decode,
                SyncRemainingCountNotificationPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncTamersPocketCountPacket.class,
                SyncTamersPocketCountPacket::encode,
                SyncTamersPocketCountPacket::decode,
                SyncTamersPocketCountPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncPhotonSiphonCombatStatePacket.class,
                SyncPhotonSiphonCombatStatePacket::encode,
                SyncPhotonSiphonCombatStatePacket::decode,
                SyncPhotonSiphonCombatStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncSmashcastScepterReadyStatePacket.class,
                SyncSmashcastScepterReadyStatePacket::encode,
                SyncSmashcastScepterReadyStatePacket::decode,
                SyncSmashcastScepterReadyStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncMultipurposeStaffrifleFireEffectPacket.class,
                SyncMultipurposeStaffrifleFireEffectPacket::encode,
                SyncMultipurposeStaffrifleFireEffectPacket::decode,
                SyncMultipurposeStaffrifleFireEffectPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncSatelliteFollowcastAmuletStatePacket.class,
                SyncSatelliteFollowcastAmuletStatePacket::encode,
                SyncSatelliteFollowcastAmuletStatePacket::decode,
                SyncSatelliteFollowcastAmuletStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SenseEvilHighlightsPacket.class,
                SenseEvilHighlightsPacket::encode,
                SenseEvilHighlightsPacket::decode,
                SenseEvilHighlightsPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                AtelierStationFluidEffectPacket.class,
                AtelierStationFluidEffectPacket::encode,
                AtelierStationFluidEffectPacket::decode,
                AtelierStationFluidEffectPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                AlchemyBrewerWaterSupplyEffectPacket.class,
                AlchemyBrewerWaterSupplyEffectPacket::encode,
                AlchemyBrewerWaterSupplyEffectPacket::decode,
                AlchemyBrewerWaterSupplyEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncReflectcastShieldEffectPacket.class,
                SyncReflectcastShieldEffectPacket::encode,
                SyncReflectcastShieldEffectPacket::decode,
                SyncReflectcastShieldEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                TerraResonancePulsePacket.class,
                TerraResonancePulsePacket::encode,
                TerraResonancePulsePacket::decode,
                TerraResonancePulsePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                TerraResonanceHighlightsPacket.class,
                TerraResonanceHighlightsPacket::encode,
                TerraResonanceHighlightsPacket::decode,
                TerraResonanceHighlightsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                DeepSensorObservationsPacket.class,
                DeepSensorObservationsPacket::encode,
                DeepSensorObservationsPacket::decode,
                DeepSensorObservationsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                GunSpellTracerPacket.class,
                GunSpellTracerPacket::encode,
                GunSpellTracerPacket::decode,
                GunSpellTracerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendToPlayer(ServerPlayer serverPlayer, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, Object packet) {
        var radiusSqr = radius * radius;
        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                sendToPlayer(player, packet);
            }
        }
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
