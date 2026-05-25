package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SatelliteFollowcastAmuletCastEvent {
    private static final CastSource FOLLOWCAST_SOURCE = CastSource.SWORD;
    private static final Map<UUID, PendingFollowcastCooldown> PENDING_FOLLOWCAST_COOLDOWNS = new HashMap<>();

    private SatelliteFollowcastAmuletCastEvent() {
    }

    private enum CastAttemptResult {
        NONE,
        CASTED,
        BLOCKED
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var equippedAmulets = getEquippedAmulets(player);
        for (var slotResult : equippedAmulets) {
            var stack = slotResult.stack();
            if (!(stack.getItem() instanceof SatelliteFollowcastAmulet amulet)) {
                continue;
            }

            amulet.initializeSpellContainer(stack);
            amulet.normalizeImbuedSpellContainer(stack);

            var result = tryFollowcast(level, player, magicData, slotResult, amulet);
            if (result == CastAttemptResult.NONE) {
                continue;
            }
            if (result == CastAttemptResult.CASTED) {
                cancelOriginalCastIfManaBecameInsufficient(event, player, magicData);
            }
            return;
        }
    }

    private static CastAttemptResult tryFollowcast(
            ServerLevel level,
            ServerPlayer player,
            MagicData ownerMagicData,
            SlotResult slotResult,
            SatelliteFollowcastAmulet amulet
    ) {
        var stack = slotResult.stack();
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return CastAttemptResult.NONE;
        }

        var maxSpellSlots = SatelliteFollowcastAmulet.clampSpellSlotCount(spellContainer.getMaxSpellCount());
        var startIndex = SatelliteFollowcastAmulet.advanceAndGetSearchStartIndex(stack, maxSpellSlots);
        for (var offset = 0; offset < maxSpellSlots; ++offset) {
            var slotIndex = (startIndex + offset) % maxSpellSlots;
            var spellData = spellContainer.getSpellAtIndex(slotIndex);
            if (spellData == SpellData.EMPTY || !amulet.canImbueSpell(spellData)) {
                continue;
            }

            var spell = spellData.getSpell();
            if (ownerMagicData.getPlayerCooldowns().isOnCooldown(spell)) {
                continue;
            }

            return tryCastSelectedSpell(level, player, ownerMagicData, slotResult, spellData, slotIndex, maxSpellSlots);
        }

        return CastAttemptResult.NONE;
    }

    private static CastAttemptResult tryCastSelectedSpell(
            ServerLevel level,
            ServerPlayer player,
            MagicData ownerMagicData,
            SlotResult slotResult,
            SpellData spellData,
            int slotIndex,
            int maxSpellSlots
    ) {
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        if (spell.requiresLearning() && !spell.isLearned(player)) {
            return CastAttemptResult.BLOCKED;
        }
        if (!spell.canBeCastedBy(spellLevel, FOLLOWCAST_SOURCE, ownerMagicData, player).isSuccess()) {
            return CastAttemptResult.BLOCKED;
        }

        var castingSlot = "satellite_followcast_amulet_" + slotResult.slotContext().identifier()
                + "_" + slotResult.slotContext().index()
                + "_" + slotIndex;
        var crystalPosition = SatelliteFollowcastAmulet.getCrystalPosition(player, slotIndex, maxSpellSlots, 0.0F);
        var forward = player.getLookAngle();
        var manaAccess = new PlayerManaAccess(player);

        if (ApprenticeCodexServerConfig.satelliteFollowcastUsesRemoteOwnerProfiles()) {
            var remoteProfile = RemoteOwnerCastProfileManager.getUsableProfile(
                    spell,
                    RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST
            );
            if (remoteProfile.isPresent()) {
                var result = RemoteOwnerCastRunner.tryCast(
                        level,
                        player,
                        slotResult.stack(),
                        spellData,
                        remoteProfile.get(),
                        RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST,
                        crystalPosition,
                        forward,
                        FOLLOWCAST_SOURCE,
                        castingSlot,
                        false
                );
                if (result.handled()) {
                    if (!result.succeeded()) {
                        return CastAttemptResult.BLOCKED;
                    }
                    addFollowcastCooldown(player, spell, FOLLOWCAST_SOURCE, slotResult.stack());
                    return CastAttemptResult.CASTED;
                }
            }
        }

        if (SpellDispenserSpellProfileManager.getProfile(spell).isEmpty()
                || !tryCastWithSpellDispenserProfile(level, player, slotResult.stack(), spellData, crystalPosition, forward, manaAccess, castingSlot)) {
            return CastAttemptResult.BLOCKED;
        }

        addFollowcastCooldown(player, spell, FOLLOWCAST_SOURCE, slotResult.stack());
        return CastAttemptResult.CASTED;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pendingCooldown = PENDING_FOLLOWCAST_COOLDOWNS.get(player.getUUID());
        if (pendingCooldown == null
                || !pendingCooldown.spellId().equals(event.getSpell().getSpellId())
                || pendingCooldown.castSource() != event.getCastSource()) {
            return;
        }

        event.setEffectiveCooldown(WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource(),
                pendingCooldown.castingStack()
        ));
    }

    private static void addFollowcastCooldown(ServerPlayer player, AbstractSpell spell, CastSource castSource, ItemStack castingStack) {
        PENDING_FOLLOWCAST_COOLDOWNS.put(player.getUUID(), new PendingFollowcastCooldown(
                spell.getSpellId(),
                castSource,
                castingStack.copy()
        ));
        try {
            MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, castSource);
        } finally {
            PENDING_FOLLOWCAST_COOLDOWNS.remove(player.getUUID());
        }
    }

    private static boolean tryCastWithSpellDispenserProfile(
            ServerLevel level,
            ServerPlayer player,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 crystalPosition,
            Vec3 forward,
            PlayerManaAccess manaAccess,
            String castingSlot
    ) {
        var validation = new SpellDispenserSpellValidator.ValidationResult(
                sourceStack.copy(),
                spellData,
                SpellDispenserSpellValidator.FailureReason.NONE
        );
        var result = SpellDispenserCastHelper.tryCast(
                level,
                crystalPosition,
                forward,
                validation,
                sourceStack,
                player.getGameProfile(),
                manaAccess,
                FOLLOWCAST_SOURCE,
                castingSlot
        );
        return result.succeeded();
    }

    private static void cancelOriginalCastIfManaBecameInsufficient(
            SpellPreCastEvent event,
            ServerPlayer player,
            MagicData magicData
    ) {
        if (!event.getCastSource().consumesMana() || (player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get())) {
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(event.getSpellId());
        if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
            return;
        }

        var requiredMana = spell.getManaCost(event.getSpellLevel());
        if (requiredMana <= magicData.getMana()) {
            return;
        }

        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.irons_spellbooks.mana_insufficient").withStyle(ChatFormatting.RED)
        ));
        event.setCanceled(true);
    }

    private static List<SlotResult> getEquippedAmulets(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof SatelliteFollowcastAmulet).stream()
                        .sorted(Comparator
                                .comparing((SlotResult slotResult) -> slotResult.slotContext().identifier())
                                .thenComparingInt(slotResult -> slotResult.slotContext().index()))
                        .toList())
                .orElse(List.of());
    }

    private static final class PlayerManaAccess implements SpellDispenserManaHelper.ManaAccess {
        private final ServerPlayer player;

        private PlayerManaAccess(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public int getCurrentMana() {
            return Mth.floor(MagicData.getPlayerMagicData(player).getMana());
        }

        @Override
        public void setCurrentMana(int mana) {
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(Math.max(0.0F, mana));
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }

        @Override
        public int getInventorySlotCount() {
            return 0;
        }

        @Override
        public @NotNull ItemStack getInventoryStack(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setInventoryStack(int slot, @NotNull ItemStack stack) {
        }

        @Override
        public boolean isManaConsumptionExempt() {
            return player.isCreative();
        }
    }

    private record PendingFollowcastCooldown(String spellId, CastSource castSource, ItemStack castingStack) {
    }
}
