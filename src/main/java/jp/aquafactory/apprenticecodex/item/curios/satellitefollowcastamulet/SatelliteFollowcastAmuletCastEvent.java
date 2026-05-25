package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffCastMode;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfile;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileManager;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
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

        var casted = SpellDispenserSpellProfileManager.getProfile(spell).isPresent()
                ? tryCastWithSpellDispenserProfile(level, player, slotResult.stack(), spellData, crystalPosition, forward, manaAccess, castingSlot)
                : tryCastWithChargedTwinBladeStaffProfile(level, player, slotResult.stack(), spellData, crystalPosition, forward, manaAccess, castingSlot);
        if (!casted) {
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

    private static boolean tryCastWithChargedTwinBladeStaffProfile(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 crystalPosition,
            Vec3 forward,
            PlayerManaAccess manaAccess,
            String castingSlot
    ) {
        var profile = ChargedTwinBladeStaffSpellProfileManager.getProfile(spellData.getSpell()).orElse(null);
        if (profile == null || spellData.getSpell().getCastType() == CastType.CONTINUOUS) {
            return false;
        }

        var spellCaster = profile.castMode() == ChargedTwinBladeStaffCastMode.PLAYER_SELF
                ? owner
                : createImpactProxy(level, owner.getGameProfile(), crystalPosition, forward);
        return runOwnerMagicInstantCast(level, owner, spellCaster, sourceStack, spellData, manaAccess, castingSlot, profile);
    }

    private static boolean runOwnerMagicInstantCast(
            ServerLevel level,
            ServerPlayer owner,
            net.minecraft.world.entity.LivingEntity spellCaster,
            ItemStack sourceStack,
            SpellData spellData,
            PlayerManaAccess manaAccess,
            String castingSlot,
            ChargedTwinBladeStaffSpellProfile profile
    ) {
        var spell = spellData.getSpell();
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        if (ownerMagicData == null) {
            return false;
        }
        if (spell.getRecastCount(spellData.getLevel(), owner) > 0 && !profile.allowInitialRecast()) {
            return false;
        }

        var originalMana = ownerMagicData.getMana();
        var restoreManaAfterCast = manaAccess.isManaConsumptionExempt();
        var originalSyncedData = ownerMagicData.getSyncedData();
        try {
            ownerMagicData.setSyncedData(new SyncedSpellData(spellCaster));
            ownerMagicData.initiateCast(spell, spellData.getLevel(), 0, FOLLOWCAST_SOURCE, castingSlot);
            ownerMagicData.setPlayerCastingItem(sourceStack.copy());
            syncOwnerManaForProxyCast(manaAccess, ownerMagicData);
            if (!spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, ownerMagicData)) {
                return false;
            }
            syncOwnerManaForProxyCast(manaAccess, ownerMagicData);
            spell.onServerPreCast(level, spellData.getLevel(), spellCaster, ownerMagicData);

            if (!SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                return false;
            }

            syncOwnerManaForProxyCast(manaAccess, ownerMagicData);
            spell.onCast(level, spellData.getLevel(), spellCaster, FOLLOWCAST_SOURCE, ownerMagicData);
            syncOwnerManaForProxyCast(manaAccess, ownerMagicData);
            spell.onServerCastComplete(level, spellData.getLevel(), spellCaster, ownerMagicData, false);
            return true;
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Satellite Followcast Amulet cast exception: spell={}",
                    spell.getSpellResource(),
                    exception
            );
            return false;
        } finally {
            try {
                ownerMagicData.resetCastingState();
            } finally {
                if (restoreManaAfterCast) {
                    ownerMagicData.setMana(originalMana);
                }
                ownerMagicData.setSyncedData(originalSyncedData);
                originalSyncedData.syncToPlayer(owner);
            }
        }
    }

    private static net.minecraftforge.common.util.FakePlayer createImpactProxy(
            ServerLevel level,
            GameProfile ownerProfile,
            Vec3 crystalPosition,
            Vec3 forward
    ) {
        var proxy = FakePlayerFactory.get(level, new GameProfile(ownerProfile.getId(), ownerProfile.getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        var normalizedForward = forward.lengthSqr() > 1.0E-6D ? forward.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        var yaw = (float) Mth.wrapDegrees(Mth.atan2(-normalizedForward.x, normalizedForward.z) * Mth.RAD_TO_DEG);
        var horizontal = Math.sqrt(normalizedForward.x * normalizedForward.x + normalizedForward.z * normalizedForward.z);
        var pitch = (float) Mth.wrapDegrees(-Mth.atan2(normalizedForward.y, horizontal) * Mth.RAD_TO_DEG);
        var feetY = crystalPosition.y - proxy.getEyeHeight(proxy.getPose());
        proxy.moveTo(crystalPosition.x, feetY, crystalPosition.z, yaw, pitch);
        proxy.setYBodyRot(yaw);
        proxy.setYHeadRot(yaw);
        proxy.yBodyRotO = yaw;
        proxy.yHeadRotO = yaw;
        proxy.setXRot(pitch);
        proxy.xRotO = pitch;
        return proxy;
    }

    private static void syncOwnerManaForProxyCast(PlayerManaAccess manaAccess, MagicData magicData) {
        magicData.setMana(manaAccess.isManaConsumptionExempt()
                ? SpellDispenserManaHelper.MAX_MANA
                : manaAccess.getCurrentMana());
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
