package jp.aquafactory.apprenticecodex.remoteownercast;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellValidator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;

public final class RemoteOwnerCastRunner {
    private RemoteOwnerCastRunner() {
    }

    public static CastResult tryCast(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastProfile profile,
            RemoteOwnerCastOrigin castOrigin,
            Vec3 providedOrigin,
            Vec3 providedForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent
    ) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return CastResult.notHandled();
        }

        var spell = spellData.getSpell();
        if (spell.getCastType() == CastType.CONTINUOUS && profile.castMode() != RemoteOwnerCastMode.LEGACY_SPELL_DISPENSER) {
            return CastResult.notHandled();
        }
        if (spell.getRecastCount(spellData.getLevel(), owner) > 0) {
            if (!profile.allowInitialRecast()) {
                return CastResult.failed();
            }
            var ownerMagicData = MagicData.getPlayerMagicData(owner);
            if (ownerMagicData == null || ownerMagicData.getPlayerRecasts().hasRecastForSpell(spell)) {
                return CastResult.failed();
            }
        }

        var origin = resolveOrigin(owner, profile, providedOrigin);
        var forward = resolveForward(owner, profile, providedForward);
        return switch (profile.castMode()) {
            case LEGACY_SPELL_DISPENSER -> tryLegacySpellDispenserCast(
                    level,
                    owner,
                    sourceStack,
                    spellData,
                    origin,
                    forward,
                    castSource,
                    castingSlot
            );
            case PLAYER_SELF -> tryOwnerMagicCast(
                    level,
                    owner,
                    owner,
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    owner.getEyePosition(),
                    owner.getLookAngle(),
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
            case PROXY_OWNER_MAGIC -> tryOwnerMagicCast(
                    level,
                    owner,
                    createProxy(level, owner, origin, forward),
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    origin,
                    forward,
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
            case REMOTE_PLAYER_GEOMETRY -> tryOwnerMagicCast(
                    level,
                    owner,
                    owner,
                    sourceStack,
                    spellData,
                    profile,
                    castOrigin,
                    origin,
                    forward,
                    castSource,
                    castingSlot,
                    postSpellPreCastEvent
            );
        };
    }

    private static CastResult tryLegacySpellDispenserCast(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            Vec3 origin,
            Vec3 forward,
            CastSource castSource,
            String castingSlot
    ) {
        var validation = new SpellDispenserSpellValidator.ValidationResult(
                sourceStack.copy(),
                spellData,
                SpellDispenserSpellValidator.FailureReason.NONE
        );
        var result = SpellDispenserCastHelper.tryCast(
                level,
                origin,
                forward,
                validation,
                sourceStack,
                owner.getGameProfile(),
                new PlayerManaAccess(owner),
                castSource,
                castingSlot
        );
        return result.succeeded() ? CastResult.success() : CastResult.failed();
    }

    private static CastResult tryOwnerMagicCast(
            ServerLevel level,
            ServerPlayer owner,
            LivingEntity spellCaster,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastProfile profile,
            RemoteOwnerCastOrigin castOrigin,
            Vec3 contextOrigin,
            Vec3 contextForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent
    ) {
        var spell = spellData.getSpell();
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        if (ownerMagicData == null) {
            return CastResult.failed();
        }

        var manaAccess = new PlayerManaAccess(owner);
        if (!canOwnerCastWithManaAccess(owner, spellData, castSource, ownerMagicData, manaAccess)) {
            return CastResult.failed();
        }
        if (postSpellPreCastEvent
                && MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(owner, spell.getSpellId(), spellData.getLevel(), spell.getSchoolType(), castSource))) {
            return CastResult.failed();
        }

        var originalMana = ownerMagicData.getMana();
        var restoreManaAfterCast = manaAccess.isManaConsumptionExempt();
        var originalSyncedData = ownerMagicData.getSyncedData();
        try {
            ownerMagicData.setSyncedData(new SyncedSpellData(spellCaster));
            ownerMagicData.initiateCast(spell, spellData.getLevel(), 0, castSource, castingSlot);
            ownerMagicData.setPlayerCastingItem(sourceStack.copy());
            syncOwnerManaForCast(manaAccess, ownerMagicData);

            var useRemoteGeometry = profile.castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY;
            try (var ignored = useRemoteGeometry
                    ? RemoteOwnerCastContext.push(owner, contextOrigin, contextForward, castOrigin)
                    : null) {
                if (!spell.checkPreCastConditions(level, spellData.getLevel(), spellCaster, ownerMagicData)) {
                    return CastResult.failed();
                }
                syncOwnerManaForCast(manaAccess, ownerMagicData);
                spell.onServerPreCast(level, spellData.getLevel(), spellCaster, ownerMagicData);

                if (!SpellDispenserManaHelper.tryConsumeSpellMana(manaAccess, spellData)) {
                    return CastResult.failed();
                }

                if (spell.getCastType() == CastType.LONG) {
                    spellCaster.tickCount++;
                    syncOwnerManaForCast(manaAccess, ownerMagicData);
                    spell.onServerCastTick(level, spellData.getLevel(), spellCaster, ownerMagicData);
                }

                syncOwnerManaForCast(manaAccess, ownerMagicData);
                spell.onCast(level, spellData.getLevel(), spellCaster, castSource, ownerMagicData);
                syncOwnerManaForCast(manaAccess, ownerMagicData);
                spell.onServerCastComplete(level, spellData.getLevel(), spellCaster, ownerMagicData, false);
            }
            return CastResult.success();
        } catch (RuntimeException exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Remote Owner Cast exception: spell={}, castMode={}, origin={}",
                    spell.getSpellResource(),
                    profile.castMode().getSerializedName(),
                    castOrigin.getSerializedName(),
                    exception
            );
            return CastResult.failed();
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

    private static boolean canOwnerCastWithManaAccess(
            ServerPlayer owner,
            SpellData spellData,
            CastSource castSource,
            MagicData ownerMagicData,
            PlayerManaAccess manaAccess
    ) {
        var originalMana = ownerMagicData.getMana();
        try {
            syncOwnerManaForCast(manaAccess, ownerMagicData);
            return spellData.getSpell().canBeCastedBy(spellData.getLevel(), castSource, ownerMagicData, owner).isSuccess();
        } finally {
            if (manaAccess.isManaConsumptionExempt()) {
                ownerMagicData.setMana(originalMana);
            }
        }
    }

    private static net.minecraftforge.common.util.FakePlayer createProxy(
            ServerLevel level,
            ServerPlayer owner,
            Vec3 eyePosition,
            Vec3 forward
    ) {
        var proxy = FakePlayerFactory.get(level, new GameProfile(owner.getUUID(), owner.getGameProfile().getName()));
        proxy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        proxy.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        proxy.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        var normalizedForward = forward.lengthSqr() > 1.0E-6D ? forward.normalize() : owner.getLookAngle();
        var yaw = (float) Mth.wrapDegrees(Mth.atan2(-normalizedForward.x, normalizedForward.z) * Mth.RAD_TO_DEG);
        var horizontal = Math.sqrt(normalizedForward.x * normalizedForward.x + normalizedForward.z * normalizedForward.z);
        var pitch = (float) Mth.wrapDegrees(-Mth.atan2(normalizedForward.y, horizontal) * Mth.RAD_TO_DEG);
        var feetY = eyePosition.y - proxy.getEyeHeight(proxy.getPose());
        proxy.moveTo(eyePosition.x, feetY, eyePosition.z, yaw, pitch);
        proxy.setYBodyRot(yaw);
        proxy.setYHeadRot(yaw);
        proxy.yBodyRotO = yaw;
        proxy.yHeadRotO = yaw;
        proxy.setXRot(pitch);
        proxy.xRotO = pitch;
        return proxy;
    }

    private static Vec3 resolveOrigin(ServerPlayer owner, RemoteOwnerCastProfile profile, Vec3 providedOrigin) {
        return profile.originMode() == RemoteOwnerOriginMode.PLAYER_SELF ? owner.getEyePosition() : providedOrigin;
    }

    private static Vec3 resolveForward(ServerPlayer owner, RemoteOwnerCastProfile profile, Vec3 providedForward) {
        return profile.directionMode() == RemoteOwnerDirectionMode.PLAYER_LOOK ? owner.getLookAngle() : providedForward;
    }

    private static void syncOwnerManaForCast(PlayerManaAccess manaAccess, MagicData magicData) {
        magicData.setMana(manaAccess.isManaConsumptionExempt()
                ? SpellDispenserManaHelper.MAX_MANA
                : manaAccess.getCurrentMana());
    }

    public record CastResult(boolean handled, boolean succeeded) {
        private static CastResult success() {
            return new CastResult(true, true);
        }

        private static CastResult failed() {
            return new CastResult(true, false);
        }

        private static CastResult notHandled() {
            return new CastResult(false, false);
        }
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
}
